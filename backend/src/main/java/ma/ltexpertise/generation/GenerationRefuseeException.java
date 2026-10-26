package ma.ltexpertise.generation;

/** Génération refusée : le rapport PDF ne se produit qu'à partir d'un dossier validé (P10). */
public class GenerationRefuseeException extends RuntimeException {

    public GenerationRefuseeException(String message) {
        super(message);
    }
}
