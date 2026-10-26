package ma.ltexpertise.importation;

import ma.ltexpertise.commun.Montants;
import ma.ltexpertise.controle.Controle;
import ma.ltexpertise.controle.ControleCoherenceService;
import ma.ltexpertise.dossier.EvaluationMainOeuvre;
import ma.ltexpertise.dossier.Fourniture;
import ma.ltexpertise.dossier.LigneDetail;
import ma.ltexpertise.dossier.Partie;
import ma.ltexpertise.dossier.RapportExpertise;
import ma.ltexpertise.dossier.ResultatControle;
import ma.ltexpertise.dossier.ValeurCommentaire;
import ma.ltexpertise.dossier.Vehicule;
import ma.ltexpertise.dossier.referentiel.TypeControle;
import ma.ltexpertise.dossier.referentiel.TypePartie;
import ma.ltexpertise.extraction.ExtracteurCommentaire;
import ma.ltexpertise.extraction.LigneDetailExtraite;
import ma.ltexpertise.extraction.PartieExtraite;
import ma.ltexpertise.extraction.RapportExtrait;
import ma.ltexpertise.extraction.VehiculeExtrait;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Recopie un rapport extrait dans le dossier (entités JPA), avec les résultats des contrôles et les champs à saisir.
 * Les textes sont tronqués à la longueur de leur colonne : une valeur mal lue ne doit pas faire échouer l'import,
 * elle sera corrigée à l'écran de vérification.
 */
public final class ConversionRapportExtrait {

    /** Champ du formulaire à surligner pour les contrôles qui portent sur une valeur précise. */
    private static final Map<String, String> CHAMP_PAR_CONTROLE = Map.of(
            "INDEMNISATION", "montantIndemnisation",
            "MONTANT_EN_LETTRES", "montantLettres",
            "TOTAL_GENERAL_HT", "totalGeneral",
            "TOTAL_GENERAL_TVA", "totalGeneral",
            "TOTAL_GENERAL_TTC", "totalGeneral",
            "PAGE2_MAIN_OEUVRE", "lignesDetail",
            "PAGE2_FOURNITURES", "lignesDetail",
            "PAGE2_LIGNES_PRIX", "lignesDetail");

    private ConversionRapportExtrait() {
    }

    /** Remplit un dossier sans données (nouveau, ou « En cours » repris du fichier Excel). */
    static void appliquer(RapportExtrait e, RapportExpertise r) {
        r.setNatureRapport(e.getNatureRapport());
        r.setTypeProcedure(e.getTypeProcedure());
        r.setCabinet(tronquer(e.getCabinet(), 100));
        r.setExpert(tronquer(e.getExpert(), 100));
        r.setCompagnie(tronquer(e.getCompagnie(), 100));
        r.setRefSinistre(tronquer(e.getRefSinistre(), 30));
        r.setDateSinistre(e.getDateSinistre());
        r.setDateMission(e.getDateMission());
        r.setDateExpertise(e.getDateExpertise());
        r.setDateAvantReparation(e.getDateAvantReparation());
        r.setDateEnCoursTravaux(e.getDateEnCoursTravaux());
        r.setDateApresTravaux(e.getDateApresTravaux());
        r.setDateRapport(e.getDateRapport());
        r.setLieuRapport(tronquer(e.getLieuRapport(), 60));
        r.setNatureContrat(e.getNatureContrat());
        r.setNatureEvenement(e.getNatureEvenement());
        r.setZonesChoc(e.getZonesChoc());
        r.setReparateur(tronquer(e.getReparateur(), 150));
        r.setAccordReparateur(e.getAccordReparateur());
        r.setTotalMainOeuvre(copie(e.getTotalMainOeuvre()));
        r.setTotalFournitures(copie(e.getTotalFournitures()));
        r.setTotalGeneral(copie(e.getTotalGeneral()));
        r.setVetusteTtc(e.getVetusteTtc());
        r.setVetusteHt(e.getVetusteHt());
        r.setDepannage(e.getDepannage());
        r.setImmobilisation(e.getImmobilisation());
        r.setTypeReforme(e.getTypeReforme());
        r.setEpavisteNom(tronquer(e.getEpavisteNom(), 100));
        r.setEpavisteVille(tronquer(e.getEpavisteVille(), 60));
        r.setValeurNeuf(e.getValeurNeuf());
        r.setValeurAssuree(e.getValeurAssuree());
        r.setValeurVenale(e.getValeurVenale());
        r.setValeurEpave(e.getValeurEpave());
        r.setTvaRecuperable(e.getTvaRecuperable());
        r.setMontantIndemnisation(e.getMontantIndemnisation());
        r.setMontantLettres(tronquer(e.getMontantLettres(), 300));
        r.setCommentaire(e.getCommentaire());
        r.setGabaritVersion(e.getGabarit());

        r.ajouterPartie(partie(TypePartie.ASSURE, e.getAssure()));
        if (!estVide(e.getAdversaire())) {
            r.ajouterPartie(partie(TypePartie.ADVERSAIRE, e.getAdversaire()));
        }
        r.definirVehicule(vehicule(e.getVehicule()));
        e.getMainOeuvre().values().forEach(l -> r.ajouterEvaluationMainOeuvre(
                new EvaluationMainOeuvre(l.poste(), l.nombreHeures(), l.tauxHoraire(), copie(l.montants()))));
        e.getFournitures().forEach((categorie, montants) -> r.ajouterFourniture(new Fourniture(categorie, copie(montants))));
        e.getLignesDetail().forEach(l -> r.ajouterLigneDetail(ligne(l)));
        lireValeursCommentaire(r);
    }

