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
import ma.ltexpertise.dossier.referentiel.TypePartie;

import java.util.Objects;

/** Assuré ou adversaire (bloc de la page 1). Contient des données personnelles (loi 09-08). */
@Entity
@Table(name = "partie")
public class Partie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rapport_id", nullable = false, updatable = false)
    private RapportExpertise rapport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypePartie typePartie;

    @Column(length = 150)
    private String nomPrenom;

    @Column(length = 100)
    private String vehiculeLibelle;

    @Column(length = 20)
    private String immatriculation;

    @Column(length = 100)
    private String compagnie;

    @Column(length = 30)
    private String numeroPolice;

    @Column(length = 150)
    private String intermediaire;

    protected Partie() {
    }

    public Partie(TypePartie typePartie) {
        this.typePartie = Objects.requireNonNull(typePartie);
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

    public TypePartie getTypePartie() {
        return typePartie;
    }

    public String getNomPrenom() {
        return nomPrenom;
    }

    public void setNomPrenom(String nomPrenom) {
        this.nomPrenom = nomPrenom;
    }

    public String getVehiculeLibelle() {
        return vehiculeLibelle;
    }

    public void setVehiculeLibelle(String vehiculeLibelle) {
        this.vehiculeLibelle = vehiculeLibelle;
    }

    public String getImmatriculation() {
        return immatriculation;
    }

    public void setImmatriculation(String immatriculation) {
        this.immatriculation = immatriculation;
    }

    public String getCompagnie() {
        return compagnie;
    }

    public void setCompagnie(String compagnie) {
        this.compagnie = compagnie;
    }

    public String getNumeroPolice() {
        return numeroPolice;
    }

    public void setNumeroPolice(String numeroPolice) {
        this.numeroPolice = numeroPolice;
    }

    public String getIntermediaire() {
        return intermediaire;
    }

    public void setIntermediaire(String intermediaire) {
        this.intermediaire = intermediaire;
    }
}
