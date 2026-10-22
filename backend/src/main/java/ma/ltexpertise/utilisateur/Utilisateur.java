package ma.ltexpertise.utilisateur;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.Objects;

/** Compte applicatif. Un compte est désactivé, jamais supprimé, pour garder le journal d'audit lisible. */
@Entity
@Table(name = "utilisateur")
@EntityListeners(AuditingEntityListener.class)
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String identifiant;

    @Column(nullable = false, length = 80)
    private String nom;

    @Column(nullable = false, length = 80)
    private String prenom;

    @Column(length = 150)
    private String email;

    @Column(nullable = false, length = 100)
    private String motDePasseHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Role role;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(nullable = false)
    private short tentativesEchouees;

    @Column(name = "verrouille_jusqu_a")
    private OffsetDateTime verrouilleJusquA;

    private OffsetDateTime derniereConnexion;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime creeLe;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime modifieLe;

    @Version
    private int version;

    protected Utilisateur() {
    }

    public Utilisateur(String identifiant, String nom, String prenom, String motDePasseHash, Role role) {
        this.identifiant = Objects.requireNonNull(identifiant);
        this.nom = Objects.requireNonNull(nom);
        this.prenom = Objects.requireNonNull(prenom);
        this.motDePasseHash = Objects.requireNonNull(motDePasseHash);
        this.role = Objects.requireNonNull(role);
    }

    public boolean aLeRole(Role attendu) {
        return role == attendu;
    }

    public Long getId() {
        return id;
    }

    public String getIdentifiant() {
        return identifiant;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public short getTentativesEchouees() {
        return tentativesEchouees;
    }

    public void setTentativesEchouees(short tentativesEchouees) {
        this.tentativesEchouees = tentativesEchouees;
    }

    public OffsetDateTime getVerrouilleJusquA() {
        return verrouilleJusquA;
    }

    public void setVerrouilleJusquA(OffsetDateTime verrouilleJusquA) {
        this.verrouilleJusquA = verrouilleJusquA;
    }

    public OffsetDateTime getDerniereConnexion() {
        return derniereConnexion;
    }

    public void setDerniereConnexion(OffsetDateTime derniereConnexion) {
        this.derniereConnexion = derniereConnexion;
    }

    public OffsetDateTime getCreeLe() {
        return creeLe;
    }

    public OffsetDateTime getModifieLe() {
        return modifieLe;
    }

    public int getVersion() {
        return version;
    }
}
