package ma.ltexpertise.extraction;

import ma.ltexpertise.commun.Montants;
import ma.ltexpertise.dossier.referentiel.CategorieFourniture;
import ma.ltexpertise.dossier.referentiel.Energie;
import ma.ltexpertise.dossier.referentiel.FamilleTravaux;
import ma.ltexpertise.dossier.referentiel.NatureContrat;
import ma.ltexpertise.dossier.referentiel.NatureEvenement;
import ma.ltexpertise.dossier.referentiel.NatureRapport;
import ma.ltexpertise.dossier.referentiel.PosteMainOeuvre;
import ma.ltexpertise.dossier.referentiel.TypeOperation;
import ma.ltexpertise.dossier.referentiel.TypePiece;
import ma.ltexpertise.dossier.referentiel.TypeProcedure;
import ma.ltexpertise.dossier.referentiel.TypeReforme;
import ma.ltexpertise.dossier.referentiel.ZoneChoc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ma.ltexpertise.extraction.Textes.date;
import static ma.ltexpertise.extraction.Textes.extraire;
import static ma.ltexpertise.extraction.Textes.nombre;
import static ma.ltexpertise.extraction.Textes.norm;
import static ma.ltexpertise.extraction.Textes.plat;

/**
 * Lecture du gabarit « TcpdfExpertiseV1 », portage de {@code docs/reference/extracteur_rapport.py}.
 *
 * <p>Page 1 : champs repérés par leur étiquette, tableau « Évaluation des dommages ». Les valeurs suivant une
 * étiquette sur la même ligne sont lues par motif plutôt que par abscisse : le bloc conclusion se décale d'un
 * rapport à l'autre. Les colonnes (assuré / adversaire, caractéristiques du véhicule, montants) gardent les
 * bornes du prototype, stables sur les rapports analysés.
 *
 * <p>Page 2 et suivantes : bornes des colonnes calculées sur la ligne d'en-tête du tableau (elles varient d'un
 * rapport à l'autre), lignes de section « zone » et « catégorie », puis commentaire de l'expert.
 */
public class ParseurTcpdfExpertiseV1 {

    public static final String GABARIT = "tcpdf-expertise-v1";

    private static final Pattern TITRE = Pattern.compile(
            "rapport\\s+d['’]?expertise(?:\\s+(non\\s+definitif|reforme))?\\s*:\\s*(\\S+)");
    private static final Pattern CABINET = Pattern.compile("cabinet\\s*:\\s*(.*?)\\s*expert\\s*:\\s*(.*)$");
    private static final Pattern SINISTRE = Pattern.compile(
            "ref\\s+sinistre\\s*:\\s*(.*?)\\s*date\\s+sinistre\\s*:\\s*(.*?)\\s*date\\s+mission\\s*:\\s*(.*?)"
                    + "\\s*assurance\\s*:\\s*(.*)$");
    private static final Pattern NATURE_CONTRAT = Pattern.compile("nature\\s+de\\s+contrat\\s*:?\\s*(.*)$");
    private static final Pattern REPARATEUR = Pattern.compile("reparateur\\s*:\\s*(.*?)\\s*accord");
    private static final Pattern NATURE_EVENEMENT = Pattern.compile("n\\.e\\s*:\\s*(.*)$");
    /** Entrée de la liste des zones de choc, colonne de droite de la page 1 : « 7 : Arriere ». */
    private static final Pattern ZONE = Pattern.compile("^([1-8])\\s*:\\s*([a-z][a-z /]*)$");
    private static final Pattern DATE_EXPERTISE = Pattern.compile("date\\s+expertise\\s+vehicule\\s*:?\\s*(\\S*)");
    private static final Pattern DATES_TRAVAUX = Pattern.compile(
            "avant\\s+reparation\\s*:?\\s*(\\S*)\\s*en\\s+cours\\s+travaux\\s*:?\\s*(\\S*)\\s*apres\\s+travaux\\s*:?\\s*(\\S*)");
    /** « Vétusté TTC », ou « Vétusté HT » quand la TVA est récupérable. */
    private static final Pattern VETUSTE = Pattern.compile("vetuste\\s+(ht|ttc)\\s*:\\s*(-?[\\d.,]+)");
    private static final Pattern DEPANNAGE = Pattern.compile("depannage\\s*:\\s*(-?[\\d.,]+)");
    private static final Pattern VALEUR = Pattern.compile("valeur\\s+(a\\s+neuf|neuf|assuree|venale|epave)\\s*:\\s*(.*)$");
    private static final Pattern INDEMNISATION = Pattern.compile("indemnisation\\s*:\\s*(-?[\\d.,]+)");
    private static final Pattern FAIT_A = Pattern.compile("fait\\s+a\\s+(.*?)\\s+le\\s*(\\S*)\\s*$");
    private static final Pattern QUANTITE = Pattern.compile("\\d+(?:[.,]\\d+)?");
    private static final Pattern COMMENTAIRE_MASQUE = Pattern.compile("[-\\sXx/.]+");

