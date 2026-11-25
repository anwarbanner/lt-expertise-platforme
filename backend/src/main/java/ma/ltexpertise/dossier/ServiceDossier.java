package ma.ltexpertise.dossier;

import ma.ltexpertise.audit.ActionAudit;
import ma.ltexpertise.audit.JournalAudit;
import ma.ltexpertise.audit.JournalAuditRepository;
import ma.ltexpertise.controle.Controle;
import ma.ltexpertise.controle.ControleCoherenceService;
import ma.ltexpertise.dossier.DossierDtos.ControleDto;
import ma.ltexpertise.dossier.DossierDtos.ArchivageGroupe;
import ma.ltexpertise.dossier.DossierDtos.DossierDetail;
import ma.ltexpertise.dossier.DossierDtos.DossierIgnore;
import ma.ltexpertise.dossier.DossierDtos.DossierModification;
import ma.ltexpertise.dossier.DossierDtos.DossierResume;
import ma.ltexpertise.dossier.DossierDtos.EvenementHistorique;
import ma.ltexpertise.dossier.DossierDtos.FournitureDto;
import ma.ltexpertise.dossier.DossierDtos.LigneDetailDto;
import ma.ltexpertise.dossier.DossierDtos.LigneMainOeuvreDto;
import ma.ltexpertise.dossier.DossierDtos.PartieDto;
import ma.ltexpertise.dossier.DossierDtos.PrixLigne;
import ma.ltexpertise.dossier.DossierDtos.ResultatArchivageGroupe;
import ma.ltexpertise.dossier.DossierDtos.ResultatValidationGroupee;
import ma.ltexpertise.dossier.DossierDtos.ValeurCommentaireDto;
import ma.ltexpertise.dossier.DossierDtos.ValidationGroupee;
import ma.ltexpertise.dossier.DossierDtos.VehiculeDto;
import ma.ltexpertise.dossier.referentiel.NatureRapport;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.dossier.referentiel.TypeControle;
import ma.ltexpertise.dossier.referentiel.TypePartie;
import ma.ltexpertise.dossier.referentiel.ZoneChoc;
import ma.ltexpertise.extraction.RapportExtrait;
import ma.ltexpertise.importation.ConversionRapportExtrait;
import ma.ltexpertise.utilisateur.Utilisateur;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Consultation (F8), recherche (F7), modification à l'écran de vérification (F5) et validation (P3) des dossiers.
 * Chaque champ modifié est tracé avec son ancienne et sa nouvelle valeur (P7) ; les contrôles et le statut sont
 * recalculés après chaque modification.
 */
@Service
public class ServiceDossier {

    private final RapportExpertiseRepository rapports;
    private final JournalAuditRepository journal;
    private final ControleCoherenceService controles;
    private final Clock horloge;
    /** Une transaction par paquet de la validation en lot. */
    private final TransactionTemplate transaction;

    /** Taille d’un paquet de la validation en lot : chaque paquet est validé et enregistré séparément. */
    private final int taillePaquet;

    public ServiceDossier(RapportExpertiseRepository rapports, JournalAuditRepository journal,
                          ControleCoherenceService controles, Clock horloge, PlatformTransactionManager transactions,
                          @Value("${lt.dossiers.taille-paquet-validation:500}") int taillePaquet) {
        this.rapports = rapports;
        this.journal = journal;
        this.controles = controles;
        this.horloge = horloge;
        this.transaction = new TransactionTemplate(transactions);
        this.taillePaquet = taillePaquet;
    }

    @Transactional(readOnly = true)
    public Page<DossierResume> rechercher(CriteresRecherche criteres, Pageable pagination) {
        return rapports.findAll(criteres.specification(), pagination).map(ServiceDossier::resume);
    }

    @Transactional(readOnly = true)
    public Map<StatutDossier, Long> compteurs() {
        Map<StatutDossier, Long> compteurs = new EnumMap<>(StatutDossier.class);
        for (StatutDossier statut : StatutDossier.values()) {
            compteurs.put(statut, rapports.countByStatut(statut));
        }
        return compteurs;
    }

    @Transactional(readOnly = true)
    public List<String> compagnies() {
        return rapports.compagnies();
    }

    @Transactional(readOnly = true)
    public Optional<DossierDetail> detail(String numero) {
        return detail(numero, null);
    }

