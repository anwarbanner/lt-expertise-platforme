package ma.ltexpertise.dossier;

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
import ma.ltexpertise.dossier.referentiel.TypeControle;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/** Résultat d'un contrôle de cohérence (P4) ou champ manquant, recalculé à chaque modification du dossier. */
@Entity
@Table(name = "resultat_controle")
public class ResultatControle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rapport_id", nullable = false, updatable = false)
    private RapportExpertise rapport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypeControle type;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(nullable = false, length = 200)
    private String libelle;

    /** Champ du formulaire à surligner (F5). */
    @Column(length = 100)
    private String champ;

    @Column(precision = 12, scale = 2)
    private BigDecimal valeurAttendue;

    @Column(precision = 12, scale = 2)
    private BigDecimal valeurObtenue;

    @Column(nullable = false)
    private boolean conforme;

    /** Un résultat non conforme et bloquant empêche la validation ; sinon c'est un avertissement. */
    @Column(nullable = false)
    private boolean bloquant;

    @Column(nullable = false)
    private OffsetDateTime executeLe;

    protected ResultatControle() {
    }

    public ResultatControle(TypeControle type, String code, String libelle, String champ,
                            BigDecimal valeurAttendue, BigDecimal valeurObtenue, boolean conforme, boolean bloquant,
                            OffsetDateTime executeLe) {
        this.type = Objects.requireNonNull(type);
        this.code = Objects.requireNonNull(code);
        this.libelle = Objects.requireNonNull(libelle);
        this.champ = champ;
        this.valeurAttendue = valeurAttendue;
        this.valeurObtenue = valeurObtenue;
        this.conforme = conforme;
        this.bloquant = bloquant;
        this.executeLe = Objects.requireNonNull(executeLe);
    }

    void rattacher(RapportExpertise rapport) {
        this.rapport = rapport;
    }

    public Long getId() {
        return id;
    }

    public RapportExpertise getRapport() {
        return rapport;
    }

    public TypeControle getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getChamp() {
        return champ;
    }

    public BigDecimal getValeurAttendue() {
        return valeurAttendue;
    }

    public BigDecimal getValeurObtenue() {
        return valeurObtenue;
    }

    public boolean isConforme() {
        return conforme;
    }

    public boolean isBloquant() {
        return bloquant;
    }

    public OffsetDateTime getExecuteLe() {
        return executeLe;
    }
}
