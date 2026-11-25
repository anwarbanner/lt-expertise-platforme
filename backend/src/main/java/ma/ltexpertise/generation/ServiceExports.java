package ma.ltexpertise.generation;

import ma.ltexpertise.audit.ActionAudit;
import ma.ltexpertise.audit.JournalAudit;
import ma.ltexpertise.audit.JournalAuditRepository;
import ma.ltexpertise.commun.Montants;
import ma.ltexpertise.commun.Tableau;
import ma.ltexpertise.dossier.CriteresRecherche;
import ma.ltexpertise.dossier.DossierDtos.ControleDto;
import ma.ltexpertise.dossier.DossierDtos.DossierDetail;
import ma.ltexpertise.dossier.DossierDtos.DossierResume;
import ma.ltexpertise.dossier.DossierDtos.LigneDetailDto;
import ma.ltexpertise.dossier.DossierDtos.PartieDto;
import ma.ltexpertise.dossier.DossierIntrouvableException;
import ma.ltexpertise.dossier.RapportExpertise;
import ma.ltexpertise.dossier.ServiceDossier;
import ma.ltexpertise.statistiques.ServiceStatistiques;
import ma.ltexpertise.statistiques.ServiceStatistiques.LignePrix;
import ma.ltexpertise.utilisateur.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exports Excel (F9) : un dossier complet, la liste filtrée des dossiers, l'analyse des prix. Chaque export est
 * tracé au journal (EXPORT_EXCEL, P7) avec son périmètre.
 */
@Service
public class ServiceExports {

    /** Lignes lues par requête lors de l'export d'une longue liste. */
    private static final int PAGE = 500;
    /** Au-delà, l'analyse des prix est tronquée (l'écran en montre 200). */
    static final int LIGNES_PRIX_MAX = 100_000;

    private final ServiceDossier dossiers;
    private final ServiceStatistiques statistiques;
    private final JournalAuditRepository journal;
    private final Clock horloge;

    public ServiceExports(ServiceDossier dossiers, ServiceStatistiques statistiques, JournalAuditRepository journal,
                          Clock horloge) {
        this.dossiers = dossiers;
        this.statistiques = statistiques;
        this.journal = journal;
        this.horloge = horloge;
    }