    /** @param id identifiant d'un import annulé portant ce n° ; {@code null} = le dossier actif */
    @Transactional(readOnly = true)
    public Optional<DossierDetail> detail(String numero, Long id) {
        return rapports.findByNumeroRapport(numero, id).map(ServiceDossier::detail);
    }

    @Transactional
    public DossierDetail modifier(String numero, DossierModification m, Utilisateur auteur) {
        RapportExpertise r = rapports.findByNumeroRapport(numero).orElseThrow(DossierIntrouvableException::new);
        if (r.estClos()) {
            throw new ValidationRefuseeException("Un dossier archivé ou annulé ne se modifie plus.");
        }
        if (r.getVersion() != m.version()) {
            throw new ObjectOptimisticLockingFailureException(RapportExpertise.class, r.getId());
        }
        OffsetDateTime maintenant = OffsetDateTime.now(horloge);
        Changements c = new Changements();
        c.champ("typeProcedure", r.getTypeProcedure(), m.typeProcedure(), r::setTypeProcedure);
        c.champ("cabinet", r.getCabinet(), vide(m.cabinet()), r::setCabinet);
        c.champ("expert", r.getExpert(), vide(m.expert()), r::setExpert);
        c.champ("compagnie", r.getCompagnie(), vide(m.compagnie()), r::setCompagnie);
        c.champ("refSinistre", r.getRefSinistre(), vide(m.refSinistre()), r::setRefSinistre);
        c.champ("dateSinistre", r.getDateSinistre(), m.dateSinistre(), r::setDateSinistre);
        c.champ("dateMission", r.getDateMission(), m.dateMission(), r::setDateMission);
        c.champ("dateExpertise", r.getDateExpertise(), m.dateExpertise(), r::setDateExpertise);
        c.champ("dateAvantReparation", r.getDateAvantReparation(), m.dateAvantReparation(), r::setDateAvantReparation);
        c.champ("dateEnCoursTravaux", r.getDateEnCoursTravaux(), m.dateEnCoursTravaux(), r::setDateEnCoursTravaux);
        c.champ("dateApresTravaux", r.getDateApresTravaux(), m.dateApresTravaux(), r::setDateApresTravaux);
        c.champ("dateRapport", r.getDateRapport(), m.dateRapport(), r::setDateRapport);
        c.champ("lieuRapport", r.getLieuRapport(), vide(m.lieuRapport()), r::setLieuRapport);
        c.champ("natureContrat", r.getNatureContrat(), m.natureContrat(), r::setNatureContrat);
        c.champ("natureEvenement", r.getNatureEvenement(), m.natureEvenement(), r::setNatureEvenement);
        c.champ("zonesChoc", codes(r.getZonesChoc()), m.zonesChoc() == null ? List.<Integer>of()
                        : m.zonesChoc().stream().filter(Objects::nonNull).distinct().sorted().toList(),
                z -> r.setZonesChoc(z.stream().map(ZoneChoc::depuisCode).toList()));
        c.champ("reparateur", r.getReparateur(), vide(m.reparateur()), r::setReparateur);
        c.champ("accordReparateur", r.getAccordReparateur(), m.accordReparateur(), r::setAccordReparateur);
        if (m.vetusteTtc() != null && m.vetusteHt() != null) {
            throw new ValidationRefuseeException("La vétusté est soit TTC, soit HT : une seule des deux est saisie.");
        }
        if (r.getNatureRapport() != NatureRapport.REFORME
                && (m.typeReforme() != null || vide(m.epavisteNom()) != null || vide(m.epavisteVille()) != null)) {
            throw new ValidationRefuseeException(
                    "La réforme et l'épaviste retenu ne se saisissent que sur un rapport de réforme.");
        }
        c.champ("vetusteTtc", r.getVetusteTtc(), m.vetusteTtc(), r::setVetusteTtc);
        c.champ("vetusteHt", r.getVetusteHt(), m.vetusteHt(), r::setVetusteHt);
        c.champ("typeReforme", r.getTypeReforme(), m.typeReforme(), r::setTypeReforme);
        c.champ("epavisteNom", r.getEpavisteNom(), vide(m.epavisteNom()), r::setEpavisteNom);
        c.champ("epavisteVille", r.getEpavisteVille(), vide(m.epavisteVille()), r::setEpavisteVille);
        c.champ("depannage", r.getDepannage(), m.depannage(), r::setDepannage);
        c.champ("valeurNeuf", r.getValeurNeuf(), m.valeurNeuf(), r::setValeurNeuf);
        c.champ("valeurAssuree", r.getValeurAssuree(), m.valeurAssuree(), r::setValeurAssuree);
        c.champ("valeurVenale", r.getValeurVenale(), m.valeurVenale(), r::setValeurVenale);
        c.champ("valeurEpave", r.getValeurEpave(), m.valeurEpave(), r::setValeurEpave);
        c.champ("tvaRecuperable", r.getTvaRecuperable(), m.tvaRecuperable(), r::setTvaRecuperable);
        c.champ("montantIndemnisation", r.getMontantIndemnisation(), m.montantIndemnisation(), r::setMontantIndemnisation);
        c.champ("montantLettres", r.getMontantLettres(), vide(m.montantLettres()), r::setMontantLettres);
        c.champ("commentaire", r.getCommentaire(), vide(m.commentaire()), commentaire -> {
            r.setCommentaire(commentaire);
            ConversionRapportExtrait.lireValeursCommentaire(r);
        });
        modifierPartie(r, TypePartie.ASSURE, m.assure(), c);
        modifierPartie(r, TypePartie.ADVERSAIRE, m.adversaire(), c);
        modifierVehicule(r, m.vehicule(), c);
        modifierPrixLignes(r, m.prixLignes(), c);

        if (!c.liste.isEmpty()) {
            for (String[] changement : c.liste) {
                journal.save(new JournalAudit(ActionAudit.MODIFICATION, auteur, maintenant)
                        .surEntite(RapportExpertise.class.getSimpleName(), r.getId(), r.getId())
                        .avecChangement(changement[0], changement[1], changement[2]));
            }
            recalculer(r, maintenant);
        }
        rapports.saveAndFlush(r);
        return detail(r);
    }

