package ma.ltexpertise.dossier;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.ltexpertise.commun.Montants;
import ma.ltexpertise.dossier.referentiel.CategorieFourniture;
import ma.ltexpertise.dossier.referentiel.CleCommentaire;
import ma.ltexpertise.dossier.referentiel.Energie;
import ma.ltexpertise.dossier.referentiel.FamilleTravaux;
import ma.ltexpertise.dossier.referentiel.NatureContrat;
import ma.ltexpertise.dossier.referentiel.NatureEvenement;
import ma.ltexpertise.dossier.referentiel.NatureRapport;
import ma.ltexpertise.dossier.referentiel.PosteMainOeuvre;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.dossier.referentiel.TypeControle;
import ma.ltexpertise.dossier.referentiel.TypeOperation;
import ma.ltexpertise.dossier.referentiel.TypePiece;
import ma.ltexpertise.dossier.referentiel.TypeProcedure;
import ma.ltexpertise.dossier.referentiel.TypeReforme;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Contrats JSON de l'API des dossiers (F5, F7, F8). */
public final class DossierDtos {

    private DossierDtos() {
    }

    /**
     * Ligne de la liste des dossiers.
     *
     * @param anomalies         contrôles bloquants en échec (incohérences et champs essentiels manquants)
     * @param libellesAnomalies leurs libellés, dans l'ordre des contrôles
     */
    public record DossierResume(Long id, String numeroRapport, StatutDossier statut, NatureRapport natureRapport,
                                TypeProcedure typeProcedure, LocalDate dateSinistre, LocalDate dateRapport,
                                String assure, String immatriculation, String compagnie, List<Integer> zonesChoc,
                                BigDecimal montantIndemnisation, OffsetDateTime creeLe, long anomalies,
                                List<String> libellesAnomalies) {
    }

    public record PartieDto(@Size(max = 150) String nomPrenom, @Size(max = 100) String vehicule,
                            @Size(max = 20) String immatriculation, @Size(max = 100) String compagnie,
                            @Size(max = 30) String numeroPolice, @Size(max = 150) String intermediaire) {
    }

    public record VehiculeDto(@Size(max = 50) String marque, @Size(max = 80) String modele,
                              @Size(max = 80) String typeMine, @Size(max = 20) String immatriculation,
                              @Size(max = 30) String numeroSerie, Energie energie, @Min(0) Integer kilometrage,
                              boolean kilometrageNonEstimable, @Min(0) Short puissanceFiscale,
                              LocalDate dateMiseCirculation, @Size(max = 50) String etatGeneral,
                              BigDecimal usurePneuAvg, BigDecimal usurePneuAvd, BigDecimal usurePneuArg,
                              BigDecimal usurePneuArd) {
    }

    public record LigneMainOeuvreDto(PosteMainOeuvre poste, BigDecimal nombreHeures, BigDecimal tauxHoraire,
                                     Montants montants) {
    }

    public record FournitureDto(CategorieFourniture categorie, Montants montants) {
    }

    /**
     * Ligne de la page 2. {@code prixCoherent} : quantité × prix × (1 + TVA) × (1 − vétusté) = total ;
     * {@code prixCalcule} : prix unitaire qui redonne le total imprimé, proposé pour corriger un prix faux.
     */
    public record LigneDetailDto(short ordre, String zoneLibelle, Integer zoneChoc, FamilleTravaux famille,
                                 BigDecimal quantite, String designation, TypePiece typePiece, BigDecimal prixUnitaire,
                                 BigDecimal vetustePct, BigDecimal tvaPct, BigDecimal totalFourniture,
                                 TypeOperation operation, BigDecimal nombreHeures, BigDecimal tarifHoraire,
                                 BigDecimal montantMoTtc, boolean prixCoherent, BigDecimal prixCalcule) {
    }

    /** Correction du prix unitaire d'une ligne de la page 2, désignée par son ordre. */
    public record PrixLigne(@NotNull Short ordre, @DecimalMin("0") BigDecimal prixUnitaire) {
    }

    /** {@code bloquant} : un échec empêche la validation ; sinon c'est un avertissement. */
    public record ControleDto(TypeControle type, String code, String libelle, String champ, BigDecimal attendu,
                              BigDecimal obtenu, boolean conforme, boolean bloquant) {
    }

    /** Valeur clé lue dans le commentaire de l'expert (F12). */
    public record ValeurCommentaireDto(CleCommentaire cle, String texte, BigDecimal montant, LocalDate date) {
    }

