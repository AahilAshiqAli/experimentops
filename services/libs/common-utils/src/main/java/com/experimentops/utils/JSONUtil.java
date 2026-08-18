package com.experimentops.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Utility class that handles conversion related to JSON objects
 */
public final class JSONUtil {
    private static final Logger logger = LoggerFactory.getLogger(JSONUtil.class);
    private static final ObjectMapper typedMapper = new ObjectMapper();
    private static final ObjectMapper nonTypedMapper = new ObjectMapper();

    private static final String CONVERSION_FAILED = "Conversion of object to JSON failed: {}";

    static {
        // since this is a helper class, and our general requirement is to exclude
        // null fields from json string (both to be iOS compatible and save bandwidth),
        // anybody that wants to include nulls should use their own mapper
        typedMapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_NULL));
        typedMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        typedMapper.registerModule(new JsonNullableModule());
        nonTypedMapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_NULL));
        nonTypedMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        nonTypedMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        nonTypedMapper.registerModule(new JsonNullableModule());

        // The only difference between typedMapper and nonTypedMapper is that non typed mapper accepts a single element as array/
    }

    // private constructor to prevent initialization
    private JSONUtil() {
    }


    /**
     * Converts a JSON string to a POJO of the specified class.
     *
     * @param <T>   the type of the deserialized object
     * @param json  the JSON string to deserialize
     * @param clazz the class of the deserialized object
     * @return a POJO (or null if it could not be deserialized)
     */
    public static <T> T toObjectFromTypedJson(String json, Class<T> clazz) {
        T obj = null;
        try {
            obj = typedMapper.readValue(json, clazz);
        } catch (IOException e) {
            logger.warn(CONVERSION_FAILED, e.getMessage());
        }
        return obj;
    }

    /**
     * Converts a JSON string to a POJO list of the specified class.
     *
     * @param <T>   the type of the deserialized object
     * @param json  the JSON string to deserialize
     * @param clazz the class of the deserialized object
     * @return a POJO (or null if it could not be deserialized)
     */
    public static <T> List<T> toListFromTypedJson(String json, Class<T> clazz) {
        try {
            return nonTypedMapper.readValue(json, nonTypedMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        }
        catch (JsonProcessingException e) {
            logger.warn(CONVERSION_FAILED, e.getMessage());
        }
        return Collections.emptyList();
    }


    /**
     * Converts a POJO to a barebones JSON string.
     *
     * @param obj the POJO to serialize to JSON
     * @return the JSON text string, or an empty string if conversion failed
     */
    public static String toNonTypedJsonFromObject(Object obj) {
        String json = StringUtils.EMPTY;
        if (obj == null) {
            return json;
        }
        try {
            nonTypedMapper.addMixIn(obj.getClass(), IgnoreSchemaProperty.class);
            typedMapper.addMixIn(org.apache.avro.specific.SpecificRecord.class, JacksonIgnoreAvroPropertiesMixIn.class);
            json = nonTypedMapper.writeValueAsString(obj);
        } catch (IOException e) {
            logger.warn(CONVERSION_FAILED, e.getMessage());
        }
        return json;
    }

    abstract static class JacksonIgnoreAvroPropertiesMixIn {
        @JsonIgnore
        public abstract org.apache.avro.Schema getSchema();

        @JsonIgnore
        public abstract org.apache.avro.specific.SpecificData getSpecificData();
    }

    /**
     * Converts a HashMap to a POJO.
     *
     * @return POJO
     */
    public static <T, M> T toObjectFromMap(M map, Class<T> clazz) {
        return nonTypedMapper.convertValue(map, clazz);
    }

    public static <K, V> Map<K, V> toMapFromJson(String json) {
        try {
            return nonTypedMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            logger.warn(CONVERSION_FAILED, e.getMessage());
        }
        return Collections.emptyMap();
    }

    public static Map<String, Object> readTree(byte[] content) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("JSON content cannot be null or empty");
        }

        try {
            return nonTypedMapper.readValue(content, new TypeReference<>() {});
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to parse JSON content", exception);
        }
    }

    @SuppressWarnings("unused")
    abstract static class IgnoreSchemaProperty {
        @JsonIgnore
        abstract void getSchema();
    }



}
