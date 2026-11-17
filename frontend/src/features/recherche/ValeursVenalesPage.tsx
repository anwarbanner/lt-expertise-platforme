import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { cles, rechercherValeurs } from "@/api/dossiers";
import { messageErreur } from "@/api/client";
import { ROUTES } from "@/app/routes";
import { Kpi } from "@/components/ui/Kpi";
import { PageHead } from "@/components/ui/PageHead";
import { ETAT_INITIAL, parametresTableau, TableauInteractif, type Colonne } from "@/components/ui/TableauInteractif";
import type { FiltresValeurs, LigneValeur } from "@/domain/dossiers";
import { date, montant, montantArrondi, nombre } from "@/domain/format";
import { age, BarreFiltres, ChampsVehicule, ChampTexte, useDiffere } from "./ChampsFiltre";

const VIDES = {
  marque: "", modele: "", typeMine: "", mecDu: "", mecAu: "", ageMinAns: "", ageMinMois: "", ageMaxAns: "", ageMaxMois: "",
  achatMin: "", achatMax: "", venaleMin: "", venaleMax: "",
};
type Saisie = typeof VIDES;

/** Âge saisi en années et mois, envoyé en mois ; vide si aucun des deux n'est renseigné. */
function enMois(ans: string, mois: string): string {
  if (ans === "" && mois === "") return "";
  return String((Number(ans) || 0) * 12 + (Number(mois) || 0));
}

function versFiltres(s: Saisie): FiltresValeurs {
  return {
    marque: s.marque, modele: s.modele, typeMine: s.typeMine, mecDu: s.mecDu, mecAu: s.mecAu,
    ageMin: enMois(s.ageMinAns, s.ageMinMois), ageMax: enMois(s.ageMaxAns, s.ageMaxMois),
    achatMin: s.achatMin, achatMax: s.achatMax, venaleMin: s.venaleMin, venaleMax: s.venaleMax,
  };
}

/**
 * Recherche des valeurs d'achat (valeur à neuf) et vénales lues dans le commentaire de l'expert, par véhicule, date
 * de mise en circulation (DMEC) et âge au sinistre (de la DMEC à la date du sinistre).
 */
