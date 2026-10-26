package ma.ltexpertise.extraction;

import ma.ltexpertise.commun.Montants;
import ma.ltexpertise.dossier.referentiel.CategorieFourniture;
import ma.ltexpertise.dossier.referentiel.NatureRapport;
import ma.ltexpertise.dossier.referentiel.PosteMainOeuvre;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Rapports extraits construits à la main pour les tests d'autres packages (les champs de RapportExtrait ne sont
 * modifiables que depuis le package extraction). Montants cohérents de référence (rapport 268202-0231) :
 * MO 1320 + 264 = 1584, fournitures 613,33 + 122,67 = 736, total 2320 = indemnisation.
 */
public final class FabriqueRapportExtrait {

    private FabriqueRapportExtrait() {
    }

    public static RapportExtrait coherent() {
        RapportExtrait r = new RapportExtrait();
        r.gabarit = ParseurTcpdfExpertiseV1.GABARIT;
        r.numeroRapport = "268202-9999";
        for (PosteMainOeuvre poste : PosteMainOeuvre.values()) {
            r.mainOeuvre.put(poste, switch (poste) {
                case MO_1_TOLERIE -> mo(poste, "12.00", "60.00", "720.00", "144.00", "864.00");
                case MO_PEINTURE -> mo(poste, "10.00", "60.00", "600.00", "120.00", "720.00");
                default -> mo(poste, "0.00", "0.00", "0.00", "0.00", "0.00");
            });
        }
        r.fournitures.put(CategorieFourniture.PIECES, montants("0.00", "0.00", "0.00"));
        r.fournitures.put(CategorieFourniture.INGREDIENT_PEINTURE, montants("613.33", "122.67", "736.00"));
        r.fournitures.put(CategorieFourniture.PETITE_FOURNITURE, montants("0.00", "0.00", "0.00"));
        r.fournitures.put(CategorieFourniture.AUTRES, montants("0.00", "0.00", "0.00"));
        r.totalMainOeuvre = montants("1320.00", "264.00", "1584.00");
        r.totalFournitures = montants("613.33", "122.67", "736.00");
        r.totalGeneral = montants("1933.33", "386.67", "2320.00");
        r.vetusteTtc = dh("0.00");
        r.tvaRecuperable = false;
        r.montantIndemnisation = dh("2320.00");
        r.montantLettres = "deux mille trois cent vingt Dirhams";
        r.lignesDetail.add(ligne("MOP", "0.00", "720.00"));
        r.lignesDetail.add(ligne("PP ET ING", "736.00", "0.00"));
        r.lignesDetail.add(ligne("MOT", "0.00", "864.00"));
        return r;
    }

    /** Rapport cohérent modifié par {@code modification}, qui reçoit un accès aux champs. */
    public static RapportExtrait coherentAvec(Consumer<Modification> modification) {
        RapportExtrait r = coherent();
        modification.accept(new Modification(r));
        return r;
    }

    /** Accès en écriture aux champs utiles aux tests de contrôle. */
    public static final class Modification {

        private final RapportExtrait r;

        Modification(RapportExtrait r) {
            this.r = r;
        }

        public Modification indemnisation(String montant, String lettres) {
            r.montantIndemnisation = montant == null ? null : dh(montant);
            r.montantLettres = lettres;
            return this;
        }

        public Modification vetusteTtc(String montant) {
            r.vetusteTtc = dh(montant);
            return this;
        }

        public Modification vetusteHt(String montant) {
            r.vetusteTtc = null;
            r.vetusteHt = dh(montant);
            return this;
        }

        public Modification tvaRecuperable(boolean recuperable) {
            r.tvaRecuperable = recuperable;
            return this;
        }

        public Modification reforme(String valeurVenale, String valeurEpave) {
            r.natureRapport = NatureRapport.REFORME;
            r.valeurVenale = valeurVenale == null ? null : dh(valeurVenale);
            r.valeurEpave = valeurEpave == null ? null : dh(valeurEpave);
            return this;
        }

        public Modification posteMainOeuvre(PosteMainOeuvre poste, String heures, String taux, String ht, String tva,
                                            String ttc) {
            r.mainOeuvre.put(poste, mo(poste, heures, taux, ht, tva, ttc));
            return this;
        }

        public Modification totalGeneral(String ht, String tva, String ttc) {
            r.totalGeneral = montants(ht, tva, ttc);
            return this;
        }

        /** Remplace le total fournitures de la dernière ligne de la page 2. */
        public Modification totalDerniereLignePage2(String fourniture, String mainOeuvre) {
            LigneDetailExtraite derniere = r.lignesDetail.getLast();
            derniere.prixUnitaire = dh(fourniture);
            derniere.totalFourniture = dh(fourniture);
            derniere.montantMoTtc = dh(mainOeuvre);
            return this;
        }

        public Modification ajouterLignePage2(String fourniture, String mainOeuvre) {
            r.lignesDetail.add(ligne("LIGNE", fourniture, mainOeuvre));
            return this;
        }

        /** Ligne de page 2 telle qu'imprimée (quantité, prix, TVA %, vétusté %, total). */
        public Modification ajouterLigneFourniture(String quantite, String prix, String tva, String vetuste,
                                                   String total) {
            LigneDetailExtraite l = ligne("PIECE", total, "0.00");
            l.quantite = dh(quantite);
            l.prixUnitaire = dh(prix);
            l.tvaPct = dh(tva);
            l.vetustePct = dh(vetuste);
            r.lignesDetail.add(l);
            return this;
        }

        public Modification champASaisir(String chemin) {
            r.champsASaisir.add(chemin);
            return this;
        }
    }

    private static LigneMainOeuvreExtraite mo(PosteMainOeuvre poste, String heures, String taux, String ht, String tva,
                                              String ttc) {
        return new LigneMainOeuvreExtraite(poste, dh(heures), dh(taux), montants(ht, tva, ttc));
    }

    private static LigneDetailExtraite ligne(String designation, String fourniture, String mainOeuvre) {
        LigneDetailExtraite l = new LigneDetailExtraite();
        l.quantite = BigDecimal.ONE;
        l.designation = designation;
        // Prix = total, sans TVA ni vétusté : la ligne vérifie quantité × prix = total.
        l.prixUnitaire = dh(fourniture);
        l.tvaPct = dh("0.00");
        l.vetustePct = dh("0.00");
        l.totalFourniture = dh(fourniture);
        l.montantMoTtc = dh(mainOeuvre);
        return l;
    }

    private static Montants montants(String ht, String tva, String ttc) {
        return new Montants(dh(ht), dh(tva), dh(ttc));
    }

    private static BigDecimal dh(String montant) {
        return new BigDecimal(montant);
    }
}
