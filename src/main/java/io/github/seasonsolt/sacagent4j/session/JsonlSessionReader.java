package io.github.seasonsolt.sacagent4j.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads append-only JSONL session files into a tree-friendly read model. */
public final class JsonlSessionReader {
    private JsonlSessionReader() {}

    public static SessionDocument read(ObjectMapper objectMapper, Path path) throws Exception {
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("session file is empty: " + path);
        }

        JsonNode rawHeader = objectMapper.readTree(lines.get(0));
        if (!(rawHeader instanceof ObjectNode header) || !header.path("type").asText().equals("session")) {
            throw new IllegalArgumentException("session file must start with a session header: " + path);
        }

        List<SessionEntry> entries = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            JsonNode node = objectMapper.readTree(lines.get(index));
            if (!(node instanceof ObjectNode)) {
                throw new IllegalArgumentException("session entry must be a JSON object at line " + (index + 1));
            }
            String id = node.path("id").asText("");
            if (id.isBlank()) {
                throw new IllegalArgumentException("session entry is missing id at line " + (index + 1));
            }
            String parentId = node.path("parentId").isNull() || node.path("parentId").isMissingNode()
                    ? null
                    : node.path("parentId").asText();
            entries.add(new SessionEntry(id, parentId, node.path("type").asText(""), node));
        }
        return new SessionDocument(path.toAbsolutePath().normalize(), header, entries);
    }
}
