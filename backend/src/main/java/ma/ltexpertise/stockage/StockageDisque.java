package ma.ltexpertise.stockage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Stockage des PDF originaux dans un dossier local configurable ({@code lt.stockage.dossier}), sauvegardé avec
 * la base (§8). Chaque fichier est écrit une seule fois puis passé en lecture seule (P1) ; un dépôt sur une clé
 * existante ne réécrit rien. Activé par {@code lt.stockage.type=disque}.
 */
@Component
@ConditionalOnProperty(name = "lt.stockage.type", havingValue = "disque")
public class StockageDisque implements StockageDocuments {

    /** Valeur enregistrée dans {@code import_fichier.bucket} pour les fichiers de ce stockage. */
    public static final String BUCKET = "disque";

    private static final Logger LOG = LoggerFactory.getLogger(StockageDisque.class);

    private final Path racine;

    public StockageDisque(ProprietesStockage proprietes) {
        this.racine = proprietes.dossier().toAbsolutePath().normalize();
        try {
            Files.createDirectories(racine);
        } catch (IOException e) {
            throw new StockageException("Création du dossier des PDF originaux " + racine, e);
        }
        LOG.info("PDF originaux stockés sur disque dans {}", racine);
    }

    @Override
    public DocumentStocke deposer(String cle, byte[] contenu, String typeMime) {
        Path cible = chemin(cle);
        if (Files.exists(cible)) {
            return new DocumentStocke(BUCKET, cle, null);
        }
        try {
            Files.createDirectories(cible.getParent());
            Path temporaire = Files.createTempFile(cible.getParent(), ".depot-", ".tmp");
            Files.write(temporaire, contenu);
            try {
                // Déplacement atomique : un lecteur ne voit jamais de fichier partiellement écrit.
                Files.move(temporaire, cible, StandardCopyOption.ATOMIC_MOVE);
            } catch (FileAlreadyExistsException e) {
                Files.deleteIfExists(temporaire); // déposé au même moment par un autre import : même contenu
            }
            if (!cible.toFile().setReadOnly()) {
                LOG.warn("Impossible de passer {} en lecture seule", cible);
            }
            return new DocumentStocke(BUCKET, cle, null);
        } catch (IOException e) {
            throw new StockageException("Dépôt de " + cle, e);
        }
    }

    @Override
    public byte[] lire(String bucket, String cle, String versionObjet) {
        try {
            return Files.readAllBytes(chemin(cle));
        } catch (NoSuchFileException e) {
            throw new DocumentIntrouvableException(bucket, cle);
        } catch (IOException e) {
            throw new StockageException("Lecture de " + cle, e);
        }
    }

    /** Chemin du fichier ; refuse toute clé qui sortirait de la racine (« ../ »). */
    private Path chemin(String cle) {
        Path chemin = racine.resolve(cle).normalize();
        if (!chemin.startsWith(racine)) {
            throw new StockageException("Clé de document invalide : " + cle, new AccessDeniedException(cle));
        }
        return chemin;
    }
}
