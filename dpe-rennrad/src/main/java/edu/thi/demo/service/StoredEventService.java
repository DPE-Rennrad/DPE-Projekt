package edu.thi.demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.thi.demo.model.StoredEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class StoredEventService {

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public void persistMockEvents(String providerKey, List<Map<String, Object>> events) {
        for (Map<String, Object> e : events) {
            if (e == null) {
                continue;
            }
            StoredEvent se = new StoredEvent();
            se.providerKey = providerKey;
            se.title = String.valueOf(e.getOrDefault("title", e.getOrDefault("name", "")));
            se.location = String.valueOf(e.getOrDefault("location", ""));

            Object dateObj = e.getOrDefault("date", e.getOrDefault("eventDate", null));
            se.eventDate = toInstant(dateObj);

            Object ppl = e.getOrDefault("passendeLeute", e.getOrDefault("matchingPeople", List.of()));
            se.passendeLeuteJson = toJsonArrayString(ppl);

            se.persist();
        }
    }

    @Transactional
    public List<Map<String, Object>> listAllAsMaps() {
        List<StoredEvent> entities = StoredEvent.listAll();
        List<Map<String, Object>> out = new ArrayList<>(entities.size());
        for (StoredEvent se : entities) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", se.id);
            m.put("provider", se.providerKey);
            m.put("title", se.title);
            m.put("location", se.location);
            m.put("date", se.eventDate != null ? se.eventDate.toString() : "");
            m.put("passendeLeute", fromJsonArrayString(se.passendeLeuteJson));
            out.add(m);
        }
        return out;
    }

    private Instant toInstant(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Instant i) {
            return i;
        }
        if (v instanceof String s) {
            try {
                return Instant.parse(s);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private String toJsonArrayString(Object value) {
        try {
            if (value == null) {
                return "[]";
            }
            if (value instanceof String s) {
                // if it's already JSON, keep it; otherwise wrap as single value
                String trimmed = s.trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    return trimmed;
                }
                return objectMapper.writeValueAsString(List.of(s));
            }
            if (value instanceof List<?>) {
                return objectMapper.writeValueAsString(value);
            }
            // fallback
            return objectMapper.writeValueAsString(List.of(String.valueOf(value)));
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> fromJsonArrayString(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
