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

describe("valeur vénale / achat", () => {
  it("propose le modèle et le type mine dans les valeurs connues, le modèle pouvant être vide", async () => {
    serveur = simulerApi((s) => s.onGet("/recherche/suggestions").reply((config) => [200, {
      marques: ["RENAULT"], modeles: ["CLIO", "MEGANE"], typesMine: config.params?.modele === "CLIO" ? ["BJA"] : ["BJA", "KZ0"] }]));
    rendreApplication(ROUTES.valeursVenales, CONSULTANT);

    const modele = await screen.findByLabelText("Modèle");
    await waitFor(() => expect(screen.getByRole("option", { name: "MEGANE" })).toBeTruthy());
    // Sans modèle : tous les types mine
    expect([...(screen.getByLabelText("Type mine") as HTMLSelectElement).options].map((o) => o.text)).toEqual(["Tous", "Non renseigné", "BJA", "KZ0"]);

    fireEvent.change(modele, { target: { value: "CLIO" } });
    await waitFor(() => expect([...(screen.getByLabelText("Type mine") as HTMLSelectElement).options].map((o) => o.text)).toEqual(["Tous", "Non renseigné", "BJA"]));
    expect(derniersParametres("/recherche/suggestions")).toEqual({ modele: "CLIO" });
    fireEvent.change(screen.getByLabelText("Type mine"), { target: { value: "BJA" } });
    await waitFor(() => expect(derniersParametres("/recherche/valeurs")).toEqual({ modele: "CLIO", typeMine: "BJA", page: 0, taille: 25 }));

    fireEvent.change(modele, { target: { value: "__VIDE__" } });
    await waitFor(() => expect(derniersParametres("/recherche/valeurs")).toEqual({ modele: "__VIDE__", page: 0, taille: 25 }));
  });

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
