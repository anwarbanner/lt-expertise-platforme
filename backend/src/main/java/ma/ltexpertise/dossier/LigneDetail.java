package ma.ltexpertise.dossier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ma.ltexpertise.dossier.referentiel.FamilleTravaux;
import ma.ltexpertise.dossier.referentiel.TypeOperation;
import ma.ltexpertise.dossier.referentiel.TypePiece;
import ma.ltexpertise.dossier.referentiel.ZoneChoc;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Une ligne du tableau « Détail fournitures / travaux » (page 2) : partie fourniture à gauche,
 * partie travaux de réparation à droite. {@code ordre} restitue l'ordre du PDF (P10).
 */
@Entity
@Table(name = "ligne_detail")
public class LigneDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rapport_id", nullable = false, updatable = false)
    private RapportExpertise rapport;

    @Column(nullable = false)
    private short ordre;

    /** Libellé de section tel qu'imprimé, ex. « Avant/Lateral gauche ». */
    @Column(length = 60)
    private String zoneLibelle;

    private ZoneChoc zoneChoc;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private FamilleTravaux famille;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal quantite;

    @Column(nullable = false, length = 150)
    private String designation;

    /** Absent pour les lignes de main d'œuvre ou d'ingrédients (MOP, PP ET ING…). */
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private TypePiece typePiece;

    @Column(precision = 12, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(precision = 5, scale = 2)
    private BigDecimal vetustePct;

    @Column(precision = 5, scale = 2)
    private BigDecimal tvaPct;

    /** Colonne « Total » (TTC) de la partie fourniture. */
    @Column(precision = 12, scale = 2)
    private BigDecimal totalFourniture;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private TypeOperation operation;

    @Column(precision = 7, scale = 2)
    private BigDecimal nombreHeures;

    @Column(precision = 12, scale = 2)
    private BigDecimal tarifHoraire;

    @Column(precision = 12, scale = 2)
    private BigDecimal montantMoTtc;

    protected LigneDetail() {
    }

    public LigneDetail(BigDecimal quantite, String designation) {
        this.quantite = Objects.requireNonNull(quantite);
        this.designation = Objects.requireNonNull(designation);
    }

    void rattacher(RapportExpertise rapport, short ordre) {
        this.rapport = rapport;
        this.ordre = ordre;
    }

    public Long getId() {
        return id;
    }

    public RapportExpertise getRapport() {
        return rapport;
    }

    public short getOrdre() {
        return ordre;
    }

    public String getZoneLibelle() {
        return zoneLibelle;
    }

    public void setZoneLibelle(String zoneLibelle) {
        this.zoneLibelle = zoneLibelle;
    }

    public ZoneChoc getZoneChoc() {
        return zoneChoc;
    }

    public void setZoneChoc(ZoneChoc zoneChoc) {
        this.zoneChoc = zoneChoc;
    }

    public FamilleTravaux getFamille() {
        return famille;
    }

    public void setFamille(FamilleTravaux famille) {
        this.famille = famille;
    }

    public BigDecimal getQuantite() {
        return quantite;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public TypePiece getTypePiece() {
        return typePiece;
    }

    public void setTypePiece(TypePiece typePiece) {
        this.typePiece = typePiece;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public BigDecimal getVetustePct() {
        return vetustePct;
    }

    public void setVetustePct(BigDecimal vetustePct) {
        this.vetustePct = vetustePct;
    }

    public BigDecimal getTvaPct() {
        return tvaPct;
    }

    public void setTvaPct(BigDecimal tvaPct) {
        this.tvaPct = tvaPct;
    }

    public BigDecimal getTotalFourniture() {
        return totalFourniture;
    }

    public void setTotalFourniture(BigDecimal totalFourniture) {
        this.totalFourniture = totalFourniture;
    }

    public TypeOperation getOperation() {
        return operation;
    }

    public void setOperation(TypeOperation operation) {
        this.operation = operation;
    }

    public BigDecimal getNombreHeures() {
        return nombreHeures;
    }

    public void setNombreHeures(BigDecimal nombreHeures) {
        this.nombreHeures = nombreHeures;
    }

    public BigDecimal getTarifHoraire() {
        return tarifHoraire;
    }

    public void setTarifHoraire(BigDecimal tarifHoraire) {
        this.tarifHoraire = tarifHoraire;
    }

    public BigDecimal getMontantMoTtc() {
        return montantMoTtc;
    }

    public void setMontantMoTtc(BigDecimal montantMoTtc) {
        this.montantMoTtc = montantMoTtc;
    }
}