    /** Validation par un gestionnaire (P3) : seulement si tous les contrôles passent et qu'aucun champ ne manque. */
    @Transactional
    public DossierDetail valider(String numero, Utilisateur gestionnaire) {
        RapportExpertise r = rapports.findByNumeroRapport(numero).orElseThrow(DossierIntrouvableException::new);
        if (r.getStatut() != StatutDossier.PRET_A_VALIDER) {
            throw new ValidationRefuseeException("Le dossier n'est pas prêt à valider : "
                    + "des contrôles échouent ou des champs restent à compléter.");
        }
        OffsetDateTime maintenant = OffsetDateTime.now(horloge);
        r.valider(gestionnaire, maintenant);
        journal.save(new JournalAudit(ActionAudit.VALIDATION, gestionnaire, maintenant)
                .surEntite(RapportExpertise.class.getSimpleName(), r.getId(), r.getId())
                .avecChangement("statut", StatutDossier.PRET_A_VALIDER.name(), StatutDossier.VALIDE.name()));
        rapports.saveAndFlush(r);
        return detail(r);
    }

    /**
     * Archivage d'un dossier validé (P6) par un gestionnaire ou un administrateur. Le dossier reste consultable et
     * compté dans les statistiques ; il ne se modifie plus.
     */
    @Transactional
    public DossierDetail archiver(String numero, Utilisateur gestionnaire) {
        RapportExpertise r = rapports.findByNumeroRapport(numero).orElseThrow(DossierIntrouvableException::new);
        StatutDossier avant = r.getStatut();
        r.archiver(gestionnaire);
        OffsetDateTime maintenant = OffsetDateTime.now(horloge);
        journal.save(new JournalAudit(ActionAudit.ARCHIVAGE, gestionnaire, maintenant)
                .surEntite(RapportExpertise.class.getSimpleName(), r.getId(), r.getId())
                .avecChangement("statut", avant.name(), StatutDossier.ARCHIVE.name()));
        rapports.saveAndFlush(r);
        return detail(r);
    }

    /**
     * Annulation d'un import erroné (P6) par un administrateur, motif obligatoire. Rien n'est supprimé : le PDF
     * original (P1) et l'historique (P7) restent. Le dossier sort des listes et des statistiques, et le bon PDF
     * portant le même n° peut être réimporté.
     */
    @Transactional
    public DossierDetail annuler(String numero, String motif, Utilisateur administrateur) {
        RapportExpertise r = rapports.findByNumeroRapport(numero).orElseThrow(DossierIntrouvableException::new);
        StatutDossier avant = r.getStatut();
        OffsetDateTime maintenant = OffsetDateTime.now(horloge);
        r.annuler(administrateur, motif, maintenant);
        journal.save(new JournalAudit(ActionAudit.ANNULATION, administrateur, maintenant)
                .surEntite(RapportExpertise.class.getSimpleName(), r.getId(), r.getId())
                .avecChangement("statut", avant.name(), StatutDossier.ANNULE.name())
                .avecDetails(Map.of("motif", r.getMotifAnnulation())));
        rapports.saveAndFlush(r);
        return detail(r);
    }

