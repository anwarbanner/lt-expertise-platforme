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
import ma.ltexpertise.dossier.referentiel.CleCommentaire;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Valeur clé extraite du commentaire de l'expert (F12). Le texte complet reste dans le rapport. */
@Entity
@Table(name = "valeur_commentaire")
public class ValeurCommentaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rapport_id", nullable = false, updatable = false)
    private RapportExpertise rapport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CleCommentaire cle;

    @Column(length = 300)
    private String valeurTexte;

    @Column(precision = 12, scale = 2)
    private BigDecimal valeurMontant;

    private LocalDate valeurDate;

    protected ValeurCommentaire() {
    }

    public ValeurCommentaire(CleCommentaire cle, String valeurTexte, BigDecimal valeurMontant, LocalDate valeurDate) {
        this.cle = Objects.requireNonNull(cle);
        this.valeurTexte = valeurTexte;
        this.valeurMontant = valeurMontant;
        this.valeurDate = valeurDate;
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

    public CleCommentaire getCle() {
        return cle;
    }

    public String getValeurTexte() {
        return valeurTexte;
    }

    public BigDecimal getValeurMontant() {
        return valeurMontant;
    }

    public LocalDate getValeurDate() {
        return valeurDate;
    }
}
