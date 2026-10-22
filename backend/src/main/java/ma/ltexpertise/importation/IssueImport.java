package ma.ltexpertise.importation;

/** Issue de l'import d'un fichier, telle que présentée dans le récapitulatif. */
public enum IssueImport {
    /** Nouveau dossier créé au statut IMPORTE. */
    ACCEPTE,
    /** PDF rattaché à un dossier « En cours » repris du fichier Excel (F14). */
    RATTACHE_DOSSIER_EN_COURS,
    /** Fichier strictement identique déjà importé (même SHA-256) : rien n'est enregistré. */
    DOUBLON_FICHIER,
    /** Fichier différent mais n° de rapport déjà enregistré : PDF conservé dans les fichiers refusés. */
    REFUSE_DOUBLON_NUMERO,
    /** Modèle de PDF non reconnu : PDF conservé dans les fichiers refusés. */
    REFUSE_GABARIT,
    /** PDF corrompu ou protégé : PDF conservé dans les fichiers refusés. */
    ERREUR_LECTURE,
    /** Fichier vide, trop volumineux ou qui n'est pas un PDF : rien n'est enregistré. */
    FICHIER_INVALIDE,
    /** Erreur technique inattendue (stockage indisponible…). */
    ERREUR;

    public boolean estAccepte() {
        return this == ACCEPTE || this == RATTACHE_DOSSIER_EN_COURS;
    }
}
