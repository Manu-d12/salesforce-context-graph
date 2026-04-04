package org.autorabit.salesforcecontextgraph.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.autorabit.salesforcecontextgraph.domain.model.GraphResult;
import org.springframework.stereotype.Service;

@Service
public class ExportAgent {

    private final ObjectMapper objectMapper;

    public ExportAgent(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(GraphResult graphResult) {
        try {
            return objectMapper.writeValueAsString(graphResult);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize analysis result", ex);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize analysis result", ex);
        }
    }
}
