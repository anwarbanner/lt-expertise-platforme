package ma.ltexpertise.importation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImportFichierRepository extends JpaRepository<ImportFichier, Long> {

    /** Détection d'un fichier déjà importé (P2). */
    boolean existsByHashSha256(String hashSha256);

    Optional<ImportFichier> findByHashSha256(String hashSha256);

    /** Liste « Fichiers refusés » : statut différent de ACCEPTE. */
    Page<ImportFichier> findByStatutImportNot(StatutImport statut, Pageable pageable);

    /** Récapitulatif d'un import en lot. */
    List<ImportFichier> findByLotImport(UUID lotImport);
}
