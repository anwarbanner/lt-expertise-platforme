package ma.ltexpertise.dossier.referentiel;

import java.util.Arrays;
import java.util.Optional;

/** Type de pièce (P8) : colonne « Type » de la page 2. */
public enum TypePiece {
    ORIGINE('O'),
    ADAPTABLE('A'),
    REEMPLOI('R');

    private final char code;

    TypePiece(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static Optional<TypePiece> depuisCode(char code) {
        char majuscule = Character.toUpperCase(code);
        return Arrays.stream(values()).filter(t -> t.code == majuscule).findFirst();
    }
}