    private static final Map<String, PosteMainOeuvre> POSTES = new LinkedHashMap<>();
    private static final Map<String, CategorieFourniture> FOURNITURES = new LinkedHashMap<>();

    static {
        POSTES.put("mo 1 tolerie", PosteMainOeuvre.MO_1_TOLERIE);
        POSTES.put("mo 2 tolerie", PosteMainOeuvre.MO_2_TOLERIE);
        POSTES.put("mo 3 tolerie", PosteMainOeuvre.MO_3_TOLERIE);
        POSTES.put("mo peinture", PosteMainOeuvre.MO_PEINTURE);
        POSTES.put("mo 1 mecanique", PosteMainOeuvre.MO_1_MECANIQUE);
        POSTES.put("mo 2 mecanique", PosteMainOeuvre.MO_2_MECANIQUE);
        POSTES.put("mo 3 mecanique", PosteMainOeuvre.MO_3_MECANIQUE);
        FOURNITURES.put("pieces", CategorieFourniture.PIECES);
        FOURNITURES.put("ingredient peinture", CategorieFourniture.INGREDIENT_PEINTURE);
        FOURNITURES.put("petite fourniture", CategorieFourniture.PETITE_FOURNITURE);
        FOURNITURES.put("autres", CategorieFourniture.AUTRES);
    }

    /** Titres des colonnes du tableau de la page 2, dans l'ordre de l'en-tête (« Désignation » y figure deux fois). */
    private static final String[] TITRES_COLONNES =
            {"qte", "designation", "type", "prix", "vetuste", "tva", "total", "designation", "nbr", "tarif", "montant"};
    private static final int QTE = 0, DESIGNATION = 1, TYPE = 2, PRIX = 3, VETUSTE_PCT = 4, TVA = 5, TOTAL = 6,
            TRAVAIL = 7, NBR = 8, TARIF = 9, MONTANT = 10;
    private static final double MARGE_COLONNE = 3;

    private enum Bloc { ENTETE, PARTIES, CONTRAT, VEHICULE, TABLEAU, CONCLUSION }

    /**
     * @param pages lignes de chaque page, page 1 en premier
     * @throws GabaritNonReconnuException si le titre, le tableau de la page 1 ou l'en-tête du tableau de la page 2
     *                                    sont introuvables
     */
    public RapportExtrait parser(List<List<Ligne>> pages) {
        if (pages.size() < 2) {
            throw new GabaritNonReconnuException("Modèle non reconnu : " + pages.size() + " page(s), au moins 2 attendues.");
        }
        Lecture lecture = new Lecture();
        lecture.page1(pages.getFirst());
        RapportExtrait r = lecture.rapport;
        if (r.numeroRapport == null) {
            throw new GabaritNonReconnuException("Modèle non reconnu : titre « RAPPORT D'EXPERTISE : n° » absent de la page 1.");
        }
        if (r.mainOeuvre.size() != PosteMainOeuvre.values().length || r.totalGeneral == null) {
            throw new GabaritNonReconnuException("Modèle non reconnu : tableau « Évaluation des dommages » incomplet.");
        }
        lecture.detail(pages.subList(1, pages.size()));
        if (r.zonesChoc.isEmpty()) {
            // Liste absente de la page 1 : zones des sections du détail (page 2)
            r.lignesDetail.stream().map(LigneDetailExtraite::getZoneChoc).filter(Objects::nonNull).distinct()
                    .sorted(Comparator.comparingInt(ZoneChoc::getCode)).forEach(r.zonesChoc::add);
        }
        lecture.champsASaisir();
        return r;
    }

