package ma.ltexpertise.importation;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Ligne de l'historique des imports et de la liste « Fichiers refusés ». */
public record ImportFichierDto(Long id, String nomFichier, String hashSha256, long tailleOctets,
                               Short nombrePages, StatutImport statutImport, String motifRefus,
                               String gabaritDetecte, UUID lotImport, String importePar,
                               OffsetDateTime importeLe) {

    static ImportFichierDto de(ImportFichier f) {
        return new ImportFichierDto(f.getId(), f.getNomFichier(), f.getHashSha256(), f.getTailleOctets(),
                f.getNombrePages(), f.getStatutImport(), f.getMotifRefus(), f.getGabaritDetecte(),
                f.getLotImport(), f.getImportePar().getIdentifiant(), f.getImporteLe());
    }
}
