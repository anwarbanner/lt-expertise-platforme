package ma.ltexpertise.importation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ma.ltexpertise.utilisateur.Utilisateur;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * PDF importé, accepté ou refusé (F1). Le fichier lui-même est dans MinIO (bucket versionné,
 * Object Lock) et n'est jamais modifié (P1) ; la base garde sa référence et son empreinte SHA-256 (P2).
 */
@Entity
@Table(name = "import_fichier")
public class ImportFichier {

    public static final String TYPE_MIME_PDF = "application/pdf";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nomFichier;

    @Column(name = "hash_sha256", nullable = false, length = 64, updatable = false)
    private String hashSha256;

    @Column(nullable = false, updatable = false)
    private long tailleOctets;

    private Short nombrePages;

    @Column(nullable = false, length = 63, updatable = false)
    private String bucket;

    @Column(nullable = false, length = 500, updatable = false)
    private String cleObjet;

    @Column(length = 100)
    private String versionObjet;

    @Column(nullable = false, length = 100)
    private String typeMime = TYPE_MIME_PDF;

    @Column(length = 40)
    private String gabaritDetecte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatutImport statutImport;

    @Column(columnDefinition = "text")
    private String motifRefus;

    private UUID lotImport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "importe_par", nullable = false, updatable = false)
    private Utilisateur importePar;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime importeLe;

    private OffsetDateTime retraiteLe;

    protected ImportFichier() {
    }

    public ImportFichier(String nomFichier, String hashSha256, long tailleOctets, String bucket, String cleObjet,
                         StatutImport statutImport, Utilisateur importePar, OffsetDateTime importeLe) {
        this.nomFichier = Objects.requireNonNull(nomFichier);
        this.hashSha256 = Objects.requireNonNull(hashSha256);
        this.tailleOctets = tailleOctets;
        this.bucket = Objects.requireNonNull(bucket);
        this.cleObjet = Objects.requireNonNull(cleObjet);
        this.statutImport = Objects.requireNonNull(statutImport);
        this.importePar = Objects.requireNonNull(importePar);
        this.importeLe = Objects.requireNonNull(importeLe);
    }

    public boolean estRefuse() {
        return statutImport != StatutImport.ACCEPTE;
    }

    public Long getId() {
        return id;
    }

    public String getNomFichier() {
        return nomFichier;
    }

    public String getHashSha256() {
        return hashSha256;
    }

    public long getTailleOctets() {
        return tailleOctets;
    }

    public Short getNombrePages() {
        return nombrePages;
    }

    public void setNombrePages(Short nombrePages) {
        this.nombrePages = nombrePages;
    }

    public String getBucket() {
        return bucket;
    }

    public String getCleObjet() {
        return cleObjet;
    }

    public String getVersionObjet() {
        return versionObjet;
    }

    public void setVersionObjet(String versionObjet) {
        this.versionObjet = versionObjet;
    }

    public String getTypeMime() {
        return typeMime;
    }

    public String getGabaritDetecte() {
        return gabaritDetecte;
    }

    public void setGabaritDetecte(String gabaritDetecte) {
        this.gabaritDetecte = gabaritDetecte;
    }

    public StatutImport getStatutImport() {
        return statutImport;
    }

    public void setStatutImport(StatutImport statutImport) {
        this.statutImport = statutImport;
    }

    public String getMotifRefus() {
        return motifRefus;
    }

    public void setMotifRefus(String motifRefus) {
        this.motifRefus = motifRefus;
    }

    public UUID getLotImport() {
        return lotImport;
    }

    public void setLotImport(UUID lotImport) {
        this.lotImport = lotImport;
    }

    public Utilisateur getImportePar() {
        return importePar;
    }

    public OffsetDateTime getImporteLe() {
        return importeLe;
    }

    public OffsetDateTime getRetraiteLe() {
        return retraiteLe;
    }

    public void setRetraiteLe(OffsetDateTime retraiteLe) {
        this.retraiteLe = retraiteLe;
    }
}