    /** Bornes des colonnes du tableau de la page 2 : début de chaque titre, dans l'ordre de {@link #TITRES_COLONNES}. */
    static double[] colonnes(Ligne entete) {
        double[] debuts = new double[TITRES_COLONNES.length];
        int suivant = 0;
        for (Mot mot : entete.mots()) {
            String lettres = norm(mot.texte()).replaceAll("[^a-z]", "");
            int position = 0;
            // Un mot peut couvrir plusieurs titres quand les cellules se touchent (« DésignationType »).
            while (suivant < TITRES_COLONNES.length && !lettres.isEmpty()
                    && lettres.startsWith(TITRES_COLONNES[suivant], position)) {
                debuts[suivant] = mot.x() + (mot.xFin() - mot.x()) * position / lettres.length();
                position += TITRES_COLONNES[suivant].length();
                suivant++;
            }
        }
        if (suivant != TITRES_COLONNES.length) {
            throw new GabaritNonReconnuException("Modèle non reconnu : en-tête du tableau « Détail fournitures / travaux » "
                    + "incomplet (colonne « " + TITRES_COLONNES[suivant] + " » introuvable).");
        }
        return debuts;
    }

    /** État de lecture d'un rapport. */
    private static final class Lecture {

        private final RapportExtrait rapport = new RapportExtrait();
        private final Set<String> masques = new LinkedHashSet<>();
        private Bloc bloc = Bloc.ENTETE;

        Lecture() {
            rapport.gabarit = GABARIT;
        }

        // ------------------------------------------------------------------ page 1

        void page1(List<Ligne> lignes) {
            for (Ligne ligne : lignes) {
                String brut = ligne.texte();
                String t = norm(brut);
                changerDeBloc(t);
                entete(ligne, brut, t);
                switch (bloc) {
                    case PARTIES -> parties(ligne, t);
                    case CONTRAT -> contrat(brut, t);
                    case VEHICULE -> {
                        vehicule(ligne, brut, t);
                        zoneDeChoc(ligne);
                    }
                    case TABLEAU -> tableau(ligne, t);
                    case CONCLUSION -> conclusion(ligne, brut, t);
                    default -> { }
                }
                // Vétusté et dépannage peuvent partager la ligne de « A déduire » ou d'« Immobilisation ».
                Matcher vetuste = VETUSTE.matcher(plat(brut));
                if (vetuste.find()) {
                    if (vetuste.group(1).equals("ht")) {
                        rapport.vetusteHt = nombre(vetuste.group(2));
                    } else {
                        rapport.vetusteTtc = nombre(vetuste.group(2));
                    }
                }
                extraire(brut, DEPANNAGE, 1).ifPresent(v -> rapport.depannage = nombre(v));
            }
        }

        private void changerDeBloc(String t) {
            if (t.startsWith("assure") && t.contains("adversaire")) {
                bloc = Bloc.PARTIES;
            } else if (t.startsWith("nature de contrat")) {
                bloc = Bloc.CONTRAT;
            } else if (t.contains("caracteristiques techniques")) {
                bloc = Bloc.VEHICULE;
            } else if (t.contains("evaluation des dommages")) {
                bloc = Bloc.TABLEAU;
            } else if (t.startsWith("a deduire")) {
                bloc = Bloc.CONCLUSION;
            }
        }

        private void entete(Ligne ligne, String brut, String t) {
            Matcher titre = TITRE.matcher(plat(brut));
            if (rapport.numeroRapport == null && titre.find()) {
                // plat() garde la longueur : la position vaut dans le texte d'origine, casse comprise.
                rapport.numeroRapport = brut.substring(titre.start(2), titre.end(2));
                String mention = titre.group(1);
                rapport.natureRapport = mention == null ? NatureRapport.DEFINITIF
                        : mention.startsWith("non") ? NatureRapport.NON_DEFINITIF : NatureRapport.REFORME;
                return;
            }
            if (rapport.numeroRapport != null && rapport.typeProcedure == null && bloc == Bloc.ENTETE) {
                procedure(t).ifPresent(p -> rapport.typeProcedure = p);
            }
            if (t.startsWith("cabinet") && rapport.cabinet == null && !masques.contains("cabinet")) {
                rapport.cabinet = texte("cabinet", extraire(brut, CABINET, 1).orElse(null));
                rapport.expert = texte("expert", extraire(brut, CABINET, 2).orElse(null));
            } else if (t.startsWith("ref sinistre") && rapport.refSinistre == null && !masques.contains("refSinistre")) {
                rapport.refSinistre = texte("refSinistre", extraire(brut, SINISTRE, 1).orElse(null));
                rapport.dateSinistre = dateTexte("dateSinistre", extraire(brut, SINISTRE, 2).orElse(null));
                rapport.dateMission = dateTexte("dateMission", extraire(brut, SINISTRE, 3).orElse(null));
                rapport.compagnie = texte("compagnie", extraire(brut, SINISTRE, 4).orElse(null));
            }
        }

