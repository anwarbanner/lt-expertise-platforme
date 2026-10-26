package ma.ltexpertise.controle;

import java.math.BigDecimal;

/**
 * Résultat d'un contrôle de cohérence.
 *
 * @param code     identifiant stable (ex. {@code MO_1_TOLERIE_HEURES_TAUX}), enregistré dans resultat_controle
 * @param nom      libellé affiché à l'écran de vérification
 * @param attendu  valeur recalculée ; {@code null} si une donnée manque
 * @param obtenu   valeur lue dans le rapport ; {@code null} si absente
 * @param ok       écart dans la tolérance
 * @param bloquant un échec empêche la validation ; sinon le résultat est seulement signalé (avertissement)
 */
public record Controle(String code, String nom, BigDecimal attendu, BigDecimal obtenu, boolean ok, boolean bloquant) {

    public Controle(String code, String nom, BigDecimal attendu, BigDecimal obtenu, boolean ok) {
        this(code, nom, attendu, obtenu, ok, true);
    }

    /** Échec qui empêche la validation du dossier. */
    public boolean bloque() {
        return bloquant && !ok;
    }
}
