package ma.ltexpertise.dossier.referentiel;

import java.util.Arrays;

/** Zones de choc codées de 1 à 8 (P8). Stockées par leur code (smallint), voir {@link ZoneChocConverter}. */
public enum ZoneChoc {
    AVANT_LATERAL_GAUCHE(1, "Avant/Latéral gauche"),
    AVANT_LATERAL_DROIT(2, "Avant/Latéral droit"),
    // Libellé déduit : aucun rapport échantillon ne porte la zone 3.
    CENTRAL_LATERAL_GAUCHE(3, "Central/Latéral gauche"),
    CENTRAL_LATERAL_DROIT(4, "Central/Latéral droit"),
    ARRIERE_LATERAL_GAUCHE(5, "Arrière/Latéral gauche"),
    ARRIERE_LATERAL_DROIT(6, "Arrière/Latéral droit"),
    ARRIERE(7, "Arrière"),
    AVANT(8, "Avant");

    private final short code;
    private final String libelle;

    ZoneChoc(int code, String libelle) {
        this.code = (short) code;
        this.libelle = libelle;
    }

    public short getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public static ZoneChoc depuisCode(int code) {
        return Arrays.stream(values())
                .filter(z -> z.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Zone de choc inconnue : " + code));
    }
}
