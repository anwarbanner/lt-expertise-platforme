package ma.ltexpertise.generation;

import ma.ltexpertise.audit.ActionAudit;
import ma.ltexpertise.audit.JournalAudit;
import ma.ltexpertise.audit.JournalAuditRepository;
import ma.ltexpertise.commun.Montants;
import ma.ltexpertise.dossier.DossierDtos.DossierDetail;
import ma.ltexpertise.dossier.DossierDtos.LigneDetailDto;
import ma.ltexpertise.dossier.DossierDtos.LigneMainOeuvreDto;
import ma.ltexpertise.dossier.DossierDtos.PartieDto;
import ma.ltexpertise.dossier.DossierDtos.VehiculeDto;
import ma.ltexpertise.dossier.DossierIntrouvableException;
import ma.ltexpertise.dossier.RapportExpertise;
import ma.ltexpertise.dossier.ServiceDossier;
import ma.ltexpertise.dossier.referentiel.CategorieFourniture;
import ma.ltexpertise.dossier.referentiel.NatureRapport;
import ma.ltexpertise.dossier.referentiel.PosteMainOeuvre;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.utilisateur.Utilisateur;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Rapport PDF régénéré (F9, P10) : seulement depuis un dossier validé, avec la structure du rapport d'origine et les
 * données en base (corrections du gestionnaire comprises). Chaque génération est tracée (EXPORT_PDF, P7) ; ces
 * traces forment l'historique des rapports générés.
 */
@Service
public class ServiceRapports {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    private final ServiceDossier dossiers;
    private final JournalAuditRepository journal;
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock horloge;
    private final RapportPdf modele = new RapportPdf();

    public ServiceRapports(ServiceDossier dossiers, JournalAuditRepository journal, NamedParameterJdbcTemplate jdbc,
                           Clock horloge) {
        this.dossiers = dossiers;
        this.journal = journal;
        this.jdbc = jdbc;
        this.horloge = horloge;
    }

    /** Rapport généré : nom de fichier proposé et contenu PDF. */
    public record RapportGenere(String nomFichier, byte[] contenu) {
    }

    /** Entrée de l'historique des rapports générés. */
    public record Generation(OffsetDateTime genereLe, String generePar, String numeroRapport, String assure,
                             String compagnie) {
    }