    /**
     * Validation en lot par un gestionnaire (P3) : les dossiers cochés, ou tous les dossiers prêts à valider qui
     * répondent aux filtres. Chaque dossier passe par la même règle et la même trace (P7) qu'une validation unitaire ;
     * ceux qui ne sont pas prêts sont ignorés avec leur motif, sans bloquer les autres.
     *
     * <p>Traitement par paquets de 500 dossiers ({@code lt.dossiers.taille-paquet-validation}), chacun dans sa propre
     * transaction : la mémoire et les verrous restent bornés sur des dizaines de milliers de dossiers. Un paquet enregistré le reste si un
     * paquet suivant échoue ; relancer la même demande reprend alors les dossiers encore prêts à valider.
     */
    public ResultatValidationGroupee validerEnLot(ValidationGroupee demande, Utilisateur gestionnaire) {
        OffsetDateTime maintenant = OffsetDateTime.now(horloge);
        Specification<RapportExpertise> tous = !demande.tousLesPrets() ? null
                : new CriteresRecherche(demande.q(), StatutDossier.PRET_A_VALIDER, demande.compagnie(), demande.zone(),
                demande.du(), demande.au(), demande.montantMin(), demande.montantMax(), null).specification();
        ResultatLot resultat = traiterEnLot(demande.numeros(), tous, r -> {
            StatutDossier avant = r.getStatut();
            if (avant != StatutDossier.PRET_A_VALIDER) {
                return motifNonPret(avant);
            }
            try {
                r.valider(gestionnaire, maintenant);
            } catch (ValidationRefuseeException e) {
                return e.getMessage();
            }
            journal.save(new JournalAudit(ActionAudit.VALIDATION, gestionnaire, maintenant)
                    .surEntite(RapportExpertise.class.getSimpleName(), r.getId(), r.getId())
                    .avecChangement("statut", avant.name(), StatutDossier.VALIDE.name()));
            return null;
        });
        return new ResultatValidationGroupee(resultat.traites(), resultat.ignores(), resultat.paquets());
    }

    /**
     * Archivage en lot (P6) par un gestionnaire ou un administrateur : les dossiers cochés, ou tous les dossiers validés
     * qui répondent aux filtres. Même règle et même trace qu'un archivage unitaire ; les dossiers non validés sont
     * ignorés avec leur motif. Mêmes paquets transactionnels que la validation en lot.
     */
    public ResultatArchivageGroupe archiverEnLot(ArchivageGroupe demande, Utilisateur gestionnaire) {
        OffsetDateTime maintenant = OffsetDateTime.now(horloge);
        Specification<RapportExpertise> tous = !demande.tousLesValides() ? null
                : new CriteresRecherche(demande.q(), StatutDossier.VALIDE, demande.compagnie(), demande.zone(),
                demande.du(), demande.au(), demande.montantMin(), demande.montantMax(), null).specification();
        ResultatLot resultat = traiterEnLot(demande.numeros(), tous, r -> {
            StatutDossier avant = r.getStatut();
            if (avant != StatutDossier.VALIDE) {
                return motifNonArchivable(avant);
            }
            try {
                r.archiver(gestionnaire);
            } catch (ValidationRefuseeException e) {
                return e.getMessage();
            }
            journal.save(new JournalAudit(ActionAudit.ARCHIVAGE, gestionnaire, maintenant)
                    .surEntite(RapportExpertise.class.getSimpleName(), r.getId(), r.getId())
                    .avecChangement("statut", avant.name(), StatutDossier.ARCHIVE.name()));
            return null;
        });
        return new ResultatArchivageGroupe(resultat.traites(), resultat.ignores(), resultat.paquets());
    }

    /** Action d'un traitement en lot sur un dossier : {@code null} si elle est appliquée, sinon le motif d'ignorance. */
    @FunctionalInterface
    private interface ActionDeLot {
        String appliquer(RapportExpertise rapport);
    }

