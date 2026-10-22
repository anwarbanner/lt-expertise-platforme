import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { afterEach, describe, expect, it } from "vitest";
import { RoutesApplication } from "./App";
import { ROUTES } from "./routes";

function afficher(adresse: string) {
  return render(<MemoryRouter initialEntries={[adresse]}><RoutesApplication /></MemoryRouter>);
}

afterEach(cleanup);

describe("routes de l'application", () => {
  it.each([
    [ROUTES.tableauDeBord, "Tableau de bord"],
    [ROUTES.imports, "Importer des rapports"],
    [ROUTES.dossiers, "Dossiers"],
    [ROUTES.dossier("268202-0284"), "Dossier 268202-0284"],
    [ROUTES.verification("268202-0313"), "Vérification du dossier 268202-0313"],
    [ROUTES.analysePrix, "Analyse des prix"],
    [ROUTES.generation, "Génération des rapports"],
    [ROUTES.utilisateurs, "Utilisateurs"],
  ])("%s affiche « %s »", (adresse, titre) => {
    afficher(adresse);

    expect(screen.getByRole("heading", { level: 1, name: titre })).toBeTruthy();
  });

  it("affiche la connexion sans la barre latérale", () => {
    afficher(ROUTES.connexion);

    expect(screen.getByRole("heading", { level: 1, name: "Bienvenue" })).toBeTruthy();
    expect(screen.queryByText("ESPACE DE TRAVAIL")).toBeNull();
  });

  it("renvoie une adresse inconnue vers le tableau de bord", () => {
    afficher("/inconnue");

    expect(screen.getByRole("heading", { level: 1, name: "Tableau de bord" })).toBeTruthy();
  });

  it("navigue par la barre latérale et marque l'entrée active", () => {
    afficher(ROUTES.tableauDeBord);

    fireEvent.click(screen.getByRole("link", { name: /Dossiers/ }));

    expect(screen.getByRole("heading", { level: 1, name: "Dossiers" })).toBeTruthy();
    expect(screen.getByRole("link", { name: /Dossiers/ }).className).toContain("active");
  });

  it("ouvre la vérification d'un dossier non validé depuis la liste", () => {
    afficher(ROUTES.dossiers);

    const ligne = screen.getByText("268202-0313").closest("tr")!;
    fireEvent.click(ligne.querySelector("button")!);

    expect(screen.getByRole("heading", { level: 1, name: "Vérification du dossier 268202-0313" })).toBeTruthy();
  });

  it("annonce la taille maximale acceptée par le backend", () => {
    afficher(ROUTES.imports);

    expect(screen.getByText(/20 Mo maximum chacun/)).toBeTruthy();
  });
});