    @Transactional
    public RapportGenere generer(String numero, Utilisateur auteur) {
        DossierDetail d = dossiers.detail(numero).orElseThrow(DossierIntrouvableException::new);
        // Un dossier archivé a été validé : son rapport se génère toujours
        if (d.statut() != StatutDossier.VALIDE && d.statut() != StatutDossier.ARCHIVE) {
            throw new GenerationRefuseeException("Le rapport PDF ne se génère qu'à partir d'un dossier validé (P10) : "
                    + "validez d'abord le dossier " + numero + ".");
        }
        OffsetDateTime maintenant = OffsetDateTime.now(horloge);
        byte[] pdf = modele.generer(parametres(d, auteur.getIdentifiant(), maintenant), lignes(d.lignesDetail()));
        journal.save(new JournalAudit(ActionAudit.EXPORT_PDF, auteur, maintenant)
                .surEntite(RapportExpertise.class.getSimpleName(), d.id(), d.id())
                .avecDetails(Map.of("export", "rapport PDF", "numeroRapport", d.numeroRapport(), "octets", pdf.length)));
        return new RapportGenere("rapport-" + d.numeroRapport().replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf", pdf);
    }

    /** Derniers rapports générés (traces EXPORT_PDF), du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<Generation> historique(int limite) {
        return jdbc.query("select j.horodatage, j.identifiant_utilisateur, r.numero_rapport, r.compagnie, "
                        + "(select p.nom_prenom from partie p where p.rapport_id = r.id and p.type_partie = 'ASSURE' limit 1) as assure "
                        + "from journal_audit j join rapport_expertise r on r.id = j.rapport_id "
                        + "where j.action = 'EXPORT_PDF' order by j.horodatage desc, j.id desc limit :limite",
                new MapSqlParameterSource("limite", Math.max(1, Math.min(limite, 200))),
                (rs, i) -> new Generation(rs.getObject("horodatage", OffsetDateTime.class),
                        rs.getString("identifiant_utilisateur"), rs.getString("numero_rapport"), rs.getString("assure"),
                        rs.getString("compagnie")));
    }

    // ------------------------------------------------------------------ mise en forme (comme le rapport d'origine)

    static Map<String, Object> parametres(DossierDetail d, String generePar, OffsetDateTime maintenant) {
        Map<String, Object> p = new HashMap<>();
        p.put("cabinet", texte(d.cabinet()));
        p.put("expert", texte(d.expert()));
        String nature = d.natureRapport() == NatureRapport.REFORME ? " REFORME"
                : d.natureRapport() == NatureRapport.NON_DEFINITIF ? " NON DEFINITIF" : "";
        p.put("titre", "RAPPORT D'EXPERTISE" + nature + " : " + d.numeroRapport());
        p.put("procedure", texte(Libelles.procedure(d.typeProcedure())));
        p.put("refSinistre", texte(d.refSinistre()));
        p.put("dateSinistre", date(d.dateSinistre()));
        p.put("sinistre", "Réf sinistre : " + texte(d.refSinistre()) + "     Date sinistre : " + date(d.dateSinistre())
                + "     Date mission : " + date(d.dateMission()) + "     Assurance : " + texte(d.compagnie()));

        PartieDto a = d.assure();
        PartieDto b = d.adversaire();
        String[] etiquettes = {"Nom & Prénom", "Véhicule", "Immatriculation", "Cie d'assurance", "N° Police", "Intermédiaire"};
        List<Function<PartieDto, String>> lecteurs = List.of(PartieDto::nomPrenom, PartieDto::vehicule,
                PartieDto::immatriculation, PartieDto::compagnie, PartieDto::numeroPolice, PartieDto::intermediaire);
        String[][] parties = new String[RapportPdf.LIGNES_PARTIES][];
        for (int i = 0; i < parties.length; i++) {
            parties[i] = new String[]{etiquettes[i], a == null ? "" : texte(lecteurs.get(i).apply(a)),
                    b == null ? "" : texte(lecteurs.get(i).apply(b))};
        }
        RapportPdf.aplatir(p, "parties", parties);
        p.put("natureContrat", "Nature de contrat : " + texte(d.natureContrat() == null ? null
                : d.natureContrat().name().replace('_', ' ')) + "     Nature de l'événement : "
                + texte(d.natureEvenement() == null ? null : d.natureEvenement().name().replace('_', ' ').toLowerCase(Locale.ROOT)));
        p.put("reparateur", "Réparateur : " + texte(d.reparateur()) + "     Accord du réparateur : "
                + (Boolean.TRUE.equals(d.accordReparateur()) ? "Oui ( X ) Non ( )"
                : Boolean.FALSE.equals(d.accordReparateur()) ? "Oui ( ) Non ( X )" : "Oui ( ) Non ( )"));

        VehiculeDto v = d.vehicule();
        RapportPdf.aplatir(p, "vehicule", new String[][]{
                {"Marque", v == null ? "" : texte(v.marque()), "Immatriculation", v == null ? "" : texte(v.immatriculation())},
                {"Modèle", v == null ? "" : texte(v.modele()), "N° série", v == null ? "" : texte(v.numeroSerie())},
                {"Type mine", v == null ? "" : texte(v.typeMine()), "Puissance", v == null ? "" : texte(v.puissanceFiscale())},
                {"Energie", v == null ? "" : texte(v.energie()), "D.M.C", v == null ? "" : date(v.dateMiseCirculation())},
                {"Kilométrage", v == null ? "" : v.kilometrageNonEstimable() ? "Non estimable"
                        : v.kilometrage() == null ? "" : v.kilometrage() + " Km", "Et. général", v == null ? "" : texte(v.etatGeneral())},
                {"Usure pneus", v == null ? "" : "AVG " + pct(v.usurePneuAvg()) + "  AVD " + pct(v.usurePneuAvd())
                        + "  ARG " + pct(v.usurePneuArg()) + "  ARD " + pct(v.usurePneuArd()),
                        "Zone(s) de choc", texte(Libelles.zones(d.zonesChoc()))}});
        p.put("datesTravaux", "Date expertise véhicule : " + date(d.dateExpertise()) + "     Avant réparation : "
                + date(d.dateAvantReparation()) + "     En cours travaux : " + date(d.dateEnCoursTravaux())
                + "     Après travaux : " + date(d.dateApresTravaux()));
        RapportPdf.aplatir(p, "evaluation", evaluation(d));

        p.put("deduire", "A Déduire     Vétusté " + (d.vetusteHt() != null ? "HT : " + montant(d.vetusteHt())
                : "TTC : " + montant(d.vetusteTtc())) + "     Dépannage : " + montant(d.depannage())
                + "     Immobilisation : " + montant(d.immobilisation()));
        String valeurs = "Valeur à neuf : " + montantOuNc(d.valeurNeuf()) + "     Valeur assurée : "
                + montantOuNc(d.valeurAssuree()) + "     Valeur vénale : " + montantOuNc(d.valeurVenale())
                + "     Valeur épave : " + montantOuNc(d.valeurEpave());
        if (d.typeReforme() != null) {
            valeurs += "\nRéforme " + (d.typeReforme().name().equals("ECONOMIQUE") ? "économique" : "technique")
                    + "     Epaviste retenu : " + texte(d.epavisteNom()) + " " + texte(d.epavisteVille());
        }
        p.put("valeurs", valeurs);
        p.put("tva", "Récupérabilité de TVA : " + (Boolean.TRUE.equals(d.tvaRecuperable()) ? "Récupérable" : "Non récupérable"));
        p.put("indemnisation", "Montant de l'indemnisation : " + montant(d.montantIndemnisation()) + " DH");
        p.put("montantLettres", texte(d.montantLettres()));
        p.put("faitA", "Fait à " + texte(d.lieuRapport()) + " le " + date(d.dateRapport()));
        p.put("mention", "Rapport régénéré le " + maintenant.atZoneSameInstant(ZoneId.systemDefault()).format(HORODATAGE)
                + " par " + generePar + ", à partir des données du dossier validé par " + texte(d.validePar())
                + (d.valideLe() == null ? "" : " le " + d.valideLe().atZoneSameInstant(ZoneId.systemDefault()).format(DATE))
                + (d.nomFichier() == null ? "." : ". Rapport d'origine : " + d.nomFichier() + "."));
        p.put("commentaire", texte(d.commentaire()));
        return p;
    }

    /** 7 postes, total main d'œuvre, 4 fournitures, total fournitures, total général : 6 colonnes chacune. */
    private static String[][] evaluation(DossierDetail d) {
        List<String[]> lignes = new ArrayList<>();
        for (PosteMainOeuvre poste : PosteMainOeuvre.values()) {
            LigneMainOeuvreDto m = d.mainOeuvre().stream().filter(x -> x.poste() == poste).findFirst().orElse(null);
            lignes.add(m == null ? new String[]{Libelles.poste(poste), "", "", "", "", ""}
                    : new String[]{Libelles.poste(poste), montant(m.nombreHeures()), montant(m.tauxHoraire()),
                    montant(ht(m.montants())), montant(tva(m.montants())), montant(ttc(m.montants()))});
        }
        lignes.add(total("Total main d'oeuvre", d.totalMainOeuvre()));
        for (CategorieFourniture c : CategorieFourniture.values()) {
            Montants m = d.fournitures().stream().filter(f -> f.categorie() == c).map(f -> f.montants()).findFirst().orElse(null);
            lignes.add(total(Libelles.fourniture(c), m));
        }
        lignes.add(total("Total fournitures (Dh)", d.totalFournitures()));
        lignes.add(total("Total général", d.totalGeneral()));
        String[][] tableau = new String[RapportPdf.LIGNES_EVALUATION][];
        for (int i = 0; i < tableau.length; i++) {
            tableau[i] = i < lignes.size() ? lignes.get(i) : new String[]{"", "", "", "", "", ""};
        }
        return tableau;
    }

    private static String[] total(String libelle, Montants m) {
        return new String[]{libelle, "", "", montant(ht(m)), montant(tva(m)), montant(ttc(m))};
    }

    static Collection<Map<String, ?>> lignes(List<LigneDetailDto> lignes) {
        List<Map<String, ?>> resultat = new ArrayList<>();
        for (LigneDetailDto l : lignes) {
            Map<String, Object> m = new HashMap<>();
            m.put("n", String.valueOf(l.ordre() + 1));
            m.put("qte", nombre(l.quantite()));
            m.put("designation", texte(l.designation()));
            m.put("type", l.typePiece() == null ? "" : l.typePiece().name().substring(0, 1));
            m.put("prix", montant(l.prixUnitaire()));
            m.put("vetuste", montant(l.vetustePct()));
            m.put("tva", montant(l.tvaPct()));
            m.put("total", montant(l.totalFourniture()));
            m.put("travail", l.operation() == null ? "" : l.operation().name().equals("ECHANGE") ? "Echange" : "Réparation");
            m.put("heures", montant(l.nombreHeures()));
            m.put("tarif", montant(l.tarifHoraire()));
            m.put("montant", montant(l.montantMoTtc()));
            m.put("zone", l.zoneLibelle() == null ? "Zone non précisée" : l.zoneLibelle());
            m.put("famille", l.famille() == null ? "Autres" : switch (l.famille()) {
                case TOLERIE -> "Tolerie";
                case PEINTURE -> "Peinture";
                case MECANIQUE -> "Mécanique";
            });
            resultat.add(m);
        }
        return resultat;
    }

    /** Montants comme dans le rapport d'origine : « 1200.00 ». */
    private static String montant(BigDecimal v) {
        return v == null ? "" : String.format(Locale.ROOT, "%.2f", v);
    }

    private static String montantOuNc(BigDecimal v) {
        return v == null ? "NC" : montant(v);
    }

    private static String nombre(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }

    private static String pct(BigDecimal v) {
        return v == null ? ": %" : ":" + nombre(v) + " %";
    }

    private static String date(LocalDate d) {
        return d == null ? "" : d.format(DATE);
    }

    private static String texte(Object v) {
        return Objects.toString(v, "");
    }

    private static BigDecimal ht(Montants m) {
        return m == null ? null : m.getHt();
    }

    private static BigDecimal tva(Montants m) {
        return m == null ? null : m.getTva();
    }

    private static BigDecimal ttc(Montants m) {
        return m == null ? null : m.getTtc();
    }
}
