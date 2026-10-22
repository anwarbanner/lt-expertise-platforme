package ma.ltexpertise.stockage;

/**
 * Stockage des PDF originaux (P1). Un document déposé n'est jamais modifié ni supprimé par l'application.
 * L'implémentation actuelle est {@link StockageMinio} ; l'interface permet d'en changer sans toucher au métier.
 */
public interface StockageDocuments {

    /**
     * Dépose le document et le verrouille contre toute modification ou suppression.
     * Idempotent : si la clé existe déjà, le document n'est pas réécrit (reprise après un échec en base).
     */
    DocumentStocke deposer(String cle, byte[] contenu, String typeMime);

    /**
     * @param versionObjet version à lire ; {@code null} pour la dernière
     * @throws DocumentIntrouvableException si le document n'existe pas
     */
    byte[] lire(String bucket, String cle, String versionObjet);
}
