package ma.ltexpertise.extraction;

/**
 * Le PDF ne suit pas le gabarit attendu : rien ne doit être enregistré (§2 du cahier des charges),
 * le fichier va dans la liste « Fichiers refusés » avec ce message pour motif.
 */
public class GabaritNonReconnuException extends RuntimeException {

    public GabaritNonReconnuException(String message) {
        super(message);
    }
}
