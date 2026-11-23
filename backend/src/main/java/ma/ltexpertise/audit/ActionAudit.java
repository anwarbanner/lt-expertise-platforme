package ma.ltexpertise.audit;

/** Actions tracées dans le journal d'audit (P7). */
public enum ActionAudit {
    CONNEXION,
    ECHEC_CONNEXION,
    IMPORT,
    IMPORT_REFUSE,
    CREATION,
    MODIFICATION,
    CHANGEMENT_STATUT,
    VALIDATION,
    ARCHIVAGE,
    ANNULATION,
    EXPORT_PDF,
    EXPORT_EXCEL,
    GESTION_UTILISATEUR
}
