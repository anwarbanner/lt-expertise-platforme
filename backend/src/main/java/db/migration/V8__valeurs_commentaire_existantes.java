package db.migration;

import ma.ltexpertise.extraction.ExtracteurCommentaire;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * V8 : valeurs clés du commentaire (F12) des dossiers déjà importés. La lecture du commentaire est arrivée avec
 * V7 ; elle est appliquée ici au texte déjà enregistré, avec le même extracteur qu'à l'import (un nouvel import du
 * même PDF serait refusé comme doublon).
 */
public class V8__valeurs_commentaire_existantes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement lecture = context.getConnection().createStatement();
             ResultSet rapports = lecture.executeQuery(
                     "select id, commentaire from rapport_expertise where commentaire is not null");
             PreparedStatement insertion = context.getConnection().prepareStatement(
                     "insert into valeur_commentaire (rapport_id, cle, valeur_texte, valeur_montant) values (?, ?, ?, ?) "
                             + "on conflict (rapport_id, cle) do nothing")) {
            while (rapports.next()) {
                for (ExtracteurCommentaire.ValeurCle v : ExtracteurCommentaire.extraire(rapports.getString("commentaire"))) {
                    insertion.setLong(1, rapports.getLong("id"));
                    insertion.setString(2, v.cle().name());
                    insertion.setString(3, v.texte());
                    insertion.setBigDecimal(4, v.montant());
                    insertion.addBatch();
                }
            }
            insertion.executeBatch();
        }
    }
}
