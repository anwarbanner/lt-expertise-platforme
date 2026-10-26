package ma.ltexpertise.stockage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

/**
 * Propriétés {@code lt.stockage.*}.
 *
 * @param type    {@code minio} (par défaut) ou {@code disque}
 * @param dossier racine des PDF originaux en mode {@code disque}
 * @param url     MinIO local du cabinet (mode {@code minio})
 */
@Validated
@ConfigurationProperties("lt.stockage")
public record ProprietesStockage(
        @DefaultValue("minio") @NotBlank String type,
        @DefaultValue("./donnees/pdf-originaux") @NotNull Path dossier,
        @NotBlank String url,
        @NotBlank String cleAcces,
        @NotBlank String cleSecrete,
        @NotBlank String bucket) {
}