    private record ResultatLot(int traites, List<DossierIgnore> ignores, int paquets) {
    }

    /**
     * Applique une action aux dossiers cochés ({@code numeros}) ou, si {@code tous} est donné, à tous ceux qui répondent
     * à ces filtres. Traitement par paquets de 500 dossiers ({@code lt.dossiers.taille-paquet-validation}), chacun dans
     * sa propre transaction : la mémoire et les verrous restent bornés sur des dizaines de milliers de dossiers. Un
     * paquet enregistré le reste si un paquet suivant échoue ; relancer la même demande reprend les dossiers restants.
     */
    private ResultatLot traiterEnLot(List<String> numerosDemandes, Specification<RapportExpertise> tous, ActionDeLot action) {
        List<DossierIgnore> ignores = new ArrayList<>();
        int traites = 0;
        int paquets = 0;
        if (tous != null) {
            // Parcours par identifiant croissant : un dossier ignoré (resté dans le filtre) n'est pas relu ensuite
            long dernierId = 0;
            Paquet p;
            do {
                long apres = dernierId;
                p = transaction.execute(etat -> traiterPaquet(rapports.findAll(
                        tous.and((racine, requete, cb) -> cb.greaterThan(racine.get("id"), apres)),
                        PageRequest.of(0, taillePaquet, Sort.by("id"))).getContent(), action));
                traites += p.traites();
                ignores.addAll(p.ignores());
                dernierId = p.dernierId();
                paquets += p.taille() > 0 ? 1 : 0;
            } while (p.taille() == taillePaquet);
        } else {
            List<String> numeros = numerosDemandes == null ? List.of()
                    : numerosDemandes.stream().filter(Objects::nonNull).distinct().toList();
            for (int debut = 0; debut < numeros.size(); debut += taillePaquet) {
                List<String> tranche = numeros.subList(debut, Math.min(debut + taillePaquet, numeros.size()));
                Paquet p = transaction.execute(etat -> {
                    List<RapportExpertise> trouves = sansDoublonAnnule(rapports.findByNumeroRapportIn(tranche));
                    Paquet resultat = traiterPaquet(trouves, action);
                    tranche.stream().filter(n -> trouves.stream().noneMatch(r -> n.equals(r.getNumeroRapport())))
                            .forEach(n -> resultat.ignores().add(new DossierIgnore(n, "Dossier introuvable.")));
                    return resultat;
                });
                traites += p.traites();
                ignores.addAll(p.ignores());
                paquets++;
            }
        }
        return new ResultatLot(traites, ignores, paquets);
    }

    /** Résultat d'un paquet ; {@code dernierId} : plus grand identifiant lu, pour reprendre au paquet suivant. */
    private record Paquet(int traites, List<DossierIgnore> ignores, long dernierId, int taille) {
    }

    private Paquet traiterPaquet(List<RapportExpertise> lot, ActionDeLot action) {
        List<DossierIgnore> ignores = new ArrayList<>();
        int traites = 0;
        long dernierId = 0;
        for (RapportExpertise r : lot) {
            dernierId = Math.max(dernierId, r.getId());
            String motif = action.appliquer(r);
            if (motif == null) {
                traites++;
            } else {
                ignores.add(new DossierIgnore(r.getNumeroRapport(), motif));
            }
        }
        rapports.flush();
        return new Paquet(traites, ignores, dernierId, lot.size());
    }

    private static String motifNonArchivable(StatutDossier statut) {
        return switch (statut) {
            case ARCHIVE -> "Déjà archivé.";
            case ANNULE -> "Import annulé.";
            default -> "Pas encore validé : seul un dossier validé s'archive.";
        };
    }

    /** Un n° porté par un dossier actif et par un import annulé : seul le dossier actif est retenu. */
    private static List<RapportExpertise> sansDoublonAnnule(List<RapportExpertise> trouves) {
        return trouves.stream().filter(r -> r.getStatut() != StatutDossier.ANNULE || trouves.stream().noneMatch(
                a -> a != r && a.getStatut() != StatutDossier.ANNULE && a.getNumeroRapport().equals(r.getNumeroRapport())))
                .toList();
    }

