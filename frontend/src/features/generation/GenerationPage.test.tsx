import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import type MockAdapter from "axios-mock-adapter";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ROUTES } from "@/app/routes";
import { simulerApi } from "@/test/api";
import { GESTIONNAIRE, rendreApplication } from "@/test/rendu";

let serveur: MockAdapter;
const fichiers: string[] = [];

beforeEach(() => {
  fichiers.length = 0;
  URL.createObjectURL = vi.fn(() => "blob:fichier");
  URL.revokeObjectURL = vi.fn();
  vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(function (this: HTMLAnchorElement) { fichiers.push(this.download); });
});

afterEach(() => {
  cleanup();
  serveur.restore();
  vi.restoreAllMocks();
});

const PDF = { "content-disposition": "attachment; filename=\"rapport-268202-0284.pdf\"; filename*=UTF-8''rapport-268202-0284.pdf" };

describe("génération des rapports", () => {
  it("ne propose que des dossiers validés, en affiche l'aperçu et télécharge le rapport PDF", async () => {
    serveur = simulerApi((s) => s.onGet("/rapports/268202-0284/pdf").reply(200, new Blob(["%PDF-1.7"]), PDF));
    rendreApplication(ROUTES.generation, GESTIONNAIRE);

    await waitFor(() => expect(serveur.history.get.find((r) => r.url === "/dossiers")?.params).toMatchObject({ statut: "VALIDE", size: 8 }));
    fireEvent.click(await screen.findByRole("button", { name: /268202-0284/ }));

    expect(await screen.findByText("Dossier 268202-0284")).toBeTruthy();
    expect(screen.getByText("RÉFÉRENCE 268202-0284")).toBeTruthy();
    expect(screen.getByText("Deux mille quatre cents dirhams")).toBeTruthy();
    const bouton = screen.getByRole("button", { name: "Générer le rapport PDF" }) as HTMLButtonElement;
    expect(bouton.disabled).toBe(false);
    fireEvent.click(bouton);

    await waitFor(() => expect(fichiers).toEqual(["rapport-268202-0284.pdf"]));
    await waitFor(() => expect(serveur.history.get.filter((r) => r.url === "/rapports/historique").length).toBeGreaterThan(1));
  });

  it("signale un dossier non validé passé dans l'adresse et n'en permet pas la génération", async () => {
    serveur = simulerApi();
    rendreApplication(`${ROUTES.generation}?dossier=268202-0313`, GESTIONNAIRE);

    expect(await screen.findByText(/n’est pas validé/)).toBeTruthy();
    expect((screen.getByRole("button", { name: "Générer le rapport PDF" }) as HTMLButtonElement).disabled).toBe(true);
  });

  it("affiche le motif d'un refus du serveur", async () => {
    serveur = simulerApi((s) => s.onGet("/rapports/268202-0284/pdf").reply(() =>
      [409, new Blob([JSON.stringify({ message: "Le rapport PDF ne se génère qu'à partir d'un dossier validé (P10)." })])]));
    rendreApplication(`${ROUTES.generation}?dossier=268202-0284`, GESTIONNAIRE);

    await screen.findByText("RÉFÉRENCE 268202-0284");
    fireEvent.click(screen.getByRole("button", { name: "Générer le rapport PDF" }));

    expect(await screen.findByText("Le rapport PDF ne se génère qu'à partir d'un dossier validé (P10).")).toBeTruthy();
    expect(fichiers).toEqual([]);
  });

  it("liste les rapports récemment générés", async () => {
    serveur = simulerApi((s) => s.onGet("/rapports/historique").reply(200, [
      { genereLe: "2026-10-22T16:00:00Z", generePar: "gestionnaire", numeroRapport: "268202-0284", assure: "Hassan Mansouri", compagnie: "Wafa Assurance" },
    ]));
    rendreApplication(ROUTES.generation, GESTIONNAIRE);

    expect(await screen.findByText("Hassan Mansouri")).toBeTruthy();
    expect(screen.getByRole("button", { name: "Régénérer" })).toBeTruthy();
  });
});

describe("exports Excel", () => {
  it("exporte toute la liste filtrée des dossiers", async () => {
    serveur = simulerApi((s) => s.onGet("/exports/dossiers").reply(200, new Blob(["xlsx"]),
      { "content-disposition": "attachment; filename=\"dossiers-20261022-1800.xlsx\"" }));
    rendreApplication(`${ROUTES.dossiers}?compagnie=Wafa%20Assurance&page=2`, GESTIONNAIRE);

    fireEvent.click(await screen.findByRole("button", { name: "Exporter en Excel" }));

    await waitFor(() => expect(fichiers).toEqual(["dossiers-20261022-1800.xlsx"]));
    const appel = serveur.history.get.find((r) => r.url === "/exports/dossiers")!;
    expect(appel.params).toEqual({ compagnie: "Wafa Assurance" });
  });
});
