import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import type MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { ROUTES } from "@/app/routes";
import type { RecapitulatifLot } from "@/domain/imports";
import { simulerApi } from "@/test/api";
import { GESTIONNAIRE, rendreApplication } from "@/test/rendu";
import { decouperEnEnvois, fusionnerLots, OCTETS_PAR_ENVOI } from "./api";

let serveur: MockAdapter | undefined;

afterEach(() => {
  cleanup();
  serveur?.restore();
});

function pdf(nom: string, octets = 10) {
  return new File([new Uint8Array(octets)], nom, { type: "application/pdf" });
}

function lot(id: string, fichiers: number): RecapitulatifLot {
  return {
    lot: id, statut: "TERMINE", total: fichiers, traites: fichiers, reussis: fichiers, aCompleter: 0, aVerifier: 0,
    pretsAValider: fichiers, refuses: 0, enErreur: 0, creeLe: "2026-11-23T10:00:00Z", termineLe: "2026-11-23T10:01:00Z",
    resultats: Array.from({ length: fichiers }, (_, i) => ({
      nomFichier: `${id}-${i}.pdf`, issue: "ACCEPTE", importId: i, rapportId: i, numeroRapport: `${id}-${i}`, motif: null,
      statutDossier: "PRET_A_VALIDER", controlesConformes: 28, controlesTotal: 28, champsASaisir: 0,
    })),
  };
}

describe("dépôt volumineux (413)", () => {
  it("découpe en envois de 40 fichiers et de 100 Mo au plus", () => {
    const trois = decouperEnEnvois(Array.from({ length: 85 }, (_, i) => pdf(`r${i}.pdf`)));
    expect(trois.map((e) => e.length)).toEqual([40, 40, 5]);

    const gros = decouperEnEnvois([pdf("a.pdf", OCTETS_PAR_ENVOI / 2), pdf("b.pdf", OCTETS_PAR_ENVOI / 2), pdf("c.pdf", 1)]);
    expect(gros.map((e) => e.map((f) => f.name))).toEqual([["a.pdf", "b.pdf"], ["c.pdf"]]);
  });

  it("fusionne les récapitulatifs des envois", () => {
    const recap = fusionnerLots([lot("A", 40), { ...lot("B", 5), statut: "EN_COURS", traites: 2 }])!;
    expect(recap).toMatchObject({ statut: "EN_COURS", total: 45, traites: 42, reussis: 45, termineLe: null });
    expect(recap.resultats).toHaveLength(45);
  });

  it("envoie 85 PDF en 3 requêtes et affiche un seul résultat", async () => {
    let numero = 0;
    serveur = simulerApi((s) => s.onPost("/imports").reply((config) => {
      const fichiers = (config.data as FormData).getAll("fichiers").length;
      const recap = lot(`L${++numero}`, fichiers);
      s.onGet(`/imports/${recap.lot}`).reply(200, recap);
      return [202, recap];
    }));
    rendreApplication(ROUTES.imports, GESTIONNAIRE);

    const champ = await screen.findByLabelText("Fichiers PDF à importer");
    fireEvent.change(champ, { target: { files: Array.from({ length: 85 }, (_, i) => pdf(`r${i}.pdf`)) } });

    await waitFor(() => expect(serveur!.history.post.filter((r) => r.url === "/imports")).toHaveLength(3));
    expect(await screen.findByText(/85\/85 traités/)).toBeTruthy();
  });
});
