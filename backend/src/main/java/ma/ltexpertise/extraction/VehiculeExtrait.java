package ma.ltexpertise.extraction;

import ma.ltexpertise.dossier.referentiel.Energie;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Caractéristiques techniques du véhicule (page 1). */
public class VehiculeExtrait {

    String marque;
    String modele;
    String typeMine;
    String immatriculation;
    String numeroSerie;
    Energie energie;
    /** Énergie telle qu'imprimée quand elle ne correspond à aucune valeur connue. */
    String energieLibelle;
    Integer kilometrage;
    boolean kilometrageNonEstimable;
    Short puissanceFiscale;
    LocalDate dateMiseCirculation;
    String etatGeneral;
    BigDecimal usurePneuAvg;
    BigDecimal usurePneuAvd;
    BigDecimal usurePneuArg;
    BigDecimal usurePneuArd;

    public String getMarque() {
        return marque;
    }

    public String getModele() {
        return modele;
    }

    public String getTypeMine() {
        return typeMine;
    }

    public String getImmatriculation() {
        return immatriculation;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public Energie getEnergie() {
        return energie;
    }

    public String getEnergieLibelle() {
        return energieLibelle;
    }

    public Integer getKilometrage() {
        return kilometrage;
    }

    public boolean isKilometrageNonEstimable() {
        return kilometrageNonEstimable;
    }

    public Short getPuissanceFiscale() {
        return puissanceFiscale;
    }

    public LocalDate getDateMiseCirculation() {
        return dateMiseCirculation;
    }

    public String getEtatGeneral() {
        return etatGeneral;
    }

    public BigDecimal getUsurePneuAvg() {
        return usurePneuAvg;
    }

    public BigDecimal getUsurePneuAvd() {
        return usurePneuAvd;
    }

    public BigDecimal getUsurePneuArg() {
        return usurePneuArg;
    }

    public BigDecimal getUsurePneuArd() {
        return usurePneuArd;
    }
}
