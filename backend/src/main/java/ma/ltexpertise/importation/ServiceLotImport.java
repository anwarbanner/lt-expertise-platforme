package ma.ltexpertise.importation;

import ma.ltexpertise.utilisateur.Utilisateur;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Suivi des lots d'import ; chaque écriture est une transaction courte, lisible pendant le traitement. */
@Service
public class ServiceLotImport {

    private final LotImportRepository lots;
    private final Clock horloge;

    public ServiceLotImport(LotImportRepository lots, Clock horloge) {
        this.lots = lots;
        this.horloge = horloge;
    }

    @Transactional
    public UUID ouvrir(int nombreFichiers, Utilisateur auteur) {
        return lots.save(new LotImport(nombreFichiers, auteur, OffsetDateTime.now(horloge))).getId();
    }

    @Transactional
    public void enregistrer(UUID lot, int ordre, ResultatImport resultat) {
        lots.findById(lot).orElseThrow().ajouterResultat(new ResultatLotImport(ordre, resultat, OffsetDateTime.now(horloge)));
    }

    @Transactional
    public void terminer(UUID lot) {
        lots.findById(lot).orElseThrow().terminer(OffsetDateTime.now(horloge));
    }

    @Transactional(readOnly = true)
    public Optional<RecapitulatifLot> recapitulatif(UUID lot) {
        return lots.findById(lot).map(RecapitulatifLot::de);
    }
}