        private void parties(Ligne ligne, String t) {
            String champ = t.startsWith("nom & prenom") ? "nomPrenom"
                    : t.startsWith("vehicule") ? "vehicule"
                    : t.startsWith("immatriculation") ? "immatriculation"
                    : t.startsWith("cie d'assurance") ? "compagnie"
                    : t.startsWith("n police") ? "numeroPolice"
                    : t.startsWith("intermediaire") ? "intermediaire" : null;
            if (champ == null) {
                return;
            }
            affecter(rapport.assure, champ, texte("assure." + champ, ligne.texte(170, 360)));
            affecter(rapport.adversaire, champ, texte("adversaire." + champ, ligne.texte(360, Double.MAX_VALUE)));
        }

        private void contrat(String brut, String t) {
            if (t.startsWith("nature de contrat")) {
                String valeur = texte("natureContrat", extraire(brut, NATURE_CONTRAT, 1).orElse(null));
                if (valeur != null) {
                    String v = norm(valeur);
                    rapport.natureContrat = v.equals("rc") ? NatureContrat.RC
                            : v.contains("dommage") ? NatureContrat.ASSURANCE_DOMMAGE : null;
                }
            } else if (t.startsWith("reparateur")) {
                rapport.reparateur = texte("reparateur", extraire(brut, REPARATEUR, 1).orElse(null));
            } else if (t.startsWith("adresse") && t.contains("oui")) {
                String sansEspaces = t.replace(" ", "");
                rapport.accordReparateur = sansEspaces.contains("oui(x)") ? Boolean.TRUE
                        : sansEspaces.contains("non(x)") ? Boolean.FALSE : null;
            }
        }

        /**
         * Zones de choc : liste verticale dans la colonne de droite du bloc véhicule, une zone par ligne
         * (« 7 : Arriere » puis « 8 : Avant »), à une hauteur qui dépend du nombre de zones.
         */
        private void zoneDeChoc(Ligne ligne) {
            Matcher zone = ZONE.matcher(norm(ligne.texte(400, Double.MAX_VALUE)));
            if (zone.find()) {
                ZoneChoc z = ZoneChoc.depuisCode(Integer.parseInt(zone.group(1)));
                if (!rapport.zonesChoc.contains(z)) {
                    rapport.zonesChoc.add(z);
                }
            }
        }

        private void vehicule(Ligne ligne, String brut, String t) {
            VehiculeExtrait v = rapport.vehicule;
            if (t.startsWith("marque")) {
                v.marque = texte("vehicule.marque", ligne.texte(100, 220));
                v.immatriculation = texte("vehicule.immatriculation", ligne.texte(300, 420));
                extraire(ligne.texte(420, Double.MAX_VALUE), NATURE_EVENEMENT, 1)
                        .ifPresent(ne -> rapport.natureEvenement = natureEvenement(ne));
            } else if (t.startsWith("modele")) {
                v.modele = texte("vehicule.modele", ligne.texte(100, 220));
                v.numeroSerie = texte("vehicule.numeroSerie", ligne.texte(300, Double.MAX_VALUE));
            } else if (t.startsWith("type mine")) {
                v.typeMine = texte("vehicule.typeMine", ligne.texte(100, 220));
                BigDecimal puissance = nombre(Textes.valeur(ligne.texte(300, Double.MAX_VALUE)));
                v.puissanceFiscale = puissance == null ? null : puissance.shortValue();
            } else if (t.startsWith("energie")) {
                String energie = texte("vehicule.energie", ligne.texte(100, 220));
                v.energie = energie(energie);
                v.energieLibelle = v.energie == null ? energie : null;
                v.dateMiseCirculation = dateTexte("vehicule.dateMiseCirculation", ligne.texte(300, Double.MAX_VALUE));
            } else if (t.startsWith("kilometrage")) {
                String km = texte("vehicule.kilometrage", ligne.texte(100, 220));
                if (km != null && km.matches(".*\\d.*")) {
                    v.kilometrage = nombre(km.replace(" ", "")).intValue();
                } else if (km != null && norm(km).contains("non estimable")) {
                    v.kilometrageNonEstimable = true;
                }
                v.etatGeneral = texte("vehicule.etatGeneral", ligne.texte(300, Double.MAX_VALUE));
            } else if (t.startsWith("usure pneus")) {
                v.usurePneuAvg = nombre(ligne.texte(110, 220));
                v.usurePneuAvd = nombre(ligne.texte(220, 400));
            } else if (t.startsWith("arg")) {
                v.usurePneuArg = nombre(ligne.texte(110, 220));
                v.usurePneuArd = nombre(ligne.texte(220, 400));
            } else if (t.startsWith("date expertise vehicule")) {
                rapport.dateExpertise = dateTexte("dateExpertise", extraire(brut, DATE_EXPERTISE, 1).orElse(null));
            } else if (t.startsWith("avant reparation")) {
                rapport.dateAvantReparation = dateTexte("dateAvantReparation", extraire(brut, DATES_TRAVAUX, 1).orElse(null));
                rapport.dateEnCoursTravaux = dateTexte("dateEnCoursTravaux", extraire(brut, DATES_TRAVAUX, 2).orElse(null));
                rapport.dateApresTravaux = dateTexte("dateApresTravaux", extraire(brut, DATES_TRAVAUX, 3).orElse(null));
            }
        }

