package ma.ltexpertise.securite;

import java.time.OffsetDateTime;

/** Compte verrouillé temporairement après trop d'échecs de connexion (§8). */
public class CompteVerrouilleException extends RuntimeException {

    private final OffsetDateTime jusqua;

    public CompteVerrouilleException(OffsetDateTime jusqua) {
        super("Compte verrouillé après plusieurs tentatives échouées.");
        this.jusqua = jusqua;
    }

    public OffsetDateTime getJusqua() {
        return jusqua;
    }
}
