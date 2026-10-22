package ma.ltexpertise.dossier;

/** Levée quand une règle du cycle de vie (P3, P6) interdit la validation ou le changement de statut. */
public class ValidationRefuseeException extends RuntimeException {

    public ValidationRefuseeException(String message) {
        super(message);
    }
}
