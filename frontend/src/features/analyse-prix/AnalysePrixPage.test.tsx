import { cleanup, fireEvent, screen, waitFor, within } from "@testing-library/react";
import type MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { ROUTES } from "@/app/routes";
import { PRIX_VIDE, simulerApi } from "@/test/api";
import { CONSULTANT, GESTIONNAIRE, rendreApplication } from "@/test/rendu";

let serveur: MockAdapter;

afterEach(() => {
  cleanup();
  serveur.restore();
});

const derniersParametres = (url: string) => serveur.history.get.filter((r) => r.url === url).at(-1)?.params;

const ANALYSE = {
  ...PRIX_VIDE, nombre: 3, dossiers: 2, minimum: 300, moyenne: 650, maximum: 1000,
  parType: [{ type: "ORIGINE", nombre: 1, moyenne: 1000 }, { type: "ADAPTABLE", nombre: 1, moyenne: 300 }, { type: "VIDE", nombre: 1, moyenne: null }],
  lignes: [
    { numeroRapport: "268202-0284", designation: "Pare-choc avant", typePiece: "ORIGINE", marque: "Renault", modele: "Clio V", typeMine: "BJA", prixUnitaire: 1000, date: "2026-10-10" },
    { numeroRapport: "268202-0313", designation: "Phare gauche", typePiece: "ADAPTABLE", marque: "Dacia", modele: null, typeMine: null, prixUnitaire: 300, date: null },
    { numeroRapport: "268202-0313", designation: "PP ET ING", typePiece: null, marque: "Dacia", modele: null, typeMine: null, prixUnitaire: null, date: null },
  ],
};

describe("analyse des prix", () => {
  it("affiche les dossiers trouvés, les prix, la répartition par type et la liste", async () => {
    serveur = simulerApi((s) => s.onGet("/statistiques/prix").reply(200, ANALYSE));
    rendreApplication(ROUTES.analysePrix, GESTIONNAIRE);

    expect(await screen.findByText("Pare-choc avant")).toBeTruthy();
    const details = screen.getByRole("region", { name: "Détails des prix" });
    expect(within(details).getAllByRole("article").map((k) => k.querySelector("small")?.textContent))
      .toEqual(["Dossiers trouvés", "Prix moyen", "Prix maximum", "Prix minimum"]);
    expect(within(details).getByText("2")).toBeTruthy();
    expect(within(details).getByText("650,00 DH")).toBeTruthy();
    expect(screen.getByRole("img", { name: "Origine : 1, Adaptable : 1, Non renseigné : 1" })).toBeTruthy();
    expect(screen.getAllByText("33,3 %").length).toBe(3);
    expect(screen.getAllByRole("columnheader").slice(0, 7).map((th) => th.textContent?.replace(/[▲▼⇅]/g, ""))).toEqual(
      ["DÉSIGNATION", "TYPE", "MARQUE", "MODÈLE", "TYPE MINE", "PRIX UNITAIRE", "DATE DU PRIX"]);
    expect(screen.getAllByText("Non renseigné").length).toBeGreaterThan(1);
  });

  it("transmet tous les filtres au serveur, dont les zones multiples et les champs vides", async () => {
    serveur = simulerApi((s) => {
      s.onGet("/statistiques/prix").reply(200, ANALYSE);
      s.onGet("/recherche/suggestions").reply(200, { marques: ["RENAULT"], modeles: ["CLIO"], typesMine: ["BJA"] });
    });
    rendreApplication(ROUTES.analysePrix, CONSULTANT);
    await screen.findByText("Pare-choc avant");

    fireEvent.change(screen.getByLabelText("Pièce"), { target: { value: "porte" } });
    fireEvent.change(screen.getByLabelText("Type de pièce"), { target: { value: "VIDE" } });
    fireEvent.click(screen.getByRole("button", { name: "Zones de choc" }));
    fireEvent.click(screen.getByLabelText("4 · Central/Latéral droit"));
    fireEvent.click(screen.getByLabelText("1 · Avant/Latéral gauche"));
    fireEvent.change(screen.getByLabelText("Marque"), { target: { value: "ren" } });
    await waitFor(() => expect(derniersParametres("/recherche/suggestions")).toEqual({ marque: "ren" }));
    fireEvent.change(await screen.findByLabelText("Modèle"), { target: { value: "CLIO" } });
    fireEvent.change(screen.getByLabelText("Type mine"), { target: { value: "__VIDE__" } });
    fireEvent.click(screen.getByLabelText("Année de MEC vide"));
    fireEvent.change(screen.getByLabelText("N° de châssis"), { target: { value: "VF1" } });

    expect((screen.getByLabelText("Année de MEC") as HTMLInputElement).disabled).toBe(true);
    await waitFor(() => expect(derniersParametres("/statistiques/prix")).toEqual({ designation: "porte", type: "VIDE", zones: "1,4",
      marque: "ren", modele: "CLIO", typeMine: "__VIDE__", annee: "__VIDE__", chassis: "VF1", page: 0, taille: 25 }));
    expect(derniersParametres("/recherche/suggestions")).toEqual({ marque: "ren", modele: "CLIO" });
  });

  it("envoie au serveur la page, le tri et les filtres de colonne", async () => {
    serveur = simulerApi((s) => s.onGet("/statistiques/prix").reply(200, { ...ANALYSE, nombre: 80 }));
    rendreApplication(ROUTES.analysePrix, CONSULTANT);
    expect(await screen.findByText("1–25 sur 80")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "2" }));
    await waitFor(() => expect(derniersParametres("/statistiques/prix")).toMatchObject({ page: 1, taille: 25 }));
    fireEvent.click(screen.getByRole("button", { name: /PRIX UNITAIRE/ }));
    fireEvent.change(screen.getByLabelText("Filtrer MARQUE"), { target: { value: "ren" } });

    await waitFor(() => expect(derniersParametres("/statistiques/prix")).toEqual({ page: 0, taille: 25, tri: "prix,asc", f_marque: "ren" }));
  });

  it("ouvre le dossier d'origine d'une pièce", async () => {
    serveur = simulerApi((s) => s.onGet("/statistiques/prix").reply(200, ANALYSE));
    rendreApplication(ROUTES.analysePrix, GESTIONNAIRE);

    fireEvent.click(await screen.findByRole("button", { name: "Pare-choc avant" }));

    expect(await screen.findByRole("heading", { level: 1, name: "Dossier 268202-0284" })).toBeTruthy();
  });

  it("redirige l'ancienne recherche de pièces vers l'analyse des prix", async () => {
    serveur = simulerApi();
    rendreApplication(ROUTES.recherchePieces, CONSULTANT);

    expect(await screen.findByRole("heading", { level: 1, name: "Analyse des prix" })).toBeTruthy();
  });

  it("indique l'absence de résultat", async () => {
    serveur = simulerApi();
    rendreApplication(ROUTES.analysePrix, GESTIONNAIRE);

    expect(await screen.findByText("Aucun prix trouvé")).toBeTruthy();
  });
});
