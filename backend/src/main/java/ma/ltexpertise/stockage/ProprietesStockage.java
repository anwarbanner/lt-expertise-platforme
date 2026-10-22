package ma.ltexpertise.stockage;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Propriétés {@code lt.stockage.*} : accès au MinIO local du cabinet. */
@Validated
@ConfigurationProperties("lt.stockage")
public record ProprietesStockage(
        @NotBlank String url,
        @NotBlank String cleAcces,
        @NotBlank String cleSecrete,
        @NotBlank String bucket) {
}
