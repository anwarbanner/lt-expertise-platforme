import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import type MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { ROUTES } from "@/app/routes";
import { dossierDetail, simulerApi } from "@/test/api";
import { ADMINISTRATEUR, CONSULTANT, GESTIONNAIRE, rendreApplication } from "@/test/rendu";

let serveur: MockAdapter;

afterEach(() => {
  cleanup();
  serveur.restore();
});

describe("annulation d'un import et archivage (P6)", () => {
  it("l'administrateur annule un import erroné avec un motif obligatoire", async () => {
    serveur = simulerApi((s) => s.onPost("/dossiers/268202-0313/annulation").reply((config) => {
      expect(JSON.parse(config.data)).toEqual({ motif: "Mauvais fichier" });
      return [200, dossierDetail("268202-0313", "ANNULE", { annulePar: "admin", annuleLe: "2026-11-23T10:00:00Z", motifAnnulation: "Mauvais fichier" })];
    }));
    rendreApplication(ROUTES.dossier("268202-0313"), ADMINISTRATEUR);

    fireEvent.click(await screen.findByRole("button", { name: "Annuler l’import" }));
    const confirmer = screen.getByRole("button", { name: "Confirmer l’annulation" }) as HTMLButtonElement;
    expect(confirmer.disabled).toBe(true);
    fireEvent.change(screen.getByLabelText(/Motif de l’annulation/), { target: { value: " Mauvais fichier " } });
    fireEvent.click(confirmer);

    await waitFor(() => expect(serveur.history.post.some((r) => r.url === "/dossiers/268202-0313/annulation")).toBe(true));
  });

  it("le gestionnaire n'annule pas mais archive un dossier validé", async () => {
    serveur = simulerApi((s) => s.onPost("/dossiers/268202-0284/archivage")
      .reply(200, dossierDetail("268202-0284", "ARCHIVE", { validePar: "gestionnaire", valideLe: "2026-11-20T10:00:00Z" })));
    rendreApplication(ROUTES.dossier("268202-0284"), GESTIONNAIRE);

    fireEvent.click(await screen.findByRole("button", { name: "Archiver" }));

    await waitFor(() => expect(serveur.history.post.some((r) => r.url === "/dossiers/268202-0284/archivage")).toBe(true));
    expect(screen.queryByRole("button", { name: "Annuler l’import" })).toBeNull();
  });

  it("le consultant ne voit ni l'archivage ni l'annulation", async () => {
    serveur = simulerApi();
    rendreApplication(ROUTES.dossier("268202-0284"), CONSULTANT);

    expect(await screen.findByRole("heading", { level: 1, name: "Dossier 268202-0284" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Archiver" })).toBeNull();
    expect(screen.queryByRole("button", { name: "Annuler l’import" })).toBeNull();
  });

  it("un import annulé affiche son motif et ne propose plus de vérification", async () => {
    serveur = simulerApi((s) => s.onGet("/dossiers/268202-0999").reply(200, dossierDetail("268202-0999", "ANNULE",
      { annulePar: "admin", annuleLe: "2026-11-23T10:00:00Z", motifAnnulation: "Rapport d'un autre dossier" })));
    rendreApplication(ROUTES.dossier("268202-0999"), ADMINISTRATEUR);

    expect(await screen.findByText(/Rapport d'un autre dossier/)).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Vérifier" })).toBeNull();
    expect(screen.queryByRole("button", { name: "Annuler l’import" })).toBeNull();
  });
});
