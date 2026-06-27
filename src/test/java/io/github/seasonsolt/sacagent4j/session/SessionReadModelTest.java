package io.github.seasonsolt.sacagent4j.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;
import io.github.seasonsolt.sacagent4j.agent.Turn;
import io.github.seasonsolt.sacagent4j.agent.context.HistoryRenderer;
import io.github.seasonsolt.sacagent4j.plan.TodoStatus;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SessionReadModelTest {
    @TempDir
    Path tempDir;

    @Test
    void summarizesSessionForHumanReview() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Path sessionPath = writeSampleSession(objectMapper);

        SessionDocument document = JsonlSessionReader.read(objectMapper, sessionPath);
        SessionSummary summary = document.summary();

        assertEquals("fix tests", summary.task());
        assertEquals("done", summary.finalSummary());
        assertTrue(summary.finished());
        assertEquals(2, summary.turns());
        assertEquals("finished", summary.status());
        assertEquals(1, summary.actionCounts().get("read_file"));
        assertEquals(1, summary.actionCounts().get("run_tests"));
        assertEquals(document.leafId(), summary.leafId());
        assertTrue(summary.render().contains("actions=read_file:1, run_tests:1"));
    }

    @Test
    void forksSessionAtSelectedEntryAndKeepsOnlyAncestry() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Path sessionPath = writeSampleSession(objectMapper);
        SessionDocument document = JsonlSessionReader.read(objectMapper, sessionPath);
        String firstTurnId = document.entries().stream()
                .filter(entry -> entry.type().equals("turn"))
                .findFirst()
                .orElseThrow()
                .id();

        Path forkPath = JsonlSessionForker.fork(objectMapper, sessionPath, firstTurnId, tempDir.resolve("forks"));
        SessionDocument fork = JsonlSessionReader.read(objectMapper, forkPath);

        assertEquals(firstTurnId, fork.leafId());
        assertEquals(3, fork.entries().size());
        assertEquals("started", fork.entries().get(0).type());
        assertEquals("turn", fork.entries().get(1).type());
        assertEquals("forked", fork.entries().get(2).type());
        assertEquals(sessionPath.toAbsolutePath().normalize().toString(), fork.header().path("forkedFrom").path("path").asText());
        assertEquals(firstTurnId, fork.header().path("forkedFrom").path("entryId").asText());
    }

    @Test
    void appendsNewEntriesToExistingForkFromSelectedLeaf() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Path sessionPath = writeSampleSession(objectMapper);
        SessionDocument document = JsonlSessionReader.read(objectMapper, sessionPath);
        String leafId = document.leafId();

        try (JsonlSessionRecorder recorder = JsonlSessionRecorder.resume(objectMapper, sessionPath, leafId)) {
            recorder.turn(2, new Action.Search("TODO"), Observation.ok("none"));
        }

        SessionDocument resumed = JsonlSessionReader.read(objectMapper, sessionPath);
        SessionEntry appended = resumed.entries().get(resumed.entries().size() - 1);
        assertEquals("turn", appended.type());
        assertEquals(leafId, appended.parentId());
        assertEquals("search", appended.node().path("action").path("type").asText());
    }

    @Test
    void replaysAncestryIntoTaskHistoryAndDurableState() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Path sessionPath = writeReplayableSession(objectMapper);

        SessionReplay replay = SessionReplay.from(objectMapper, sessionPath, null);

        assertEquals("continue feature", replay.task());
        assertEquals(4, replay.history().size());
        assertEquals("collect evidence", replay.state().plan().get(0).content());
        assertEquals(TodoStatus.completed, replay.state().plan().get(0).status());
        assertEquals(12, replay.state().virtualFileSummary().get("notes/decision.md"));
        Turn lastTurn = replay.history().get(replay.history().size() - 1);
        assertTrue(lastTurn.action() instanceof Action.ReadFile);
        assertEquals("source code", lastTurn.observation().output());
    }

    @Test
    void summarizesOnlyActiveBranchAncestry() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Path sessionPath = writeSampleSession(objectMapper);
        SessionDocument document = JsonlSessionReader.read(objectMapper, sessionPath);
        String firstTurnId = document.entries().stream()
                .filter(entry -> entry.type().equals("turn"))
                .findFirst()
                .orElseThrow()
                .id();

        try (JsonlSessionRecorder recorder = JsonlSessionRecorder.resume(objectMapper, sessionPath, firstTurnId)) {
            recorder.started("branch task", 2);
            recorder.turn(0, new Action.Search("TODO"), Observation.ok("none"));
            recorder.finished(true, "branch done", 1);
        }

        SessionSummary summary = JsonlSessionReader.read(objectMapper, sessionPath).summary();

        assertEquals("branch task", summary.task());
        assertEquals("branch done", summary.finalSummary());
        assertEquals(2, summary.turns());
        assertEquals(1, summary.actionCounts().get("read_file"));
        assertEquals(1, summary.actionCounts().get("search"));
        assertFalse(summary.actionCounts().containsKey("run_tests"));
    }

    @Test
    void replayedOffloadKeepsCompactContentSizeInPromptHistory() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonlSessionRecorder recorder = new JsonlSessionRecorder(objectMapper, new Workspace(tempDir), ".sac-agent4j/sessions");
        recorder.started("offload replay", 4);
        recorder.turn(
                0,
                new Action.OffloadContext("failure-log", "full failure log", "0123456789"),
                Observation.ok("context offloaded: failure-log (10 chars)")
        );
        recorder.finished(true, "paused", 1);
        recorder.close();

        SessionReplay replay = SessionReplay.from(objectMapper, recorder.path().orElseThrow(), null);
        String rendered = new HistoryRenderer(objectMapper).render(replay.history());

        assertTrue(rendered.contains("\"contentChars\":10"));
        assertFalse(rendered.contains("\"contentChars\":0"));
    }

    @Test
    void rendersSessionTreeWithBranchesForTeamReview() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Path sessionPath = writeSampleSession(objectMapper);
        SessionDocument document = JsonlSessionReader.read(objectMapper, sessionPath);
        String firstTurnId = document.entries().stream()
                .filter(entry -> entry.type().equals("turn"))
                .findFirst()
                .orElseThrow()
                .id();

        try (JsonlSessionRecorder recorder = JsonlSessionRecorder.resume(objectMapper, sessionPath, firstTurnId)) {
            recorder.started("branch from first read", 2);
            recorder.turn(0, new Action.Search("TODO"), Observation.ok("none"));
        }

        SessionDocument branched = JsonlSessionReader.read(objectMapper, sessionPath);
        String tree = branched.tree().render();

        assertTrue(tree.contains("session=" + branched.sessionId()));
        assertTrue(tree.contains("leaf=" + branched.leafId()));
        assertTrue(tree.contains(branched.entries().get(0).id() + " started task=\"fix tests\""));
        assertTrue(tree.contains(firstTurnId + " turn step=0 action=read_file"));
        assertTrue(tree.contains("started task=\"branch from first read\""));
        assertTrue(tree.contains("turn step=0 action=search"));
        assertTrue(tree.contains("finished=true summary=\"done\""));
    }

    private Path writeSampleSession(ObjectMapper objectMapper) throws Exception {
        JsonlSessionRecorder recorder = new JsonlSessionRecorder(objectMapper, new Workspace(tempDir), ".sac-agent4j/sessions");
        recorder.started("fix tests", 4);
        recorder.turn(0, new Action.ReadFile("README.md"), Observation.ok("readme"));
        recorder.turn(1, new Action.RunTests(), Observation.ok("tests passed"));
        recorder.finished(true, "done", 2);
        recorder.close();

        Path sessionPath = recorder.path().orElseThrow();
        assertEquals(5, Files.readAllLines(sessionPath).size());
        return sessionPath;
    }

    private Path writeReplayableSession(ObjectMapper objectMapper) throws Exception {
        JsonlSessionRecorder recorder = new JsonlSessionRecorder(objectMapper, new Workspace(tempDir), ".sac-agent4j/sessions");
        recorder.started("continue feature", 8);
        recorder.turn(0, new Action.SetPlan(List.of("collect evidence", "implement resume")), Observation.ok("plan set: 2 items"));
        recorder.turn(1, new Action.UpdateTodo(1, TodoStatus.completed), Observation.ok("todo updated: 1 -> completed"));
        recorder.turn(2, new Action.WriteVirtualFile("notes/decision.md", "resume first"), Observation.ok("virtual file written"));
        recorder.turn(3, new Action.ReadFile("src/Main.java"), Observation.ok("source code"));
        recorder.finished(true, "paused", 4);
        recorder.close();
        return recorder.path().orElseThrow();
    }
}
