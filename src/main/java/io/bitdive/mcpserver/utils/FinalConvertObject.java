package io.bitdive.mcpserver.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.bitdive.mcpserver.dto.hierarchy_method.HierarchyMethodUIDto;
import io.bitdive.mcpserver.dto.hierarchy_method.heap_map.ModuleAnalyticsDTO;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class FinalConvertObject {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    public static String getStringObjectString(Object objectsVal) {
        try {
           return objectMapper.writeValueAsString(objectsVal);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> getStringObject(Object objectsVal) {
        try {
            String inPointJson = objectMapper.writeValueAsString(objectsVal);
            return objectMapper.readValue(inPointJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Map<String, Object>> getListOfMaps(Object objectsVal) {
        try {
            String json = objectMapper.writeValueAsString(objectsVal);
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
