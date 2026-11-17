import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { cles, rechercherPieces } from "@/api/dossiers";
import { messageErreur } from "@/api/client";
import { ROUTES } from "@/app/routes";
import { PageHead } from "@/components/ui/PageHead";
import { ETAT_INITIAL, parametresTableau, TableauInteractif, type Colonne } from "@/components/ui/TableauInteractif";
import type { FiltresPieces, LignePiece } from "@/domain/dossiers";
import { date, montant, nombre } from "@/domain/format";
import { LIBELLES_TYPE_PIECE, ZONES_CHOC } from "@/domain/referentiels";
import { BarreFiltres, ChampsVehicule, ChampTexte, ChampTypePiece, useDiffere } from "./ChampsFiltre";

const VIDES: Required<FiltresPieces> = { piece: "", type: "", zone: "", marque: "", modele: "", typeMine: "", annee: "", chassis: "" };

/** Recherche des pièces relevées dans les rapports, par pièce, zone de choc et véhicule (MEC, châssis). */
export default function RecherchePiecesPage() {
  const navigate = useNavigate();
  const [saisie, setSaisie] = useState(VIDES);
  const filtres = useDiffere(saisie);
  const [tableau, setTableau] = useState(ETAT_INITIAL);
  const changer = (champ: keyof FiltresPieces, valeur: string) => { setSaisie((f) => ({ ...f, [champ]: valeur })); setTableau((t) => ({ ...t, page: 0 })); };
  const parametres = parametresTableau(tableau);
  const resultat = useQuery({ queryKey: cles.pieces({ ...filtres, ...parametres }), queryFn: () => rechercherPieces(filtres, parametres), placeholderData: keepPreviousData });
  const r = resultat.data;

  const colonnes: Colonne<LignePiece>[] = [
    { cle: "dossier", titre: "DOSSIER", texte: (l) => l.numeroRapport, rendu: (l) => <button className="link-button" onClick={() => navigate(ROUTES.dossier(l.numeroRapport))}>{l.numeroRapport}</button> },
    { cle: "piece", titre: "PIÈCE", texte: (l) => l.designation, rendu: (l) => <strong>{l.designation}</strong> },
    { cle: "type", titre: "TYPE", texte: (l) => LIBELLES_TYPE_PIECE[l.typePiece] ?? l.typePiece, rendu: (l) => <span className={`price-type ${l.typePiece === "ORIGINE" ? "part" : "supply"}`}>{LIBELLES_TYPE_PIECE[l.typePiece] ?? l.typePiece}</span> },
    { cle: "zone", titre: "ZONE DE CHOC", texte: (l) => l.zoneChoc === null ? "—" : `${l.zoneChoc} · ${ZONES_CHOC[l.zoneChoc] ?? "?"}` },
    { cle: "marque", titre: "MARQUE", texte: (l) => l.marque ?? "—" },
    { cle: "modele", titre: "MODÈLE", texte: (l) => l.modele ?? "—" },
    { cle: "typeMine", titre: "TYPE MINE", texte: (l) => l.typeMine ?? "—" },
    { cle: "mec", titre: "MEC", texte: (l) => date(l.dateMiseCirculation) },
    { cle: "chassis", titre: "N° DE CHÂSSIS", texte: (l) => l.numeroChassis ?? "—" },
    { cle: "prix", titre: "PRIX UNITAIRE", texte: (l) => montant(l.prixUnitaire), numerique: true },
    { cle: "date", titre: "DATE", texte: (l) => date(l.date) },
  ];

  return <main className="main pricing-page">
    <PageHead title="Recherche de pièces" subtitle="Retrouvez les pièces relevées dans les rapports, par véhicule et par zone de choc" />
    <BarreFiltres reinitialiser={() => { setSaisie(VIDES); setTableau(ETAT_INITIAL); }}>
      <ChampTexte libelle="Pièce" valeur={saisie.piece} changer={(v) => changer("piece", v)} placeholder="Ex. porte, optique…" />
      <ChampTypePiece valeur={saisie.type} changer={(v) => changer("type", v)} />
      <label><span>Zone de choc</span><select aria-label="Zone de choc" value={saisie.zone} onChange={(e) => changer("zone", e.target.value)}>
        <option value="">Toutes</option>{Object.entries(ZONES_CHOC).map(([code, libelle]) => <option key={code} value={code}>{code} · {libelle}</option>)}
      </select></label>
      <ChampsVehicule marque={saisie.marque} modele={saisie.modele} typeMine={saisie.typeMine} changer={changer} />
      <ChampTexte libelle="Année de MEC" type="number" valeur={saisie.annee} changer={(v) => changer("annee", v)} placeholder="Ex. 2022" />
      <ChampTexte libelle="N° de châssis" valeur={saisie.chassis} changer={(v) => changer("chassis", v)} placeholder="Tout ou partie" />
    </BarreFiltres>
    {resultat.isPending && <p className="etat-chargement">Recherche en cours…</p>}
    {resultat.isError && <p className="import-erreur" role="alert">{messageErreur(resultat.error, "Recherche des pièces indisponible.")}</p>}
    {r && <article className="card pricing-results">
      <div className="section-head"><div><h2>Pièces trouvées</h2><p>{nombre(r.nombre)} pièce{r.nombre !== 1 ? "s" : ""}</p></div></div>
      <TableauInteractif colonnes={colonnes} lignes={r.lignes} total={r.nombre} etat={tableau} changer={setTableau} cleLigne={(l, i) => `${l.numeroRapport}-${i}`} />
    </article>}
  </main>;
}
