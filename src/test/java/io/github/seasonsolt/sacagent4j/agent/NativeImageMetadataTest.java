package io.github.seasonsolt.sacagent4j.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class NativeImageMetadataTest {
    private static final String REFLECT_CONFIG = "/META-INF/native-image/io.github.seasonsolt/sac-agent4j/reflect-config.json";

    @Test
    void reflectionMetadataCoversJacksonRecords() throws Exception {
        Set<String> configuredTypes = configuredReflectionTypes();

        for (Class<? extends Action> actionType : ActionCatalog.runtimeTypesByClass().keySet()) {
            assertTrue(configuredTypes.contains(actionType.getName()), "missing native reflection metadata for " + actionType.getName());
        }
        assertTrue(configuredTypes.contains(Observation.class.getName()), "missing native reflection metadata for Observation");
        assertTrue(configuredTypes.contains(Turn.class.getName()), "missing native reflection metadata for Turn");
    }

    private static Set<String> configuredReflectionTypes() throws Exception {
        try (InputStream input = NativeImageMetadataTest.class.getResourceAsStream(REFLECT_CONFIG)) {
            assertTrue(input != null, "missing native reflect config resource: " + REFLECT_CONFIG);
            JsonNode root = new ObjectMapper().readTree(input);
            Set<String> names = new HashSet<>();
            for (JsonNode entry : root) {
                names.add(entry.path("name").asText());
            }
            return names;
        }
    }
}
