package io.github.seasonsolt.sacagent4j.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonlSessionRecorderTest {
    @TempDir
    Path tempDir;

    @Test
    void writesPiStyleSessionTreeEntries() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonlSessionRecorder recorder = new JsonlSessionRecorder(objectMapper, new Workspace(tempDir), ".sac-agent4j/sessions");
        recorder.started("fix bug", 4);
        recorder.turn(0, new Action.ReadFile("README.md"), Observation.ok("hello"));
        recorder.finished(true, "done", 1);
        recorder.close();

        List<String> lines = Files.readAllLines(recorder.path().orElseThrow());
        assertEquals(4, lines.size());

        JsonNode header = objectMapper.readTree(lines.get(0));
        JsonNode started = objectMapper.readTree(lines.get(1));
        JsonNode turn = objectMapper.readTree(lines.get(2));
        JsonNode finished = objectMapper.readTree(lines.get(3));

        assertEquals("session", header.path("type").asText());
        assertEquals(JsonlSessionRecorder.CURRENT_VERSION, header.path("version").asInt());
        assertEquals("started", started.path("type").asText());
        assertTrue(started.path("parentId").isNull());
        assertEquals(started.path("id").asText(), turn.path("parentId").asText());
        assertEquals(turn.path("id").asText(), finished.path("parentId").asText());
        assertEquals("read_file", turn.path("action").path("type").asText());
        assertTrue(finished.path("finished").asBoolean());
    }

    @Test
    void storesOffloadContextByHandleInsteadOfFullContent() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonlSessionRecorder recorder = new JsonlSessionRecorder(objectMapper, new Workspace(tempDir), ".sac-agent4j/sessions");
        recorder.turn(0, new Action.OffloadContext("failure-log", "large failure log", "very long content"), Observation.ok("context offloaded"));
        recorder.close();

        List<String> lines = Files.readAllLines(recorder.path().orElseThrow());
        JsonNode turn = objectMapper.readTree(lines.get(1));

        assertEquals("offload_context", turn.path("action").path("type").asText());
        assertEquals("failure-log", turn.path("action").path("key").asText());
        assertEquals("large failure log", turn.path("action").path("title").asText());
        assertEquals("very long content".length(), turn.path("action").path("contentChars").asInt());
        assertFalse(turn.path("action").has("content"));
    }
}