    /** Un dossier : fiche, évaluation des dommages, détail des pièces, contrôles, valeurs du commentaire. */
    @Transactional
    public byte[] dossier(String numero, Utilisateur auteur) {
        DossierDetail d = dossiers.detail(numero).orElseThrow(DossierIntrouvableException::new);
        try (ClasseurExcel x = new ClasseurExcel()) {
            ClasseurExcel.Feuille fiche = x.feuille("Dossier", "Rubrique", "Valeur");
            fiche.ligne("N° de rapport", d.numeroRapport()).ligne("Statut", Libelles.statut(d.statut()))
                    .ligne("Procédure", Libelles.procedure(d.typeProcedure())).ligne("Cabinet", d.cabinet())
                    .ligne("Expert", d.expert()).ligne("Compagnie", d.compagnie()).ligne("Réf. sinistre", d.refSinistre())
                    .ligne("Date du sinistre", d.dateSinistre()).ligne("Date de mission", d.dateMission())
                    .ligne("Date d'expertise", d.dateExpertise()).ligne("Date du rapport", d.dateRapport())
                    .ligne("Lieu du rapport", d.lieuRapport()).ligne("Zones de choc", Libelles.zones(d.zonesChoc()))
                    .ligne("Réparateur", d.reparateur()).ligne("Accord du réparateur", Libelles.ouiNon(d.accordReparateur()));
            partie(fiche, "Assuré", d.assure());
            partie(fiche, "Adversaire", d.adversaire());
            if (d.vehicule() != null) {
                fiche.ligne("Véhicule : marque", d.vehicule().marque()).ligne("Véhicule : modèle", d.vehicule().modele())
                        .ligne("Véhicule : immatriculation", d.vehicule().immatriculation())
                        .ligne("Véhicule : n° de série", d.vehicule().numeroSerie())
                        .ligne("Véhicule : kilométrage", d.vehicule().kilometrage());
            }
            boolean vetusteHt = d.vetusteHt() != null;
            fiche.ligne("Total général HT", ht(d.totalGeneral())).ligne("Total général TTC", ttc(d.totalGeneral()))
                    .ligne(vetusteHt ? "Vétusté HT" : "Vétusté TTC", vetusteHt ? d.vetusteHt() : d.vetusteTtc())
                    .ligne("Dépannage", d.depannage()).ligne("TVA récupérable", Libelles.ouiNon(d.tvaRecuperable()))
                    .ligne("Valeur à neuf", d.valeurNeuf()).ligne("Valeur vénale", d.valeurVenale())
                    .ligne("Valeur épave", d.valeurEpave())
                    .ligne("Réforme (épaviste retenu)", Libelles.reforme(d.typeReforme()))
                    .ligne("Épaviste retenu : nom", d.epavisteNom()).ligne("Épaviste retenu : ville", d.epavisteVille())
                    .ligne("Montant de l'indemnisation", d.montantIndemnisation())
                    .ligne("Montant en lettres", d.montantLettres()).ligne("Commentaire de l'expert", d.commentaire())
                    .ligne("Validé par", d.validePar()).ligne("Validé le", d.valideLe());
            fiche.ajuster();

            ClasseurExcel.Feuille evaluation = x.feuille("Évaluation", "Poste", "Heures", "Taux", "HT", "TVA", "TTC");
            d.mainOeuvre().forEach(m -> evaluation.ligne(Libelles.poste(m.poste()), m.nombreHeures(), m.tauxHoraire(),
                    ht(m.montants()), tva(m.montants()), ttc(m.montants())));
            evaluation.ligne("Total main d'œuvre", null, null, ht(d.totalMainOeuvre()), tva(d.totalMainOeuvre()), ttc(d.totalMainOeuvre()));
            d.fournitures().forEach(f -> evaluation.ligne(Libelles.fourniture(f.categorie()), null, null,
                    ht(f.montants()), tva(f.montants()), ttc(f.montants())));
            evaluation.ligne("Total fournitures", null, null, ht(d.totalFournitures()), tva(d.totalFournitures()), ttc(d.totalFournitures()));
            evaluation.ligne("Total général", null, null, ht(d.totalGeneral()), tva(d.totalGeneral()), ttc(d.totalGeneral()));
            evaluation.ajuster();

            ClasseurExcel.Feuille pieces = x.feuille("Détail des pièces", "N°", "Zone", "Qté", "Désignation", "Type",
                    "Prix unitaire", "Vétusté %", "TVA %", "Total", "Opération", "Heures", "Tarif", "Montant MO TTC", "Prix cohérent");
            for (LigneDetailDto l : d.lignesDetail()) {
                pieces.ligne(l.ordre() + 1, l.zoneLibelle(), l.quantite(), l.designation(),
                        l.typePiece() == null ? null : l.typePiece().name().substring(0, 1), l.prixUnitaire(),
                        l.vetustePct(), l.tvaPct(), l.totalFourniture(), l.operation(), l.nombreHeures(), l.tarifHoraire(),
                        l.montantMoTtc(), l.prixCoherent());
            }
            pieces.ajuster();

            ClasseurExcel.Feuille controles = x.feuille("Contrôles", "Contrôle", "Attendu", "Trouvé", "Conforme", "Bloquant");
            for (ControleDto c : d.controles()) {
                controles.ligne(c.libelle(), c.attendu(), c.obtenu(), c.conforme(), c.bloquant());
            }
            controles.ajuster();

            if (!d.valeursCommentaire().isEmpty()) {
                ClasseurExcel.Feuille valeurs = x.feuille("Commentaire", "Valeur clé", "Valeur");
                d.valeursCommentaire().forEach(v -> valeurs.ligne(Libelles.commentaire(v.cle()),
                        v.montant() != null ? v.montant() : v.date() != null ? v.date() : v.texte()));
                valeurs.ajuster();
            }
            byte[] octets = x.octets();
            tracer(auteur, d.id(), Map.of("export", "dossier", "numeroRapport", d.numeroRapport()));
            return octets;
        }
    }

