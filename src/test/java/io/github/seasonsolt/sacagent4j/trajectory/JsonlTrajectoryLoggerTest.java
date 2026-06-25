package io.github.seasonsolt.sacagent4j.trajectory;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonlTrajectoryLoggerTest {
    @TempDir
    Path tempDir;

    @Test
    void writesStartedTurnAndFinishedEvents() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonlTrajectoryLogger logger = new JsonlTrajectoryLogger(objectMapper, new Workspace(tempDir), ".sac-agent4j/runs");
        logger.started("task", 3);
        logger.turn(0, new Action.ReadFile("README.md"), Observation.ok("hello"));
        logger.finished(true, "done", 1);
        logger.close();

        List<String> lines = Files.readAllLines(logger.path());
        assertEquals(3, lines.size());

        JsonNode started = objectMapper.readTree(lines.get(0));
        JsonNode turn = objectMapper.readTree(lines.get(1));
        JsonNode finished = objectMapper.readTree(lines.get(2));

        assertEquals("started", started.path("event").asText());
        assertEquals("turn", turn.path("event").asText());
        assertEquals("read_file", turn.path("action").path("type").asText());
        assertEquals("finished", finished.path("event").asText());
        assertTrue(finished.path("finished").asBoolean());
    }
}
