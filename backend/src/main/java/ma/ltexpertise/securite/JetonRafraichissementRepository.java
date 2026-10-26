package ma.ltexpertise.securite;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface JetonRafraichissementRepository extends JpaRepository<JetonRafraichissement, Long> {

    @EntityGraph(attributePaths = "utilisateur")
    Optional<JetonRafraichissement> findByEmpreinte(String empreinte);

    @Modifying
    @Query("update JetonRafraichissement j set j.revoqueLe = :maintenant "
            + "where j.utilisateur.id = :utilisateur and j.revoqueLe is null")
    int revoquerTout(@Param("utilisateur") Long utilisateurId, @Param("maintenant") OffsetDateTime maintenant);
}
