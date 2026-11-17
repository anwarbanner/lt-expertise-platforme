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
    { numeroRapport: "268202-0284", designation: "Pare-choc avant", typePiece: "ORIGINE", marque: "Renault", modele: "Clio V", typeMine: "BJA", prixUnitaire: 1000, date: "2026-10-10" },
    { numeroRapport: "268202-0313", designation: "Phare gauche", typePiece: "ADAPTABLE", marque: "Dacia", modele: null, typeMine: null, prixUnitaire: 300, date: null },
  ],
};

describe("analyse des prix", () => {
  it("affiche les indicateurs et les lignes calculés par le serveur", async () => {
    serveur = simulerApi((s) => s.onGet("/statistiques/prix").reply(200, ANALYSE));
    rendreApplication(ROUTES.analysePrix, GESTIONNAIRE);

    expect(await screen.findByText("Pare-choc avant")).toBeTruthy();
    expect(screen.getByText("Phare gauche")).toBeTruthy();
    expect(screen.getAllByText("300,00 DH").length).toBeGreaterThan(0);
    expect(screen.getAllByText("1 000,00 DH").length).toBeGreaterThan(0);
    expect(screen.getByText("Clio V")).toBeTruthy();
    expect(screen.getByText("BJA")).toBeTruthy();
    expect(screen.getAllByRole("columnheader").slice(0, 8).map((th) => th.textContent?.replace(/[▲▼⇅]/g, ""))).toEqual(
      ["DOSSIER", "DÉSIGNATION", "TYPE", "MARQUE", "MODÈLE", "TYPE MINE", "PRIX UNITAIRE", "DATE DU PRIX"]);
  });

  it("transmet les filtres au serveur", async () => {
    serveur = simulerApi((s) => s.onGet("/statistiques/prix").reply(200, ANALYSE));
    rendreApplication(ROUTES.analysePrix, GESTIONNAIRE);
    await screen.findByText("Pare-choc avant");

    fireEvent.change(screen.getByLabelText("Type de pièce"), { target: { value: "ORIGINE" } });
    fireEvent.change(screen.getByLabelText("Marque"), { target: { value: "Renault" } });
    fireEvent.change(screen.getByLabelText("Modèle"), { target: { value: "Clio" } });
    fireEvent.change(screen.getByLabelText("Type mine"), { target: { value: "BJA" } });
    fireEvent.change(screen.getByLabelText("Désignation de la pièce"), { target: { value: "phare" } });

    await waitFor(() => expect(serveur.history.get.filter((r) => r.url === "/statistiques/prix").at(-1)?.params)
      .toEqual({ designation: "phare", type: "ORIGINE", marque: "Renault", modele: "Clio", typeMine: "BJA", page: 0, taille: 25 }));
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
