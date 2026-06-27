package io.github.seasonsolt.sacagent4j.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SessionCatalogTest {
    @TempDir
    Path tempDir;

    @Test
    void listsSessionsNewestFirstWithCompactRows() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Path older = writeSession(objectMapper, "inspect repo", "old done");
        Thread.sleep(2);
        Path newer = writeSession(objectMapper, "fix tests", "new done");
        Path root = older.getParent().getParent();

        List<SessionListItem> items = JsonlSessionCatalog.list(objectMapper, root);

        assertEquals(2, items.size());
        assertEquals(newer.toAbsolutePath().normalize(), items.get(0).path());
        assertEquals("fix tests", items.get(0).task());
        assertEquals("finished", items.get(0).status());
        assertEquals("old done", items.get(1).finalSummary());
        assertTrue(items.get(0).render().contains("task=\"fix tests\""));
        assertTrue(items.get(0).render().contains("path=" + newer.toAbsolutePath().normalize()));
    }

    private Path writeSession(ObjectMapper objectMapper, String task, String summary) throws Exception {
        JsonlSessionRecorder recorder = new JsonlSessionRecorder(objectMapper, new Workspace(tempDir), ".sac-agent4j/sessions");
        recorder.started(task, 4);
        recorder.turn(0, new Action.ReadFile("README.md"), Observation.ok("readme"));
        recorder.finished(true, summary, 1);
        recorder.close();
        return recorder.path().orElseThrow();
    }
}
