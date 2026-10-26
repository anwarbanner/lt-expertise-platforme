package ma.ltexpertise.importation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LotImportRepository extends JpaRepository<LotImport, UUID> {
}
