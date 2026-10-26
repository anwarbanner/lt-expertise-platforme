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
];

function ouvrir(adresse: string = ROUTES.dossiers, utilisateur = GESTIONNAIRE) {
  serveur = simulerApi((s) => {
    s.onGet("/dossiers").reply((config) => [200, config.params?.statut === "PRET_A_VALIDER" ? page(LISTE.slice(0, 2), 0, 1, 2) : page(LISTE, 0, 10)]);
    s.onPost("/dossiers/validation-groupee").reply(200, { valides: 2, paquets: 1, ignores: [{ numeroRapport: "268202-0009", motif: "Déjà validé." }] });
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

  it("la case « page » coche tous les dossiers prêts de la page", async () => {
    ouvrir();

    fireEvent.click(await screen.findByLabelText("Cocher les dossiers prêts de la page"));

    expect(screen.getByText("2 dossier(s) sélectionné(s)")).toBeTruthy();
  });

  it("n'est pas proposée au consultant", async () => {
    ouvrir(ROUTES.dossiers, CONSULTANT);

    expect(await screen.findByText("268202-0001")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Valider la sélection" })).toBeNull();
    expect(screen.queryByLabelText("Cocher 268202-0001")).toBeNull();
  });
});
