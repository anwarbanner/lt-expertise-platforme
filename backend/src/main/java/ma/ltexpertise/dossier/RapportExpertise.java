package ma.ltexpertise.dossier;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import ma.ltexpertise.commun.Montants;
import ma.ltexpertise.dossier.referentiel.NatureContrat;
import ma.ltexpertise.dossier.referentiel.NatureEvenement;
import ma.ltexpertise.dossier.referentiel.NatureRapport;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.dossier.referentiel.TypePartie;
import ma.ltexpertise.dossier.referentiel.TypeProcedure;
import ma.ltexpertise.dossier.referentiel.TypeReforme;
import ma.ltexpertise.dossier.referentiel.ZoneChoc;
import ma.ltexpertise.importation.ImportFichier;
import ma.ltexpertise.utilisateur.Role;
import ma.ltexpertise.utilisateur.Utilisateur;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Dossier d'expertise : racine de l'agrégat. Parties, véhicule, évaluation des dommages,
 * lignes de détail, valeurs du commentaire et résultats de contrôle sont enregistrés et supprimés avec lui.
 */
@Entity
@Table(name = "rapport_expertise")
@EntityListeners(AuditingEntityListener.class)
public class RapportExpertise {

    private static final Set<StatutDossier> STATUTS_VALIDABLES =
            EnumSet.of(StatutDossier.IMPORTE, StatutDossier.A_COMPLETER, StatutDossier.A_VERIFIER, StatutDossier.PRET_A_VALIDER);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)
    private String numeroRapport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NatureRapport natureRapport = NatureRapport.DEFINITIF;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private TypeProcedure typeProcedure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatutDossier statut;

    @Column(length = 100)
    private String cabinet;

    @Column(length = 100)
    private String expert;

    @Column(length = 100)
    private String compagnie;

    @Column(length = 30)
    private String refSinistre;

    private LocalDate dateSinistre;
    private LocalDate dateMission;
    private LocalDate dateExpertise;
    private LocalDate dateAvantReparation;
    private LocalDate dateEnCoursTravaux;
    private LocalDate dateApresTravaux;
    private LocalDate dateRapport;

    @Column(length = 60)
    private String lieuRapport;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private NatureContrat natureContrat;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private NatureEvenement natureEvenement;

    /** Zones de choc (P8) : un rapport peut en lister plusieurs sur la page 1. */
    @ElementCollection
    @CollectionTable(name = "rapport_zone_choc", joinColumns = @JoinColumn(name = "rapport_id"))
    @Column(name = "zone_choc", nullable = false)
    private Set<ZoneChoc> zonesChoc = new HashSet<>();

    @Column(length = 150)
    private String reparateur;

    private Boolean accordReparateur;

    @Embedded
    @AttributeOverride(name = "ht", column = @Column(name = "total_mo_ht", precision = 12, scale = 2))
    @AttributeOverride(name = "tva", column = @Column(name = "total_mo_tva", precision = 12, scale = 2))
    @AttributeOverride(name = "ttc", column = @Column(name = "total_mo_ttc", precision = 12, scale = 2))
    private Montants totalMainOeuvre;

    @Embedded
    @AttributeOverride(name = "ht", column = @Column(name = "total_fournitures_ht", precision = 12, scale = 2))
    @AttributeOverride(name = "tva", column = @Column(name = "total_fournitures_tva", precision = 12, scale = 2))
    @AttributeOverride(name = "ttc", column = @Column(name = "total_fournitures_ttc", precision = 12, scale = 2))
    private Montants totalFournitures;

    @Embedded
    @AttributeOverride(name = "ht", column = @Column(name = "total_general_ht", precision = 12, scale = 2))
    @AttributeOverride(name = "tva", column = @Column(name = "total_general_tva", precision = 12, scale = 2))
    @AttributeOverride(name = "ttc", column = @Column(name = "total_general_ttc", precision = 12, scale = 2))
    private Montants totalGeneral;

    @Column(precision = 12, scale = 2)
    private BigDecimal vetusteTtc;

    /** Imprimée à la place de la vétusté TTC quand la TVA est récupérable. */
    @Column(precision = 12, scale = 2)
    private BigDecimal vetusteHt;

    @Column(precision = 12, scale = 2)
    private BigDecimal depannage;

    @Column(precision = 12, scale = 2)
    private BigDecimal immobilisation;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private TypeReforme typeReforme;

    @Column(length = 100)
    private String epavisteNom;

    @Column(length = 60)
    private String epavisteVille;

    @Column(precision = 12, scale = 2)
    private BigDecimal valeurNeuf;

    @Column(precision = 12, scale = 2)
    private BigDecimal valeurAssuree;

    @Column(precision = 12, scale = 2)
    private BigDecimal valeurVenale;

    @Column(precision = 12, scale = 2)
    private BigDecimal valeurEpave;

    /** P5 : récupérable, indemnisation sur base HT ; sinon TTC. */
    private Boolean tvaRecuperable;

    @Column(precision = 12, scale = 2)
    private BigDecimal montantIndemnisation;

    @Column(length = 300)
    private String montantLettres;

    /** Texte intégral du commentaire de l'expert, toujours conservé (F12). */
    @Column(columnDefinition = "text")
    private String commentaire;

    @Column(length = 40)
    private String gabaritVersion;

    /** PDF original (P1) ; absent pour un dossier repris du fichier Excel (F14). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_fichier_id")
    private ImportFichier importFichier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par")
    private Utilisateur validePar;

    private OffsetDateTime valideLe;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime creeLe;

    @CreatedBy
    @Column(length = 50, updatable = false)
    private String creePar;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime modifieLe;

    @LastModifiedBy
    @Column(length = 50)
    private String modifiePar;

    @Version
    private int version;

    @OneToMany(mappedBy = "rapport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Partie> parties = new ArrayList<>();

    @OneToOne(mappedBy = "rapport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Vehicule vehicule;

    @OneToMany(mappedBy = "rapport", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy // ordre d'insertion = ordre du PDF
    private List<EvaluationMainOeuvre> evaluationsMainOeuvre = new ArrayList<>();

    @OneToMany(mappedBy = "rapport", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy
    private List<Fourniture> fournitures = new ArrayList<>();

    @OneToMany(mappedBy = "rapport", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre")
    private List<LigneDetail> lignesDetail = new ArrayList<>();

    @OneToMany(mappedBy = "rapport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ValeurCommentaire> valeursCommentaire = new ArrayList<>();

    @OneToMany(mappedBy = "rapport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResultatControle> resultatsControle = new ArrayList<>();

    protected RapportExpertise() {
    }

    /**
     * @param numeroRapport obligatoire sauf pour un dossier {@link StatutDossier#EN_COURS}
     * @param statutInitial tout statut sauf {@link StatutDossier#VALIDE}, atteint uniquement par {@link #valider}
     */
    public RapportExpertise(String numeroRapport, StatutDossier statutInitial) {
        verifierStatutHorsValidation(statutInitial);
        this.numeroRapport = numeroRapport;
        this.statut = statutInitial;
    }

    // ------------------------------------------------------------------ cycle de vie (P3, P6)

    /**
     * Valide le dossier (P3). Seul un gestionnaire ou un administrateur actif (qui a tous les droits) peut valider ;
     * un rapport non définitif ne peut pas l'être.
     */
    public void valider(Utilisateur gestionnaire, OffsetDateTime maintenant) {
        Objects.requireNonNull(gestionnaire, "gestionnaire");
        boolean habilite = gestionnaire.aLeRole(Role.GESTIONNAIRE) || gestionnaire.aLeRole(Role.ADMINISTRATEUR);
        if (!habilite || !gestionnaire.isActif()) {
            throw new ValidationRefuseeException("Seul un gestionnaire ou un administrateur actif peut valider un dossier.");
        }
        if (natureRapport == NatureRapport.NON_DEFINITIF) {
            throw new ValidationRefuseeException("Un rapport non définitif ne peut pas être validé.");
        }
        if (!STATUTS_VALIDABLES.contains(statut)) {
            throw new ValidationRefuseeException("Un dossier au statut " + statut + " ne peut pas être validé.");
        }
        this.statut = StatutDossier.VALIDE;
        this.validePar = gestionnaire;
        this.valideLe = Objects.requireNonNull(maintenant, "maintenant");
    }

    /** Change le statut, sauf vers VALIDE qui passe obligatoirement par {@link #valider}. */
    public void changerStatut(StatutDossier nouveauStatut) {
        verifierStatutHorsValidation(nouveauStatut);
        if (statut == StatutDossier.VALIDE) {
            // Un dossier validé puis modifié doit être validé de nouveau (P3, P6) ; la trace reste au journal.
            validePar = null;
            valideLe = null;
        }
        this.statut = nouveauStatut;
    }

    private static void verifierStatutHorsValidation(StatutDossier statut) {
        Objects.requireNonNull(statut, "statut");
        if (statut == StatutDossier.VALIDE) {
            throw new ValidationRefuseeException("Le statut VALIDE s'obtient uniquement par la validation d'un gestionnaire.");
        }
    }

    // ------------------------------------------------------------------ composition de l'agrégat

    public void ajouterPartie(Partie partie) {
        if (getPartie(partie.getTypePartie()).isPresent()) {
            throw new IllegalArgumentException("Partie déjà présente : " + partie.getTypePartie());
        }
        partie.rattacher(this);
        parties.add(partie);
    }

    public void retirerPartie(TypePartie type) {
        parties.removeIf(p -> p.getTypePartie() == type);
    }

    public void definirVehicule(Vehicule vehicule) {
        if (vehicule != null) {
            vehicule.rattacher(this);
        }
        this.vehicule = vehicule;
    }

    public void ajouterEvaluationMainOeuvre(EvaluationMainOeuvre evaluation) {
        evaluation.rattacher(this);
        evaluationsMainOeuvre.add(evaluation);
    }

    public void ajouterFourniture(Fourniture fourniture) {
        fourniture.rattacher(this);
        fournitures.add(fourniture);
    }

    /** Ajoute la ligne à la suite des précédentes, dans l'ordre du PDF. */
    public void ajouterLigneDetail(LigneDetail ligne) {
        short ordre = (short) (lignesDetail.isEmpty() ? 0 : lignesDetail.getLast().getOrdre() + 1);
        ligne.rattacher(this, ordre);
        lignesDetail.add(ligne);
    }

    public void ajouterValeurCommentaire(ValeurCommentaire valeur) {
        valeur.rattacher(this);
        valeursCommentaire.add(valeur);
    }

    /**
     * Remplace les valeurs clés du commentaire (F12) par celles données. Une clé déjà présente est mise à jour sur
     * place : Hibernate insère avant de supprimer, un retrait suivi d'un ajout violerait l'unicité (rapport, clé).
     */
    public void remplacerValeursCommentaire(List<ValeurCommentaire> nouvelles) {
        valeursCommentaire.removeIf(v -> nouvelles.stream().noneMatch(n -> n.getCle() == v.getCle()));
        for (ValeurCommentaire n : nouvelles) {
            valeursCommentaire.stream().filter(v -> v.getCle() == n.getCle()).findFirst().ifPresentOrElse(
                    v -> v.remplacer(n.getValeurTexte(), n.getValeurMontant(), n.getValeurDate()),
                    () -> ajouterValeurCommentaire(n));
        }
    }

    /** Supprime les résultats de contrôle avant un nouveau calcul (après une modification). */
    public void viderResultatsControle() {
        resultatsControle.clear();
    }

    public void ajouterResultatControle(ResultatControle resultat) {
        resultat.rattacher(this);
        resultatsControle.add(resultat);
    }

    public Optional<Partie> getPartie(TypePartie type) {
        return parties.stream().filter(p -> p.getTypePartie() == type).findFirst();
    }

    public List<Partie> getParties() {
        return Collections.unmodifiableList(parties);
    }

    public Vehicule getVehicule() {
        return vehicule;
    }

    public List<EvaluationMainOeuvre> getEvaluationsMainOeuvre() {
        return Collections.unmodifiableList(evaluationsMainOeuvre);
    }

    public List<Fourniture> getFournitures() {
        return Collections.unmodifiableList(fournitures);
    }

    public List<LigneDetail> getLignesDetail() {
        return Collections.unmodifiableList(lignesDetail);
    }

    public List<ValeurCommentaire> getValeursCommentaire() {
        return Collections.unmodifiableList(valeursCommentaire);
    }

    public List<ResultatControle> getResultatsControle() {
        return Collections.unmodifiableList(resultatsControle);
    }

    // ------------------------------------------------------------------ accesseurs

    public Long getId() {
        return id;
    }

    public String getNumeroRapport() {
        return numeroRapport;
    }

    public void setNumeroRapport(String numeroRapport) {
        this.numeroRapport = numeroRapport;
    }

    public NatureRapport getNatureRapport() {
        return natureRapport;
    }

    public void setNatureRapport(NatureRapport natureRapport) {
        this.natureRapport = Objects.requireNonNull(natureRapport);
    }

    public TypeProcedure getTypeProcedure() {
        return typeProcedure;
    }

    public void setTypeProcedure(TypeProcedure typeProcedure) {
        this.typeProcedure = typeProcedure;
    }

    public StatutDossier getStatut() {
        return statut;
    }

    public String getCabinet() {
        return cabinet;
    }

    public void setCabinet(String cabinet) {
        this.cabinet = cabinet;
    }

    public String getExpert() {
        return expert;
    }

    public void setExpert(String expert) {
        this.expert = expert;
    }

    public String getCompagnie() {
        return compagnie;
    }

    public void setCompagnie(String compagnie) {
        this.compagnie = compagnie;
    }

    public String getRefSinistre() {
        return refSinistre;
    }

    public void setRefSinistre(String refSinistre) {
        this.refSinistre = refSinistre;
    }

    public LocalDate getDateSinistre() {
        return dateSinistre;
    }

    public void setDateSinistre(LocalDate dateSinistre) {
        this.dateSinistre = dateSinistre;
    }

    public LocalDate getDateMission() {
        return dateMission;
    }

    public void setDateMission(LocalDate dateMission) {
        this.dateMission = dateMission;
    }

    public LocalDate getDateExpertise() {
        return dateExpertise;
    }

    public void setDateExpertise(LocalDate dateExpertise) {
        this.dateExpertise = dateExpertise;
    }

    public LocalDate getDateAvantReparation() {
        return dateAvantReparation;
    }

    public void setDateAvantReparation(LocalDate dateAvantReparation) {
        this.dateAvantReparation = dateAvantReparation;
    }

    public LocalDate getDateEnCoursTravaux() {
        return dateEnCoursTravaux;
    }

    public void setDateEnCoursTravaux(LocalDate dateEnCoursTravaux) {
        this.dateEnCoursTravaux = dateEnCoursTravaux;
    }

    public LocalDate getDateApresTravaux() {
        return dateApresTravaux;
    }

    public void setDateApresTravaux(LocalDate dateApresTravaux) {
        this.dateApresTravaux = dateApresTravaux;
    }

    public LocalDate getDateRapport() {
        return dateRapport;
    }

    public void setDateRapport(LocalDate dateRapport) {
        this.dateRapport = dateRapport;
    }

    public String getLieuRapport() {
        return lieuRapport;
    }

    public void setLieuRapport(String lieuRapport) {
        this.lieuRapport = lieuRapport;
    }

    public NatureContrat getNatureContrat() {
        return natureContrat;
    }

    public void setNatureContrat(NatureContrat natureContrat) {
        this.natureContrat = natureContrat;
    }

    public NatureEvenement getNatureEvenement() {
        return natureEvenement;
    }

    public void setNatureEvenement(NatureEvenement natureEvenement) {
        this.natureEvenement = natureEvenement;
    }

    /** Zones de choc dans l'ordre de leur code. */
    public List<ZoneChoc> getZonesChoc() {
        return zonesChoc.stream().sorted(Comparator.comparingInt(ZoneChoc::getCode)).toList();
    }

    public void setZonesChoc(Collection<ZoneChoc> zones) {
        zonesChoc.clear();
        zonesChoc.addAll(zones);
    }

    public String getReparateur() {
        return reparateur;
    }

    public void setReparateur(String reparateur) {
        this.reparateur = reparateur;
    }

    public Boolean getAccordReparateur() {
        return accordReparateur;
    }

    public void setAccordReparateur(Boolean accordReparateur) {
        this.accordReparateur = accordReparateur;
    }

    public Montants getTotalMainOeuvre() {
        return totalMainOeuvre;
    }

    public void setTotalMainOeuvre(Montants totalMainOeuvre) {
        this.totalMainOeuvre = totalMainOeuvre;
    }

    public Montants getTotalFournitures() {
        return totalFournitures;
    }

    public void setTotalFournitures(Montants totalFournitures) {
        this.totalFournitures = totalFournitures;
    }

    public Montants getTotalGeneral() {
        return totalGeneral;
    }

    public void setTotalGeneral(Montants totalGeneral) {
        this.totalGeneral = totalGeneral;
    }

    public BigDecimal getVetusteTtc() {
        return vetusteTtc;
    }

    public void setVetusteTtc(BigDecimal vetusteTtc) {
        this.vetusteTtc = vetusteTtc;
    }

    public BigDecimal getVetusteHt() {
        return vetusteHt;
    }

    public void setVetusteHt(BigDecimal vetusteHt) {
        this.vetusteHt = vetusteHt;
    }

    public BigDecimal getDepannage() {
        return depannage;
    }

    public void setDepannage(BigDecimal depannage) {
        this.depannage = depannage;
    }

    public BigDecimal getImmobilisation() {
        return immobilisation;
    }

    public void setImmobilisation(BigDecimal immobilisation) {
        this.immobilisation = immobilisation;
    }

    public TypeReforme getTypeReforme() {
        return typeReforme;
    }

    public void setTypeReforme(TypeReforme typeReforme) {
        this.typeReforme = typeReforme;
    }

    public String getEpavisteNom() {
        return epavisteNom;
    }

    public void setEpavisteNom(String epavisteNom) {
        this.epavisteNom = epavisteNom;
    }

    public String getEpavisteVille() {
        return epavisteVille;
    }

    public void setEpavisteVille(String epavisteVille) {
        this.epavisteVille = epavisteVille;
    }

    public BigDecimal getValeurNeuf() {
        return valeurNeuf;
    }

    public void setValeurNeuf(BigDecimal valeurNeuf) {
        this.valeurNeuf = valeurNeuf;
    }

    public BigDecimal getValeurAssuree() {
        return valeurAssuree;
    }

    public void setValeurAssuree(BigDecimal valeurAssuree) {
        this.valeurAssuree = valeurAssuree;
    }

    public BigDecimal getValeurVenale() {
        return valeurVenale;
    }

    public void setValeurVenale(BigDecimal valeurVenale) {
        this.valeurVenale = valeurVenale;
    }

    public BigDecimal getValeurEpave() {
        return valeurEpave;
    }

    public void setValeurEpave(BigDecimal valeurEpave) {
        this.valeurEpave = valeurEpave;
    }

    public Boolean getTvaRecuperable() {
        return tvaRecuperable;
    }

    public void setTvaRecuperable(Boolean tvaRecuperable) {
        this.tvaRecuperable = tvaRecuperable;
    }

    public BigDecimal getMontantIndemnisation() {
        return montantIndemnisation;
    }

    public void setMontantIndemnisation(BigDecimal montantIndemnisation) {
        this.montantIndemnisation = montantIndemnisation;
    }

    public String getMontantLettres() {
        return montantLettres;
    }

    public void setMontantLettres(String montantLettres) {
        this.montantLettres = montantLettres;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public String getGabaritVersion() {
        return gabaritVersion;
    }

    public void setGabaritVersion(String gabaritVersion) {
        this.gabaritVersion = gabaritVersion;
    }

    public ImportFichier getImportFichier() {
        return importFichier;
    }

    public void setImportFichier(ImportFichier importFichier) {
        this.importFichier = importFichier;
    }

    public Utilisateur getValidePar() {
        return validePar;
    }

    public OffsetDateTime getValideLe() {
        return valideLe;
    }

    public OffsetDateTime getCreeLe() {
        return creeLe;
    }

    public String getCreePar() {
        return creePar;
    }

    public OffsetDateTime getModifieLe() {
        return modifieLe;
    }

    public String getModifiePar() {
        return modifiePar;
    }

    public int getVersion() {
        return version;
    }
}