    private static String motifNonPret(StatutDossier statut) {
        return switch (statut) {
            case VALIDE, ARCHIVE -> "Déjà validé.";
            case ANNULE -> "Import annulé.";
            case A_VERIFIER -> "Un contrôle bloquant échoue : à corriger à l'écran de vérification.";
            case A_COMPLETER -> "Une donnée essentielle manque (montant, procédure ou TVA).";
            default -> "Le dossier n'est pas prêt à valider (statut " + statut + ").";
        };
    }

    @Transactional(readOnly = true)
    public List<EvenementHistorique> historique(String numero, Long id) {
        RapportExpertise r = rapports.findByNumeroRapport(numero, id).orElseThrow(DossierIntrouvableException::new);
        return journal.findByRapportIdOrderByHorodatageAscIdAsc(r.getId()).stream()
                .map(j -> new EvenementHistorique(j.getHorodatage(), j.getAction().name(), j.getIdentifiantUtilisateur(),
                        j.getChamp(), j.getAncienneValeur(), j.getNouvelleValeur(), j.getDetails()))
                .toList();
    }

    private void recalculer(RapportExpertise r, OffsetDateTime maintenant) {
        RapportExtrait extrait = RapportExtrait.depuis(r);
        List<Controle> resultats = controles.controler(extrait);
        r.viderResultatsControle();
        ConversionRapportExtrait.ajouterResultats(r, resultats, extrait.getChampsASaisir(), maintenant);
        r.changerStatut(controles.statutApresImport(resultats, extrait.getChampsASaisir()));
    }

    private static void modifierPartie(RapportExpertise r, TypePartie type, PartieDto m, Changements c) {
        String prefixe = type == TypePartie.ASSURE ? "assure." : "adversaire.";
        Optional<Partie> existante = r.getPartie(type);
        if (m == null || estVide(m)) {
            if (existante.isPresent() && type == TypePartie.ADVERSAIRE) {
                c.liste.add(new String[]{"adversaire", "présent", null});
                r.retirerPartie(type);
            }
            if (type == TypePartie.ADVERSAIRE || m == null) {
                return;
            }
        }
        Partie p = existante.orElseGet(() -> {
            Partie nouvelle = new Partie(type);
            r.ajouterPartie(nouvelle);
            return nouvelle;
        });
        c.champ(prefixe + "nomPrenom", p.getNomPrenom(), vide(m.nomPrenom()), p::setNomPrenom);
        c.champ(prefixe + "vehicule", p.getVehiculeLibelle(), vide(m.vehicule()), p::setVehiculeLibelle);
        c.champ(prefixe + "immatriculation", p.getImmatriculation(), vide(m.immatriculation()), p::setImmatriculation);
        c.champ(prefixe + "compagnie", p.getCompagnie(), vide(m.compagnie()), p::setCompagnie);
        c.champ(prefixe + "numeroPolice", p.getNumeroPolice(), vide(m.numeroPolice()), p::setNumeroPolice);
        c.champ(prefixe + "intermediaire", p.getIntermediaire(), vide(m.intermediaire()), p::setIntermediaire);
    }

    /**
     * Correction du prix unitaire des lignes de la page 2 (prix faux imprimé par le logiciel de l'expert). Le PDF
     * d'origine reste intact (P1) ; l'ancien et le nouveau prix sont tracés (P7).
     */
    private static void modifierPrixLignes(RapportExpertise r, List<PrixLigne> prix, Changements c) {
        if (prix == null) {
            return;
        }
        for (PrixLigne p : prix) {
            r.getLignesDetail().stream().filter(l -> l.getOrdre() == p.ordre()).findFirst().ifPresent(l ->
                    c.champ("ligne " + (l.getOrdre() + 1) + " (" + l.getDesignation() + ") : prix unitaire",
                            l.getPrixUnitaire(), p.prixUnitaire(), l::setPrixUnitaire));
        }
    }

