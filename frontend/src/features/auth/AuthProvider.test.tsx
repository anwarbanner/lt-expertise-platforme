import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import MockAdapter from "axios-mock-adapter";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { api, definirJetonAcces, jetonAccesCourant } from "@/api/client";
import { useAuth } from "./AuthContext";
import { AuthProvider } from "./AuthProvider";

const SESSION = {
  jetonAcces: "jeton",
  expireDans: 600,
  utilisateur: { identifiant: "consultant", nom: "Alaoui", prenom: "Nadia", role: "CONSULTATION" },
};

function Etat() {
  const { etat, connecter, deconnecter } = useAuth();
  return <div>
    <span data-testid="etat">{etat.statut === "connecte" ? `connecte:${etat.utilisateur.identifiant}` : etat.statut}</span>
    <button onClick={() => void connecter("consultant", "secret")}>connecter</button>
    <button onClick={() => void deconnecter()}>deconnecter</button>
  </div>;
}

function rendre() {
  render(<QueryClientProvider client={new QueryClient()}><AuthProvider><Etat /></AuthProvider></QueryClientProvider>);
}

let serveur: MockAdapter;

beforeEach(() => {
  serveur = new MockAdapter(api);
  definirJetonAcces(null);
});

afterEach(() => {
  cleanup();
  serveur.restore();
});

describe("session", () => {
  it("se rétablit au chargement grâce au cookie de rafraîchissement", async () => {
    serveur.onPost("/auth/rafraichir").reply(200, SESSION);

    rendre();

    expect(screen.getByTestId("etat").textContent).toBe("chargement");
    expect((await screen.findByText("connecte:consultant")).textContent).toBeTruthy();
    expect(jetonAccesCourant()).toBe("jeton");
  });

  it("reste déconnectée sans cookie valide", async () => {
    serveur.onPost("/auth/rafraichir").reply(401);

    rendre();

    expect(await screen.findByText("deconnecte")).toBeTruthy();
  });

  it("se connecte puis se déconnecte en révoquant la session côté serveur", async () => {
    serveur.onPost("/auth/rafraichir").reply(401);
    serveur.onPost("/auth/connexion").reply(200, SESSION);
    serveur.onPost("/auth/deconnexion").reply(204);
    rendre();
    await screen.findByText("deconnecte");

    fireEvent.click(screen.getByText("connecter"));
    await screen.findByText("connecte:consultant");
    fireEvent.click(screen.getByText("deconnecter"));

    expect(await screen.findByText("deconnecte")).toBeTruthy();
    expect(jetonAccesCourant()).toBeNull();
    expect(serveur.history.post.map((r) => r.url)).toContain("/auth/deconnexion");
  });
});
