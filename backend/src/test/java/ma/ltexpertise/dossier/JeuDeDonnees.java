package ma.ltexpertise.dossier;

import ma.ltexpertise.commun.Montants;
import ma.ltexpertise.dossier.referentiel.CategorieFourniture;
import ma.ltexpertise.dossier.referentiel.CleCommentaire;
import ma.ltexpertise.dossier.referentiel.Energie;
import ma.ltexpertise.dossier.referentiel.FamilleTravaux;
import ma.ltexpertise.dossier.referentiel.NatureContrat;
import ma.ltexpertise.dossier.referentiel.NatureEvenement;
import ma.ltexpertise.dossier.referentiel.PosteMainOeuvre;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.dossier.referentiel.TypeControle;
import ma.ltexpertise.dossier.referentiel.TypeOperation;
import ma.ltexpertise.dossier.referentiel.TypePartie;
import ma.ltexpertise.dossier.referentiel.TypePiece;
import ma.ltexpertise.dossier.referentiel.TypeProcedure;
import ma.ltexpertise.dossier.referentiel.ZoneChoc;
import ma.ltexpertise.importation.ImportFichier;
import ma.ltexpertise.importation.StatutImport;
import ma.ltexpertise.utilisateur.Role;
import ma.ltexpertise.utilisateur.Utilisateur;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Données fictives (aucune donnée réelle, cf. CLAUDE.md), aux montants cohérents (P4) :
 * MO 1 200 HT + 240 TVA = 1 440 TTC ; fournitures 5 600 + 120 = 5 720 ; total 7 160 = indemnisation.
 */
final class JeuDeDonnees {

    static final OffsetDateTime MAINTENANT = OffsetDateTime.of(2026, 10, 22, 10, 0, 0, 0, ZoneOffset.ofHours(1));
    static final BigDecimal ZERO = new BigDecimal("0.00");

    private JeuDeDonnees() {
    }

    static BigDecimal dh(String montant) {
        return new BigDecimal(montant);
    }

    static Utilisateur utilisateur(String identifiant, Role role) {
        return new Utilisateur(identifiant, "Test", "Utilisateur", "$2a$10$hachefictif", role);
    }

    static ImportFichier importFichier(String hash, Utilisateur importePar) {
        return new ImportFichier("rapport-test.pdf", hash, 56_872, "rapports-originaux",
                "originaux/" + hash.substring(0, 2) + "/" + hash + ".pdf",
                StatutImport.ACCEPTE, importePar, MAINTENANT);
    }

    static String hash(char chiffre) {
        return String.valueOf(chiffre).repeat(64);
    }

