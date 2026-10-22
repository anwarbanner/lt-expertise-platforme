package ma.ltexpertise.dossier.referentiel;

/** Les 7 postes de main d'œuvre du tableau Évaluation des dommages (P8). */
public enum PosteMainOeuvre {
    MO_1_TOLERIE(FamilleTravaux.TOLERIE),
    MO_2_TOLERIE(FamilleTravaux.TOLERIE),
    MO_3_TOLERIE(FamilleTravaux.TOLERIE),
    MO_PEINTURE(FamilleTravaux.PEINTURE),
    MO_1_MECANIQUE(FamilleTravaux.MECANIQUE),
    MO_2_MECANIQUE(FamilleTravaux.MECANIQUE),
    MO_3_MECANIQUE(FamilleTravaux.MECANIQUE);

    private final FamilleTravaux famille;

    PosteMainOeuvre(FamilleTravaux famille) {
        this.famille = famille;
    }

    public FamilleTravaux getFamille() {
        return famille;
    }
}
