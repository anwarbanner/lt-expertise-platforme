package ma.ltexpertise.dossier.referentiel;

/** Cycle de vie d'un dossier (P6). Le passage à VALIDE se fait uniquement par {@code RapportExpertise.valider}. */
public enum StatutDossier {
    EN_COURS,
    IMPORTE,
    A_COMPLETER,
    A_VERIFIER,
    /** Contrôles conformes et champs complets : en attente de la validation d'un gestionnaire (P3). */
    PRET_A_VALIDER,
    VALIDE,
    ARCHIVE
}
