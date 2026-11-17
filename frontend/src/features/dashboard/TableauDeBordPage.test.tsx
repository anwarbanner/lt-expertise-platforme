import { cleanup, screen } from "@testing-library/react";
import type MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { ROUTES } from "@/app/routes";
import type { TableauDeBord } from "@/domain/dossiers";
import { TABLEAU_VIDE, simulerApi } from "@/test/api";
import { ADMINISTRATEUR, CONSULTANT, GESTIONNAIRE, rendreApplication } from "@/test/rendu";

let serveur: MockAdapter;

afterEach(() => {
  cleanup();
  serveur.restore();
});

const DERNIER = { numeroRapport: "268202-0313", importeLe: "2026-10-20T10:00:00Z", compagnie: "Wafa Assurance", statut: "A_VERIFIER" } as const;

const COMPLET: TableauDeBord = {
  ...TABLEAU_VIDE, dossiers: 12, montantTotal: 48000, coutMoyen: 4000, enAttenteValidation: 3, dossiersEnAnomalie: 2,
  parStatut: { A_VERIFIER: 4, A_COMPLETER: 1, PRET_A_VALIDER: 3, VALIDE: 4 },
  parZone: [{ cle: "4", nombre: 7 }], parCompagnie: [{ cle: "Wafa Assurance", nombre: 9 }],
  alertes: [{ numeroRapport: "268202-0313", incoherences: 1, champsManquants: 0, premierControle: "Total main d’œuvre incohérent" }],
  derniersDossiers: [{ ...DERNIER, montantIndemnisation: 2400 }],
};

/** Ce que le serveur renvoie hors administrateur : aucun montant. */
const SANS_MONTANTS: TableauDeBord = {
  ...COMPLET, montantTotal: null, coutMoyen: null, derniersDossiers: [{ ...DERNIER, montantIndemnisation: null }],
};

describe("tableau de bord selon le rôle", () => {
  it("l'administrateur voit les montants", async () => {
    serveur = simulerApi((s) => s.onGet("/statistiques/tableau-de-bord").reply(200, COMPLET));
    rendreApplication(ROUTES.tableauDeBord, ADMINISTRATEUR);

    expect(await screen.findByText("Montant total des indemnisations")).toBeTruthy();
    expect(screen.getByText("Montants indemnisés par mois")).toBeTruthy();
    expect(screen.getByText("MONTANT")).toBeTruthy();
  });

  it("le gestionnaire voit ses tâches et les anomalies, sans montant", async () => {
    serveur = simulerApi((s) => s.onGet("/statistiques/tableau-de-bord").reply(200, SANS_MONTANTS));
    rendreApplication(ROUTES.tableauDeBord, GESTIONNAIRE);

    expect(await screen.findByText("Prêts à valider")).toBeTruthy();
    expect(screen.getByText("Champs à saisir")).toBeTruthy();
    expect(screen.getByText("Alertes récentes")).toBeTruthy();
    expect(screen.getByText("Total main d’œuvre incohérent")).toBeTruthy();
    expect(screen.getByText("Derniers dossiers traités")).toBeTruthy();
    expect(screen.queryByText("Montant total des indemnisations")).toBeNull();
    expect(screen.queryByText("MONTANT")).toBeNull();
    expect(screen.queryByText(/DH/)).toBeNull();
  });

  it("la consultation voit les statistiques des zones de choc, sans montant", async () => {
    serveur = simulerApi((s) => s.onGet("/statistiques/tableau-de-bord").reply(200, SANS_MONTANTS));
    rendreApplication(ROUTES.tableauDeBord, CONSULTANT);

    expect(await screen.findByText("Sinistres par zone de choc")).toBeTruthy();
    expect(screen.getByText("Zone de choc la plus fréquente")).toBeTruthy();
    expect(screen.getByText("Dossiers par mois")).toBeTruthy();
    expect(screen.queryByText("Alertes récentes")).toBeNull();
    expect(screen.queryByText("Montant total des indemnisations")).toBeNull();
    expect(screen.queryByText(/DH/)).toBeNull();
  });
});
