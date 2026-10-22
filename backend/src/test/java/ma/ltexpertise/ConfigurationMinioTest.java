package ma.ltexpertise;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

/** MinIO jetable, même image locale que docker-compose.dev.yml (plus publiée sur Docker Hub). */
@TestConfiguration(proxyBeanMethods = false)
public class ConfigurationMinioTest {

    public static final DockerImageName IMAGE_MINIO = DockerImageName
            .parse("lt-expertise/minio:RELEASE.2025-09-07T16-13-09Z")
            .asCompatibleSubstituteFor("minio/minio");

    @Bean
    MinIOContainer minio() {
        return new MinIOContainer(IMAGE_MINIO);
    }

    @Bean
    DynamicPropertyRegistrar proprietesMinio(MinIOContainer minio) {
        return proprietes -> {
            proprietes.add("lt.stockage.url", minio::getS3URL);
            proprietes.add("lt.stockage.cle-acces", minio::getUserName);
            proprietes.add("lt.stockage.cle-secrete", minio::getPassword);
        };
    }
}
