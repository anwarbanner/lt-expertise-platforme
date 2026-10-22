package ma.ltexpertise.audit;

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
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Trace d'une action (P7) : une ligne par champ modifié, avec l'ancienne et la nouvelle valeur.
 * Ajout seul : l'entité est immuable et un trigger PostgreSQL refuse UPDATE, DELETE et TRUNCATE.
 */
@Entity
@Immutable
@Table(name = "journal_audit")
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private OffsetDateTime horodatage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    /** Copie figée de l'identifiant, lisible même si le compte change. */
    @Column(length = 50)
    private String identifiantUtilisateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ActionAudit action;

    @Column(length = 50)
    private String typeEntite;

    private Long entiteId;

    /** Dossier concerné : identifiant seul, pour ne pas lier le module d'audit au module dossier. */
    private Long rapportId;

    @Column(length = 100)
    private String champ;

    @Column(columnDefinition = "text")
    private String ancienneValeur;

    @Column(columnDefinition = "text")
    private String nouvelleValeur;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> details;

    @Column(length = 45)
    private String adresseIp;

    protected JournalAudit() {
    }

    public JournalAudit(ActionAudit action, Utilisateur utilisateur, OffsetDateTime horodatage) {
        this.action = Objects.requireNonNull(action);
        this.horodatage = Objects.requireNonNull(horodatage);
        this.utilisateur = utilisateur;
        this.identifiantUtilisateur = utilisateur == null ? null : utilisateur.getIdentifiant();
    }

    /** Précise l'entité concernée ; à appeler avant l'enregistrement. */
    public JournalAudit surEntite(String typeEntite, Long entiteId, Long rapportId) {
        this.typeEntite = typeEntite;
        this.entiteId = entiteId;
        this.rapportId = rapportId;
        return this;
    }

    /** Précise le champ modifié ; à appeler avant l'enregistrement. */
    public JournalAudit avecChangement(String champ, String ancienneValeur, String nouvelleValeur) {
        this.champ = champ;
        this.ancienneValeur = ancienneValeur;
        this.nouvelleValeur = nouvelleValeur;
        return this;
    }

    public JournalAudit avecDetails(Map<String, Object> details) {
        this.details = details;
        return this;
    }

    public JournalAudit depuisAdresse(String adresseIp) {
        this.adresseIp = adresseIp;
        return this;
    }

    public Long getId() {
        return id;
    }

    public OffsetDateTime getHorodatage() {
        return horodatage;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public String getIdentifiantUtilisateur() {
        return identifiantUtilisateur;
    }

    public ActionAudit getAction() {
        return action;
    }

    public String getTypeEntite() {
        return typeEntite;
    }

    public Long getEntiteId() {
        return entiteId;
    }

    public Long getRapportId() {
        return rapportId;
    }

    public String getChamp() {
        return champ;
    }

    public String getAncienneValeur() {
        return ancienneValeur;
    }

    public String getNouvelleValeur() {
        return nouvelleValeur;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public String getAdresseIp() {
        return adresseIp;
    }
}
