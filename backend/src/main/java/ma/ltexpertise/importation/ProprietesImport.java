package ma.ltexpertise.importation;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Propriétés {@code lt.import.*}. */
@Validated
@ConfigurationProperties("lt.import")
public record ProprietesImport(@Positive long tailleMaxOctets) {
}
