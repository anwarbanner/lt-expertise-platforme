package ma.ltexpertise.importation;

import ma.ltexpertise.audit.ActionAudit;
import ma.ltexpertise.audit.JournalAudit;
import ma.ltexpertise.audit.JournalAuditRepository;
import ma.ltexpertise.commun.Empreintes;
import ma.ltexpertise.dossier.RapportExpertise;
import ma.ltexpertise.dossier.RapportExpertiseRepository;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.stockage.DocumentStocke;
import ma.ltexpertise.stockage.StockageDocuments;
import ma.ltexpertise.utilisateur.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Import d'un PDF (F1) : contrôle du fichier, détection des doublons (empreinte puis n° de rapport, P2),
 * conservation de l'original dans le stockage verrouillé (P1), création du dossier au statut IMPORTE
 * et trace dans le journal d'audit (P7). Les PDF refusés sont conservés avec leur motif (§3).
 */
@Service
public class ServiceImport {

    private static final byte[] SIGNATURE_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final int LONGUEUR_MAX_NOM = 255;

    private final StockageDocuments stockage;
    private final AnalyseurGabarit analyseur;
    private final ImportFichierRepository imports;
    private final RapportExpertiseRepository rapports;
    private final JournalAuditRepository journal;
    private final ProprietesImport proprietes;
    private final Clock horloge;

    public ServiceImport(StockageDocuments stockage, AnalyseurGabarit analyseur, ImportFichierRepository imports,
                         RapportExpertiseRepository rapports, JournalAuditRepository journal,
                         ProprietesImport proprietes, Clock horloge) {
        this.stockage = stockage;
        this.analyseur = analyseur;
        this.imports = imports;
        this.rapports = rapports;
        this.journal = journal;
        this.proprietes = proprietes;
        this.horloge = horloge;
    }

    /**
     * Importe un fichier. Chaque fichier d'un lot est traité dans sa propre transaction :
     * un fichier en erreur n'annule pas les autres.
     *
     * @param lot identifiant du lot, ou {@code null} pour un dépôt unitaire
     */
    @Transactional
    public ResultatImport importer(String nomFichierDepose, byte[] contenu, Utilisateur auteur, UUID lot) {
        OffsetDateTime maintenant = OffsetDateTime.now(horloge);
        String nomFichier = nettoyerNom(nomFichierDepose);

        Optional<String> invalidite = verifierFichier(contenu);
        if (invalidite.isPresent()) {
            journaliserRefus(auteur, maintenant, nomFichier, null, null, IssueImport.FICHIER_INVALIDE, invalidite.get());
            return ResultatImport.sansEnregistrement(nomFichier, IssueImport.FICHIER_INVALIDE, null, invalidite.get());
        }

        String hash = Empreintes.sha256(contenu);
        Optional<ImportFichier> dejaImporte = imports.findByHashSha256(hash);
        if (dejaImporte.isPresent()) {
            String motif = "Fichier déjà importé le " + dejaImporte.get().getImporteLe().toLocalDate()
                    + " (« " + dejaImporte.get().getNomFichier() + " »).";
            journaliserRefus(auteur, maintenant, nomFichier, hash, dejaImporte.get().getId(),
                    IssueImport.DOUBLON_FICHIER, motif);
            return ResultatImport.sansEnregistrement(nomFichier, IssueImport.DOUBLON_FICHIER,
                    dejaImporte.get().getId(), motif);
        }

        AnalyseGabarit analyse = analyseur.analyser(contenu);
        DocumentStocke document = stockage.deposer(cleObjet(hash), contenu, ImportFichier.TYPE_MIME_PDF);

        ImportFichier fichier = new ImportFichier(nomFichier, hash, contenu.length, document.bucket(),
                document.cle(), StatutImport.ACCEPTE, auteur, maintenant);
        fichier.setVersionObjet(document.versionObjet());
        fichier.setLotImport(lot);
        if (analyse.nombrePages() > 0) {
            fichier.setNombrePages((short) analyse.nombrePages());
        }

        if (!analyse.lisible()) {
            return refuser(fichier, StatutImport.ERREUR_LECTURE, IssueImport.ERREUR_LECTURE, analyse.motif(), null);
        }
        if (!analyse.reconnu()) {
            return refuser(fichier, StatutImport.REFUSE_GABARIT, IssueImport.REFUSE_GABARIT, analyse.motif(), null);
        }
        fichier.setGabaritDetecte(analyse.gabarit());

        Optional<RapportExpertise> existant = rapports.findByNumeroRapport(analyse.numeroRapport());
        if (existant.isPresent() && !estDossierEnCoursSansPdf(existant.get())) {
            String motif = "N° de rapport " + analyse.numeroRapport() + " déjà enregistré (dossier "
                    + existant.get().getId() + ").";
            return refuser(fichier, StatutImport.REFUSE_DOUBLON_NUMERO, IssueImport.REFUSE_DOUBLON_NUMERO, motif,
                    analyse.numeroRapport());
        }

        imports.save(fichier);
        RapportExpertise rapport;
        IssueImport issue;
        if (existant.isPresent()) {
            rapport = existant.get();
            rapport.changerStatut(StatutDossier.IMPORTE);
            issue = IssueImport.RATTACHE_DOSSIER_EN_COURS;
        } else {
            rapport = new RapportExpertise(analyse.numeroRapport(), StatutDossier.IMPORTE);
            issue = IssueImport.ACCEPTE;
        }
        rapport.setNatureRapport(analyse.natureRapport());
        rapport.setGabaritVersion(analyse.gabarit());
        rapport.setImportFichier(fichier);
        rapports.save(rapport);

        journal.save(new JournalAudit(ActionAudit.IMPORT, auteur, maintenant)
                .surEntite(ImportFichier.class.getSimpleName(), fichier.getId(), rapport.getId())
                .avecDetails(details(nomFichier, hash, issue, null)));
        return new ResultatImport(nomFichier, issue, fichier.getId(), rapport.getId(), analyse.numeroRapport(), null);
    }