    /** (Re)lit les valeurs clés du commentaire de l'expert (F12) : à l'import et après sa modification. */
    public static void lireValeursCommentaire(RapportExpertise r) {
        r.remplacerValeursCommentaire(ExtracteurCommentaire.extraire(r.getCommentaire()).stream()
                .map(v -> new ValeurCommentaire(v.cle(), v.texte(), v.montant(), null)).toList());
    }

    /** Enregistre les contrôles de cohérence et les champs à saisir (écran de vérification, alertes). */
    public static void ajouterResultats(RapportExpertise r, List<Controle> controles, List<String> champsASaisir,
                                 OffsetDateTime maintenant) {
        for (Controle c : controles) {
            r.ajouterResultatControle(new ResultatControle(TypeControle.COHERENCE, c.code(), tronquer(c.nom(), 200),
                    CHAMP_PAR_CONTROLE.get(c.code()), c.attendu(), c.obtenu(), c.ok(), c.bloquant(), maintenant));
        }
        // Un champ absent du PDF est signalé ; seules les données essentielles empêchent la validation.
        for (String champ : champsASaisir) {
            r.ajouterResultatControle(new ResultatControle(TypeControle.CHAMP_MANQUANT, "CHAMP_MANQUANT",
                    "Champ absent du PDF : " + champ, champ, null, null, false,
                    ControleCoherenceService.champEssentiel(champ), maintenant));
        }
    }

    private static Partie partie(TypePartie type, PartieExtraite e) {
        Partie p = new Partie(type);
        p.setNomPrenom(tronquer(e.getNomPrenom(), 150));
        p.setVehiculeLibelle(tronquer(e.getVehicule(), 100));
        p.setImmatriculation(tronquer(e.getImmatriculation(), 20));
        p.setCompagnie(tronquer(e.getCompagnie(), 100));
        p.setNumeroPolice(tronquer(e.getNumeroPolice(), 30));
        p.setIntermediaire(tronquer(e.getIntermediaire(), 150));
        return p;
    }

    private static Vehicule vehicule(VehiculeExtrait e) {
        Vehicule v = new Vehicule();
        v.setMarque(tronquer(e.getMarque(), 50));
        v.setModele(tronquer(e.getModele(), 80));
        v.setTypeMine(tronquer(e.getTypeMine(), 80));
        v.setImmatriculation(tronquer(e.getImmatriculation(), 20));
        v.setNumeroSerie(tronquer(e.getNumeroSerie(), 30));
        v.setEnergie(e.getEnergie());
        v.setKilometrage(e.getKilometrage() != null && e.getKilometrage() >= 0 ? e.getKilometrage() : null);
        v.setKilometrageNonEstimable(e.isKilometrageNonEstimable());
        v.setPuissanceFiscale(e.getPuissanceFiscale() != null && e.getPuissanceFiscale() >= 0 ? e.getPuissanceFiscale() : null);
        v.setDateMiseCirculation(e.getDateMiseCirculation());
        v.setEtatGeneral(tronquer(e.getEtatGeneral(), 50));
        v.setUsurePneuAvg(pourcentage(e.getUsurePneuAvg()));
        v.setUsurePneuAvd(pourcentage(e.getUsurePneuAvd()));
        v.setUsurePneuArg(pourcentage(e.getUsurePneuArg()));
        v.setUsurePneuArd(pourcentage(e.getUsurePneuArd()));
        return v;
    }

    private static LigneDetail ligne(LigneDetailExtraite e) {
        String designation = e.getDesignation() == null ? "(sans désignation)" : tronquer(e.getDesignation(), 150);
        LigneDetail l = new LigneDetail(e.getQuantite(), designation);
        l.setZoneLibelle(tronquer(e.getZoneLibelle(), 60));
        l.setZoneChoc(e.getZoneChoc());
        l.setFamille(e.getFamille());
        l.setTypePiece(e.getTypePiece());
        l.setPrixUnitaire(e.getPrixUnitaire());
        l.setVetustePct(pourcentage(e.getVetustePct()));
        l.setTvaPct(pourcentage(e.getTvaPct()));
        l.setTotalFourniture(e.getTotalFourniture());
        l.setOperation(e.getOperation());
        l.setNombreHeures(e.getNombreHeures());
        l.setTarifHoraire(e.getTarifHoraire());
        l.setMontantMoTtc(e.getMontantMoTtc());
        return l;
    }

    private static boolean estVide(PartieExtraite p) {
        return p.getNomPrenom() == null && p.getVehicule() == null && p.getImmatriculation() == null
                && p.getCompagnie() == null && p.getNumeroPolice() == null && p.getIntermediaire() == null;
    }

    private static Montants copie(Montants m) {
        return m == null ? null : new Montants(m.getHt(), m.getTva(), m.getTtc());
    }

    /** Un pourcentage hors de 0–100 est une valeur mal lue : laissée vide plutôt que rejetée par la base. */
    private static BigDecimal pourcentage(BigDecimal valeur) {
        return valeur == null || valeur.signum() < 0 || valeur.compareTo(BigDecimal.valueOf(100)) > 0 ? null : valeur;
    }

    private static String tronquer(String valeur, int longueur) {
        return valeur == null || valeur.length() <= longueur ? valeur : valeur.substring(0, longueur);
    }
}