    /** Rapport complet : en-tête, parties, véhicule, 7 postes MO, 4 fournitures, 3 lignes de détail. */
    static RapportExpertise rapportComplet(String numero, ImportFichier source) {
        RapportExpertise r = new RapportExpertise(numero, StatutDossier.A_VERIFIER);
        r.setTypeProcedure(TypeProcedure.PROCEDURE_CLASSIQUE);
        r.setCabinet("CABINET TEST");
        r.setExpert("EXPERT TEST");
        r.setCompagnie("COMPAGNIE TEST");
        r.setRefSinistre("000000000001");
        r.setDateSinistre(LocalDate.of(2026, 10, 5));
        r.setDateMission(LocalDate.of(2026, 10, 6));
        r.setDateExpertise(LocalDate.of(2026, 10, 7));
        r.setDateAvantReparation(LocalDate.of(2026, 10, 7));
        r.setDateEnCoursTravaux(LocalDate.of(2026, 10, 7));
        r.setDateApresTravaux(LocalDate.of(2026, 10, 14));
        r.setDateRapport(LocalDate.of(2026, 10, 17));
        r.setLieuRapport("Casablanca");
        r.setNatureContrat(NatureContrat.RC);
        r.setNatureEvenement(NatureEvenement.CIRCULATION_HORS_STATIONNEMENT);
        r.setZoneChoc(ZoneChoc.AVANT_LATERAL_GAUCHE);
        r.setReparateur("GARAGE TEST");
        r.setAccordReparateur(true);
        r.setTotalMainOeuvre(new Montants(dh("1200.00"), dh("240.00"), dh("1440.00")));
        r.setTotalFournitures(new Montants(dh("5600.00"), dh("120.00"), dh("5720.00")));
        r.setTotalGeneral(new Montants(dh("6800.00"), dh("360.00"), dh("7160.00")));
        r.setVetusteTtc(ZERO);
        r.setDepannage(ZERO);
        r.setTvaRecuperable(false);
        r.setMontantIndemnisation(dh("7160.00"));
        r.setMontantLettres("sept mille cent soixante Dirhams");
        r.setCommentaire("- LA VALEUR A NEUF DU VEHICULE A ETE RETENUE SUR LA BASE DU PRIX CATALOGUE DISPONIBLE : 379900.00");
        r.setGabaritVersion("tcpdf-expertise-v1");
        r.setImportFichier(source);

        Partie assure = new Partie(TypePartie.ASSURE);
        assure.setNomPrenom("ASSURE FICTIF");
        assure.setVehiculeLibelle("MARQUE MODELE");
        assure.setImmatriculation("00000-A-00");
        assure.setCompagnie("COMPAGNIE TEST");
        assure.setNumeroPolice("0000000000");
        r.ajouterPartie(assure);
        r.ajouterPartie(new Partie(TypePartie.ADVERSAIRE));

        Vehicule v = new Vehicule();
        v.setMarque("MARQUE");
        v.setModele("MODELE");
        v.setTypeMine("MODELE");
        v.setImmatriculation("00000-A-00");
        v.setNumeroSerie("VF0000000000000000");
        v.setEnergie(Energie.DIESEL);
        v.setKilometrage(281_277);
        v.setPuissanceFiscale((short) 6);
        v.setDateMiseCirculation(LocalDate.of(2022, 10, 7));
        v.setEtatGeneral("Normal");
        v.setUsurePneuAvg(dh("20.00"));
        v.setUsurePneuAvd(dh("20.00"));
        v.setUsurePneuArg(dh("20.00"));
        v.setUsurePneuArd(dh("20.00"));
        r.definirVehicule(v);

        for (PosteMainOeuvre poste : PosteMainOeuvre.values()) {
            boolean travaille = poste == PosteMainOeuvre.MO_1_TOLERIE || poste == PosteMainOeuvre.MO_PEINTURE;
            r.ajouterEvaluationMainOeuvre(travaille
                    ? new EvaluationMainOeuvre(poste, dh("10.00"), dh("60.00"),
                            new Montants(dh("600.00"), dh("120.00"), dh("720.00")))
                    : new EvaluationMainOeuvre(poste, ZERO, ZERO, new Montants(ZERO, ZERO, ZERO)));
        }

        r.ajouterFourniture(new Fourniture(CategorieFourniture.PIECES, new Montants(dh("5000.00"), ZERO, dh("5000.00"))));
        r.ajouterFourniture(new Fourniture(CategorieFourniture.INGREDIENT_PEINTURE,
                new Montants(dh("600.00"), dh("120.00"), dh("720.00"))));
        r.ajouterFourniture(new Fourniture(CategorieFourniture.PETITE_FOURNITURE, new Montants(ZERO, ZERO, ZERO)));
        r.ajouterFourniture(new Fourniture(CategorieFourniture.AUTRES, new Montants(ZERO, ZERO, ZERO)));

        r.ajouterLigneDetail(ligne("PP ET ING", FamilleTravaux.PEINTURE, null, "600.00", "20.00", "720.00",
                TypeOperation.ECHANGE, "10.00", "60.00", "720.00"));
        r.ajouterLigneDetail(ligne("OPTIQUE AVG", FamilleTravaux.TOLERIE, TypePiece.REEMPLOI, "5000.00", "0.00",
                "5000.00", TypeOperation.ECHANGE, "10.00", "60.00", "720.00"));
        r.ajouterLigneDetail(ligne("PARE CHOC AV", FamilleTravaux.TOLERIE, null, "0.00", "0.00", "0.00",
                TypeOperation.REPARATION, "0.00", "0.00", "0.00"));

        r.ajouterValeurCommentaire(new ValeurCommentaire(CleCommentaire.VALEUR_NEUF_CATALOGUE, null,
                dh("379900.00"), null));
        r.ajouterValeurCommentaire(new ValeurCommentaire(CleCommentaire.CADRE_EXPERTISE, "CLASSIQUE", null, null));

        r.ajouterResultatControle(new ResultatControle(TypeControle.COHERENCE, "INDEMNISATION",
                "Total général TTC - vétusté = indemnisation", "montantIndemnisation",
                dh("7160.00"), dh("7160.00"), true, MAINTENANT));
        return r;
    }

    private static LigneDetail ligne(String designation, FamilleTravaux famille, TypePiece type, String prix,
                                     String tva, String total, TypeOperation operation, String heures,
                                     String tarif, String montantMo) {
        LigneDetail l = new LigneDetail(dh("1.00"), designation);
        l.setZoneLibelle("Avant/Lateral gauche");
        l.setZoneChoc(ZoneChoc.AVANT_LATERAL_GAUCHE);
        l.setFamille(famille);
        l.setTypePiece(type);
        l.setPrixUnitaire(dh(prix));
        l.setVetustePct(ZERO);
        l.setTvaPct(dh(tva));
        l.setTotalFourniture(dh(total));
        l.setOperation(operation);
        l.setNombreHeures(dh(heures));
        l.setTarifHoraire(dh(tarif));
        l.setMontantMoTtc(dh(montantMo));
        return l;
    }
}
