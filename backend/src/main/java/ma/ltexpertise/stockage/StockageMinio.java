package ma.ltexpertise.stockage;

import io.minio.BucketExistsArgs;
import io.minio.EnableObjectLegalHoldArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectLockConfigurationArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Optional;

/**
 * Stockage MinIO auto-hébergé. Le bucket est créé avec Object Lock (ce qui active le versionnage) et chaque
 * document reçoit un « legal hold » : ni l'application ni un utilisateur MinIO ordinaire ne peut l'écraser
 * ou le supprimer (P1). Seul un administrateur peut lever ce verrou explicitement.
 */
@Component
public class StockageMinio implements StockageDocuments {

    private static final Logger LOG = LoggerFactory.getLogger(StockageMinio.class);

    private final MinioClient client;
    private final String bucket;

    public StockageMinio(MinioClient client, ProprietesStockage proprietes) {
        this.client = client;
        this.bucket = proprietes.bucket();
    }

    public String getBucket() {
        return bucket;
    }

    void initialiser() {
        boolean existe = appel("vérification du bucket",
                () -> client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()));
        if (!existe) {
            appel("création du bucket", () -> {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).objectLock(true).build());
                return null;
            });
            LOG.info("Bucket {} créé avec Object Lock", bucket);
            return;
        }
        try {
            client.getObjectLockConfiguration(GetObjectLockConfigurationArgs.builder().bucket(bucket).build());
        } catch (ErrorResponseException e) {
            throw new StockageException("Le bucket " + bucket + " existe sans Object Lock : "
                    + "les PDF originaux ne seraient pas protégés (P1). Recréer le bucket avec Object Lock.", e);
        } catch (Exception e) {
            throw new StockageException("Lecture de la configuration Object Lock du bucket " + bucket, e);
        }
    }

    @Override
    public DocumentStocke deposer(String cle, byte[] contenu, String typeMime) {
        String version = stat(cle)
                .map(StatObjectResponse::versionId)
                .orElseGet(() -> appel("dépôt de " + cle, () -> client.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(cle)
                        .stream(new ByteArrayInputStream(contenu), contenu.length, -1)
                        .contentType(typeMime)
                        .build()).versionId()));
        // Toujours (re)poser le verrou : couvre une reprise après un échec entre le dépôt et le verrouillage.
        appel("verrouillage de " + cle, () -> {
            client.enableObjectLegalHold(EnableObjectLegalHoldArgs.builder()
                    .bucket(bucket).object(cle).versionId(version).build());
            return null;
        });
        return new DocumentStocke(bucket, cle, version);
    }

    @Override
    public byte[] lire(String bucketDocument, String cle, String versionObjet) {
        try (GetObjectResponse reponse = client.getObject(GetObjectArgs.builder()
                .bucket(bucketDocument).object(cle).versionId(versionObjet).build())) {
            return reponse.readAllBytes();
        } catch (ErrorResponseException e) {
            if (estIntrouvable(e)) {
                throw new DocumentIntrouvableException(bucketDocument, cle);
            }
            throw new StockageException("Lecture de " + cle, e);
        } catch (Exception e) {
            throw new StockageException("Lecture de " + cle, e);
        }
    }

    private Optional<StatObjectResponse> stat(String cle) {
        try {
            return Optional.of(client.statObject(StatObjectArgs.builder().bucket(bucket).object(cle).build()));
        } catch (ErrorResponseException e) {
            if (estIntrouvable(e)) {
                return Optional.empty();
            }
            throw new StockageException("Consultation de " + cle, e);
        } catch (Exception e) {
            throw new StockageException("Consultation de " + cle, e);
        }
    }

    private static boolean estIntrouvable(ErrorResponseException e) {
        String code = e.errorResponse().code();
        return "NoSuchKey".equals(code) || "NoSuchVersion".equals(code);
    }

    private static <T> T appel(String operation, OperationMinio<T> appel) {
        try {
            return appel.executer();
        } catch (Exception e) {
            throw new StockageException("MinIO : échec de " + operation, e);
        }
    }

    @FunctionalInterface
    private interface OperationMinio<T> {
        T executer() throws Exception;
    }
}