        private void tableau(Ligne ligne, String t) {
            for (Map.Entry<String, PosteMainOeuvre> poste : POSTES.entrySet()) {
                if (t.startsWith(poste.getKey())) {
                    List<BigDecimal> v = nombres(ligne, 140);
                    rapport.mainOeuvre.put(poste.getValue(), new LigneMainOeuvreExtraite(poste.getValue(),
                            valeur(v, 0), valeur(v, 1), new Montants(valeur(v, 2), valeur(v, 3), valeur(v, 4))));
                    return;
                }
            }
            for (Map.Entry<String, CategorieFourniture> fourniture : FOURNITURES.entrySet()) {
                if (t.startsWith(fourniture.getKey())) {
                    rapport.fournitures.put(fourniture.getValue(), montants(ligne));
                    return;
                }
            }
            if (t.startsWith("total main d'oeuvre")) {
                rapport.totalMainOeuvre = montants(ligne);
            } else if (t.startsWith("total fournitures")) {
                rapport.totalFournitures = montants(ligne);
            } else if (t.startsWith("total general")) {
                rapport.totalGeneral = montants(ligne);
            }
        }

        private void conclusion(Ligne ligne, String brut, String t) {
            String sansEspaces = t.replace(" ", "");
            Matcher valeur = VALEUR.matcher(plat(brut));
            if (t.startsWith("immobilisation")) {
                rapport.immobilisation = nombre(Textes.valeur(ligne.texte(100, 280)));
            } else if (sansEspaces.contains("reformeeconomique")) {
                rapport.typeReforme = sansEspaces.contains("economique(x)") ? TypeReforme.ECONOMIQUE
                        : sansEspaces.contains("technique(x)") ? TypeReforme.TECHNIQUE : null;
            } else if (valeur.find()) {
                String quoi = valeur.group(1);
                BigDecimal montant = nombre(Textes.valeur(brut.substring(valeur.start(2), valeur.end(2))));
                // L'épaviste retenu (rapports de réforme) est imprimé à gauche des deux premières valeurs.
                String avant = Textes.valeur(brut.substring(0, valeur.start()));
                switch (quoi.replaceAll("\\s+", " ")) {
                    case "a neuf", "neuf" -> {
                        rapport.valeurNeuf = montant;
                        rapport.epavisteNom = avant;
                    }
                    case "assuree" -> {
                        rapport.valeurAssuree = montant;
                        rapport.epavisteVille = avant;
                    }
                    case "venale" -> rapport.valeurVenale = montant;
                    default -> rapport.valeurEpave = montant;
                }
            } else if (t.contains("recuperabilite de tva")) {
                rapport.tvaRecuperable = !t.contains("non recuperable");
            } else if (t.contains("indemnisation")) {
                extraire(brut, INDEMNISATION, 1).ifPresent(v -> rapport.montantIndemnisation = nombre(v));
            } else if (t.contains("dirham") && rapport.montantLettres == null) {
                rapport.montantLettres = brut;
            } else if (t.startsWith("expert") && t.contains("fait a")) {
                rapport.lieuRapport = texte("lieuRapport", extraire(brut, FAIT_A, 1).orElse(null));
                rapport.dateRapport = dateTexte("dateRapport", extraire(brut, FAIT_A, 2).orElse(null));
            }
        }

