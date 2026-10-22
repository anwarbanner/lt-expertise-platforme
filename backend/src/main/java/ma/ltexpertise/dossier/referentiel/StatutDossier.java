package ma.ltexpertise.dossier.referentiel;

/** Cycle de vie d'un dossier (P6). Le passage à VALIDE se fait uniquement par {@code RapportExpertise.valider}. */
public enum StatutDossier {
    EN_COURS,
    IMPORTE,
    A_COMPLETER,
    A_VERIFIER,
    VALIDE,
    ARCHIVE
}
