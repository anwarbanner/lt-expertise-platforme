package ma.ltexpertise.importation;

/** PDF original relu depuis le stockage, empreinte vérifiée. */
public record DocumentOriginal(String nomFichier, byte[] contenu) {
}
