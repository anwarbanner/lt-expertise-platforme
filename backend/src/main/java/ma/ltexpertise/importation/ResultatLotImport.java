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
import ma.ltexpertise.dossier.referentiel.StatutDossier;

import java.time.OffsetDateTime;

/** Résultat d'un fichier d'un lot ; copie de {@link ResultatImport}, consultable après le traitement. */
@Entity
@Table(name = "resultat_lot_import")
public class ResultatLotImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false, updatable = false)
    private LotImport lot;

    @Column(nullable = false)
    private short ordre;

    @Column(nullable = false, length = 255)
    private String nomFichier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private IssueImport issue;

    private Long importId;

    private Long rapportId;

    @Column(length = 30)
    private String numeroRapport;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private StatutDossier statutDossier;

    @Column(columnDefinition = "text")
    private String motif;

    private Short controlesConformes;

    private Short controlesTotal;

    @Column(name = "champs_a_saisir")
    private Short champsASaisir;

    @Column(nullable = false)
    private OffsetDateTime traiteLe;

    protected ResultatLotImport() {
    }

    ResultatLotImport(int ordre, ResultatImport r, OffsetDateTime traiteLe) {
        this.ordre = (short) ordre;
        this.nomFichier = r.nomFichier();
        this.issue = r.issue();
        this.importId = r.importId();
        this.rapportId = r.rapportId();
        this.numeroRapport = r.numeroRapport();
        this.statutDossier = r.statutDossier();
        this.motif = r.motif();
        this.controlesConformes = court(r.controlesConformes());
        this.controlesTotal = court(r.controlesTotal());
        this.champsASaisir = court(r.champsASaisir());
        this.traiteLe = traiteLe;
    }

    void rattacher(LotImport lot) {
        this.lot = lot;
    }

    ResultatImport versResultat() {
        return new ResultatImport(nomFichier, issue, importId, rapportId, numeroRapport, motif, statutDossier,
                entier(controlesConformes), entier(controlesTotal), entier(champsASaisir));
    }

    public short getOrdre() {
        return ordre;
    }

    public IssueImport getIssue() {
        return issue;
    }

    public StatutDossier getStatutDossier() {
        return statutDossier;
    }

    private static Short court(Integer valeur) {
        return valeur == null ? null : valeur.shortValue();
    }

    private static Integer entier(Short valeur) {
        return valeur == null ? null : valeur.intValue();
    }
}
