package ma.ltexpertise.extraction;

/**
 * Mot du PDF avec sa position, en points, origine en haut à gauche de la page.
 *
 * @param x      abscisse du début du mot
 * @param xFin   abscisse de la fin du mot
 * @param y      ordonnée du centre vertical du mot
 */
public record Mot(double x, double xFin, double y, String texte) {
}
