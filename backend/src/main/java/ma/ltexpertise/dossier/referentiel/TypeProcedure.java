package ma.ltexpertise.dossier.referentiel;

/** Procédure du rapport, lue sous le titre de la page 1. */
public enum TypeProcedure {
    PROCEDURE_CLASSIQUE("Procedure classique"),
    PRISE_EN_CHARGE_ET_PLUS_QUE_20000("PEC et Plus que 20 000"),
    PLUS_DE_20000("Plus de 20000"),
    PRISE_EN_CHARGE("Prise En Charge"),
    WAFA_24("Wafa 24 heures");

    private final String libellePdf;

    TypeProcedure(String libellePdf) {
        this.libellePdf = libellePdf;
    }

    /** Libellé tel qu'imprimé dans le PDF. */
    public String getLibellePdf() {
        return libellePdf;
    }
}
