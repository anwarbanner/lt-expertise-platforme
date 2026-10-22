package ma.ltexpertise.stockage;

/** Référence d'un document déposé, conservée dans {@code import_fichier}. */
public record DocumentStocke(String bucket, String cle, String versionObjet) {
}
