package ma.ltexpertise.commun;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Empreintes de fichiers (P2 : détection des doublons, contrôle d'intégrité des PDF originaux). */
public final class Empreintes {

    private Empreintes() {
    }

    /** SHA-256 en hexadécimal minuscule (64 caractères), format de {@code import_fichier.hash_sha256}. */
    public static String sha256(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible dans la JVM", e);
        }
    }
}
