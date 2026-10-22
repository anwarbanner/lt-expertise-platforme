package ma.ltexpertise.commun;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/**
 * Triplet HT / TVA / TTC tel qu'imprimé dans le rapport.
 * Les noms de colonnes sont fixés par {@code @AttributeOverride} à chaque usage.
 */
@Embeddable
public class Montants {

    @Column(precision = 12, scale = 2)
    private BigDecimal ht;

    @Column(precision = 12, scale = 2)
    private BigDecimal tva;

    @Column(precision = 12, scale = 2)
    private BigDecimal ttc;

    protected Montants() {
    }

    public Montants(BigDecimal ht, BigDecimal tva, BigDecimal ttc) {
        this.ht = ht;
        this.tva = tva;
        this.ttc = ttc;
    }

    public BigDecimal getHt() {
        return ht;
    }

    public BigDecimal getTva() {
        return tva;
    }

    public BigDecimal getTtc() {
        return ttc;
    }
}
