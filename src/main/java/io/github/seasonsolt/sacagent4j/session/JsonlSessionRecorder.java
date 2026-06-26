package io.github.seasonsolt.sacagent4j.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Writes a Pi-style session file: one JSON object per line, with id/parentId links.
 *
 * <p>The session file is human-readable and branch-ready. It is not a checkpoint:
 * it records the action/observation history needed to inspect or later rebuild
 * context from the active leaf.</p>
 */
public final class JsonlSessionRecorder implements SessionRecorder {
    public static final int CURRENT_VERSION = 1;

    private final ObjectMapper objectMapper;
    private final BufferedWriter writer;
    private final Path path;
    private final String sessionId;
    private String leafId;

    public JsonlSessionRecorder(ObjectMapper objectMapper, Workspace workspace, String relativeDirectory) throws IOException {
        this.objectMapper = objectMapper;
        String directory = relativeDirectory == null || relativeDirectory.isBlank()
                ? ".sac-agent4j/sessions"
                : relativeDirectory;
        Path dir = workspace.resolveForWrite(directory).resolve(safeWorkspaceName(workspace.root()));
        Files.createDirectories(dir);
        this.sessionId = UUID.randomUUID().toString();
        String fileName = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(':', '-') + "_" + sessionId + ".jsonl";
        this.path = dir.resolve(fileName);
        this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        write(header(workspace.root().toString()));
    }

    @Override
    public void started(String task, int maxSteps) throws IOException {
        ObjectNode entry = baseEntry("started");
        entry.put("task", task);
        entry.put("maxSteps", maxSteps);
        append(entry);
    }

    @Override
    public void turn(int step, Action action, Observation observation) throws IOException {
        ObjectNode entry = baseEntry("turn");
        entry.put("step", step);
        entry.set("action", actionForSession(action));
        entry.set("observation", objectMapper.valueToTree(observation));
        append(entry);
    }

    @Override
    public void finished(boolean finished, String summary, int turns) throws IOException {
        ObjectNode entry = baseEntry("finished");
        entry.put("finished", finished);
        entry.put("summary", summary);
        entry.put("turns", turns);
        append(entry);
    }

    @Override
    public Optional<Path> path() {
        return Optional.of(path);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    private ObjectNode header(String cwd) {
        ObjectNode entry = objectMapper.createObjectNode();
        entry.put("type", "session");
        entry.put("version", CURRENT_VERSION);
        entry.put("id", sessionId);
        entry.put("timestamp", Instant.now().toString());
        entry.put("cwd", cwd);
        return entry;
    }

    private ObjectNode baseEntry(String type) {
        ObjectNode entry = objectMapper.createObjectNode();
        entry.put("type", type);
        entry.put("id", shortId());
        if (leafId == null) {
            entry.putNull("parentId");
        } else {
            entry.put("parentId", leafId);
        }
        entry.put("timestamp", Instant.now().toString());
        return entry;
    }

    private void append(ObjectNode entry) throws IOException {
        write(entry);
        leafId = entry.path("id").asText();
    }

    private void write(ObjectNode entry) throws IOException {
        writer.write(objectMapper.writeValueAsString(entry));
        writer.newLine();
        writer.flush();
    }

    private ObjectNode actionForSession(Action action) {
        if (action instanceof Action.OffloadContext offloadContext) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", "offload_context");
            node.put("key", offloadContext.key());
            node.put("title", offloadContext.title());
            node.put("contentChars", offloadContext.content() == null ? 0 : offloadContext.content().length());
            return node;
        }
        return objectMapper.valueToTree(action);
    }

    private static String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static String safeWorkspaceName(Path root) {
        String raw = root.toString().replaceFirst("^[/\\\\]+", "").replaceAll("[/\\\\:]+", "-");
        return "--" + (raw.isBlank() ? "workspace" : raw) + "--";
    }
}
