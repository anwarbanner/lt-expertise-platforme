import { cleanup, fireEvent, screen, waitFor, within } from "@testing-library/react";
import type MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { ROUTES } from "@/app/routes";
import { compte, page, simulerApi } from "@/test/api";
import { ADMINISTRATEUR, rendreApplication } from "@/test/rendu";

let serveur: MockAdapter;

afterEach(() => {
  cleanup();
  serveur.restore();
});

const COMPTES = [
  compte("admin", { id: 1, nom: "El Khatib", prenom: "Amine", role: "ADMINISTRATEUR", derniereConnexion: "2026-10-22T08:00:00Z" }),
  compte("gestionnaire", { id: 2 }),
  compte("consultant", { id: 3, nom: "Alaoui", prenom: "Nadia", role: "CONSULTATION", verrouille: true }),
];

/** Ligne du tableau des comptes (le nom de l'administrateur connecté figure aussi dans la barre du haut). */
function ligne(nom: string) {
  return screen.getAllByText(nom).map((e) => e.closest("tr")).find(Boolean)!;
}

describe("comptes utilisateurs", () => {
  it("liste les comptes avec rôle, statut et compteurs", async () => {
    serveur = simulerApi((s) => s.onGet("/utilisateurs").reply(200, COMPTES));
    rendreApplication(ROUTES.utilisateurs, ADMINISTRATEUR);

    expect(await screen.findByText("3 utilisateurs · 3 actifs · 1 verrouillé")).toBeTruthy();
    expect(within(ligne("Nadia Alaoui")).getByText("Verrouillé")).toBeTruthy();
    expect(within(ligne("Samira Benali")).getByText("Jamais")).toBeTruthy();
  });

  it("n'offre pas à l'administrateur de désactiver son propre compte", async () => {
    serveur = simulerApi((s) => s.onGet("/utilisateurs").reply(200, COMPTES));
    rendreApplication(ROUTES.utilisateurs, ADMINISTRATEUR);
    await screen.findByText("3 utilisateurs · 3 actifs · 1 verrouillé");

    expect(within(ligne("Amine El Khatib")).queryByRole("button", { name: "Désactiver" })).toBeNull();
    expect(within(ligne("Samira Benali")).getByRole("button", { name: "Désactiver" })).toBeTruthy();
  });

  it("désactive un compte en conservant ses autres données", async () => {
    serveur = simulerApi((s) => {
      s.onGet("/utilisateurs").reply(200, COMPTES);
      s.onPut("/utilisateurs/2").reply(200, { ...COMPTES[1], actif: false });
    });
    rendreApplication(ROUTES.utilisateurs, ADMINISTRATEUR);
    await screen.findByText("Samira Benali");

    fireEvent.click(within(ligne("Samira Benali")).getByRole("button", { name: "Désactiver" }));

    await waitFor(() => expect(serveur.history.put).toHaveLength(1));
    expect(JSON.parse(serveur.history.put[0].data)).toEqual({ nom: "Benali", prenom: "Samira", email: null, role: "GESTIONNAIRE", actif: false });
  });

  it("crée un compte après contrôle du formulaire", async () => {
    serveur = simulerApi((s) => {
      s.onGet("/utilisateurs").reply(200, COMPTES);
      s.onPost("/utilisateurs").reply(201, compte("y.idrissi", { id: 4 }));
    });
    rendreApplication(ROUTES.utilisateurs, ADMINISTRATEUR);
    await screen.findByText("Samira Benali");

    fireEvent.click(screen.getByRole("button", { name: "Ajouter un utilisateur" }));
    const panneau = screen.getByRole("dialog", { name: "Ajouter un utilisateur" });
    fireEvent.click(within(panneau).getByRole("button", { name: "Créer l’utilisateur" }));

    expect(await within(panneau).findByText("Saisissez le prénom.")).toBeTruthy();
    expect(within(panneau).getByText("10 caractères au moins.")).toBeTruthy();
    expect(serveur.history.post).toHaveLength(0);

    fireEvent.change(within(panneau).getByLabelText(/^Prénom/), { target: { value: "Youssef" } });
    fireEvent.change(within(panneau).getByLabelText(/^Nom/), { target: { value: "Idrissi" } });
    fireEvent.change(within(panneau).getByLabelText(/^Identifiant/), { target: { value: "y.idrissi" } });
    fireEvent.change(within(panneau).getByLabelText(/^Mot de passe initial/), { target: { value: "motdepasse-solide" } });
    fireEvent.click(within(panneau).getByLabelText(/Consultation/));
    fireEvent.click(within(panneau).getByRole("button", { name: "Créer l’utilisateur" }));

    await waitFor(() => expect(serveur.history.post).toHaveLength(1));
    expect(JSON.parse(serveur.history.post[0].data)).toEqual({
      identifiant: "y.idrissi", nom: "Idrissi", prenom: "Youssef", email: null, role: "CONSULTATION", motDePasse: "motdepasse-solide",
    });
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
  });

  it("affiche le refus du serveur sans fermer le panneau", async () => {
    serveur = simulerApi((s) => {
      s.onGet("/utilisateurs").reply(200, COMPTES);
      s.onPut("/utilisateurs/2").reply(409, { message: "Compte introuvable." });
    });
    rendreApplication(ROUTES.utilisateurs, ADMINISTRATEUR);
    await screen.findByText("Samira Benali");

    fireEvent.click(within(ligne("Samira Benali")).getByRole("button", { name: "Modifier" }));
    const panneau = screen.getByRole("dialog", { name: "Modifier l’utilisateur" });
    expect((within(panneau).getByLabelText(/^Identifiant/) as HTMLInputElement).readOnly).toBe(true);
    fireEvent.click(within(panneau).getByRole("button", { name: "Enregistrer" }));

    expect(await within(panneau).findByText("Compte introuvable.")).toBeTruthy();
  });

  it("réinitialise un mot de passe confirmé", async () => {
    serveur = simulerApi((s) => {
      s.onGet("/utilisateurs").reply(200, COMPTES);
      s.onPost("/utilisateurs/3/mot-de-passe").reply(204);
    });
    rendreApplication(ROUTES.utilisateurs, ADMINISTRATEUR);
    await screen.findByText("Nadia Alaoui");

    fireEvent.click(within(ligne("Nadia Alaoui")).getByRole("button", { name: "Réinitialiser le mot de passe" }));
    const panneau = screen.getByRole("dialog", { name: "Réinitialiser le mot de passe" });
    fireEvent.change(within(panneau).getByLabelText(/^Nouveau mot de passe/), { target: { value: "nouveau-secret-1" } });
    fireEvent.change(within(panneau).getByLabelText(/^Confirmation/), { target: { value: "autre-secret-1" } });
    fireEvent.click(within(panneau).getByRole("button", { name: "Réinitialiser" }));
    expect(await within(panneau).findByText("Les deux mots de passe diffèrent.")).toBeTruthy();

    fireEvent.change(within(panneau).getByLabelText(/^Confirmation/), { target: { value: "nouveau-secret-1" } });
    fireEvent.click(within(panneau).getByRole("button", { name: "Réinitialiser" }));

    await waitFor(() => expect(serveur.history.post).toHaveLength(1));
    expect(JSON.parse(serveur.history.post[0].data)).toEqual({ motDePasse: "nouveau-secret-1" });
  });
});

