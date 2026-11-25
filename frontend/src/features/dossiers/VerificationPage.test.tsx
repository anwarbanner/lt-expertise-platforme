import { cleanup, fireEvent, screen, waitFor, within } from "@testing-library/react";
import type MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { ROUTES } from "@/app/routes";
import type { Controle, LigneDetail } from "@/domain/dossiers";
import type { StatutDossier } from "@/domain/referentiels";
import { dossierDetail, simulerApi } from "@/test/api";
import { GESTIONNAIRE, rendreApplication } from "@/test/rendu";

let serveur: MockAdapter;

afterEach(() => {
  cleanup();
  serveur.restore();
});

const NUMERO = "258202-2725";
const PRIX_FAUX: Controle = {
  type: "COHERENCE", code: "PAGE2_LIGNES_PRIX", champ: "lignesDetail", attendu: 2, obtenu: 1, conforme: false, bloquant: true,
  libelle: "Page 2 : quantité × prix × (1 + TVA) × (1 − vétusté) = total, pour chaque ligne — prix unitaire à corriger (ligne 2 CHEVILLE)",
};
const LIGNE = {
  zoneLibelle: "Central/Lateral droit", zoneChoc: 4, famille: "TOLERIE", typePiece: "ORIGINE", vetustePct: 10, tvaPct: 20,
  operation: "ECHANGE", nombreHeures: null, tarifHoraire: null, montantMoTtc: 0,
} as const;
const LIGNES: LigneDetail[] = [
  { ...LIGNE, ordre: 0, quantite: 1, designation: "AILE AR D", prixUnitaire: 1000, totalFourniture: 1080, prixCoherent: true, prixCalcule: 1000 },
  { ...LIGNE, ordre: 1, quantite: 3, designation: "CHEVILLE", prixUnitaire: 9.1, totalFourniture: 88.45, prixCoherent: false, prixCalcule: 27.3 },
];
const CHAMP_ABSENT: Controle = {
  type: "CHAMP_MANQUANT", code: "CHAMP_MANQUANT", champ: "adversaire.intermediaire", attendu: null, obtenu: null, conforme: false,
  bloquant: false, libelle: "Champ absent du PDF : adversaire.intermediaire",
};

function ouvrir(autres: Parameters<typeof dossierDetail>[2] = {}, statut: StatutDossier = "PRET_A_VALIDER") {
  const d = dossierDetail(NUMERO, statut, {
    zonesChoc: [7, 8], controles: [CHAMP_ABSENT], champsASaisir: ["adversaire.intermediaire"], ...autres,
  });
  serveur = simulerApi((s) => {
    s.onGet(`/dossiers/${NUMERO}`).reply(200, d);
    s.onPut(`/dossiers/${NUMERO}`).reply((config) => [200, { ...d, ...JSON.parse(config.data), version: d.version + 1 }]);
  });
  rendreApplication(ROUTES.verification(NUMERO), GESTIONNAIRE);
  return d;
}

