package ma.ltexpertise.controle;

import ma.ltexpertise.commun.Montants;
import ma.ltexpertise.dossier.referentiel.CategorieFourniture;
import ma.ltexpertise.dossier.referentiel.NatureRapport;
import ma.ltexpertise.dossier.referentiel.PosteMainOeuvre;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.extraction.LigneDetailExtraite;
import ma.ltexpertise.extraction.LigneMainOeuvreExtraite;
import ma.ltexpertise.extraction.RapportExtrait;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Contrôles de cohérence des montants (P4) et traitement de la TVA (P5), avec une tolérance de 0,05 :
 * <ul>
 *   <li>pour chaque poste de main d'œuvre : heures × taux = HT et HT + TVA = TTC ;</li>
 *   <li>somme des postes = totaux main d'œuvre et fournitures, MO + fournitures = total général (HT, TVA, TTC) ;</li>
 *   <li>total général − vétusté = indemnisation. La vétusté imprimée fixe la méthode : « Vétusté HT » se déduit
 *       du total général HT, « Vétusté TTC » du total général TTC ; sans vétusté lue, le HT si la TVA est
 *       récupérable (P5), sinon le TTC ;
 *       pour un rapport de réforme, valeur vénale − valeur épave ;</li>
 *   <li>page 2 = page 1 : main d'œuvre TTC, et fournitures (la colonne « Total » de la page 2 est vétusté déduite) ;</li>
 *   <li>montant en lettres = montant en chiffres ;</li>
 *   <li>chaque ligne de la page 2 : quantité × prix × (1 + TVA) × (1 − vétusté) = total. Sur les rapports réels,
 *       ce contrôle signale les prix unitaires faux imprimés par le logiciel de l'expert (prix négatifs ou sans
 *       rapport avec le total). Il est bloquant : le gestionnaire corrige le prix des lignes signalées à l'écran
 *       de vérification (le total imprimé, juste, donne le prix attendu).</li>
 * </ul>
 * Soit 28 contrôles par rapport. Les sommes de la page 2 additionnent des lignes arrondies chacune au centime :
 * leur tolérance est portée à un demi-centime par ligne quand elle dépasse 0,05 (un rapport réel de 39 lignes
 * s'écarte de 0,06).
 */
@Service
public class ControleCoherenceService {

    public static final BigDecimal TOLERANCE = new BigDecimal("0.05");
    static final BigDecimal TOLERANCE_PAR_LIGNE = new BigDecimal("0.005");
    static final BigDecimal TOLERANCE_PRIX_PAR_UNITE = new BigDecimal("0.006");
    public static final String CODE_LIGNES_PRIX = "PAGE2_LIGNES_PRIX";
    /** Données sans lesquelles un dossier ne peut pas être validé (contrainte ck_rapport_complet_si_valide). */
    private static final Set<String> CHAMPS_ESSENTIELS = Set.of("montantIndemnisation", "typeProcedure", "tvaRecuperable");

    public List<Controle> controler(RapportExtrait r) {
        List<Controle> controles = new ArrayList<>();

        for (PosteMainOeuvre poste : PosteMainOeuvre.values()) {
            LigneMainOeuvreExtraite l = r.getMainOeuvre().get(poste);
            Montants m = l == null ? null : l.montants();
            controles.add(controle(poste + "_HEURES_TAUX", libelle(poste) + " : heures × taux = HT",
                    l == null ? null : produit(l.nombreHeures(), l.tauxHoraire()), m == null ? null : m.getHt()));
            controles.add(controle(poste + "_HT_TVA", libelle(poste) + " : HT + TVA = TTC",
                    m == null ? null : somme(m.getHt(), m.getTva()), m == null ? null : m.getTtc()));
        }

        Collection<Montants> mainOeuvre = r.getMainOeuvre().values().stream().map(LigneMainOeuvreExtraite::montants).toList();
        Collection<Montants> fournitures = r.getFournitures().values();
        for (String colonne : List.of("HT", "TVA", "TTC")) {
            Function<Montants, BigDecimal> lire = lecteur(colonne);
            controles.add(controle("SOMME_MO_" + colonne, "Somme des postes de main d'œuvre = total (" + colonne + ")",
                    r.getMainOeuvre().size() == PosteMainOeuvre.values().length ? sommeDe(mainOeuvre, lire) : null,
                    lire(r.getTotalMainOeuvre(), lire)));
            controles.add(controle("SOMME_FOURNITURES_" + colonne, "Somme des fournitures = total (" + colonne + ")",
                    fournitures.size() == CategorieFourniture.values().length ? sommeDe(fournitures, lire) : null,
                    lire(r.getTotalFournitures(), lire)));
            controles.add(controle("TOTAL_GENERAL_" + colonne, "Main d'œuvre + fournitures = total général (" + colonne + ")",
                    somme(lire(r.getTotalMainOeuvre(), lire), lire(r.getTotalFournitures(), lire)),
                    lire(r.getTotalGeneral(), lire)));
        }

        controles.add(controle("INDEMNISATION", libelleIndemnisation(r), indemnisationAttendue(r), r.getMontantIndemnisation()));
        controles.add(controle("MONTANT_EN_LETTRES", "Montant en lettres = montant en chiffres",
                MontantEnLettres.versNombre(r.getMontantLettres()).orElse(null), r.getMontantIndemnisation()));

        BigDecimal toleranceSomme = TOLERANCE.max(
                TOLERANCE_PAR_LIGNE.multiply(BigDecimal.valueOf(r.getLignesDetail().size())));
        controles.add(controle("PAGE2_MAIN_OEUVRE", "Page 2 = page 1 : main d'œuvre TTC",
                sommeDe(r.getLignesDetail(), LigneDetailExtraite::getMontantMoTtc),
                lire(r.getTotalMainOeuvre(), Montants::getTtc), toleranceSomme));
        controles.add(controle("PAGE2_FOURNITURES", "Page 2 = page 1 : fournitures TTC − vétusté",
                sommeDe(r.getLignesDetail(), LigneDetailExtraite::getTotalFourniture),
                difference(lire(r.getTotalFournitures(), Montants::getTtc), zeroSiAbsent(r.getVetuste())), toleranceSomme));

        controles.add(controleLignesPrix(r.getLignesDetail()));
        return controles;
    }

    /**
     * Prix de chaque ligne de la page 2 (attendu = nombre de lignes, obtenu = lignes cohérentes). Bloquant : le
     * logiciel de l'expert imprime parfois un prix unitaire faux alors que le total de la ligne est juste ; le
     * gestionnaire corrige le prix avant de valider. Le libellé nomme les lignes en écart (numéro dans le PDF).
     */
    static Controle controleLignesPrix(List<LigneDetailExtraite> lignes) {
        List<String> ecarts = new ArrayList<>();
        for (int i = 0; i < lignes.size(); i++) {
            if (!ligneCoherente(lignes.get(i))) {
                String designation = lignes.get(i).getDesignation();
                ecarts.add("ligne " + (i + 1) + (designation == null ? "" : " " + designation));
            }
        }
        String libelle = "Page 2 : quantité × prix × (1 + TVA) × (1 − vétusté) = total, pour chaque ligne";
        if (!ecarts.isEmpty()) {
            libelle += " — prix unitaire à corriger (" + String.join(", ", ecarts) + ")";
        }
        return new Controle(CODE_LIGNES_PRIX, libelle, BigDecimal.valueOf(lignes.size()),
                BigDecimal.valueOf(lignes.size() - ecarts.size()), ecarts.isEmpty());
    }

    static boolean ligneCoherente(LigneDetailExtraite l) {
        return prixCoherent(l.getQuantite(), l.getPrixUnitaire(), l.getTvaPct(), l.getVetustePct(), l.getTotalFourniture());
    }

    /**
     * quantité × prix × (1 + TVA) × (1 − vétusté) = total ? Le prix unitaire imprimé est arrondi au centime :
     * l'écart toléré croît avec la quantité (0,6 centime par unité, TVA comprise), sans descendre sous 0,05.
     */
    public static boolean prixCoherent(BigDecimal quantite, BigDecimal prix, BigDecimal tvaPct, BigDecimal vetustePct,
                                       BigDecimal total) {
        if (quantite == null || prix == null || tvaPct == null || vetustePct == null || total == null) {
            return false;
        }
        BigDecimal attendu = quantite.multiply(prix).multiply(coefficient(tvaPct, vetustePct))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal tolerance = TOLERANCE.max(quantite.multiply(TOLERANCE_PRIX_PAR_UNITE));
        return attendu.subtract(total).abs().compareTo(tolerance) <= 0;
    }

    /**
     * Prix unitaire qui redonne le total imprimé : total / (quantité × (1 + TVA) × (1 − vétusté)), au centime.
     * Proposé au gestionnaire pour corriger un prix faux ; {@code null} si une donnée manque ou si le diviseur est nul.
     */
    public static BigDecimal prixDepuisTotal(BigDecimal quantite, BigDecimal tvaPct, BigDecimal vetustePct,
                                             BigDecimal total) {
        if (quantite == null || tvaPct == null || vetustePct == null || total == null) {
            return null;
        }
        BigDecimal diviseur = quantite.multiply(coefficient(tvaPct, vetustePct));
        return diviseur.signum() == 0 ? null : total.divide(diviseur, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal coefficient(BigDecimal tvaPct, BigDecimal vetustePct) {
        BigDecimal cent = BigDecimal.valueOf(100);
        return BigDecimal.ONE.add(tvaPct.divide(cent)).multiply(BigDecimal.ONE.subtract(vetustePct.divide(cent)));
    }

    /**
     * Statut d'un dossier après import : À vérifier si un contrôle bloquant échoue, À compléter s'il manque une
     * donnée essentielle, sinon Prêt à valider. Les autres champs absents du PDF et les contrôles informatifs sont
     * seulement signalés : l'application extrait, la saisie n'est pas obligatoire. Jamais VALIDE : seule l'action
     * d'un gestionnaire y conduit (P3).
     */
    public StatutDossier statutApresImport(List<Controle> controles, List<String> champsASaisir) {
        if (controles.stream().anyMatch(Controle::bloque)) {
            return StatutDossier.A_VERIFIER;
        }
        return champsASaisir.stream().anyMatch(CHAMPS_ESSENTIELS::contains)
                ? StatutDossier.A_COMPLETER : StatutDossier.PRET_A_VALIDER;
    }

    /** Un champ absent empêche-t-il la validation ? Oui pour les données qui fondent l'indemnisation. */
    public static boolean champEssentiel(String champ) {
        return CHAMPS_ESSENTIELS.contains(champ);
    }

    private static String libelleIndemnisation(RapportExtrait r) {
        if (r.getNatureRapport() == NatureRapport.REFORME) {
            return "Réforme : valeur vénale − valeur épave = indemnisation";
        }
        return calculSurHt(r) ? "Total général HT − vétusté HT = indemnisation"
                : "Total général TTC − vétusté TTC = indemnisation";
    }

    private static BigDecimal indemnisationAttendue(RapportExtrait r) {
        if (r.getNatureRapport() == NatureRapport.REFORME) {
            return difference(r.getValeurVenale(), r.getValeurEpave());
        }
        BigDecimal base = lire(r.getTotalGeneral(), calculSurHt(r) ? Montants::getHt : Montants::getTtc);
        return difference(base, zeroSiAbsent(r.getVetuste()));
    }

    /**
     * L'indemnisation se calcule-t-elle sur le HT ? Oui si le rapport imprime « Vétusté HT », non s'il imprime
     * « Vétusté TTC » ; sans vétusté, selon la récupérabilité de la TVA (P5).
     */
    public static boolean calculSurHt(RapportExtrait r) {
        if (r.getVetusteHt() != null) {
            return true;
        }
        if (r.getVetusteTtc() != null) {
            return false;
        }
        return Boolean.TRUE.equals(r.getTvaRecuperable());
    }

    /** Libellé du poste tel qu'imprimé dans le tableau « Évaluation des dommages ». */
    private static String libelle(PosteMainOeuvre poste) {
        return switch (poste) {
            case MO_1_TOLERIE -> "MO 1 Tôlerie";
            case MO_2_TOLERIE -> "MO 2 Tôlerie";
            case MO_3_TOLERIE -> "MO 3 Tôlerie";
            case MO_PEINTURE -> "MO Peinture";
            case MO_1_MECANIQUE -> "MO 1 Mécanique";
            case MO_2_MECANIQUE -> "MO 2 Mécanique";
            case MO_3_MECANIQUE -> "MO 3 Mécanique";
        };
    }

    private static Controle controle(String code, String nom, BigDecimal attendu, BigDecimal obtenu) {
        return controle(code, nom, attendu, obtenu, TOLERANCE);
    }

    private static Controle controle(String code, String nom, BigDecimal attendu, BigDecimal obtenu,
                                     BigDecimal tolerance) {
        boolean ok = attendu != null && obtenu != null && attendu.subtract(obtenu).abs().compareTo(tolerance) <= 0;
        return new Controle(code, nom, attendu, obtenu, ok);
    }

    private static Function<Montants, BigDecimal> lecteur(String colonne) {
        return switch (colonne) {
            case "HT" -> Montants::getHt;
            case "TVA" -> Montants::getTva;
            default -> Montants::getTtc;
        };
    }

    private static BigDecimal lire(Montants montants, Function<Montants, BigDecimal> lire) {
        return montants == null ? null : lire.apply(montants);
    }

    private static <T> BigDecimal sommeDe(Collection<T> elements, Function<T, BigDecimal> lire) {
        BigDecimal total = BigDecimal.ZERO;
        for (T element : elements) {
            BigDecimal valeur = element == null ? null : lire.apply(element);
            if (valeur == null) {
                return null;
            }
            total = total.add(valeur);
        }
        return total;
    }

    private static BigDecimal produit(BigDecimal a, BigDecimal b) {
        return a == null || b == null ? null : a.multiply(b).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal somme(BigDecimal a, BigDecimal b) {
        return a == null || b == null ? null : a.add(b);
    }

    private static BigDecimal difference(BigDecimal a, BigDecimal b) {
        return a == null || b == null ? null : a.subtract(b);
    }

    private static BigDecimal zeroSiAbsent(BigDecimal valeur) {
        return Objects.requireNonNullElse(valeur, BigDecimal.ZERO);
    }
}
