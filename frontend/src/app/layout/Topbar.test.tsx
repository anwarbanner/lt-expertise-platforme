import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import type MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { ROUTES } from "@/app/routes";
import { simulerApi } from "@/test/api";
import { GESTIONNAIRE, rendreApplication } from "@/test/rendu";

let serveur: MockAdapter;

afterEach(() => {
  cleanup();
  serveur.restore();
});

describe("barre du haut", () => {
  it("lance la recherche globale dans la liste des dossiers", async () => {
    serveur = simulerApi();
    rendreApplication(ROUTES.tableauDeBord, GESTIONNAIRE);

    const champ = screen.getByLabelText("Recherche globale");
    fireEvent.change(champ, { target: { value: "32148-B" } });
    fireEvent.submit(champ.closest("form")!);

    expect(await screen.findByRole("heading", { level: 1, name: "Dossiers" })).toBeTruthy();
    await waitFor(() => expect(serveur.history.get.find((r) => r.url === "/dossiers")?.params).toMatchObject({ q: "32148-B" }));
    expect((screen.getByLabelText("Recherche") as HTMLInputElement).value).toBe("32148-B");
    expect((screen.getByLabelText("Recherche globale") as HTMLInputElement).value).toBe("");
  });

  it("place le curseur dans la recherche avec Ctrl+K", () => {
    serveur = simulerApi();
    rendreApplication(ROUTES.tableauDeBord, GESTIONNAIRE);

    fireEvent.keyDown(window, { key: "k", ctrlKey: true });

    expect(document.activeElement).toBe(screen.getByLabelText("Recherche globale"));
  });

  it("signale les dossiers à traiter et ouvre la liste filtrée", async () => {
    serveur = simulerApi((s) => s.onGet("/dossiers/compteurs").reply(200, { A_VERIFIER: 3, PRET_A_VALIDER: 1, VALIDE: 9 }));
    rendreApplication(ROUTES.tableauDeBord, GESTIONNAIRE);

    fireEvent.click(await screen.findByRole("button", { name: "Notifications : 4 dossiers à traiter" }));
    expect(screen.getByRole("menuitem", { name: /1 dossier · Prêt à valider/ })).toBeTruthy();
    fireEvent.click(screen.getByRole("menuitem", { name: /3 dossiers · À vérifier/ }));

    await waitFor(() => expect(serveur.history.get.some((r) => r.url === "/dossiers" && r.params?.statut === "A_VERIFIER")).toBe(true));
  });
});
