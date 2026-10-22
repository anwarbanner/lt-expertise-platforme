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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import ma.ltexpertise.dossier.referentiel.Energie;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Véhicule expertisé, tel que décrit dans ce rapport (un véhicule par rapport) :
 * kilométrage et usure sont propres au sinistre.
 */
@Entity
@Table(name = "vehicule")
public class Vehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rapport_id", nullable = false, updatable = false)
    private RapportExpertise rapport;

    @Column(length = 50)
    private String marque;

    @Column(length = 80)
    private String modele;

    @Column(length = 80)
    private String typeMine;

    @Column(length = 20)
    private String immatriculation;

    @Column(length = 30)
    private String numeroSerie;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private Energie energie;

    private Integer kilometrage;

    @Column(nullable = false)
    private boolean kilometrageNonEstimable;

    private Short puissanceFiscale;

    private LocalDate dateMiseCirculation;

    @Column(length = 50)
    private String etatGeneral;

    @Column(precision = 5, scale = 2)
    private BigDecimal usurePneuAvg;

    @Column(precision = 5, scale = 2)
    private BigDecimal usurePneuAvd;

    @Column(precision = 5, scale = 2)
    private BigDecimal usurePneuArg;

    @Column(precision = 5, scale = 2)
    private BigDecimal usurePneuArd;

    public Vehicule() {
    }

    void rattacher(RapportExpertise rapport) {
        this.rapport = rapport;
    }

    public Long getId() {
        return id;
    }

    public RapportExpertise getRapport() {
        return rapport;
    }

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public String getModele() {
        return modele;
    }

    public void setModele(String modele) {
        this.modele = modele;
    }

    public String getTypeMine() {
        return typeMine;
    }

    public void setTypeMine(String typeMine) {
        this.typeMine = typeMine;
    }

    public String getImmatriculation() {
        return immatriculation;
    }

    public void setImmatriculation(String immatriculation) {
        this.immatriculation = immatriculation;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public Energie getEnergie() {
        return energie;
    }

    public void setEnergie(Energie energie) {
        this.energie = energie;
    }

    public Integer getKilometrage() {
        return kilometrage;
    }

    public void setKilometrage(Integer kilometrage) {
        this.kilometrage = kilometrage;
    }

    public boolean isKilometrageNonEstimable() {
        return kilometrageNonEstimable;
    }

    public void setKilometrageNonEstimable(boolean kilometrageNonEstimable) {
        this.kilometrageNonEstimable = kilometrageNonEstimable;
    }

    public Short getPuissanceFiscale() {
        return puissanceFiscale;
    }

    public void setPuissanceFiscale(Short puissanceFiscale) {
        this.puissanceFiscale = puissanceFiscale;
    }

    public LocalDate getDateMiseCirculation() {
        return dateMiseCirculation;
    }

    public void setDateMiseCirculation(LocalDate dateMiseCirculation) {
        this.dateMiseCirculation = dateMiseCirculation;
    }

    public String getEtatGeneral() {
        return etatGeneral;
    }

    public void setEtatGeneral(String etatGeneral) {
        this.etatGeneral = etatGeneral;
    }

    public BigDecimal getUsurePneuAvg() {
        return usurePneuAvg;
    }

    public void setUsurePneuAvg(BigDecimal usurePneuAvg) {
        this.usurePneuAvg = usurePneuAvg;
    }

    public BigDecimal getUsurePneuAvd() {
        return usurePneuAvd;
    }

    public void setUsurePneuAvd(BigDecimal usurePneuAvd) {
        this.usurePneuAvd = usurePneuAvd;
    }

    public BigDecimal getUsurePneuArg() {
        return usurePneuArg;
    }

    public void setUsurePneuArg(BigDecimal usurePneuArg) {
        this.usurePneuArg = usurePneuArg;
    }

    public BigDecimal getUsurePneuArd() {
        return usurePneuArd;
    }

    public void setUsurePneuArd(BigDecimal usurePneuArd) {
        this.usurePneuArd = usurePneuArd;
    }
}
