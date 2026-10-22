package ma.ltexpertise.dossier.referentiel;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stocke une {@link ZoneChoc} par son code 1 à 8, comme dans le PDF. */
@Converter(autoApply = true)
public class ZoneChocConverter implements AttributeConverter<ZoneChoc, Short> {

    @Override
    public Short convertToDatabaseColumn(ZoneChoc zone) {
        return zone == null ? null : zone.getCode();
    }

    @Override
    public ZoneChoc convertToEntityAttribute(Short code) {
        return code == null ? null : ZoneChoc.depuisCode(code);
    }
}
