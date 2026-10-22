package ma.ltexpertise.importation;

/** Le PDF relu ne correspond plus à l'empreinte enregistrée à l'import : il ne doit pas être servi (P1). */
public class EmpreinteInvalideException extends RuntimeException {

    public EmpreinteInvalideException(Long importId) {
        super("Empreinte SHA-256 du PDF original différente de celle enregistrée (import " + importId + ")");
    }
}
