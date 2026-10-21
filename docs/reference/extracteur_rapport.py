"""
Prototype d'extraction des rapports d'expertise automobile (gabarit TCPDF, 2 pages).

- Extrait automatiquement tous les champs chiffrés.
- Les champs texte masqués (XXXX) ou vides sont mis à None et listés dans "a_saisir"
  pour être complétés manuellement dans l'application.
- Vérifie la cohérence des montants (totaux, page 1 vs page 2, montant en lettres).

Usage :
    python extracteur_rapport.py rapport4.pdf rapport5.pdf     -> JSON + CSV dans ./sortie
    python extracteur_rapport.py dossier_des_pdf/
"""
import csv
import hashlib
import json
import re
import sys
import unicodedata
from pathlib import Path

import pymupdf

GABARIT_VERSION = "tcpdf-expertise-v1"
TOLERANCE = 0.05


# ---------------------------------------------------------------- utilitaires

def norm(s):
    """Minuscules, sans accents ni caractères mal encodés (le PDF perd les accents)."""
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode()
    return re.sub(r"\s+", " ", s.lower()).strip()


def nombre(s):
    """'1200.00' / ':85' / ':0.00' -> float ; sinon None."""
    if s is None:
        return None
    m = re.search(r"-?\d+(?:[.,]\d+)?", s)
    return float(m.group().replace(",", ".")) if m else None


def valeur_texte(s):
    """Retourne None si la valeur est masquée (XXXX, XX/XX/XXXX) ou vide."""
    if s is None:
        return None
    s = s.strip(" :")
    if not s or re.fullmatch(r"[Xx/ ]+", s) or s.upper() == "NC":
        return None
    return s


def lignes(page):
    """Regroupe les mots par ligne visuelle : [(y, [(x, mot), ...]), ...]."""
    groupes = []
    for x0, y0, x1, y1, mot, *_ in page.get_text("words"):
        y = (y0 + y1) / 2
        for g in groupes:
            if abs(g[0] - y) <= 2.5:
                g[1].append((x0, mot))
                break
        else:
            groupes.append([y, [(x0, mot)]])
    groupes.sort(key=lambda g: g[0])
    return [(y, sorted(mots)) for y, mots in groupes]


def texte(mots, xmin=0, xmax=9999):
    return " ".join(m for x, m in mots if xmin <= x < xmax)


def plat(s):
    """Comme norm() mais conserve la longueur, pour retrouver la position dans l'original."""
    return "".join((unicodedata.normalize("NFKD", c).encode("ascii", "ignore").decode() or c)[0]
                   for c in s).lower()


def apres(ligne_txt, etiquette, fin=None):
    """Valeur qui suit 'etiquette :' (jusqu'à 'fin'), avec la casse et les accents d'origine."""
    motif = re.escape(etiquette) + r"\s*:?\s*(.*?)" + (r"\s*" + re.escape(fin) if fin else r"$")
    m = re.search(motif, plat(ligne_txt))
    return ligne_txt[m.start(1):m.end(1)].strip(" :") if m else None


# ------------------------------------------------------ montant en lettres

UNITES = {"zero": 0, "un": 1, "une": 1, "deux": 2, "trois": 3, "quatre": 4, "cinq": 5,
          "six": 6, "sept": 7, "huit": 8, "neuf": 9, "dix": 10, "onze": 11, "douze": 12,
          "treize": 13, "quatorze": 14, "quinze": 15, "seize": 16, "vingt": 20,
          "vingts": 20, "trente": 30, "quarante": 40, "cinquante": 50, "soixante": 60}


def lettres_vers_nombre(txt):
    """'deux mille quatre cents dirhams' -> 2400. Gère cent(s), mille, million(s)."""
    mots = re.split(r"[\s-]+", norm(txt).split("dirham")[0])
    total, courant = 0, 0
    for m in mots:
        if m in ("", "et"):
            continue
        if m in UNITES:
            v = UNITES[m]
            # quatre-vingt : 4 * 20
            courant = courant * 20 if v == 20 and courant == 4 else courant + v
        elif m in ("cent", "cents"):
            courant = max(courant, 1) * 100
        elif m == "mille":
            total += max(courant, 1) * 1000
            courant = 0
        elif m in ("million", "millions"):
            total += max(courant, 1) * 1_000_000
            courant = 0
        else:
            return None
    return total + courant


