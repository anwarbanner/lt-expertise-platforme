import MockAdapter from "axios-mock-adapter";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api, definirJetonAcces, jetonAccesCourant, messageErreur, surExpirationSession } from "./client";

const SESSION = (jeton: string) => ({
  jetonAcces: jeton,
  expireDans: 600,
  utilisateur: { identifiant: "gestionnaire", nom: "Benali", prenom: "Samira", role: "GESTIONNAIRE" },
});

let serveur: MockAdapter;

beforeEach(() => {
  serveur = new MockAdapter(api);
  definirJetonAcces(null);
  surExpirationSession(() => {});
});

afterEach(() => serveur.restore());

describe("client API", () => {
  it("joint le jeton d'accès aux appels, sauf à ceux d'authentification", async () => {
    definirJetonAcces("jeton-1");
    serveur.onGet("/imports").reply(200, {});
    serveur.onPost("/auth/connexion").reply(200, SESSION("x"));

    await api.get("/imports");
    await api.post("/auth/connexion", {});

    expect(serveur.history.get[0].headers?.Authorization).toBe("Bearer jeton-1");
    expect(serveur.history.post[0].headers?.Authorization).toBeUndefined();
  });

  it("sur un 401, rafraîchit la session puis rejoue la requête avec le nouveau jeton", async () => {
    definirJetonAcces("expire");
    serveur.onGet("/imports").replyOnce(401).onGet("/imports").reply(200, { ok: true });
    serveur.onPost("/auth/rafraichir").reply(200, SESSION("neuf"));

    const reponse = await api.get("/imports");

    expect(reponse.data).toEqual({ ok: true });
    expect(jetonAccesCourant()).toBe("neuf");
    expect(serveur.history.get[1].headers?.Authorization).toBe("Bearer neuf");
  });

  it("ne rafraîchit qu'une fois pour plusieurs requêtes en échec simultanées", async () => {
    definirJetonAcces("expire");
    serveur.onGet("/a").replyOnce(401).onGet("/a").reply(200, "a");
    serveur.onGet("/b").replyOnce(401).onGet("/b").reply(200, "b");
    serveur.onPost("/auth/rafraichir").reply(200, SESSION("neuf"));

    const [a, b] = await Promise.all([api.get("/a"), api.get("/b")]);

    expect([a.data, b.data]).toEqual(["a", "b"]);
    expect(serveur.history.post.filter((r) => r.url === "/auth/rafraichir")).toHaveLength(1);
  });

  it("ferme la session quand le rafraîchissement échoue (inactivité, déconnexion)", async () => {
    const expiree = vi.fn();
    surExpirationSession(expiree);
    definirJetonAcces("expire");
    serveur.onGet("/imports").reply(401);
    serveur.onPost("/auth/rafraichir").reply(401, { message: "Session expirée, veuillez vous reconnecter." });

    await expect(api.get("/imports")).rejects.toMatchObject({ response: { status: 401 } });

    expect(expiree).toHaveBeenCalledOnce();
    expect(jetonAccesCourant()).toBeNull();
  });

  it("ne tente pas de rafraîchir un échec de connexion", async () => {
    serveur.onPost("/auth/connexion").reply(401, { message: "Identifiant ou mot de passe incorrect." });

    await expect(api.post("/auth/connexion", {})).rejects.toBeTruthy();

    expect(serveur.history.post).toHaveLength(1);
  });

  it("traduit les erreurs en message lisible", async () => {
    serveur.onPost("/auth/connexion").replyOnce(423, { message: "Compte verrouillé." });
    serveur.onGet("/panne").networkErrorOnce();

    const verrouille = await api.post("/auth/connexion", {}).catch((e) => e);
    const panne = await api.get("/panne").catch((e) => e);

    expect(messageErreur(verrouille)).toBe("Compte verrouillé.");
    expect(messageErreur(panne)).toContain("Serveur injoignable");
    expect(messageErreur(new Error("x"), "Par défaut")).toBe("Par défaut");
  });
});
