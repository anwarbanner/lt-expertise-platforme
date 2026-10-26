package ma.ltexpertise.importation;

import ma.ltexpertise.utilisateur.Utilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Traite les fichiers d'un lot l'un après l'autre, chacun dans sa propre transaction : un fichier en erreur
 * n'empêche pas les suivants. Le résultat de chaque fichier est enregistré dès qu'il est connu.
 */
@Component
public class TraitementLotImport {

    private static final Logger LOG = LoggerFactory.getLogger(TraitementLotImport.class);

    private final ServiceImport imports;
    private final ServiceLotImport lots;

    public TraitementLotImport(ServiceImport imports, ServiceLotImport lots) {
        this.imports = imports;
        this.lots = lots;
    }

    /** Traitement en arrière-plan (lots de plusieurs fichiers). */
    @Async(ConfigurationImportAsynchrone.EXECUTEUR_IMPORTS)
    public void lancer(UUID lot, List<FichierDepose> fichiers, Utilisateur auteur) {
        traiter(lot, fichiers, auteur);
    }

    /** Traitement dans le fil de la requête (dépôt d'un seul fichier). */
    public void traiter(UUID lot, List<FichierDepose> fichiers, Utilisateur auteur) {
        try {
            for (int ordre = 0; ordre < fichiers.size(); ordre++) {
                lots.enregistrer(lot, ordre, importerUn(fichiers.get(ordre), auteur, lot));
            }
        } finally {
            lots.terminer(lot);
        }
    }

    private ResultatImport importerUn(FichierDepose fichier, Utilisateur auteur, UUID lot) {
        String nom = ServiceImport.nettoyerNom(fichier.nom());
        try {
            return imports.importer(fichier.nom(), fichier.contenu(), auteur, lot);
        } catch (DataIntegrityViolationException e) {
            if (estViolationUnicite(e)) {
                // Même fichier ou même n° déposé simultanément par un autre utilisateur.
                return ResultatImport.sansEnregistrement(nom, IssueImport.DOUBLON_FICHIER, null,
                        "Fichier importé au même moment par un autre utilisateur.");
            }
            LOG.error("Import de « {} » (lot {}) refusé par une contrainte de la base", nom, lot, e);
            return ResultatImport.sansEnregistrement(nom, IssueImport.ERREUR, null,
                    "Données extraites refusées par la base, contacter l'administrateur.");
        } catch (RuntimeException e) {
            LOG.error("Import de « {} » (lot {}) en erreur", nom, lot, e);
            return ResultatImport.sansEnregistrement(nom, IssueImport.ERREUR, null,
                    "Erreur technique, réessayer ou contacter l'administrateur.");
        }
    }

    /** Contraintes d'unicité nommées uk_* dans les migrations (n° de rapport, empreinte, clé d'objet). */
    private static boolean estViolationUnicite(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains("uk_");
    }
}