    private static void modifierVehicule(RapportExpertise r, VehiculeDto m, Changements c) {
        if (m == null) {
            return;
        }
        Vehicule v = r.getVehicule();
        if (v == null) {
            v = new Vehicule();
            r.definirVehicule(v);
        }
        c.champ("vehicule.marque", v.getMarque(), vide(m.marque()), v::setMarque);
        c.champ("vehicule.modele", v.getModele(), vide(m.modele()), v::setModele);
        c.champ("vehicule.typeMine", v.getTypeMine(), vide(m.typeMine()), v::setTypeMine);
        c.champ("vehicule.immatriculation", v.getImmatriculation(), vide(m.immatriculation()), v::setImmatriculation);
        c.champ("vehicule.numeroSerie", v.getNumeroSerie(), vide(m.numeroSerie()), v::setNumeroSerie);
        c.champ("vehicule.energie", v.getEnergie(), m.energie(), v::setEnergie);
        c.champ("vehicule.kilometrage", v.getKilometrage(), m.kilometrage(), v::setKilometrage);
        c.champ("vehicule.kilometrageNonEstimable", v.isKilometrageNonEstimable(), m.kilometrageNonEstimable(),
                v::setKilometrageNonEstimable);
        c.champ("vehicule.puissanceFiscale", v.getPuissanceFiscale(), m.puissanceFiscale(), v::setPuissanceFiscale);
        c.champ("vehicule.dateMiseCirculation", v.getDateMiseCirculation(), m.dateMiseCirculation(), v::setDateMiseCirculation);
        c.champ("vehicule.etatGeneral", v.getEtatGeneral(), vide(m.etatGeneral()), v::setEtatGeneral);
        c.champ("vehicule.usurePneuAvg", v.getUsurePneuAvg(), m.usurePneuAvg(), v::setUsurePneuAvg);
        c.champ("vehicule.usurePneuAvd", v.getUsurePneuAvd(), m.usurePneuAvd(), v::setUsurePneuAvd);
        c.champ("vehicule.usurePneuArg", v.getUsurePneuArg(), m.usurePneuArg(), v::setUsurePneuArg);
        c.champ("vehicule.usurePneuArd", v.getUsurePneuArd(), m.usurePneuArd(), v::setUsurePneuArd);
    }

    /** Applique et note les seuls champs dont la valeur change (BigDecimal comparés en valeur, 12.0 = 12.00). */
    private static final class Changements {

        private final List<String[]> liste = new ArrayList<>();

        <T> void champ(String nom, T ancien, T nouveau, Consumer<T> appliquer) {
            if (egaux(ancien, nouveau)) {
                return;
            }
            appliquer.accept(nouveau);
            liste.add(new String[]{nom, texte(ancien), texte(nouveau)});
        }

        private static boolean egaux(Object a, Object b) {
            if (a instanceof java.math.BigDecimal x && b instanceof java.math.BigDecimal y) {
                return x.compareTo(y) == 0;
            }
            return Objects.equals(a, b);
        }

        private static String texte(Object valeur) {
            return valeur == null ? null : valeur.toString();
        }
    }

    // ------------------------------------------------------------------ correspondances

    static DossierResume resume(RapportExpertise r) {
        Optional<Partie> assure = r.getPartie(TypePartie.ASSURE);
        String immatriculation = r.getVehicule() != null && r.getVehicule().getImmatriculation() != null
                ? r.getVehicule().getImmatriculation() : assure.map(Partie::getImmatriculation).orElse(null);
        List<String> anomalies = r.getResultatsControle().stream().filter(c -> !c.isConforme() && c.isBloquant())
                .map(ResultatControle::getLibelle).toList();
        return new DossierResume(r.getId(), r.getNumeroRapport(), r.getStatut(), r.getNatureRapport(),
                r.getTypeProcedure(), r.getDateSinistre(), r.getDateRapport(),
                assure.map(Partie::getNomPrenom).orElse(null), immatriculation, r.getCompagnie(), codes(r.getZonesChoc()),
                r.getMontantIndemnisation(), r.getCreeLe(), anomalies.size(), anomalies);
    }

