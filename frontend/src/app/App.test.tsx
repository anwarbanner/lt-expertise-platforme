import { cleanup, fireEvent, screen, waitFor, within } from "@testing-library/react";
import type MockAdapter from "axios-mock-adapter";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { simulerApi } from "@/test/api";
import { ADMINISTRATEUR, CONSULTANT, GESTIONNAIRE, rendreApplication } from "@/test/rendu";
import { ROUTES } from "./routes";

let serveur: MockAdapter;

beforeEach(() => {
  serveur = simulerApi();
});

afterEach(() => {
  cleanup();
  serveur.restore();
});

function titre(nom: string) {
  return screen.findByRole("heading", { level: 1, name: nom });
}

describe("routes de l'application", () => {
  it.each([
    [ROUTES.tableauDeBord, "Tableau de bord"],
    [ROUTES.imports, "Importer des rapports"],
    [ROUTES.dossiers, "Dossiers"],
    [ROUTES.dossier("268202-0284"), "Dossier 268202-0284"],
    [ROUTES.verification("268202-0313"), "Vérification du dossier 268202-0313"],
    [ROUTES.analysePrix, "Analyse des prix"],
    [ROUTES.generation, "Génération des rapports"],
  ])("%s affiche « %s » au gestionnaire", async (adresse, nom) => {
    rendreApplication(adresse, GESTIONNAIRE);

    expect(await titre(nom)).toBeTruthy();
  });

  it("renvoie une adresse inconnue vers le tableau de bord", async () => {
    rendreApplication("/inconnue", GESTIONNAIRE);

    expect(await titre("Tableau de bord")).toBeTruthy();
  });
});

describe("routes protégées", () => {
  it("renvoie vers la connexion sans session", async () => {
    rendreApplication(ROUTES.dossiers, null);

    expect(await titre("Bienvenue")).toBeTruthy();
    expect(screen.queryByText("ESPACE DE TRAVAIL")).toBeNull();
  });

  it("attend la fin du rétablissement de la session avant de décider", async () => {
    rendreApplication(ROUTES.dossiers, null, { statut: "chargement" });

    expect(screen.getByRole("status").textContent).toContain("Chargement");
  });

  it("n'affiche pas la connexion à un utilisateur déjà connecté", async () => {
    rendreApplication(ROUTES.connexion, GESTIONNAIRE);

    expect(await titre("Tableau de bord")).toBeTruthy();
  });

  it.each([
    [ROUTES.imports, CONSULTANT],
    [ROUTES.verification("268202-0313"), CONSULTANT],
    [ROUTES.utilisateurs, GESTIONNAIRE],
    [ROUTES.imports, ADMINISTRATEUR],
  ])("%s est refusé au rôle %o", async (adresse, utilisateur) => {
    rendreApplication(adresse, utilisateur);

    expect(await titre("Accès refusé")).toBeTruthy();
  });

  it("l'administrateur accède aux utilisateurs", async () => {
    rendreApplication(ROUTES.utilisateurs, ADMINISTRATEUR);

    expect(await titre("Utilisateurs")).toBeTruthy();
  });
});

describe("mise en page commune", () => {
  it.each([
    [GESTIONNAIRE, ["Tableau de bord", "Importer des rapports", "Dossiers", "Analyse des prix", "Génération des rapports"]],
    [CONSULTANT, ["Tableau de bord", "Dossiers", "Analyse des prix", "Génération des rapports"]],
    [ADMINISTRATEUR, ["Tableau de bord", "Dossiers", "Analyse des prix", "Génération des rapports", "Utilisateurs"]],
  ])("le menu de %o ne propose que ses écrans", async (utilisateur, entrees) => {
    rendreApplication(ROUTES.tableauDeBord, utilisateur);

    expect(screen.getAllByRole("link").map((lien) => lien.textContent)).toEqual(entrees);
  });

  it("affiche l'utilisateur connecté et le déconnecte", async () => {
    const { auth } = rendreApplication(ROUTES.tableauDeBord, GESTIONNAIRE);

    expect(screen.getByText("Samira Benali")).toBeTruthy();
    expect(screen.getByText("Gestionnaire")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: /Samira Benali/ }));
    fireEvent.click(screen.getByRole("menuitem", { name: "Se déconnecter" }));

    await waitFor(() => expect(auth.deconnecter).toHaveBeenCalledOnce());
  });

  it("navigue par la barre latérale et marque l'entrée active", async () => {
    rendreApplication(ROUTES.tableauDeBord, GESTIONNAIRE);

    fireEvent.click(screen.getByRole("link", { name: /Dossiers/ }));

    expect(await titre("Dossiers")).toBeTruthy();
    expect(screen.getByRole("link", { name: /Dossiers/ }).className).toContain("active");
  });

  it.each([
    [GESTIONNAIRE, "Vérification du dossier 268202-0313"],
    [CONSULTANT, "Dossier 268202-0313"],
  ])("ouvre un dossier non validé selon le rôle de %o", async (utilisateur, attendu) => {
    rendreApplication(ROUTES.dossiers, utilisateur);

    const ligne = (await screen.findByText("268202-0313")).closest("tr")!;
    fireEvent.click(within(ligne).getByRole("button", { name: "Ouvrir" }));

    expect(await titre(attendu)).toBeTruthy();
  });
});

describe("écran d'import", () => {
  it("annonce la taille maximale acceptée par le backend et charge l'historique", async () => {
    rendreApplication(ROUTES.imports, GESTIONNAIRE);

    expect(screen.getByText(/20 Mo maximum chacun/)).toBeTruthy();
    expect(await screen.findByText("Aucun import pour l’instant.")).toBeTruthy();
  });

  it("dépose un PDF et affiche le résultat calculé par le serveur", async () => {
    serveur.onPost("/imports").reply(201, {
      lot: "11111111-1111-1111-1111-111111111111", statut: "TERMINE", total: 1, traites: 1, reussis: 1,
      aCompleter: 0, aVerifier: 0, pretsAValider: 1, refuses: 0, enErreur: 0,
      creeLe: "2026-10-22T10:00:00Z", termineLe: "2026-10-22T10:00:02Z",
      resultats: [{ nomFichier: "rapport.pdf", issue: "ACCEPTE", importId: 1, rapportId: 7, numeroRapport: "268202-0001",
        motif: null, statutDossier: "PRET_A_VALIDER", controlesConformes: 28, controlesTotal: 28, champsASaisir: 0 }],
    });
    rendreApplication(ROUTES.imports, GESTIONNAIRE);

    const pdf = new File(["%PDF-1.7"], "rapport.pdf", { type: "application/pdf" });
    fireEvent.change(screen.getByLabelText("Fichiers PDF à importer"), { target: { files: [pdf] } });

    expect(await screen.findByText("268202-0001")).toBeTruthy();
    expect(screen.getByText("28/28")).toBeTruthy();
    expect(screen.getAllByText("Prêt à valider").length).toBeGreaterThan(0);
    expect(serveur.history.post).toHaveLength(1);
  });

  it("ignore un fichier qui n'est pas un PDF sans l'envoyer", async () => {
    rendreApplication(ROUTES.imports, GESTIONNAIRE);

    const texte = new File(["texte"], "notes.txt", { type: "text/plain" });
    fireEvent.change(screen.getByLabelText("Fichiers PDF à importer"), { target: { files: [texte] } });

    expect(screen.getByText("Ignoré — notes.txt : pas un PDF")).toBeTruthy();
    expect(serveur.history.post).toHaveLength(0);
  });
});
