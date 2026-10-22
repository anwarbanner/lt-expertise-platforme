package ma.ltexpertise.dossier;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import ma.ltexpertise.commun.Montants;
import ma.ltexpertise.dossier.referentiel.PosteMainOeuvre;

import java.math.BigDecimal;
import java.util.Objects;

/** Une ligne de main d'œuvre du tableau Évaluation des dommages (page 1). */
@Entity
@Table(name = "evaluation_main_oeuvre")
public class EvaluationMainOeuvre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rapport_id", nullable = false, updatable = false)
    private RapportExpertise rapport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PosteMainOeuvre poste;

    @Column(precision = 7, scale = 2)
    private BigDecimal nombreHeures;

    /** « Prix unitaire » du PDF. */
    @Column(precision = 12, scale = 2)
    private BigDecimal tauxHoraire;

    @Embedded
    @AttributeOverride(name = "ht", column = @Column(name = "montant_ht", precision = 12, scale = 2))
    @AttributeOverride(name = "tva", column = @Column(name = "montant_tva", precision = 12, scale = 2))
    @AttributeOverride(name = "ttc", column = @Column(name = "montant_ttc", precision = 12, scale = 2))
    private Montants montants;

    protected EvaluationMainOeuvre() {
    }

    public EvaluationMainOeuvre(PosteMainOeuvre poste, BigDecimal nombreHeures, BigDecimal tauxHoraire,
                                Montants montants) {
        this.poste = Objects.requireNonNull(poste);
        this.nombreHeures = nombreHeures;
        this.tauxHoraire = tauxHoraire;
        this.montants = montants;
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

    public PosteMainOeuvre getPoste() {
        return poste;
    }

    public BigDecimal getNombreHeures() {
        return nombreHeures;
    }

    public void setNombreHeures(BigDecimal nombreHeures) {
        this.nombreHeures = nombreHeures;
    }

    public BigDecimal getTauxHoraire() {
        return tauxHoraire;
    }

    public void setTauxHoraire(BigDecimal tauxHoraire) {
        this.tauxHoraire = tauxHoraire;
    }

    public Montants getMontants() {
        return montants;
    }

    public void setMontants(Montants montants) {
        this.montants = montants;
    }
}