describe("écran de vérification", () => {
  it("un champ absent du PDF est un avertissement qui ne bloque pas la validation", async () => {
    ouvrir();

    expect(await screen.findByText("1 champ(s) absent(s) du PDF")).toBeTruthy();
    expect((screen.getByRole("button", { name: "Valider le dossier" }) as HTMLButtonElement).disabled).toBe(false);
  });

  it("un prix unitaire faux bloque la validation ; la ligne est en rouge et son prix se corrige", async () => {
    ouvrir({ controles: [PRIX_FAUX], lignesDetail: LIGNES }, "A_VERIFIER");

    expect(await screen.findByText(/prix unitaire à corriger \(ligne 2 CHEVILLE\)/)).toBeTruthy();
    expect((screen.getByRole("button", { name: "Valider le dossier" }) as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByRole("button", { name: "Corriger les prix" }));

    const ligneFausse = screen.getByText("CHEVILLE").closest("tr")!;
    expect(ligneFausse.className).toContain("prix-faux");
    expect(screen.getByText("AILE AR D").closest("tr")!.className).not.toContain("prix-faux");
    expect(screen.queryByLabelText("Prix unitaire ligne 1")).toBeNull();
    fireEvent.click(within(ligneFausse).getByRole("button", { name: /27,30/ }));
    expect((screen.getByLabelText("Prix unitaire ligne 2") as HTMLInputElement).value).toBe("27.3");
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(serveur.history.put).toHaveLength(1));
    expect(JSON.parse(serveur.history.put[0].data).prixLignes).toEqual([{ ordre: 1, prixUnitaire: 27.3 }]);
  });

  it("coche plusieurs zones de choc et les envoie au serveur", async () => {
    ouvrir();

    const arriere = await screen.findByLabelText("7 · Arrière") as HTMLInputElement;
    expect(arriere.checked).toBe(true);
    expect((screen.getByLabelText("8 · Avant") as HTMLInputElement).checked).toBe(true);
    fireEvent.click(screen.getByLabelText("1 · Avant/Latéral gauche"));
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(serveur.history.put).toHaveLength(1));
    expect(JSON.parse(serveur.history.put[0].data).zonesChoc).toEqual([1, 7, 8]);
  });

  it("la vétusté est TTC ou HT : le type choisi change le champ envoyé et le calcul affiché", async () => {
    ouvrir({ vetusteTtc: 320 });

    fireEvent.click(await screen.findByRole("button", { name: /Conclusion/ }));
    expect(screen.getByText("Indemnisation = total général TTC − vétusté TTC")).toBeTruthy();
    fireEvent.change(screen.getByLabelText(/Type de vétusté/), { target: { value: "HT" } });
    expect((screen.getByLabelText(/^Vétusté HT/) as HTMLInputElement).value).toBe("320");
    expect(screen.getByText("Indemnisation = total général HT − vétusté HT")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(serveur.history.put).toHaveLength(1));
    const envoi = JSON.parse(serveur.history.put[0].data);
    expect(envoi.vetusteHt).toBe(320);
    expect(envoi.vetusteTtc).toBeNull();
  });

  it("l’épaviste retenu (réforme économique ou technique) se saisit sur un rapport de réforme", async () => {
    ouvrir({ natureRapport: "REFORME", typeReforme: "TECHNIQUE" });

    fireEvent.click(await screen.findByRole("button", { name: /Conclusion/ }));
    const reforme = screen.getByLabelText(/Épaviste retenu : réforme/) as HTMLSelectElement;
    expect(reforme.value).toBe("TECHNIQUE");
    fireEvent.change(reforme, { target: { value: "ECONOMIQUE" } });
    fireEvent.change(screen.getByLabelText(/Épaviste retenu : nom/), { target: { value: "EPAVISTE TEST" } });
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));

    await waitFor(() => expect(serveur.history.put).toHaveLength(1));
    const envoi = JSON.parse(serveur.history.put[0].data);
    expect(envoi.typeReforme).toBe("ECONOMIQUE");
    expect(envoi.epavisteNom).toBe("EPAVISTE TEST");
  });

  it("pas de champ épaviste sur un rapport qui n’est pas de réforme", async () => {
    ouvrir();

    fireEvent.click(await screen.findByRole("button", { name: /Conclusion/ }));
    expect(screen.queryByLabelText(/Épaviste retenu/)).toBeNull();
  });

  it("présente les valeurs clés du commentaire 2026", async () => {
    ouvrir({
      commentaire: "- LA VALEUR A NEUF DU VEHICULE A ETE RETENUE SUR LA BASE DU PRIX CATALOGUE DISPONIBLE : 519000.00",
      valeursCommentaire: [
        { cle: "VALEUR_NEUF_CATALOGUE", texte: null, montant: 519000, date: null },
        { cle: "DIVERGENCE_USAGE", texte: "NON", montant: null, date: null },
        { cle: "CADRE_EXPERTISE", texte: "FORFAITAIRE", montant: null, date: null },
      ],
    });

    fireEvent.click(await screen.findByRole("button", { name: /Conclusion/ }));

    expect(screen.getByText("Valeur à neuf (prix catalogue)")).toBeTruthy();
    expect(screen.getByText("519 000,00 DH")).toBeTruthy();
    expect(screen.getByText("Divergence usage / contrat constatée").nextElementSibling?.textContent).toBe("Non");
    expect(screen.getByText("FORFAITAIRE")).toBeTruthy();
  });
});
