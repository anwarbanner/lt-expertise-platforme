package ma.ltexpertise.extraction;

import ma.ltexpertise.commun.Montants;
import ma.ltexpertise.dossier.referentiel.PosteMainOeuvre;

import java.math.BigDecimal;

/** Une des 7 lignes de main d'œuvre du tableau « Évaluation des dommages ». */
public record LigneMainOeuvreExtraite(PosteMainOeuvre poste, BigDecimal nombreHeures, BigDecimal tauxHoraire,
                                      Montants montants) {
}
