package io.github.seasonsolt.sacagent4j.session;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Finds JSONL session files and renders them as a compact catalog. */
public final class JsonlSessionCatalog {
    private JsonlSessionCatalog() {}

    public static List<SessionListItem> list(ObjectMapper objectMapper, Path root) throws Exception {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        if (!Files.exists(normalizedRoot)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(normalizedRoot)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .map(path -> readItem(objectMapper, path))
                    .sorted(Comparator.comparing(SessionListItem::timestamp).reversed()
                            .thenComparing(item -> item.path().toString()))
                    .toList();
        }
    }

    private static SessionListItem readItem(ObjectMapper objectMapper, Path path) {
        try {
            SessionDocument document = JsonlSessionReader.read(objectMapper, path);
            SessionSummary summary = document.summary();
            Instant timestamp = Instant.parse(document.header().path("timestamp").asText());
            return new SessionListItem(
                    document.path(),
                    summary.sessionId(),
                    timestamp,
                    summary.task(),
                    summary.status(),
                    summary.finalSummary(),
                    summary.turns(),
                    summary.leafId()
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException("failed to read session file: " + path, exception);
        }
    }
}