        // ------------------------------------------------------------------ page 2 et suivantes

        void detail(List<List<Ligne>> pages) {
            double[] colonnes = null;
            boolean dansTableau = false;
            boolean dansCommentaire = false;
            String zone = null;
            ZoneChoc zoneChoc = null;
            FamilleTravaux famille = null;
            List<String> commentaire = new ArrayList<>();

            for (List<Ligne> page : pages) {
                for (Ligne ligne : page) {
                    String t = norm(ligne.texte());
                    if (t.startsWith("expert") && t.contains("fait a")) {
                        continue; // pied de page
                    }
                    if (t.startsWith("cabinet") || t.startsWith("ref sinistre") || t.startsWith("detail fourniture")) {
                        continue; // en-tête répété sur chaque page
                    }
                    if (t.startsWith("qte") && t.contains("designation")) {
                        colonnes = colonnes(ligne);
                        dansTableau = true;
                        continue;
                    }
                    if (t.startsWith("commentaire rapport")) {
                        dansTableau = false;
                        dansCommentaire = true;
                        continue;
                    }
                    if (t.startsWith("o: piece")) {
                        dansTableau = false;
                        continue;
                    }
                    if (dansCommentaire) {
                        commentaire.add(ligne.texte());
                        continue;
                    }
                    if (!dansTableau) {
                        continue;
                    }
                    String premier = ligne.mots().getFirst().texte();
                    if (QUANTITE.matcher(premier).matches()) {
                        rapport.lignesDetail.add(ligneDetail(ligne, colonnes, zone, zoneChoc, famille));
                    } else {
                        String libelle = ligne.mots(0, 260).stream().map(Mot::texte)
                                .filter(m -> !m.equals(".")).reduce((a, b) -> a + " " + b).orElse("").strip();
                        if (premier.startsWith("·") || premier.startsWith("-") || premier.startsWith("•")) {
                            famille = famille(libelle);
                        } else {
                            zone = libelle;
                            zoneChoc = zoneDepuisLibelle(libelle);
                            famille = null;
                        }
                    }
                }
            }
            if (colonnes == null) {
                throw new GabaritNonReconnuException(
                        "Modèle non reconnu : en-tête du tableau « Détail fournitures / travaux » introuvable.");
            }
            String texteCommentaire = String.join(" ", commentaire).strip();
            if (texteCommentaire.isEmpty()) {
                rapport.commentaire = null;
            } else if (COMMENTAIRE_MASQUE.matcher(texteCommentaire).matches()) {
                masques.add("commentaire");
                rapport.commentaire = null;
            } else {
                rapport.commentaire = texteCommentaire;
            }
        }

        private LigneDetailExtraite ligneDetail(Ligne ligne, double[] c, String zone, ZoneChoc zoneChoc,
                                                FamilleTravaux famille) {
            LigneDetailExtraite l = new LigneDetailExtraite();
            l.zoneLibelle = zone;
            l.zoneChoc = zoneChoc;
            l.famille = famille;
            l.quantite = nombre(colonne(ligne, c, QTE, DESIGNATION));
            List<Mot> designation = new ArrayList<>(ligne.mots(c[DESIGNATION] - MARGE_COLONNE, c[PRIX] - MARGE_COLONNE));
            // Le type (O, A, R) est le dernier mot avant le prix s'il est aligné sur la colonne « Type ».
            if (!designation.isEmpty()) {
                Mot dernier = designation.getLast();
                if (dernier.texte().matches("[OAR]") && dernier.x() >= c[TYPE] - 10) {
                    l.typePiece = TypePiece.depuisCode(dernier.texte().charAt(0)).orElse(null);
                    designation.removeLast();
                }
            }
            l.designation = designation.stream().map(Mot::texte).reduce((a, b) -> a + " " + b).orElse(null);
            l.prixUnitaire = nombre(colonne(ligne, c, PRIX, VETUSTE_PCT));
            l.vetustePct = nombre(colonne(ligne, c, VETUSTE_PCT, TVA));
            l.tvaPct = nombre(colonne(ligne, c, TVA, TOTAL));
            l.totalFourniture = nombre(colonne(ligne, c, TOTAL, TRAVAIL));
            String operation = norm(colonne(ligne, c, TRAVAIL, NBR));
            l.operation = operation.startsWith("echange") ? TypeOperation.ECHANGE
                    : operation.startsWith("reparation") ? TypeOperation.REPARATION : null;
            l.nombreHeures = nombre(colonne(ligne, c, NBR, TARIF));
            l.tarifHoraire = nombre(colonne(ligne, c, TARIF, MONTANT));
            l.montantMoTtc = nombre(ligne.texte(c[MONTANT] - MARGE_COLONNE, Double.MAX_VALUE));
            return l;
        }