# ------------------------------------------------------------ page 1

POSTES_MO = ["mo 1 tolerie", "mo 2 tolerie", "mo 3 tolerie", "mo peinture",
             "mo 1 mecanique", "mo 2 mecanique", "mo 3 mecanique"]
POSTES_FOURN = {"pieces": "pieces", "ingredient peinture": "ingredient_peinture",
                "petite fourniture": "petite_fourniture", "autres": "autres"}
TOTAUX = {"total main d'oeuvre": "total_main_oeuvre",
          "total fournitures (dh)": "total_fournitures", "total general": "total_general"}


def parse_page1(page, r):
    for y, mots in lignes(page):
        brut = " ".join(texte(mots).split())     # texte d'origine (pour les valeurs)
        t = norm(brut)                           # texte normalisé (pour les étiquettes)

        if t.startswith("rapport d'expertise"):
            r["numero_rapport"] = apres(brut, "rapport d'expertise")
        elif re.fullmatch(r"(procedure classique|wafa 24 heures|.*procedure.*)", t):
            r["procedure"] = texte(mots)
        elif t.startswith("cabinet"):
            r["cabinet"] = valeur_texte(texte(mots, 50, 430))
            r["expert"] = valeur_texte(texte(mots, 470))
        elif t.startswith("ref sinistre"):
            r["ref_sinistre"] = valeur_texte(apres(brut, "ref sinistre", "date sinistre"))
            r["date_sinistre"] = valeur_texte(apres(brut, "date sinistre", "date mission"))
            r["date_mission"] = valeur_texte(apres(brut, "date mission", "assurance"))
            r["compagnie"] = valeur_texte(texte(mots, 380))

        # Assuré (x~174) / Adversaire (x~364)
        for lib, cle in [("nom & prenom", "nom"), ("vehicule", "vehicule"),
                         ("immatriculation", "immatriculation"), ("cie d'assurance", "compagnie"),
                         ("n police", "num_police"), ("intermediaire", "intermediaire")]:
            if t.startswith(lib) and y < 190:
                r["assure"][cle] = valeur_texte(texte(mots, 170, 360))
                r["adversaire"][cle] = valeur_texte(texte(mots, 360))

        if t.startswith("nature de contrat"):
            r["nature_contrat"] = valeur_texte(apres(brut, "nature de contrat"))
        elif t.startswith("reparateur"):
            r["reparateur"] = valeur_texte(apres(brut, "reparateur", "accord"))
        elif t.startswith("adresse") and "oui" in t:
            r["accord_reparateur"] = bool(re.search(r"oui \( ?x ?\)", t))

        # Caractéristiques du véhicule
        elif t.startswith("marque"):
            r["vehicule"]["marque"] = valeur_texte(texte(mots, 100, 220))
            r["vehicule"]["immatriculation"] = valeur_texte(texte(mots, 300, 420))
        elif t.startswith("modele"):
            r["vehicule"]["modele"] = valeur_texte(texte(mots, 100, 220))
            r["vehicule"]["num_serie"] = valeur_texte(texte(mots, 300))
        elif t.startswith("type mine"):
            r["vehicule"]["type_mine"] = valeur_texte(texte(mots, 100, 220))
            r["vehicule"]["puissance"] = nombre(valeur_texte(texte(mots, 300)))
        elif t.startswith("energie"):
            r["vehicule"]["energie"] = valeur_texte(texte(mots, 100, 220))
            r["vehicule"]["dmc"] = valeur_texte(texte(mots, 300))
        elif t.startswith("kilometrage"):
            km = valeur_texte(texte(mots, 100, 220))
            r["vehicule"]["kilometrage"] = nombre(km) if km and re.search(r"\d", km) else None
            r["vehicule"]["kilometrage_note"] = km if km and not re.search(r"\d", km) else None
            r["vehicule"]["etat_general"] = valeur_texte(texte(mots, 300))
        elif t.startswith("usure pneus"):
            r["vehicule"]["usure_avg"] = nombre(texte(mots, 110, 220))
            r["vehicule"]["usure_avd"] = nombre(texte(mots, 220, 400))
            zone = texte(mots, 400)
            m = re.match(r"(\d+)\s*:\s*(.+)", zone)
            if m:
                r["vehicule"]["zone_choc_code"] = int(m.group(1))
                r["vehicule"]["zone_choc_libelle"] = m.group(2).strip()
        elif t.startswith("arg"):
            r["vehicule"]["usure_arg"] = nombre(texte(mots, 110, 220))
            r["vehicule"]["usure_ard"] = nombre(texte(mots, 220, 400))

        elif t.startswith("date expertise vehicule"):
            r["date_expertise"] = valeur_texte(apres(brut, "date expertise vehicule"))
        elif t.startswith("avant reparation"):
            r["date_avant_reparation"] = valeur_texte(texte(mots, 130, 220))
            r["date_en_cours_travaux"] = valeur_texte(texte(mots, 300, 380))
            r["date_apres_travaux"] = valeur_texte(texte(mots, 455))

        # Évaluation des dommages
        elif any(t.startswith(p) for p in POSTES_MO):
            poste = next(p for p in POSTES_MO if t.startswith(p))
            v = [nombre(m) for x, m in mots if x > 140]
            r["evaluation_mo"].append({"poste": poste.upper(), "nbr_h": v[0], "prix_unitaire": v[1],
                                       "total_ht": v[2], "tva": v[3], "total_ttc": v[4]})
        elif any(t.startswith(p) for p in POSTES_FOURN) and len(mots) >= 3 and y > 400:
            cle = next(c for p, c in POSTES_FOURN.items() if t.startswith(p))
            v = [nombre(m) for x, m in mots if x > 340]
            r["fournitures"][cle] = {"ht": v[0], "tva": v[1], "ttc": v[2]}
        elif any(t.startswith(p) for p in TOTAUX):
            cle = next(c for p, c in TOTAUX.items() if t.startswith(p))
            v = [nombre(m) for x, m in mots if x > 340]
            r["totaux"][cle] = {"ht": v[0], "tva": v[1], "ttc": v[2]}

        elif t.startswith("a deduire"):
            r["vetuste_ttc"] = nombre(texte(mots, 280))
        elif t.startswith("immobilisation"):
            r["immobilisation"] = nombre(texte(mots, 100, 280))
            r["depannage"] = nombre(texte(mots, 280))
        elif "reformeeconomique" in t.replace(" ", ""):
            tt = t.replace(" ", "")
            r["reforme"] = ("economique" if "economique(x)" in tt else
                            "technique" if "technique(x)" in tt else None)
        elif t.startswith("valeur"):
            lib = {"valeur a neuf": "valeur_neuf", "valeur neuf": "valeur_neuf",
                   "valeur assuree": "valeur_assuree", "valeur venale": "valeur_venale",
                   "valeur epave": "valeur_epave"}
            for k, cle in lib.items():
                if t.startswith(k):
                    r[cle] = nombre(valeur_texte(apres(brut, k)))
        elif t.startswith("recuperabilite de tva"):
            r["tva_recuperable"] = "non recuperable" not in t
        elif t.startswith("montant de l'indemnisation"):
            r["montant_indemnisation"] = nombre(texte(mots, 220))
        elif "dirham" in t:
            r["montant_lettres"] = texte(mots)


