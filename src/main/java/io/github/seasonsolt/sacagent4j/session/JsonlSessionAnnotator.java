package io.github.seasonsolt.sacagent4j.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;

/** Appends human/team annotations to existing session entries. */
public final class JsonlSessionAnnotator {
    private JsonlSessionAnnotator() {}

    public static String note(ObjectMapper objectMapper, Path sessionPath, String entryId, String title, String body) throws Exception {
        SessionDocument document = JsonlSessionReader.read(objectMapper, sessionPath);
        if (document.entries().stream().noneMatch(entry -> entry.id().equals(entryId))) {
            throw new IllegalArgumentException("session entry not found: " + entryId);
        }
        String noteId = shortId();
        ObjectNode note = objectMapper.createObjectNode();
        note.put("type", "note");
        note.put("id", noteId);
        note.put("parentId", entryId);
        note.put("timestamp", Instant.now().toString());
        note.put("title", title == null ? "" : title);
        note.put("body", body == null ? "" : body);

        try (BufferedWriter writer = Files.newBufferedWriter(
                sessionPath.toAbsolutePath().normalize(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            writer.write(objectMapper.writeValueAsString(note));
            writer.newLine();
        }
        return noteId;
    }

    private static String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
