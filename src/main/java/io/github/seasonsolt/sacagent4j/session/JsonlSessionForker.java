package io.github.seasonsolt.sacagent4j.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Creates a new session JSONL file that preserves ancestry up to a selected entry. */
public final class JsonlSessionForker {
    private JsonlSessionForker() {}

    public static Path fork(ObjectMapper objectMapper, Path sourcePath, String entryId, Path outputDirectory) throws Exception {
        SessionDocument source = JsonlSessionReader.read(objectMapper, sourcePath);
        Files.createDirectories(outputDirectory);

        String forkSessionId = UUID.randomUUID().toString();
        String fileName = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(':', '-') + "_" + forkSessionId + ".jsonl";
        Path forkPath = outputDirectory.resolve(fileName);

        ObjectNode header = source.header().deepCopy();
        header.put("id", forkSessionId);
        header.put("timestamp", Instant.now().toString());
        ObjectNode forkedFrom = header.putObject("forkedFrom");
        forkedFrom.put("path", source.path().toString());
        forkedFrom.put("sessionId", source.sessionId());
        forkedFrom.put("entryId", entryId);

        ObjectNode forked = objectMapper.createObjectNode();
        forked.put("type", "forked");
        forked.put("id", shortId());
        forked.put("parentId", entryId);
        forked.put("timestamp", Instant.now().toString());
        forked.set("from", forkedFrom.deepCopy());

        try (BufferedWriter writer = Files.newBufferedWriter(forkPath, StandardCharsets.UTF_8)) {
            writer.write(objectMapper.writeValueAsString(header));
            writer.newLine();
            for (SessionEntry entry : source.ancestryTo(entryId)) {
                writer.write(objectMapper.writeValueAsString(entry.node()));
                writer.newLine();
            }
            writer.write(objectMapper.writeValueAsString(forked));
            writer.newLine();
        }

        return forkPath;
    }

    private static String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
