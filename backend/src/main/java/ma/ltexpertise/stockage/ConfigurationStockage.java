package ma.ltexpertise.stockage;

import io.minio.MinioClient;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Stockage MinIO, choisi par défaut ({@code lt.stockage.type=minio}). */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "lt.stockage.type", havingValue = "minio", matchIfMissing = true)
public class ConfigurationStockage {

    @Bean
    MinioClient minioClient(ProprietesStockage proprietes) {
        return MinioClient.builder()
                .endpoint(proprietes.url())
                .credentials(proprietes.cleAcces(), proprietes.cleSecrete())
                .build();
    }

    /** Crée le bucket au démarrage s'il manque ; l'application refuse de démarrer sans Object Lock (P1). */
    @Bean
    ApplicationRunner initialisationStockage(StockageMinio stockage) {
        return arguments -> stockage.initialiser();
    }
}
