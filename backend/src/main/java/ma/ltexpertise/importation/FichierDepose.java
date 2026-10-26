package ma.ltexpertise.importation;

/** Fichier reçu, lu en mémoire pendant la requête : le traitement en arrière-plan ne dépend plus du multipart. */
public record FichierDepose(String nom, byte[] contenu) {
}
