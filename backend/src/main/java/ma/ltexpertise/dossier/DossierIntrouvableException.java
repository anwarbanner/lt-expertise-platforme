package ma.ltexpertise.dossier;

public class DossierIntrouvableException extends RuntimeException {

    public DossierIntrouvableException() {
        super("Dossier introuvable.");
    }
}