# ------------------------------------------------------------ page 2

# Colonnes du tableau "Détail Fourniture / Travaux", dans l'ordre de l'en-tête.
# Leur position varie d'un rapport à l'autre : les bornes sont lues sur l'en-tête.
ENTETE_P2 = [("qte", "qte"), ("designation", "designation"), ("type_piece", "type"),
             ("prix", "prix"), ("vetuste_pct", "vetuste%"), ("tva_pct", "tva"),
             ("total", "total"), ("travail", "designation"), ("nbr_h", "nbr"),
             ("tarif", "tarif"), ("montant_ttc", "montant")]
NUMERIQUES_P2 = {"qte", "prix", "vetuste_pct", "tva_pct", "total", "nbr_h", "tarif", "montant_ttc"}


def colonnes_depuis_entete(mots):
    """[(cle, xmin, xmax), ...] à partir des positions des titres de colonnes."""
    debuts, i = [], 0
    for x, m in mots:
        if i < len(ENTETE_P2) and norm(m) == ENTETE_P2[i][1]:
            debuts.append((ENTETE_P2[i][0], x - 3))
            i += 1
    if i != len(ENTETE_P2):
        raise ValueError("En-tete du tableau page 2 non reconnu (gabarit modifie ?)")
    return [(cle, x, debuts[k + 1][1] if k + 1 < len(debuts) else 9999)
            for k, (cle, x) in enumerate(debuts)]


