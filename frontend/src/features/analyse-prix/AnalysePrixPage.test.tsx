import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import type MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { ROUTES } from "@/app/routes";
import { PRIX_VIDE, simulerApi } from "@/test/api";
import { GESTIONNAIRE, rendreApplication } from "@/test/rendu";

let serveur: MockAdapter;

afterEach(() => {
  cleanup();
  serveur.restore();
});

const ANALYSE = {
  ...PRIX_VIDE, nombre: 2, minimum: 300, moyenne: 650, maximum: 1000, moyenneParType: { ORIGINE: 1000, ADAPTABLE: 300 },
  lignes: [
    { designation: "Pare-choc avant", typePiece: "ORIGINE", compagnie: "Wafa Assurance", marque: "Renault", modele: "Clio V", prixUnitaire: 1000, vetustePct: 20, numeroRapport: "268202-0284", date: "2026-10-10" },
    { designation: "Phare gauche", typePiece: "ADAPTABLE", compagnie: "RMA", marque: "Dacia", modele: null, prixUnitaire: 300, vetustePct: null, numeroRapport: "268202-0313", date: null },
  ],
  compagnies: ["RMA", "Wafa Assurance"], marques: ["Dacia", "Renault"],
};

describe("analyse des prix", () => {
  it("affiche les indicateurs et les lignes calculés par le serveur", async () => {
    serveur = simulerApi((s) => s.onGet("/statistiques/prix").reply(200, ANALYSE));
    rendreApplication(ROUTES.analysePrix, GESTIONNAIRE);

    expect(await screen.findByText("Pare-choc avant")).toBeTruthy();
    expect(screen.getByText("Phare gauche")).toBeTruthy();
    expect(screen.getAllByText("300,00 DH").length).toBeGreaterThan(0);
    expect(screen.getAllByText("1 000,00 DH").length).toBeGreaterThan(0);
    expect(screen.getByText("Renault Clio V")).toBeTruthy();
    expect(screen.getByRole("option", { name: "RMA" })).toBeTruthy();
  });

  it("transmet les filtres au serveur", async () => {
    serveur = simulerApi((s) => s.onGet("/statistiques/prix").reply(200, ANALYSE));
    rendreApplication(ROUTES.analysePrix, GESTIONNAIRE);
    await screen.findByText("Pare-choc avant");

    fireEvent.change(screen.getByLabelText("Type de pièce"), { target: { value: "ORIGINE" } });
    fireEvent.change(screen.getByLabelText("Marque"), { target: { value: "Renault" } });
    fireEvent.change(screen.getByLabelText("Rechercher une pièce"), { target: { value: " phare " } });

    await waitFor(() => expect(serveur.history.get.at(-1)?.params).toEqual({ q: "phare", type: "ORIGINE", marque: "Renault" }));
  });

  it("ouvre le dossier d'origine d'un prix", async () => {
    serveur = simulerApi((s) => s.onGet("/statistiques/prix").reply(200, ANALYSE));
    rendreApplication(ROUTES.analysePrix, GESTIONNAIRE);

    fireEvent.click(await screen.findByRole("button", { name: "268202-0284" }));

    expect(await screen.findByRole("heading", { level: 1, name: "Dossier 268202-0284" })).toBeTruthy();
  });

  it("indique l'absence de résultat", async () => {
    serveur = simulerApi();
    rendreApplication(ROUTES.analysePrix, GESTIONNAIRE);

    expect(await screen.findByText("Aucun prix trouvé")).toBeTruthy();
  });
});
