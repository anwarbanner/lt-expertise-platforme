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
import ma.ltexpertise.dossier.referentiel.CategorieFourniture;

import java.util.Objects;

/** Une ligne « fournitures » du tableau Évaluation des dommages (page 1). */
@Entity
@Table(name = "fourniture")
public class Fourniture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rapport_id", nullable = false, updatable = false)
    private RapportExpertise rapport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategorieFourniture categorie;

    @Embedded
    @AttributeOverride(name = "ht", column = @Column(name = "montant_ht", precision = 12, scale = 2))
    @AttributeOverride(name = "tva", column = @Column(name = "montant_tva", precision = 12, scale = 2))
    @AttributeOverride(name = "ttc", column = @Column(name = "montant_ttc", precision = 12, scale = 2))
    private Montants montants;

    protected Fourniture() {
    }

    public Fourniture(CategorieFourniture categorie, Montants montants) {
        this.categorie = Objects.requireNonNull(categorie);
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

    public CategorieFourniture getCategorie() {
        return categorie;
    }

    public Montants getMontants() {
        return montants;
    }

    public void setMontants(Montants montants) {
        this.montants = montants;
    }
}
