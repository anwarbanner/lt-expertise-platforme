package ma.ltexpertise.securite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Jeton de rafraîchissement : seule son empreinte est conservée, le jeton lui-même n'existe que dans le cookie. */
@Entity
@Table(name = "jeton_rafraichissement")
public class JetonRafraichissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, updatable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false, length = 64, updatable = false)
    private String empreinte;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime creeLe;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime expireLe;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime finSessionLe;

    private OffsetDateTime revoqueLe;

    protected JetonRafraichissement() {
    }

    JetonRafraichissement(Utilisateur utilisateur, String empreinte, OffsetDateTime creeLe, OffsetDateTime expireLe,
                          OffsetDateTime finSessionLe) {
        this.utilisateur = Objects.requireNonNull(utilisateur);
        this.empreinte = Objects.requireNonNull(empreinte);
        this.creeLe = Objects.requireNonNull(creeLe);
        this.expireLe = Objects.requireNonNull(expireLe);
        this.finSessionLe = Objects.requireNonNull(finSessionLe);
    }

    boolean estValide(OffsetDateTime maintenant) {
        return revoqueLe == null && maintenant.isBefore(expireLe);
    }

    void revoquer(OffsetDateTime maintenant) {
        if (revoqueLe == null) {
            revoqueLe = maintenant;
        }
    }

    Utilisateur getUtilisateur() {
        return utilisateur;
    }

    OffsetDateTime getFinSessionLe() {
        return finSessionLe;
    }

    OffsetDateTime getRevoqueLe() {
        return revoqueLe;
    }
}
