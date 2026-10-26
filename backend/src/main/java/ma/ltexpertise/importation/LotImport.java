package ma.ltexpertise.importation;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import ma.ltexpertise.utilisateur.Utilisateur;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Dépôt d'un ou plusieurs PDF, avec le résultat de chaque fichier au fur et à mesure du traitement. */
@Entity
@Table(name = "lot_import")
public class LotImport {

    public enum Statut { EN_COURS, TERMINE }

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Statut statut = Statut.EN_COURS;

    @Column(nullable = false)
    private int nombreFichiers;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cree_par", nullable = false, updatable = false)
    private Utilisateur creePar;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime creeLe;

    private OffsetDateTime termineLe;

    @OneToMany(mappedBy = "lot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre")
    private List<ResultatLotImport> resultats = new ArrayList<>();

    protected LotImport() {
    }

    public LotImport(int nombreFichiers, Utilisateur creePar, OffsetDateTime creeLe) {
        this.id = UUID.randomUUID();
        this.nombreFichiers = nombreFichiers;
        this.creePar = Objects.requireNonNull(creePar);
        this.creeLe = Objects.requireNonNull(creeLe);
    }

    void ajouterResultat(ResultatLotImport resultat) {
        resultat.rattacher(this);
        resultats.add(resultat);
    }

    void terminer(OffsetDateTime maintenant) {
        statut = Statut.TERMINE;
        termineLe = maintenant;
    }

    public UUID getId() {
        return id;
    }

    public Statut getStatut() {
        return statut;
    }

    public int getNombreFichiers() {
        return nombreFichiers;
    }

    public Utilisateur getCreePar() {
        return creePar;
    }

    public OffsetDateTime getCreeLe() {
        return creeLe;
    }

    public OffsetDateTime getTermineLe() {
        return termineLe;
    }

    public List<ResultatLotImport> getResultats() {
        return Collections.unmodifiableList(resultats);
    }
}