    /** Liste filtrée des dossiers, lue par pages de 500 (toute la liste, pas seulement la page affichée). */
    @Transactional
    public byte[] liste(CriteresRecherche criteres, Utilisateur auteur) {
        try (ClasseurExcel x = new ClasseurExcel()) {
            ClasseurExcel.Feuille f = x.feuille("Dossiers", "N° de rapport", "Statut", "Procédure", "Date du sinistre",
                    "Date du rapport", "Assuré", "Immatriculation", "Compagnie", "Zones de choc", "Indemnisation",
                    "Anomalies bloquantes", "Importé le");
            int lignes = 0;
            Page<DossierResume> page;
            int numero = 0;
            do {
                page = dossiers.rechercher(criteres, PageRequest.of(numero++, PAGE, Sort.by(Sort.Direction.DESC, "creeLe", "id")));
                for (DossierResume d : page.getContent()) {
                    f.ligne(d.numeroRapport(), Libelles.statut(d.statut()), Libelles.procedure(d.typeProcedure()),
                            d.dateSinistre(), d.dateRapport(), d.assure(), d.immatriculation(), d.compagnie(),
                            Libelles.zones(d.zonesChoc()), d.montantIndemnisation(), d.anomalies(), d.creeLe());
                    lignes++;
                }
            } while (page.hasNext());
            f.ajuster();
            byte[] octets = x.octets();
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("export", "liste des dossiers");
            details.put("lignes", lignes);
            details.put("filtres", criteres.toString());
            tracer(auteur, null, details);
            return octets;
        }
    }

    /** Toutes les lignes de prix répondant aux filtres (l'écran n'en affiche que les 200 plus récentes). */
    @Transactional
    public byte[] prix(ServiceStatistiques.FiltresPrix filtres, Utilisateur auteur) {
        ServiceStatistiques.AnalysePrix a = statistiques.prix(filtres, Tableau.tout(LIGNES_PRIX_MAX));
        try (ClasseurExcel x = new ClasseurExcel()) {
            ClasseurExcel.Feuille f = x.feuille("Analyse des prix", "N° de rapport", "Désignation", "Type", "Marque",
                    "Modèle", "Type mine", "Prix unitaire", "Date du prix");
            for (LignePrix l : a.lignes()) {
                f.ligne(l.numeroRapport(), l.designation(), l.typePiece(), l.marque(), l.modele(), l.typeMine(),
                        l.prixUnitaire(), l.date());
            }
            f.ajuster();
            ClasseurExcel.Feuille synthese = x.feuille("Synthèse", "Indicateur", "Valeur");
            synthese.ligne("Dossiers trouvés", a.dossiers()).ligne("Lignes", a.nombre())
                    .ligne("Prix moyen", a.moyenne()).ligne("Prix maximum", a.maximum()).ligne("Prix minimum", a.minimum());
            a.parType().forEach(t -> synthese.ligne("Lignes " + t.type(), t.nombre()).ligne("Prix moyen " + t.type(), t.moyenne()));
            synthese.ajuster();
            byte[] octets = x.octets();
            tracer(auteur, null, Map.of("export", "analyse des prix", "lignes", a.lignes().size()));
            return octets;
        }
    }

    private void tracer(Utilisateur auteur, Long rapportId, Map<String, Object> details) {
        JournalAudit trace = new JournalAudit(ActionAudit.EXPORT_EXCEL, auteur, OffsetDateTime.now(horloge));
        if (rapportId != null) {
            trace.surEntite(RapportExpertise.class.getSimpleName(), rapportId, rapportId);
        }
        journal.save(trace.avecDetails(details));
    }

    private static void partie(ClasseurExcel.Feuille f, String role, PartieDto p) {
        if (p == null) {
            return;
        }
        f.ligne(role + " : nom", p.nomPrenom()).ligne(role + " : véhicule", p.vehicule())
                .ligne(role + " : immatriculation", p.immatriculation()).ligne(role + " : compagnie", p.compagnie())
                .ligne(role + " : n° de police", p.numeroPolice());
    }

    private static java.math.BigDecimal ht(Montants m) {
        return m == null ? null : m.getHt();
    }

    private static java.math.BigDecimal tva(Montants m) {
        return m == null ? null : m.getTva();
    }

    private static java.math.BigDecimal ttc(Montants m) {
        return m == null ? null : m.getTtc();
    }
}