def parse_page2(page, r):
    zone, categorie, colonnes = None, None, None
    dans_tableau = False
    for y, mots in lignes(page):
        t = norm(texte(mots))
        if t.startswith("qte designation"):
            colonnes = colonnes_depuis_entete(mots)
            dans_tableau = True
            continue
        if not dans_tableau:
            continue
        if t.startswith("o: piece") or t.startswith("commentaire"):
            dans_tableau = False
            continue
        premier = mots[0][1]
        if not premier.isdigit():                # ligne de section
            lib = texte(mots, 0, 260).strip(" .�-·")
            if re.match(r"^\w", premier):        # "Arriere" -> zone du véhicule
                zone, categorie = lib, None
            else:                                # "- Peinture" -> catégorie
                categorie = lib
            continue
        ligne = {}
        for cle, xmin, xmax in colonnes:
            val = texte(mots, xmin, xmax) or None
            ligne[cle] = nombre(val) if cle in NUMERIQUES_P2 else val
        ligne["zone"], ligne["categorie"] = zone, categorie
        r["lignes_detail"].append(ligne)

    for y, mots in lignes(page):
        if norm(texte(mots)).startswith("commentaire rapport"):
            suivantes = [texte(m) for yy, m in lignes(page) if y < yy < 800]
            r["commentaire"] = valeur_texte(" ".join(suivantes))


# ------------------------------------------------------------ contrôles

def controles(r):
    res = []

    def check(nom, attendu, obtenu):
        ok = attendu is not None and obtenu is not None and abs(attendu - obtenu) <= TOLERANCE
        res.append({"controle": nom, "attendu": attendu, "obtenu": obtenu, "ok": ok})

    mo, tot, fo = r["evaluation_mo"], r["totaux"], r["fournitures"]
    for l in mo:
        check(f"{l['poste']} : heures x prix = HT", round(l["nbr_h"] * l["prix_unitaire"], 2), l["total_ht"])
        check(f"{l['poste']} : HT + TVA = TTC", round(l["total_ht"] + l["tva"], 2), l["total_ttc"])
    for c in ("ht", "tva", "ttc"):
        check(f"Somme MO = Total main d'oeuvre ({c})", round(sum(l[f"total_{c}" if c != "tva" else "tva"] for l in mo), 2),
              tot["total_main_oeuvre"][c])
        check(f"Somme fournitures = Total fournitures ({c})", round(sum(v[c] for v in fo.values()), 2),
              tot["total_fournitures"][c])
        check(f"MO + fournitures = Total general ({c})",
              round(tot["total_main_oeuvre"][c] + tot["total_fournitures"][c], 2), tot["total_general"][c])

    check("Total general TTC - vetuste = indemnisation",
          round(tot["total_general"]["ttc"] - (r["vetuste_ttc"] or 0), 2), r["montant_indemnisation"])
    if r.get("montant_lettres"):
        check("Montant en lettres = montant en chiffres",
              lettres_vers_nombre(r["montant_lettres"]), r["montant_indemnisation"])

    # Page 2 vs page 1
    lignes_p2 = r["lignes_detail"]
    check("Page 2 : somme main d'oeuvre TTC = page 1",
          round(sum(l["montant_ttc"] or 0 for l in lignes_p2), 2), tot["total_main_oeuvre"]["ttc"])
    check("Page 2 : somme fournitures = page 1 (TTC)",
          round(sum(l["total"] or 0 for l in lignes_p2), 2), tot["total_fournitures"]["ttc"])
    return res


