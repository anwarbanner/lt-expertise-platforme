package ma.ltexpertise.securite;

/** Identifiant inconnu, mot de passe faux ou compte désactivé : volontairement indiscernables. */
public class IdentifiantsInvalidesException extends RuntimeException {

    public IdentifiantsInvalidesException() {
        super("Identifiant ou mot de passe incorrect.");
    }
}
