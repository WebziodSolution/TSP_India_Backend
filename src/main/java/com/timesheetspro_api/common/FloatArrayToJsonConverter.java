package com.timesheetspro_api.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class FloatArrayToJsonConverter implements AttributeConverter<float[], String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not convert float[] to JSON", e);
        }
    }

    // @Override
    // public float[] convertToEntityAttribute(String dbData) {
    // if (dbData == null || dbData.isEmpty()) {
    // return null;
    // }
    // try {
    // return objectMapper.readValue(dbData, float[].class);
    // } catch (Exception e) {
    // throw new RuntimeException("Could not convert JSON to float[]", e);
    // }
    // }
    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty() || "null".equalsIgnoreCase(dbData.trim())) {
            return null;
        }
        try {
            dbData = dbData.trim();
            // If double-serialized / stored as a quoted string literal
            if (dbData.startsWith("\"") && dbData.endsWith("\"")) {
                dbData = objectMapper.readValue(dbData, String.class);
            }
            return objectMapper.readValue(dbData, float[].class);
        } catch (Exception e) {
            throw new RuntimeException("Could not convert JSON to float[]: " + dbData, e);
        }
    }

}
