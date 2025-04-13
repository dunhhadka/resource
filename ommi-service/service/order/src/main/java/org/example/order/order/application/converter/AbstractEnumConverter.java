package org.example.order.order.application.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Objects;

@Converter
public abstract class AbstractEnumConverter<E extends Enum<E> & CustomEnumValue<V>, V>
        implements AttributeConverter<E, V> {

    private final Class<E> enumType;

    protected AbstractEnumConverter(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public V convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public E convertToEntityAttribute(V dbData) {
        if (dbData == null) return null;
        E[] enums = enumType.getEnumConstants();
        for (E e : enums) {
            if(Objects.equals(e.getValue(), dbData)) {
                return e;
            }
        }
        return null;
    }
}
