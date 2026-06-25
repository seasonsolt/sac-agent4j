package io.github.seasonsolt.sacagent4j.trajectory;

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

/**
 * Writes one JSON object per line under {@code .sac-agent4j/runs/} by default.
 *
 * <p>JSONL keeps the format append-only and easy to inspect with command-line
 * tools. Each line is independently parseable.</p>
 */
public final class JsonlTrajectoryLogger implements TrajectoryLogger {
    private final ObjectMapper objectMapper;
    private final BufferedWriter writer;
    private final Path path;

    public JsonlTrajectoryLogger(ObjectMapper objectMapper, Workspace workspace, String relativeDirectory) throws IOException {
        this.objectMapper = objectMapper;
        String directory = relativeDirectory == null || relativeDirectory.isBlank()
                ? ".sac-agent4j/runs"
                : relativeDirectory;
        Path dir = workspace.resolveForWrite(directory);
        Files.createDirectories(dir);
        String fileName = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(':', '-') + ".jsonl";
        this.path = dir.resolve(fileName);
        this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
    }

    public Path path() {
        return path;
    }

    @Override
    public void started(String task, int maxSteps) throws Exception {
        ObjectNode event = baseEvent("started");
        event.put("task", task);
        event.put("maxSteps", maxSteps);
        write(event);
    }

    @Override
    public void turn(int step, Action action, Observation observation) throws Exception {
        ObjectNode event = baseEvent("turn");
        event.put("step", step);
        event.set("action", objectMapper.valueToTree(action));
        event.set("observation", objectMapper.valueToTree(observation));
        write(event);
    }

    @Override
    public void finished(boolean finished, String summary, int turns) throws Exception {
        ObjectNode event = baseEvent("finished");
        event.put("finished", finished);
        event.put("summary", summary);
        event.put("turns", turns);
        write(event);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    private ObjectNode baseEvent(String eventType) {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("timestamp", Instant.now().toString());
        event.put("event", eventType);
        return event;
    }

    private void write(ObjectNode event) throws IOException {
        writer.write(objectMapper.writeValueAsString(event));
        writer.newLine();
        writer.flush();
    }
}
