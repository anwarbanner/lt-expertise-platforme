package ma.ltexpertise.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Ajout et lecture seulement : volontairement pas de {@code JpaRepository} ni de {@link ListCrudRepository},
 * pour ne pas exposer delete* sur le journal (P7).
 */
public interface JournalAuditRepository extends Repository<JournalAudit, Long> {

    JournalAudit save(JournalAudit trace);

    Optional<JournalAudit> findById(Long id);

    /** Historique d'un dossier (F5). */
    List<JournalAudit> findByRapportIdOrderByHorodatageAscIdAsc(Long rapportId);

    Page<JournalAudit> findAllByOrderByHorodatageDesc(Pageable pageable);
}
