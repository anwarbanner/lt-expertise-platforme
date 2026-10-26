package ma.ltexpertise.extraction;

import ma.ltexpertise.dossier.referentiel.FamilleTravaux;
import ma.ltexpertise.dossier.referentiel.TypeOperation;
import ma.ltexpertise.dossier.referentiel.TypePiece;
import ma.ltexpertise.dossier.referentiel.ZoneChoc;

import java.math.BigDecimal;

/** Une ligne du tableau « Détail fournitures / travaux » (page 2 et suivantes). */
public class LigneDetailExtraite {

    String zoneLibelle;
    ZoneChoc zoneChoc;
    FamilleTravaux famille;
    BigDecimal quantite;
    String designation;
    TypePiece typePiece;
    BigDecimal prixUnitaire;
    BigDecimal vetustePct;
    BigDecimal tvaPct;
    /** Colonne « Total » : TTC, vétusté déduite. */
    BigDecimal totalFourniture;
    TypeOperation operation;
    BigDecimal nombreHeures;
    BigDecimal tarifHoraire;
    BigDecimal montantMoTtc;

    public String getZoneLibelle() {
        return zoneLibelle;
    }

    public ZoneChoc getZoneChoc() {
        return zoneChoc;
    }

    public FamilleTravaux getFamille() {
        return famille;
    }

    public BigDecimal getQuantite() {
        return quantite;
    }

    public String getDesignation() {
        return designation;
    }

    public TypePiece getTypePiece() {
        return typePiece;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public BigDecimal getVetustePct() {
        return vetustePct;
    }

    public BigDecimal getTvaPct() {
        return tvaPct;
    }

    public BigDecimal getTotalFourniture() {
        return totalFourniture;
    }

    public TypeOperation getOperation() {
        return operation;
    }

    public BigDecimal getNombreHeures() {
        return nombreHeures;
    }

    public BigDecimal getTarifHoraire() {
        return tarifHoraire;
    }

    public BigDecimal getMontantMoTtc() {
        return montantMoTtc;
    }
}
