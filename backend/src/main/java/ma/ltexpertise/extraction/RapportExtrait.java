package ma.ltexpertise.extraction;

import ma.ltexpertise.commun.Montants;
import ma.ltexpertise.dossier.EvaluationMainOeuvre;
import ma.ltexpertise.dossier.Fourniture;
import ma.ltexpertise.dossier.LigneDetail;
import ma.ltexpertise.dossier.Partie;
import ma.ltexpertise.dossier.RapportExpertise;
import ma.ltexpertise.dossier.Vehicule;
import ma.ltexpertise.dossier.referentiel.TypePartie;
import ma.ltexpertise.dossier.referentiel.CategorieFourniture;
import ma.ltexpertise.dossier.referentiel.NatureContrat;
import ma.ltexpertise.dossier.referentiel.NatureEvenement;
import ma.ltexpertise.dossier.referentiel.NatureRapport;
import ma.ltexpertise.dossier.referentiel.PosteMainOeuvre;
import ma.ltexpertise.dossier.referentiel.TypeProcedure;
import ma.ltexpertise.dossier.referentiel.TypeReforme;
import ma.ltexpertise.dossier.referentiel.ZoneChoc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Données lues dans un rapport, avant enregistrement. Les champs absents ou masqués valent {@code null}
 * et leur chemin figure dans {@link #getChampsASaisir()} : l'utilisateur les complète à l'écran de vérification.
 */
public class RapportExtrait {

    String gabarit;

    // En-tête
    String numeroRapport;
    NatureRapport natureRapport = NatureRapport.DEFINITIF;
    TypeProcedure typeProcedure;
    String cabinet;
    String expert;
    String refSinistre;
    LocalDate dateSinistre;
    LocalDate dateMission;
    String compagnie;

    // Parties et contrat
    final PartieExtraite assure = new PartieExtraite();
    final PartieExtraite adversaire = new PartieExtraite();
    NatureContrat natureContrat;
    String reparateur;
    Boolean accordReparateur;

    // Véhicule et sinistre
    final VehiculeExtrait vehicule = new VehiculeExtrait();
    NatureEvenement natureEvenement;
    /** Zones de choc de la page 1, dans l'ordre du PDF (plusieurs possibles). */
    final List<ZoneChoc> zonesChoc = new ArrayList<>();
    LocalDate dateExpertise;
    LocalDate dateAvantReparation;
    LocalDate dateEnCoursTravaux;
    LocalDate dateApresTravaux;

    // Évaluation des dommages
    final Map<PosteMainOeuvre, LigneMainOeuvreExtraite> mainOeuvre = new EnumMap<>(PosteMainOeuvre.class);
    final Map<CategorieFourniture, Montants> fournitures = new EnumMap<>(CategorieFourniture.class);
    Montants totalMainOeuvre;
    Montants totalFournitures;
    Montants totalGeneral;

    // Conclusion
    BigDecimal vetusteTtc;
    /** Imprimée à la place de la vétusté TTC quand la TVA est récupérable (colonne à ajouter au modèle, V2). */
    BigDecimal vetusteHt;
    BigDecimal immobilisation;
    BigDecimal depannage;
    TypeReforme typeReforme;
    String epavisteNom;
    String epavisteVille;
    BigDecimal valeurNeuf;
    BigDecimal valeurAssuree;
    BigDecimal valeurVenale;
    BigDecimal valeurEpave;
    Boolean tvaRecuperable;
    BigDecimal montantIndemnisation;
    String montantLettres;
    String lieuRapport;
    LocalDate dateRapport;

    // Page 2 et suivantes
    final List<LigneDetailExtraite> lignesDetail = new ArrayList<>();
    String commentaire;

    final List<String> champsASaisir = new ArrayList<>();

    /**
     * Reconstitue les données d'un dossier enregistré, pour recalculer contrôles et champs à saisir après une
     * modification à l'écran de vérification. À appeler dans une transaction (collections du dossier).
     */
    public static RapportExtrait depuis(RapportExpertise d) {
        RapportExtrait r = new RapportExtrait();
        r.gabarit = d.getGabaritVersion();
        r.numeroRapport = d.getNumeroRapport();
        r.natureRapport = d.getNatureRapport();
        r.typeProcedure = d.getTypeProcedure();
        r.cabinet = d.getCabinet();
        r.expert = d.getExpert();
        r.refSinistre = d.getRefSinistre();
        r.dateSinistre = d.getDateSinistre();
        r.dateMission = d.getDateMission();
        r.compagnie = d.getCompagnie();
        d.getPartie(TypePartie.ASSURE).ifPresent(p -> copier(p, r.assure));
        d.getPartie(TypePartie.ADVERSAIRE).ifPresent(p -> copier(p, r.adversaire));
        r.natureContrat = d.getNatureContrat();
        r.reparateur = d.getReparateur();
        r.accordReparateur = d.getAccordReparateur();
        Vehicule v = d.getVehicule();
        if (v != null) {
            r.vehicule.marque = v.getMarque();
            r.vehicule.modele = v.getModele();
            r.vehicule.typeMine = v.getTypeMine();
            r.vehicule.immatriculation = v.getImmatriculation();
            r.vehicule.numeroSerie = v.getNumeroSerie();
            r.vehicule.energie = v.getEnergie();
            r.vehicule.kilometrage = v.getKilometrage();
            r.vehicule.kilometrageNonEstimable = v.isKilometrageNonEstimable();
            r.vehicule.puissanceFiscale = v.getPuissanceFiscale();
            r.vehicule.dateMiseCirculation = v.getDateMiseCirculation();
            r.vehicule.etatGeneral = v.getEtatGeneral();
            r.vehicule.usurePneuAvg = v.getUsurePneuAvg();
            r.vehicule.usurePneuAvd = v.getUsurePneuAvd();
            r.vehicule.usurePneuArg = v.getUsurePneuArg();
            r.vehicule.usurePneuArd = v.getUsurePneuArd();
        }
        r.natureEvenement = d.getNatureEvenement();
        r.zonesChoc.addAll(d.getZonesChoc());
        r.dateExpertise = d.getDateExpertise();
        r.dateAvantReparation = d.getDateAvantReparation();
        r.dateEnCoursTravaux = d.getDateEnCoursTravaux();
        r.dateApresTravaux = d.getDateApresTravaux();
        for (EvaluationMainOeuvre e : d.getEvaluationsMainOeuvre()) {
            r.mainOeuvre.put(e.getPoste(), new LigneMainOeuvreExtraite(e.getPoste(), e.getNombreHeures(),
                    e.getTauxHoraire(), e.getMontants()));
        }
        for (Fourniture f : d.getFournitures()) {
            r.fournitures.put(f.getCategorie(), f.getMontants());
        }
        r.totalMainOeuvre = d.getTotalMainOeuvre();
        r.totalFournitures = d.getTotalFournitures();
        r.totalGeneral = d.getTotalGeneral();
        r.vetusteTtc = d.getVetusteTtc();
        r.vetusteHt = d.getVetusteHt();
        r.immobilisation = d.getImmobilisation();
        r.depannage = d.getDepannage();
        r.typeReforme = d.getTypeReforme();
        r.epavisteNom = d.getEpavisteNom();
        r.epavisteVille = d.getEpavisteVille();
        r.valeurNeuf = d.getValeurNeuf();
        r.valeurAssuree = d.getValeurAssuree();
        r.valeurVenale = d.getValeurVenale();
        r.valeurEpave = d.getValeurEpave();
        r.tvaRecuperable = d.getTvaRecuperable();
        r.montantIndemnisation = d.getMontantIndemnisation();
        r.montantLettres = d.getMontantLettres();
        r.lieuRapport = d.getLieuRapport();
        r.dateRapport = d.getDateRapport();
        for (LigneDetail l : d.getLignesDetail()) {
            LigneDetailExtraite x = new LigneDetailExtraite();
            x.zoneLibelle = l.getZoneLibelle();
            x.zoneChoc = l.getZoneChoc();
            x.famille = l.getFamille();
            x.quantite = l.getQuantite();
            x.designation = l.getDesignation();
            x.typePiece = l.getTypePiece();
            x.prixUnitaire = l.getPrixUnitaire();
            x.vetustePct = l.getVetustePct();
            x.tvaPct = l.getTvaPct();
            x.totalFourniture = l.getTotalFourniture();
            x.operation = l.getOperation();
            x.nombreHeures = l.getNombreHeures();
            x.tarifHoraire = l.getTarifHoraire();
            x.montantMoTtc = l.getMontantMoTtc();
            r.lignesDetail.add(x);
        }
        r.commentaire = d.getCommentaire();
        r.champsASaisir.addAll(r.champsManquants(!r.adversaire.estVide()));
        return r;
    }

    private static void copier(Partie source, PartieExtraite cible) {
        cible.nomPrenom = source.getNomPrenom();
        cible.vehicule = source.getVehiculeLibelle();
        cible.immatriculation = source.getImmatriculation();
        cible.compagnie = source.getCompagnie();
        cible.numeroPolice = source.getNumeroPolice();
        cible.intermediaire = source.getIntermediaire();
    }

    /** Chemins des champs obligatoires restés vides ; ceux de l'adversaire seulement s'il y en a un. */
    List<String> champsManquants(boolean inclureAdversaire) {
        Map<String, Object> champs = new LinkedHashMap<>();
        champs.put("cabinet", cabinet);
        champs.put("expert", expert);
        champs.put("refSinistre", refSinistre);
        champs.put("dateSinistre", dateSinistre);
        champs.put("dateMission", dateMission);
        champs.put("compagnie", compagnie);
        champs.put("typeProcedure", typeProcedure);
        champs.put("tvaRecuperable", tvaRecuperable);
        ajouterPartie(champs, "assure", assure);
        if (inclureAdversaire) {
            ajouterPartie(champs, "adversaire", adversaire);
        }
        champs.put("natureContrat", natureContrat);
        champs.put("reparateur", reparateur);
        champs.put("vehicule.marque", vehicule.marque);
        champs.put("vehicule.modele", vehicule.modele);
        champs.put("vehicule.typeMine", vehicule.typeMine);
        champs.put("vehicule.immatriculation", vehicule.immatriculation);
        champs.put("vehicule.numeroSerie", vehicule.numeroSerie);
        champs.put("vehicule.energie", vehicule.energie);
        if (!vehicule.kilometrageNonEstimable) {
            champs.put("vehicule.kilometrage", vehicule.kilometrage);
        }
        champs.put("vehicule.dateMiseCirculation", vehicule.dateMiseCirculation);
        champs.put("vehicule.etatGeneral", vehicule.etatGeneral);
        champs.put("dateExpertise", dateExpertise);
        champs.put("dateAvantReparation", dateAvantReparation);
        champs.put("dateEnCoursTravaux", dateEnCoursTravaux);
        champs.put("dateApresTravaux", dateApresTravaux);
        champs.put("montantIndemnisation", montantIndemnisation);
        champs.put("montantLettres", montantLettres);
        champs.put("lieuRapport", lieuRapport);
        champs.put("dateRapport", dateRapport);
        champs.put("commentaire", commentaire);
        return champs.entrySet().stream().filter(e -> e.getValue() == null).map(Map.Entry::getKey).toList();
    }

    private static void ajouterPartie(Map<String, Object> champs, String prefixe, PartieExtraite p) {
        champs.put(prefixe + ".nomPrenom", p.nomPrenom);
        champs.put(prefixe + ".vehicule", p.vehicule);
        champs.put(prefixe + ".immatriculation", p.immatriculation);
        champs.put(prefixe + ".compagnie", p.compagnie);
        champs.put(prefixe + ".numeroPolice", p.numeroPolice);
        champs.put(prefixe + ".intermediaire", p.intermediaire);
    }

    public String getGabarit() {
        return gabarit;
    }

    public String getNumeroRapport() {
        return numeroRapport;
    }

    public NatureRapport getNatureRapport() {
        return natureRapport;
    }

    public TypeProcedure getTypeProcedure() {
        return typeProcedure;
    }

    public String getCabinet() {
        return cabinet;
    }

    public String getExpert() {
        return expert;
    }

    public String getRefSinistre() {
        return refSinistre;
    }

    public LocalDate getDateSinistre() {
        return dateSinistre;
    }

    public LocalDate getDateMission() {
        return dateMission;
    }

    public String getCompagnie() {
        return compagnie;
    }

    public PartieExtraite getAssure() {
        return assure;
    }

    public PartieExtraite getAdversaire() {
        return adversaire;
    }

    public NatureContrat getNatureContrat() {
        return natureContrat;
    }

    public String getReparateur() {
        return reparateur;
    }

    public Boolean getAccordReparateur() {
        return accordReparateur;
    }

    public VehiculeExtrait getVehicule() {
        return vehicule;
    }

    public NatureEvenement getNatureEvenement() {
        return natureEvenement;
    }

    public List<ZoneChoc> getZonesChoc() {
        return Collections.unmodifiableList(zonesChoc);
    }

    public LocalDate getDateExpertise() {
        return dateExpertise;
    }

    public LocalDate getDateAvantReparation() {
        return dateAvantReparation;
    }

    public LocalDate getDateEnCoursTravaux() {
        return dateEnCoursTravaux;
    }

    public LocalDate getDateApresTravaux() {
        return dateApresTravaux;
    }

    public Map<PosteMainOeuvre, LigneMainOeuvreExtraite> getMainOeuvre() {
        return Collections.unmodifiableMap(mainOeuvre);
    }

    public Map<CategorieFourniture, Montants> getFournitures() {
        return Collections.unmodifiableMap(fournitures);
    }

    public Montants getTotalMainOeuvre() {
        return totalMainOeuvre;
    }

    public Montants getTotalFournitures() {
        return totalFournitures;
    }

    public Montants getTotalGeneral() {
        return totalGeneral;
    }

    public BigDecimal getVetusteTtc() {
        return vetusteTtc;
    }

    public BigDecimal getVetusteHt() {
        return vetusteHt;
    }

    /** Vétusté à déduire, telle qu'imprimée : « Vétusté TTC » ou « Vétusté HT » (jamais les deux). */
    public BigDecimal getVetuste() {
        return vetusteTtc != null ? vetusteTtc : vetusteHt;
    }

    public BigDecimal getImmobilisation() {
        return immobilisation;
    }

    public BigDecimal getDepannage() {
        return depannage;
    }

    public TypeReforme getTypeReforme() {
        return typeReforme;
    }

    public String getEpavisteNom() {
        return epavisteNom;
    }

    public String getEpavisteVille() {
        return epavisteVille;
    }

    public BigDecimal getValeurNeuf() {
        return valeurNeuf;
    }

    public BigDecimal getValeurAssuree() {
        return valeurAssuree;
    }

    public BigDecimal getValeurVenale() {
        return valeurVenale;
    }

    public BigDecimal getValeurEpave() {
        return valeurEpave;
    }

    public Boolean getTvaRecuperable() {
        return tvaRecuperable;
    }

    public BigDecimal getMontantIndemnisation() {
        return montantIndemnisation;
    }

    public String getMontantLettres() {
        return montantLettres;
    }

    public String getLieuRapport() {
        return lieuRapport;
    }

    public LocalDate getDateRapport() {
        return dateRapport;
    }

    public List<LigneDetailExtraite> getLignesDetail() {
        return Collections.unmodifiableList(lignesDetail);
    }

    public String getCommentaire() {
        return commentaire;
    }

    /** Chemins des champs vides ou masqués, ex. {@code assure.nomPrenom}, {@code vehicule.marque}. */
    public List<String> getChampsASaisir() {
        return Collections.unmodifiableList(champsASaisir);
    }
}