    static DossierDetail detail(RapportExpertise r) {
        Vehicule v = r.getVehicule();
        return new DossierDetail(r.getId(), r.getNumeroRapport(), r.getStatut(), r.getNatureRapport(),
                r.getTypeProcedure(), r.getCabinet(), r.getExpert(), r.getCompagnie(), r.getRefSinistre(),
                r.getDateSinistre(), r.getDateMission(), r.getDateExpertise(), r.getDateAvantReparation(),
                r.getDateEnCoursTravaux(), r.getDateApresTravaux(), r.getDateRapport(), r.getLieuRapport(),
                r.getNatureContrat(), r.getNatureEvenement(), codes(r.getZonesChoc()), r.getReparateur(),
                r.getAccordReparateur(),
                r.getPartie(TypePartie.ASSURE).map(ServiceDossier::partie).orElse(null),
                r.getPartie(TypePartie.ADVERSAIRE).map(ServiceDossier::partie).orElse(null),
                v == null ? null : new VehiculeDto(v.getMarque(), v.getModele(), v.getTypeMine(), v.getImmatriculation(),
                        v.getNumeroSerie(), v.getEnergie(), v.getKilometrage(), v.isKilometrageNonEstimable(),
                        v.getPuissanceFiscale(), v.getDateMiseCirculation(), v.getEtatGeneral(), v.getUsurePneuAvg(),
                        v.getUsurePneuAvd(), v.getUsurePneuArg(), v.getUsurePneuArd()),
                r.getEvaluationsMainOeuvre().stream().map(e -> new LigneMainOeuvreDto(e.getPoste(), e.getNombreHeures(),
                        e.getTauxHoraire(), e.getMontants())).toList(),
                r.getFournitures().stream().map(f -> new FournitureDto(f.getCategorie(), f.getMontants())).toList(),
                r.getTotalMainOeuvre(), r.getTotalFournitures(), r.getTotalGeneral(), r.getVetusteTtc(), r.getVetusteHt(),
                r.getDepannage(), r.getImmobilisation(), r.getTypeReforme(), r.getEpavisteNom(), r.getEpavisteVille(),
                r.getValeurNeuf(), r.getValeurAssuree(), r.getValeurVenale(), r.getValeurEpave(), r.getTvaRecuperable(),
                r.getMontantIndemnisation(), r.getMontantLettres(), r.getCommentaire(),
                r.getValeursCommentaire().stream().sorted(Comparator.comparing(ValeurCommentaire::getCle))
                        .map(vc -> new ValeurCommentaireDto(vc.getCle(), vc.getValeurTexte(), vc.getValeurMontant(),
                                vc.getValeurDate())).toList(),
                r.getLignesDetail().stream().map(l -> new LigneDetailDto(l.getOrdre(), l.getZoneLibelle(),
                        code(l.getZoneChoc()), l.getFamille(), l.getQuantite(), l.getDesignation(), l.getTypePiece(),
                        l.getPrixUnitaire(), l.getVetustePct(), l.getTvaPct(), l.getTotalFourniture(), l.getOperation(),
                        l.getNombreHeures(), l.getTarifHoraire(), l.getMontantMoTtc(),
                        ControleCoherenceService.prixCoherent(l.getQuantite(), l.getPrixUnitaire(), l.getTvaPct(),
                                l.getVetustePct(), l.getTotalFourniture()),
                        ControleCoherenceService.prixDepuisTotal(l.getQuantite(), l.getTvaPct(), l.getVetustePct(),
                                l.getTotalFourniture()))).toList(),
                r.getResultatsControle().stream().map(c -> new ControleDto(c.getType(), c.getCode(), c.getLibelle(),
                        c.getChamp(), c.getValeurAttendue(), c.getValeurObtenue(), c.isConforme(), c.isBloquant())).toList(),
                r.getResultatsControle().stream().filter(c -> c.getType() == TypeControle.CHAMP_MANQUANT)
                        .map(ResultatControle::getChamp).toList(),
                r.getImportFichier() == null ? null : r.getImportFichier().getId(),
                r.getImportFichier() == null ? null : r.getImportFichier().getNomFichier(),
                r.getValidePar() == null ? null : r.getValidePar().getIdentifiant(), r.getValideLe(),
                r.getAnnulePar() == null ? null : r.getAnnulePar().getIdentifiant(), r.getAnnuleLe(),
                r.getMotifAnnulation(), r.getCreeLe(), r.getVersion());
    }

    private static PartieDto partie(Partie p) {
        return new PartieDto(p.getNomPrenom(), p.getVehiculeLibelle(), p.getImmatriculation(), p.getCompagnie(),
                p.getNumeroPolice(), p.getIntermediaire());
    }

    private static boolean estVide(PartieDto p) {
        return vide(p.nomPrenom()) == null && vide(p.vehicule()) == null && vide(p.immatriculation()) == null
                && vide(p.compagnie()) == null && vide(p.numeroPolice()) == null && vide(p.intermediaire()) == null;
    }

    private static Integer code(ZoneChoc zone) {
        return zone == null ? null : (int) zone.getCode();
    }

    private static List<Integer> codes(List<ZoneChoc> zones) {
        return zones.stream().map(ServiceDossier::code).toList();
    }

    /** Une chaîne vide saisie à l'écran vaut « non renseigné ». */
    private static String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.strip();
    }
}