describe("journal d'audit", () => {
  const entree = {
    id: 1, horodatage: "2026-10-22T09:15:00Z", utilisateur: "gestionnaire", action: "MODIFICATION", typeEntite: "RapportExpertise",
    entiteId: 7, numeroRapport: "268202-0313", champ: "montantIndemnisation", ancienneValeur: "2400.00", nouvelleValeur: "2500.00",
    details: null, adresseIp: "192.168.1.20",
  };

  it("affiche les événements et filtre par action côté serveur", async () => {
    serveur = simulerApi((s) => s.onGet("/journal-audit").reply(200, page([entree], 0, 20, 45)));
    rendreApplication(ROUTES.utilisateurs, ADMINISTRATEUR);

    fireEvent.click(screen.getByRole("tab", { name: /Journal d’audit/ }));

    expect(await screen.findByText("Dossier 268202-0313 — montantIndemnisation : 2400.00 → 2500.00")).toBeTruthy();
    expect(screen.getByText("192.168.1.20")).toBeTruthy();
    expect(screen.getByText("1–20 sur 45 événements")).toBeTruthy();

    fireEvent.change(screen.getByLabelText("Type d’action"), { target: { value: "VALIDATION" } });
    await waitFor(() => expect(serveur.history.get.at(-1)?.params).toEqual({ action: "VALIDATION", page: 0, size: 20 }));

    fireEvent.click(screen.getByRole("button", { name: "Page suivante" }));
    await waitFor(() => expect(serveur.history.get.at(-1)?.params).toEqual({ action: "VALIDATION", page: 1, size: 20 }));
  });
});

describe("détail d'un import refusé", () => {
  it("affiche le fichier et le motif sans l'empreinte", async () => {
    const refus = {
      id: 2, horodatage: "2026-10-22T14:48:00Z", utilisateur: "gestionnaire", action: "IMPORT_REFUSE", typeEntite: null, entiteId: null,
      numeroRapport: null, champ: null, ancienneValeur: null, nouvelleValeur: null, adresseIp: null,
      details: { issue: "DOUBLON_FICHIER", motif: "Fichier déjà importé.", hashSha256: "0a29f2", nomFichier: "rapport4.pdf" },
    };
    serveur = simulerApi((s) => s.onGet("/journal-audit").reply(200, page([refus])));
    rendreApplication(ROUTES.utilisateurs, ADMINISTRATEUR);

    fireEvent.click(screen.getByRole("tab", { name: /Journal d’audit/ }));

    expect(await screen.findByText("rapport4.pdf : Fichier déjà importé.")).toBeTruthy();
    expect(screen.queryByText(/0a29f2/)).toBeNull();
  });
});
