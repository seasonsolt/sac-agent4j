package io.github.seasonsolt.sacagent4j.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SessionHandoffTest {
    @TempDir
    Path tempDir;

    @Test
    void rendersMarkdownHandoffForSelectedAncestry() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonlSessionRecorder recorder = new JsonlSessionRecorder(objectMapper, new Workspace(tempDir), ".sac-agent4j/sessions");
        recorder.started("fix tests", 4);
        recorder.turn(0, new Action.ReadFile("README.md"), Observation.ok("readme"));
        recorder.finished(true, "done", 1);
        recorder.close();
        Path sessionPath = recorder.path().orElseThrow();
        SessionDocument document = JsonlSessionReader.read(objectMapper, sessionPath);

        String markdown = SessionHandoff.render(document, document.leafId());

        assertTrue(markdown.contains("# Session Handoff"));
        assertTrue(markdown.contains("Task: fix tests"));
        assertTrue(markdown.contains("Selected entry: " + document.leafId()));
        assertTrue(markdown.contains("```text\n" + document.tree().render()));
        assertTrue(markdown.contains("java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar --resume-session"));
        assertTrue(markdown.contains("--resume-entry " + document.leafId()));
    }
}