# ------------------------------------------------------------ orchestration

def rapport_vide():
    return {"assure": {}, "adversaire": {}, "vehicule": {}, "evaluation_mo": [],
            "fournitures": {}, "totaux": {}, "lignes_detail": []}


def champs_a_saisir(d, prefixe=""):
    """Liste les champs texte restés vides (masqués ou absents)."""
    out = []
    for k, v in d.items():
        chemin = f"{prefixe}{k}"
        if isinstance(v, dict):
            out += champs_a_saisir(v, chemin + ".")
        elif v is None and not k.startswith("valeur_") and k not in ("reforme", "kilometrage_note",
                                                                     "immobilisation", "puissance"):
            out.append(chemin)
    return out


def extraire(chemin_pdf):
    doc = pymupdf.open(chemin_pdf)
    if doc.page_count < 2 or "RAPPORT D'EXPERTISE" not in doc[0].get_text():
        raise ValueError("Gabarit non reconnu : le fichier doit être vérifié manuellement.")
    r = rapport_vide()
    r["fichier"] = Path(chemin_pdf).name
    r["hash_sha256"] = hashlib.sha256(Path(chemin_pdf).read_bytes()).hexdigest()
    r["gabarit"] = GABARIT_VERSION
    parse_page1(doc[0], r)
    parse_page2(doc[1], r)
    r["controles"] = controles(r)
    r["a_saisir"] = champs_a_saisir({k: v for k, v in r.items()
                                     if k not in ("evaluation_mo", "lignes_detail", "controles")})
    nb_ko = sum(not c["ok"] for c in r["controles"])
    r["statut"] = "a_verifier" if nb_ko else ("a_completer" if r["a_saisir"] else "valide")
    return r


def main(args):
    fichiers = []
    for a in args or ["."]:
        p = Path(a)
        fichiers += sorted(p.glob("*.pdf")) if p.is_dir() else [p]
    sortie = Path("sortie")
    sortie.mkdir(exist_ok=True)
    resume = []
    for f in fichiers:
        try:
            r = extraire(f)
        except Exception as e:
            print(f"[ERREUR] {f.name} : {e}")
            continue
        (sortie / f"{f.stem}.json").write_text(json.dumps(r, ensure_ascii=False, indent=2), encoding="utf-8")
        ko = [c for c in r["controles"] if not c["ok"]]
        print(f"[{r['statut'].upper():11}] {f.name}  n°{r['numero_rapport']}  "
              f"indemnisation={r['montant_indemnisation']} Dh  "
              f"controles={len(r['controles']) - len(ko)}/{len(r['controles'])} OK  "
              f"champs a saisir={len(r['a_saisir'])}")
        for c in ko:
            print(f"      ! {c['controle']} : attendu {c['attendu']}, trouve {c['obtenu']}")
        t = r["totaux"]
        resume.append({
            "fichier": f.name, "numero_rapport": r["numero_rapport"], "procedure": r.get("procedure"),
            "nature_contrat": r.get("nature_contrat"), "zone_choc": r["vehicule"].get("zone_choc_libelle"),
            "mo_ht": t["total_main_oeuvre"]["ht"], "mo_ttc": t["total_main_oeuvre"]["ttc"],
            "fournitures_ttc": t["total_fournitures"]["ttc"], "tva": t["total_general"]["tva"],
            "total_general_ttc": t["total_general"]["ttc"], "vetuste_ttc": r.get("vetuste_ttc"),
            "indemnisation": r["montant_indemnisation"], "statut": r["statut"],
        })
    if resume:
        with open(sortie / "resume.csv", "w", newline="", encoding="utf-8-sig") as fh:
            w = csv.DictWriter(fh, fieldnames=resume[0].keys(), delimiter=";")
            w.writeheader()
            w.writerows(resume)
        print(f"\n-> {len(resume)} rapport(s) exporte(s) dans {sortie.resolve()}")


if __name__ == "__main__":
    main(sys.argv[1:])
