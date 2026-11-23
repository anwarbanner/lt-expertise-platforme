package ma.ltexpertise.dossier;

import ma.ltexpertise.dossier.referentiel.StatutDossier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Accès à l'agrégat dossier. Les entités filles (parties, lignes…) n'ont pas de repository propre :
 * elles sont lues et enregistrées à travers le rapport. Les recherches multicritères (F7)
 * passeront par {@link JpaSpecificationExecutor}.
 */
public interface RapportExpertiseRepository
        extends JpaRepository<RapportExpertise, Long>, JpaSpecificationExecutor<RapportExpertise> {

    /** Détection d'un numéro déjà enregistré (P2). */
    boolean existsByNumeroRapport(String numeroRapport);

    /**
     * Dossiers portant ce n°, du plus récent au plus ancien : au plus un dossier actif, et les imports annulés
     * (le n° n'est unique que parmi les dossiers non annulés).
     */
    List<RapportExpertise> findAllByNumeroRapportOrderByIdDesc(String numeroRapport);

    /** Le dossier actif portant ce n° ; à défaut, le dernier import annulé. */
    default Optional<RapportExpertise> findByNumeroRapport(String numeroRapport) {
        List<RapportExpertise> trouves = findAllByNumeroRapportOrderByIdDesc(numeroRapport);
        return trouves.stream().filter(r -> r.getStatut() != StatutDossier.ANNULE).findFirst()
                .or(() -> trouves.stream().findFirst());
    }

    /** Le dossier actif portant ce n°, ou, si {@code id} est donné, ce dossier précis (un import annulé). */
    default Optional<RapportExpertise> findByNumeroRapport(String numeroRapport, Long id) {
        if (id == null) {
            return findByNumeroRapport(numeroRapport);
        }
        return findById(id).filter(r -> numeroRapport.equals(r.getNumeroRapport()));
    }

    /** Dossiers cochés pour une validation en lot. */
    List<RapportExpertise> findByNumeroRapportIn(Collection<String> numeros);

    /** Rapprochement d'un dossier « En cours » par référence sinistre (F14). */
    List<RapportExpertise> findByRefSinistre(String refSinistre);

    Page<RapportExpertise> findByStatut(StatutDossier statut, Pageable pageable);

    long countByStatut(StatutDossier statut);

    @Query("select distinct r.compagnie from RapportExpertise r where r.compagnie is not null order by r.compagnie")
    List<String> compagnies();

    /** Alertes d'anomalies : dossiers ayant au moins un contrôle bloquant non conforme. */
    @Query("""
            select r from RapportExpertise r
            where exists (select 1 from ResultatControle c where c.rapport = r and c.conforme = false
                          and c.bloquant = true)
            """)
    Page<RapportExpertise> findAvecAnomalies(Pageable pageable);
}