    /** Fiche complète d'un dossier : toutes les données extraites ou saisies, contrôles et PDF d'origine. */
    public record DossierDetail(Long id, String numeroRapport, StatutDossier statut, NatureRapport natureRapport,
                                TypeProcedure typeProcedure, String cabinet, String expert, String compagnie,
                                String refSinistre, LocalDate dateSinistre, LocalDate dateMission,
                                LocalDate dateExpertise, LocalDate dateAvantReparation, LocalDate dateEnCoursTravaux,
                                LocalDate dateApresTravaux, LocalDate dateRapport, String lieuRapport,
                                NatureContrat natureContrat, NatureEvenement natureEvenement, List<Integer> zonesChoc,
                                String reparateur, Boolean accordReparateur, PartieDto assure, PartieDto adversaire,
                                VehiculeDto vehicule, List<LigneMainOeuvreDto> mainOeuvre,
                                List<FournitureDto> fournitures, Montants totalMainOeuvre, Montants totalFournitures,
                                Montants totalGeneral, BigDecimal vetusteTtc, BigDecimal vetusteHt,
                                BigDecimal depannage, BigDecimal immobilisation, TypeReforme typeReforme,
                                String epavisteNom, String epavisteVille, BigDecimal valeurNeuf,
                                BigDecimal valeurAssuree, BigDecimal valeurVenale, BigDecimal valeurEpave,
                                Boolean tvaRecuperable, BigDecimal montantIndemnisation, String montantLettres,
                                String commentaire, List<ValeurCommentaireDto> valeursCommentaire,
                                List<LigneDetailDto> lignesDetail, List<ControleDto> controles,
                                List<String> champsASaisir, Long importId, String nomFichier, String validePar,
                                OffsetDateTime valideLe, String annulePar, OffsetDateTime annuleLe,
                                String motifAnnulation, OffsetDateTime creeLe, int version) {
    }

    /**
     * Champs modifiables à l'écran de vérification (F5). Les tableaux de montants restent ceux du rapport.
     * {@code version} est celle lue avec le dossier : une modification concurrente est refusée.
     */
    public record DossierModification(
            int version,
            TypeProcedure typeProcedure,
            @Size(max = 100) String cabinet, @Size(max = 100) String expert, @Size(max = 100) String compagnie,
            @Size(max = 30) String refSinistre,
            LocalDate dateSinistre, LocalDate dateMission, LocalDate dateExpertise, LocalDate dateAvantReparation,
            LocalDate dateEnCoursTravaux, LocalDate dateApresTravaux, LocalDate dateRapport,
            @Size(max = 60) String lieuRapport,
            NatureContrat natureContrat, NatureEvenement natureEvenement,
            List<@Min(1) @Max(8) Integer> zonesChoc,
            @Size(max = 150) String reparateur, Boolean accordReparateur,
            @Valid PartieDto assure, @Valid PartieDto adversaire, @Valid VehiculeDto vehicule,
            @DecimalMin("0") BigDecimal vetusteTtc, @DecimalMin("0") BigDecimal depannage,
            @DecimalMin("0") BigDecimal valeurNeuf, @DecimalMin("0") BigDecimal valeurAssuree,
            @DecimalMin("0") BigDecimal valeurVenale, @DecimalMin("0") BigDecimal valeurEpave,
            Boolean tvaRecuperable,
            @DecimalMin("0") BigDecimal montantIndemnisation, @Size(max = 300) String montantLettres,
            String commentaire,
            List<@Valid PrixLigne> prixLignes) {
    }

    /**
     * Validation en lot (P3, par un gestionnaire) : les numéros cochés, ou tous les dossiers prêts à valider qui
     * répondent aux filtres de la liste ({@code tousLesPrets}). Les dossiers non prêts sont ignorés et listés.
     */
    public record ValidationGroupee(List<String> numeros, boolean tousLesPrets, String q, String compagnie,
                                    Integer zone, LocalDate du, LocalDate au, BigDecimal montantMin,
                                    BigDecimal montantMax) {
    }

    public record DossierIgnore(String numeroRapport, String motif) {
    }

    /** Archivage en lot : les numéros cochés, ou tous les dossiers validés répondant aux filtres. */
    public record ArchivageGroupe(List<String> numeros, boolean tousLesValides, String q, String compagnie,
                                  Integer zone, LocalDate du, LocalDate au, BigDecimal montantMin,
                                  BigDecimal montantMax) {
    }

    /** {@code paquets} : nombre de paquets de 500 dossiers traités, chacun dans sa propre transaction. */
    public record ResultatArchivageGroupe(int archives, List<DossierIgnore> ignores, int paquets) {
    }

    /** {@code paquets} : nombre de paquets de 500 dossiers traités, chacun dans sa propre transaction. */
    public record ResultatValidationGroupee(int valides, List<DossierIgnore> ignores, int paquets) {
    }

    /** Entrée de l'historique d'un dossier (P7). */
    public record EvenementHistorique(OffsetDateTime horodatage, String action, String utilisateur, String champ,
                                      String ancienneValeur, String nouvelleValeur, Map<String, Object> details) {
    }
}