    /** PDF original, relu depuis le stockage et contrôlé contre l'empreinte enregistrée (P1). */
    @Transactional(readOnly = true)
    public Optional<DocumentOriginal> lireOriginal(Long importId) {
        return imports.findById(importId).map(fichier -> {
            byte[] contenu = stockage.lire(fichier.getBucket(), fichier.getCleObjet(), fichier.getVersionObjet());
            if (!Empreintes.sha256(contenu).equals(fichier.getHashSha256())) {
                throw new EmpreinteInvalideException(importId);
            }
            return new DocumentOriginal(fichier.getNomFichier(), contenu);
        });
    }

    /** Historique des imports (F1) ou, si {@code refusesSeulement}, liste « Fichiers refusés » (§3). */
    @Transactional(readOnly = true)
    public Page<ImportFichierDto> historique(boolean refusesSeulement, Pageable pagination) {
        Page<ImportFichier> page = refusesSeulement
                ? imports.findByStatutImportNot(StatutImport.ACCEPTE, pagination)
                : imports.findAll(pagination);
        return page.map(ImportFichierDto::de);
    }

    private ResultatImport refuser(ImportFichier fichier, StatutImport statut, IssueImport issue, String motif,
                                   String numeroRapport) {
        fichier.setStatutImport(statut);
        fichier.setMotifRefus(motif);
        imports.save(fichier);
        journal.save(new JournalAudit(ActionAudit.IMPORT_REFUSE, fichier.getImportePar(), fichier.getImporteLe())
                .surEntite(ImportFichier.class.getSimpleName(), fichier.getId(), null)
                .avecDetails(details(fichier.getNomFichier(), fichier.getHashSha256(), issue, motif)));
        return new ResultatImport(fichier.getNomFichier(), issue, fichier.getId(), null, numeroRapport, motif);
    }

    private void journaliserRefus(Utilisateur auteur, OffsetDateTime maintenant, String nomFichier, String hash,
                                  Long importExistantId, IssueImport issue, String motif) {
        journal.save(new JournalAudit(ActionAudit.IMPORT_REFUSE, auteur, maintenant)
                .surEntite(ImportFichier.class.getSimpleName(), importExistantId, null)
                .avecDetails(details(nomFichier, hash, issue, motif)));
    }

    private Optional<String> verifierFichier(byte[] contenu) {
        if (contenu == null || contenu.length == 0) {
            return Optional.of("Fichier vide.");
        }
        if (contenu.length > proprietes.tailleMaxOctets()) {
            return Optional.of("Fichier trop volumineux (" + contenu.length + " octets, maximum "
                    + proprietes.tailleMaxOctets() + ").");
        }
        // Le type annoncé par le navigateur n'est pas fiable : on contrôle la signature du fichier.
        if (contenu.length < SIGNATURE_PDF.length
                || !Arrays.equals(contenu, 0, SIGNATURE_PDF.length, SIGNATURE_PDF, 0, SIGNATURE_PDF.length)) {
            return Optional.of("Le fichier n'est pas un PDF.");
        }
        return Optional.empty();
    }

    private static boolean estDossierEnCoursSansPdf(RapportExpertise rapport) {
        return rapport.getStatut() == StatutDossier.EN_COURS && rapport.getImportFichier() == null;
    }

    /** Clé dérivée de l'empreinte : un même contenu a toujours la même clé, ce qui rend le dépôt idempotent. */
    static String cleObjet(String hash) {
        return "originaux/" + hash.substring(0, 2) + "/" + hash + ".pdf";
    }

    /** Garde le seul nom du fichier (certains navigateurs envoient le chemin complet). */
    static String nettoyerNom(String nomDepose) {
        if (nomDepose == null || nomDepose.isBlank()) {
            return "sans-nom.pdf";
        }
        String nom = nomDepose.substring(Math.max(nomDepose.lastIndexOf('/'), nomDepose.lastIndexOf('\\')) + 1)
                .strip();
        if (nom.isEmpty()) {
            return "sans-nom.pdf";
        }
        return nom.length() > LONGUEUR_MAX_NOM ? nom.substring(0, LONGUEUR_MAX_NOM) : nom;
    }

    private static Map<String, Object> details(String nomFichier, String hash, IssueImport issue, String motif) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("nomFichier", nomFichier);
        if (hash != null) {
            details.put("hashSha256", hash);
        }
        details.put("issue", issue.name());
        if (motif != null) {
            details.put("motif", motif);
        }
        return details;
    }
}
