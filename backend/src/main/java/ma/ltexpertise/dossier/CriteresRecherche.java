package ma.ltexpertise.dossier;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.dossier.referentiel.ZoneChoc;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Recherche des dossiers (F7) : texte libre sur n° de rapport, réf. sinistre, réparateur, compagnie, nom et
 * immatriculation (parties et véhicule) ; filtres par statut, compagnie, zone de choc, période (date du rapport),
 * tranche de montant et anomalies.
 */
public record CriteresRecherche(String q, StatutDossier statut, String compagnie, Integer zone, LocalDate du,
                                LocalDate au, BigDecimal montantMin, BigDecimal montantMax, Boolean anomalies) {

    Specification<RapportExpertise> specification() {
        return (racine, requete, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String motif = "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
                Subquery<Long> partie = requete.subquery(Long.class);
                var p = partie.from(Partie.class);
                partie.select(p.get("id")).where(cb.equal(p.get("rapport"), racine), cb.or(
                        cb.like(cb.lower(p.get("nomPrenom")), motif), cb.like(cb.lower(p.get("immatriculation")), motif)));
                Subquery<Long> vehicule = requete.subquery(Long.class);
                var v = vehicule.from(Vehicule.class);
                vehicule.select(v.get("id")).where(cb.equal(v.get("rapport"), racine),
                        cb.like(cb.lower(v.get("immatriculation")), motif));
                conditions.add(cb.or(
                        cb.like(cb.lower(racine.get("numeroRapport")), motif),
                        cb.like(cb.lower(racine.get("refSinistre")), motif),
                        cb.like(cb.lower(racine.get("reparateur")), motif),
                        cb.like(cb.lower(racine.get("compagnie")), motif),
                        cb.exists(partie), cb.exists(vehicule)));
            }
            if (statut != null) {
                conditions.add(cb.equal(racine.get("statut"), statut));
            } else {
                // Un import annulé n'apparaît que dans l'onglet « Annulés »
                conditions.add(cb.notEqual(racine.get("statut"), StatutDossier.ANNULE));
            }
            if (compagnie != null && !compagnie.isBlank()) {
                conditions.add(cb.equal(cb.lower(racine.get("compagnie")), compagnie.strip().toLowerCase(Locale.ROOT)));
            }
            if (zone != null) {
                // Un dossier à plusieurs zones répond à chacune d'elles
                conditions.add(cb.isMember(ZoneChoc.depuisCode(zone), racine.<java.util.Collection<ZoneChoc>>get("zonesChoc")));
            }
            if (du != null) {
                conditions.add(cb.greaterThanOrEqualTo(racine.get("dateRapport"), du));
            }
            if (au != null) {
                conditions.add(cb.lessThanOrEqualTo(racine.get("dateRapport"), au));
            }
            if (montantMin != null) {
                conditions.add(cb.greaterThanOrEqualTo(racine.get("montantIndemnisation"), montantMin));
            }
            if (montantMax != null) {
                conditions.add(cb.lessThanOrEqualTo(racine.get("montantIndemnisation"), montantMax));
            }
            if (Boolean.TRUE.equals(anomalies)) {
                Subquery<Long> controle = requete.subquery(Long.class);
                var c = controle.from(ResultatControle.class);
                controle.select(c.get("id")).where(cb.equal(c.get("rapport"), racine), cb.isFalse(c.get("conforme")),
                        cb.isTrue(c.get("bloquant")));
                conditions.add(cb.exists(controle));
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }
}
