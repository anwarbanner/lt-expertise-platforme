import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import type MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { ROUTES } from "@/app/routes";
import { dossierResume, page, simulerApi } from "@/test/api";
import { CONSULTANT, GESTIONNAIRE, rendreApplication } from "@/test/rendu";

let serveur: MockAdapter;

afterEach(() => {
  cleanup();
  serveur.restore();
});

const LISTE = [
  dossierResume("268202-0001", "PRET_A_VALIDER"),
  dossierResume("268202-0002", "PRET_A_VALIDER"),
  dossierResume("268202-0003", "A_VERIFIER", { anomalies: 1 }),
  dossierResume("268202-0004", "VALIDE"),
];

function ouvrir(adresse: string = ROUTES.dossiers, utilisateur = GESTIONNAIRE) {
  serveur = simulerApi((s) => {
    s.onGet("/dossiers").reply((config) => [200, config.params?.statut === "PRET_A_VALIDER" ? page(LISTE.slice(0, 2), 0, 1, 2)
      : config.params?.statut === "VALIDE" ? page(LISTE.slice(3), 0, 1, 1) : page(LISTE, 0, 10)]);
    s.onPost("/dossiers/validation-groupee").reply(200, { valides: 2, paquets: 1, ignores: [{ numeroRapport: "268202-0009", motif: "Déjà validé." }] });
    s.onPost("/dossiers/archivage-groupe").reply(200, { archives: 1, paquets: 1, ignores: [] });
  });
  rendreApplication(adresse, utilisateur);
}

describe("validation en lot", () => {
  it("valide les dossiers cochés après confirmation", async () => {
    ouvrir();

    fireEvent.click(await screen.findByLabelText("Cocher 268202-0001"));
    fireEvent.click(screen.getByLabelText("Cocher 268202-0002"));
    expect((screen.getByLabelText("Cocher 268202-0003") as HTMLInputElement).disabled).toBe(true);
    fireEvent.click(screen.getByRole("button", { name: "Valider la sélection" }));
    expect(screen.getByText(/Valider 2 dossier\(s\) \?/)).toBeTruthy();
    expect(serveur.history.post).toHaveLength(0);
    fireEvent.click(screen.getByRole("button", { name: "Confirmer" }));

    await waitFor(() => expect(serveur.history.post).toHaveLength(1));
    expect(JSON.parse(serveur.history.post[0].data)).toEqual({ numeros: ["268202-0001", "268202-0002"] });
    expect(await screen.findByText("2 dossier(s) validé(s)")).toBeTruthy();
    expect(screen.getByText("268202-0009 : Déjà validé.")).toBeTruthy();
  });

  it("valide tous les dossiers prêts répondant aux filtres", async () => {
    ouvrir(`${ROUTES.dossiers}?compagnie=Wafa%20Assurance`);

    fireEvent.click(await screen.findByRole("button", { name: /Valider tous les prêts à valider \(selon les filtres\) \(2\)/ }));
    fireEvent.click(screen.getByRole("button", { name: "Confirmer" }));

    await waitFor(() => expect(serveur.history.post).toHaveLength(1));
    expect(JSON.parse(serveur.history.post[0].data)).toEqual({ tousLesPrets: true, compagnie: "Wafa Assurance" });
  });

  it("la case « page » coche tous les dossiers prêts et validés de la page", async () => {
    ouvrir();

    fireEvent.click(await screen.findByLabelText("Cocher les dossiers de la page (prêts et validés)"));

    expect(screen.getByText("3 dossier(s) sélectionné(s)")).toBeTruthy();
  });

  it("archive les dossiers validés cochés, sans envoyer les prêts", async () => {
    ouvrir();

    fireEvent.click(await screen.findByLabelText("Cocher 268202-0001"));
    fireEvent.click(screen.getByLabelText("Cocher 268202-0004"));
    fireEvent.click(screen.getByRole("button", { name: "Archiver la sélection (1)" }));
    expect(screen.getByText(/Archiver 1 dossier\(s\) validé\(s\) \?/)).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Confirmer" }));

    await waitFor(() => expect(serveur.history.post).toHaveLength(1));
    expect(serveur.history.post[0].url).toBe("/dossiers/archivage-groupe");
    expect(JSON.parse(serveur.history.post[0].data)).toEqual({ numeros: ["268202-0004"] });
    expect(await screen.findByText("1 dossier(s) archivé(s)")).toBeTruthy();
  });

  it("archive tous les validés répondant aux filtres", async () => {
    ouvrir(`${ROUTES.dossiers}?compagnie=Wafa%20Assurance`);

    fireEvent.click(await screen.findByRole("button", { name: /Archiver tous les validés \(selon les filtres\) \(1\)/ }));
    fireEvent.click(screen.getByRole("button", { name: "Confirmer" }));

    await waitFor(() => expect(serveur.history.post).toHaveLength(1));
    expect(JSON.parse(serveur.history.post[0].data)).toEqual({ tousLesValides: true, compagnie: "Wafa Assurance" });
  });

  it("n'est pas proposée au consultant", async () => {
    ouvrir(ROUTES.dossiers, CONSULTANT);

    expect(await screen.findByText("268202-0001")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Valider la sélection" })).toBeNull();
    expect(screen.queryByLabelText("Cocher 268202-0001")).toBeNull();
  });
});

describe("onglet « À vérifier »", () => {
  const A_VERIFIER = [
    dossierResume("268202-0003", "A_VERIFIER", { anomalies: 4, libellesAnomalies: ["Total MO incohérent", "Date du sinistre absente", "Prix unitaire faux (PP ET ING)", "Immatriculation absente"] }),
    dossierResume("268202-0005", "A_VERIFIER", { anomalies: 1, libellesAnomalies: ["Montant de l’indemnisation incohérent"] }),
  ];

  it("ajoute une colonne qui détaille les anomalies de chaque dossier", async () => {
    serveur = simulerApi((s) => s.onGet("/dossiers").reply(200, page(A_VERIFIER, 0, 10)));
    rendreApplication(`${ROUTES.dossiers}?statut=A_VERIFIER`, GESTIONNAIRE);

    expect(await screen.findByRole("columnheader", { name: "ANOMALIES" })).toBeTruthy();
    expect(screen.getByText("Total MO incohérent")).toBeTruthy();
    expect(screen.getByText("Prix unitaire faux (PP ET ING)")).toBeTruthy();
    expect(screen.queryByText("Immatriculation absente")).toBeNull();
    expect(screen.getByText("+ 1 autre")).toBeTruthy();
    expect(screen.getByText("Montant de l’indemnisation incohérent")).toBeTruthy();
  });

  it("n'affiche pas la colonne dans les autres onglets", async () => {
    serveur = simulerApi((s) => s.onGet("/dossiers").reply(200, page(A_VERIFIER, 0, 10)));
    rendreApplication(ROUTES.dossiers, GESTIONNAIRE);

    expect(await screen.findByText("268202-0005")).toBeTruthy();
    expect(screen.queryByRole("columnheader", { name: "ANOMALIES" })).toBeNull();
  });
});