export default function ValeursVenalesPage() {
  const navigate = useNavigate();
  const [saisie, setSaisie] = useState(VIDES);
  const filtres = useDiffere(versFiltres(saisie));
  const [tableau, setTableau] = useState(ETAT_INITIAL);
  const changer = (champ: keyof Saisie, valeur: string) => { setSaisie((f) => ({ ...f, [champ]: valeur })); setTableau((t) => ({ ...t, page: 0 })); };
  const parametres = parametresTableau(tableau);
  const resultat = useQuery({ queryKey: cles.valeurs({ ...filtres, ...parametres }), queryFn: () => rechercherValeurs(filtres, parametres), placeholderData: keepPreviousData });
  const r = resultat.data;

  const colonnes: Colonne<LigneValeur>[] = [
    { cle: "dossier", titre: "DOSSIER", texte: (l) => l.numeroRapport, rendu: (l) => <button className="link-button" onClick={() => navigate(ROUTES.dossier(l.numeroRapport))}>{l.numeroRapport}</button> },
    { cle: "marque", titre: "MARQUE", texte: (l) => l.marque ?? "—" },
    { cle: "modele", titre: "MODÈLE", texte: (l) => l.modele ?? "—" },
    { cle: "typeMine", titre: "TYPE MINE", texte: (l) => l.typeMine ?? "—" },
    { cle: "dmec", titre: "DMEC", texte: (l) => date(l.dateMiseCirculation) },
    { cle: "sinistre", titre: "DATE DU SINISTRE", texte: (l) => date(l.dateSinistre) },
    { cle: "age", titre: "ÂGE", texte: (l) => age(l.ageMois) },
    { cle: "achat", titre: "VALEUR D’ACHAT", texte: (l) => montant(l.valeurAchat), numerique: true },
    { cle: "venale", titre: "VALEUR VÉNALE", texte: (l) => montant(l.valeurVenale), rendu: (l) => <strong>{montant(l.valeurVenale)}</strong>, numerique: true },
  ];

  return <main className="main pricing-page">
    <PageHead title="Valeur vénale / achat" subtitle="Valeurs d’achat et vénales relevées dans le commentaire de l’expert" />
    <BarreFiltres reinitialiser={() => { setSaisie(VIDES); setTableau(ETAT_INITIAL); }}>
      <ChampsVehicule marque={saisie.marque} modele={saisie.modele} typeMine={saisie.typeMine} changer={changer} />
      <ChampTexte libelle="DMEC du" type="date" valeur={saisie.mecDu} changer={(v) => changer("mecDu", v)} />
      <ChampTexte libelle="DMEC au" type="date" valeur={saisie.mecAu} changer={(v) => changer("mecAu", v)} />
      <fieldset className="champ-double"><legend>Âge minimum (ans / mois)</legend>
        <input aria-label="Âge minimum en années" type="number" min={0} value={saisie.ageMinAns} onChange={(e) => changer("ageMinAns", e.target.value)} placeholder="ans" />
        <input aria-label="Âge minimum en mois" type="number" min={0} max={11} value={saisie.ageMinMois} onChange={(e) => changer("ageMinMois", e.target.value)} placeholder="mois" />
      </fieldset>
      <fieldset className="champ-double"><legend>Âge maximum (ans / mois)</legend>
        <input aria-label="Âge maximum en années" type="number" min={0} value={saisie.ageMaxAns} onChange={(e) => changer("ageMaxAns", e.target.value)} placeholder="ans" />
        <input aria-label="Âge maximum en mois" type="number" min={0} max={11} value={saisie.ageMaxMois} onChange={(e) => changer("ageMaxMois", e.target.value)} placeholder="mois" />
      </fieldset>
      <fieldset className="champ-double"><legend>Valeur d’achat (DH)</legend>
        <input aria-label="Valeur d’achat minimum" type="number" min={0} value={saisie.achatMin} onChange={(e) => changer("achatMin", e.target.value)} placeholder="min" />
        <input aria-label="Valeur d’achat maximum" type="number" min={0} value={saisie.achatMax} onChange={(e) => changer("achatMax", e.target.value)} placeholder="max" />
      </fieldset>
      <fieldset className="champ-double"><legend>Valeur vénale (DH)</legend>
        <input aria-label="Valeur vénale minimum" type="number" min={0} value={saisie.venaleMin} onChange={(e) => changer("venaleMin", e.target.value)} placeholder="min" />
        <input aria-label="Valeur vénale maximum" type="number" min={0} value={saisie.venaleMax} onChange={(e) => changer("venaleMax", e.target.value)} placeholder="max" />
      </fieldset>
    </BarreFiltres>
    {resultat.isPending && <p className="etat-chargement">Recherche en cours…</p>}
    {resultat.isError && <p className="import-erreur" role="alert">{messageErreur(resultat.error, "Recherche des valeurs indisponible.")}</p>}
    {r && <>
      <section className="kpi-grid kpi-trois">
        <Kpi label="Dossiers trouvés" value={nombre(r.nombre)} icon="folder" />
        <Kpi label="Valeur d’achat moyenne" value={montantArrondi(r.moyenneAchat)} icon="car" tone="blue" />
        <Kpi label="Valeur vénale moyenne" value={montantArrondi(r.moyenneVenale)} icon="chart" tone="green" />
      </section>
      <article className="card pricing-results">
        <div className="section-head"><div><h2>Valeurs relevées</h2><p>{nombre(r.nombre)} dossier{r.nombre !== 1 ? "s" : ""}</p></div></div>
        <TableauInteractif colonnes={colonnes} lignes={r.lignes} total={r.nombre} etat={tableau} changer={setTableau} cleLigne={(l) => l.numeroRapport} />
      </article>
    </>}
  </main>;
}
