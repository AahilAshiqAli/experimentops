package com.experimentops.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hypersistence.utils.hibernate.type.json.JsonType;

public class ExperimentOpsJsonType extends JsonType {
    private static final ObjectMapper nonTypedMapper = new ObjectMapper();
    static {
        nonTypedMapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_NULL));
        nonTypedMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        nonTypedMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    public ExperimentOpsJsonType() {
        super(nonTypedMapper);
    }
}