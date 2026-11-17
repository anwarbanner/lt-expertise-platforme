import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import type MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { ROUTES } from "@/app/routes";
import { simulerApi } from "@/test/api";
import { CONSULTANT, GESTIONNAIRE, rendreApplication } from "@/test/rendu";
import { age } from "./ChampsFiltre";

let serveur: MockAdapter;

afterEach(() => {
  cleanup();
  serveur.restore();
});

const derniersParametres = (url: string) => serveur.history.get.filter((r) => r.url === url).at(-1)?.params;

describe("recherche de pièces", () => {
  it("affiche les pièces trouvées et transmet tous les filtres", async () => {
    serveur = simulerApi((s) => {
      s.onGet("/recherche/suggestions").reply(200, { marques: ["RENAULT"], modeles: ["CLIO"], typesMine: ["BJA"] });
      s.onGet("/recherche/pieces").reply(200, { nombre: 1, lignes: [{ numeroRapport: "268202-0284", designation: "PORTE AV D", typePiece: "REEMPLOI",
        zoneChoc: 4, marque: "RENAULT", modele: "CLIO", typeMine: "BJA", dateMiseCirculation: "2022-10-07", numeroChassis: "VF1234", prixUnitaire: 850, date: "2026-10-17" }] });
    });
    rendreApplication(ROUTES.recherchePieces, CONSULTANT);

    expect(await screen.findByText("PORTE AV D")).toBeTruthy();
    expect(screen.getByRole("cell", { name: "4 · Central/Latéral droit" })).toBeTruthy();
    expect(screen.getByText("VF1234")).toBeTruthy();

    fireEvent.change(screen.getByLabelText("Pièce"), { target: { value: "porte" } });
    fireEvent.change(screen.getByLabelText("Type de pièce"), { target: { value: "REEMPLOI" } });
    fireEvent.change(screen.getByLabelText("Zone de choc"), { target: { value: "4" } });
    fireEvent.change(screen.getByLabelText("Marque"), { target: { value: "RENAULT" } });
    fireEvent.change(screen.getByLabelText("Modèle"), { target: { value: "CLIO" } });
    fireEvent.change(screen.getByLabelText("Type mine"), { target: { value: "BJA" } });
    fireEvent.change(screen.getByLabelText("Année de MEC"), { target: { value: "2022" } });
    fireEvent.change(screen.getByLabelText("N° de châssis"), { target: { value: "VF1" } });

    await waitFor(() => expect(derniersParametres("/recherche/pieces")).toEqual({ piece: "porte", type: "REEMPLOI", zone: "4",
      marque: "RENAULT", modele: "CLIO", typeMine: "BJA", annee: "2022", chassis: "VF1", page: 0, taille: 25 }));
  });
});

describe("pagination des recherches", () => {
  it("envoie au serveur la page, le tri et les filtres de colonne", async () => {
    serveur = simulerApi((s) => s.onGet("/recherche/pieces").reply(200, { nombre: 80, page: 0, taille: 25, lignes: [{ numeroRapport: "268202-0284",
      designation: "PORTE AV D", typePiece: "REEMPLOI", zoneChoc: 4, marque: "RENAULT", modele: "CLIO", typeMine: "BJA",
      dateMiseCirculation: "2022-10-07", numeroChassis: "VF1234", prixUnitaire: 850, date: "2026-10-17" }] }));
    rendreApplication(ROUTES.recherchePieces, CONSULTANT);
    expect(await screen.findByText("1–25 sur 80")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "2" }));
    await waitFor(() => expect(derniersParametres("/recherche/pieces")).toMatchObject({ page: 1, taille: 25 }));
    fireEvent.click(screen.getByRole("button", { name: /PRIX UNITAIRE/ }));
    fireEvent.change(screen.getByLabelText("Filtrer MARQUE"), { target: { value: "ren" } });

    await waitFor(() => expect(derniersParametres("/recherche/pieces")).toEqual({ page: 0, taille: 25, tri: "prix,asc", f_marque: "ren" }));
  });
});

describe("valeur vénale / achat", () => {
  it("affiche l'âge en années et mois, et envoie l'âge saisi en mois", async () => {
    serveur = simulerApi((s) => s.onGet("/recherche/valeurs").reply(200, { nombre: 1, moyenneAchat: 250000, moyenneVenale: 120000,
      lignes: [{ numeroRapport: "268202-0284", marque: "RENAULT", modele: "CLIO", typeMine: "BJA", dateMiseCirculation: "2022-10-07",
        dateSinistre: "2026-10-05", ageMois: 47, valeurAchat: 250000, valeurVenale: 120000 }] }));
    rendreApplication(ROUTES.valeursVenales, GESTIONNAIRE);

    expect(await screen.findByText("3 ans 11 mois")).toBeTruthy();
    expect(screen.getAllByText("120 000,00 DH").length).toBeGreaterThan(0);

    fireEvent.change(screen.getByLabelText("Âge minimum en années"), { target: { value: "3" } });
    fireEvent.change(screen.getByLabelText("Âge minimum en mois"), { target: { value: "6" } });
    fireEvent.change(screen.getByLabelText("Âge maximum en années"), { target: { value: "5" } });
    fireEvent.change(screen.getByLabelText("DMEC du"), { target: { value: "2020-01-01" } });
    fireEvent.change(screen.getByLabelText("Valeur d’achat minimum"), { target: { value: "100000" } });
    fireEvent.change(screen.getByLabelText("Valeur vénale maximum"), { target: { value: "150000" } });

    await waitFor(() => expect(derniersParametres("/recherche/valeurs")).toEqual({ ageMin: "42", ageMax: "60", mecDu: "2020-01-01",
      achatMin: "100000", venaleMax: "150000", page: 0, taille: 25 }));
  });

  it.each([[47, "3 ans 11 mois"], [12, "1 an"], [5, "5 mois"], [0, "0 mois"], [null, "—"]])("âge de %o mois : « %s »", (mois, texte) => {
    expect(age(mois)).toBe(texte);
  });
});
