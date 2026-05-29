package com.experimentops.utils.model.converter;

import com.experimentops.utils.model.enums.ExperimentOpsEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

@Converter(autoApply = true)
public class ExperimentOpsEnumConverter<E extends ExperimentOpsEnum> implements AttributeConverter<E, Integer> {
    private final Class<E> enumClass;

    public ExperimentOpsEnumConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public Integer convertToDatabaseColumn(E attribute) {
        return (attribute == null) ? null : attribute.getCode();
    }

    @Override
    public E convertToEntityAttribute(Integer code) {
        return (code == null) ? null : Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.getCode() == code)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
