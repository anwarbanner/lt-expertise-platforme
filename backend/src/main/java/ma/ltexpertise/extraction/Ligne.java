package ma.ltexpertise.extraction;

import java.util.List;
import java.util.stream.Collectors;

/** Ligne visuelle : mots de même ordonnée (à la tolérance près), triés de gauche à droite. */
public record Ligne(double y, List<Mot> mots) {

    public Ligne {
        mots = List.copyOf(mots);
    }

    /** Texte complet de la ligne, mots séparés par une espace. */
    public String texte() {
        return texte(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /** Texte des mots qui commencent dans {@code [xMin, xMax[}. */
    public String texte(double xMin, double xMax) {
        return mots.stream()
                .filter(m -> m.x() >= xMin && m.x() < xMax)
                .map(Mot::texte)
                .collect(Collectors.joining(" "));
    }

    /** Mots qui commencent dans {@code [xMin, xMax[}. */
    public List<Mot> mots(double xMin, double xMax) {
        return mots.stream().filter(m -> m.x() >= xMin && m.x() < xMax).toList();
    }
}