        // ------------------------------------------------------------------ champs à saisir

        void champsASaisir() {
            // Un adversaire entièrement vide signifie qu'il n'y en a pas ; masqué, il est à saisir.
            boolean adversaire = !rapport.adversaire.estVide() || masques.stream().anyMatch(m -> m.startsWith("adversaire."));
            rapport.champsASaisir.addAll(rapport.champsManquants(adversaire));
        }

        // ------------------------------------------------------------------ outils

        /** Valeur texte du champ ; une valeur masquée (XXXX) est notée pour la liste des champs à saisir. */
        private String texte(String champ, String brut) {
            if (Textes.estMasquee(brut)) {
                masques.add(champ);
            }
            return Textes.valeur(brut);
        }

        private java.time.LocalDate dateTexte(String champ, String brut) {
            if (Textes.estMasquee(brut)) {
                masques.add(champ);
            }
            return date(brut);
        }

        private static void affecter(PartieExtraite partie, String champ, String valeur) {
            switch (champ) {
                case "nomPrenom" -> partie.nomPrenom = valeur;
                case "vehicule" -> partie.vehicule = valeur;
                case "immatriculation" -> partie.immatriculation = valeur;
                case "compagnie" -> partie.compagnie = valeur;
                case "numeroPolice" -> partie.numeroPolice = valeur;
                default -> partie.intermediaire = valeur;
            }
        }

        private static List<BigDecimal> nombres(Ligne ligne, double xMin) {
            return ligne.mots(xMin, Double.MAX_VALUE).stream().map(m -> nombre(m.texte())).toList();
        }

        private static Montants montants(Ligne ligne) {
            List<BigDecimal> v = nombres(ligne, 340);
            return new Montants(valeur(v, 0), valeur(v, 1), valeur(v, 2));
        }

        private static BigDecimal valeur(List<BigDecimal> valeurs, int indice) {
            return indice < valeurs.size() ? valeurs.get(indice) : null;
        }

        private static String colonne(Ligne ligne, double[] c, int debut, int fin) {
            return ligne.texte(c[debut] - MARGE_COLONNE, c[fin] - MARGE_COLONNE);
        }

        private static Optional<TypeProcedure> procedure(String t) {
            String compact = t.replace(" ", "");
            return Arrays.stream(TypeProcedure.values())
                    .filter(p -> compact.equals(norm(p.getLibellePdf()).replace(" ", "")))
                    .findFirst();
        }

        private static NatureEvenement natureEvenement(String libelle) {
            String v = norm(libelle);
            return v.startsWith("circulation") ? NatureEvenement.CIRCULATION_HORS_STATIONNEMENT
                    : v.startsWith("stationnement") ? NatureEvenement.STATIONNEMENT
                    : v.startsWith("autre") ? NatureEvenement.AUTRE : null;
        }

        private static Energie energie(String libelle) {
            if (libelle == null) {
                return null;
            }
            String v = norm(libelle);
            return switch (v) {
                case "diesel", "gasoil" -> Energie.DIESEL;
                case "essence" -> Energie.ESSENCE;
                case "hybride" -> Energie.HYBRIDE;
                case "electrique" -> Energie.ELECTRIQUE;
                default -> null;
            };
        }

        private static FamilleTravaux famille(String libelle) {
            String v = norm(libelle);
            return v.contains("peinture") ? FamilleTravaux.PEINTURE
                    : v.contains("toler") ? FamilleTravaux.TOLERIE
                    : v.contains("mecanique") ? FamilleTravaux.MECANIQUE : null;
        }

        private static ZoneChoc zoneDepuisLibelle(String libelle) {
            String v = norm(libelle);
            return Arrays.stream(ZoneChoc.values()).filter(z -> norm(z.getLibelle()).equals(v)).findFirst().orElse(null);
        }
    }
}
