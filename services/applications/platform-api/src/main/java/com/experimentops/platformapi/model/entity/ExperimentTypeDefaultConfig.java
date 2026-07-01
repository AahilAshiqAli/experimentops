package com.experimentops.platformapi.model.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentTypeDefaultConfig {
    private String name;
    private Datatype datatype;
    private Object defaultValue;

    public enum Datatype {
        BOOLEAN("boolean"),
        STRING("string"),
        NUMBER("number"),
        LIST("list");

        private final String value;

        Datatype(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static Datatype fromValue(String value) {
            for (Datatype datatype : Datatype.values()) {
                if (datatype.value.equals(value)) {
                    return datatype;
                }
            }
            throw new IllegalArgumentException("Unexpected datatype value '" + value + "'");
        }
    }
}
