package ma.ltexpertise.extraction;

/** Assuré ou adversaire tel que lu dans le bloc de la page 1 ; {@code null} = vide ou masqué. */
public class PartieExtraite {

    String nomPrenom;
    String vehicule;
    String immatriculation;
    String compagnie;
    String numeroPolice;
    String intermediaire;

    boolean estVide() {
        return nomPrenom == null && vehicule == null && immatriculation == null && compagnie == null
                && numeroPolice == null && intermediaire == null;
    }

    public String getNomPrenom() {
        return nomPrenom;
    }

    public String getVehicule() {
        return vehicule;
    }

    public String getImmatriculation() {
        return immatriculation;
    }

    public String getCompagnie() {
        return compagnie;
    }

    public String getNumeroPolice() {
        return numeroPolice;
    }

    public String getIntermediaire() {
        return intermediaire;
    }
}
