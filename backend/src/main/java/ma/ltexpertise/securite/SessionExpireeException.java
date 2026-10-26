package ma.ltexpertise.securite;

/** Jeton de rafraîchissement absent, déjà utilisé ou expiré (inactivité, fin de session) : reconnexion nécessaire. */
public class SessionExpireeException extends RuntimeException {

    public SessionExpireeException() {
        super("Session expirée, veuillez vous reconnecter.");
    }
}
