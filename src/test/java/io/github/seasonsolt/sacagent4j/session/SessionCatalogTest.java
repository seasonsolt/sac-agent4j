package io.github.seasonsolt.sacagent4j.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SessionCatalogTest {
    @TempDir
    Path tempDir;

    @Test
    void listsSessionsNewestFirstWithCompactRows() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Path root = tempDir.resolve("sessions");
        Path older = writeFixedTimestampSession(objectMapper, root.resolve("older.jsonl"), "old1", "inspect repo", "2026-06-27T00:00:00Z", "old done");
        Path newer = writeFixedTimestampSession(objectMapper, root.resolve("newer.jsonl"), "new1", "fix tests", "2026-06-27T00:00:01Z", "new done");

        List<SessionListItem> items = JsonlSessionCatalog.list(objectMapper, root);

        assertEquals(2, items.size());
        assertEquals(newer.toAbsolutePath().normalize(), items.get(0).path());
        assertEquals("fix tests", items.get(0).task());
        assertEquals("finished", items.get(0).status());
        assertEquals("old done", items.get(1).finalSummary());
        assertTrue(items.get(0).render().contains("task=\"fix tests\""));
        assertTrue(items.get(0).render().contains("path=" + newer.toAbsolutePath().normalize()));
    }

    @Test
    void returnsEmptyListForMissingRoot() throws Exception {
        List<SessionListItem> items = JsonlSessionCatalog.list(new ObjectMapper(), tempDir.resolve("missing"));

        assertTrue(items.isEmpty());
    }

    @Test
    void rendersQuotedFieldsOnOnePhysicalLine() {
        SessionListItem item = new SessionListItem(
                tempDir.resolve("session\nwith\ttab.jsonl"),
                "session\n1",
                java.time.Instant.parse("2026-06-27T00:00:00Z"),
                "fix \"tests\"\nnow",
                "finished\nstatus",
                "done\rwith\ttabs",
                2,
                "leaf\t1"
        );

        String rendered = item.render();

        assertTrue(rendered.contains("task=\"fix \\\"tests\\\"\\nnow\""));
        assertTrue(rendered.contains("status=finished\\nstatus"));
        assertTrue(rendered.contains("summary=\"done\\rwith\\ttabs\""));
        assertTrue(rendered.contains("leaf=leaf\\t1"));
        assertTrue(rendered.contains("session=session\\n1"));
        assertTrue(rendered.contains("path=" + tempDir.resolve("session\nwith\ttab.jsonl").toString()
                .replace("\n", "\\n")
                .replace("\t", "\\t")));
        assertEquals(1, rendered.lines().count());
    }

    @Test
    void sortsEqualTimestampsByPath() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Path root = tempDir.resolve("sessions");
        Path laterPathLexically = writeFixedTimestampSession(objectMapper, root.resolve("b.jsonl"), "b1", "b task", "2026-06-27T00:00:00Z", "done");
        Path earlierPathLexically = writeFixedTimestampSession(objectMapper, root.resolve("a.jsonl"), "a1", "a task", "2026-06-27T00:00:00Z", "done");

        List<SessionListItem> items = JsonlSessionCatalog.list(objectMapper, root);

        assertEquals(2, items.size());
        assertEquals(earlierPathLexically.toAbsolutePath().normalize(), items.get(0).path());
        assertEquals(laterPathLexically.toAbsolutePath().normalize(), items.get(1).path());
    }

    @Test
    void malformedSessionFileFailsWithPath() throws Exception {
        Path bad = tempDir.resolve("sessions/bad.jsonl");
        Files.createDirectories(bad.getParent());
        Files.writeString(bad, "not-json\n");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsonlSessionCatalog.list(new ObjectMapper(), bad.getParent())
        );

        assertTrue(exception.getMessage().contains(bad.toString()));
        assertTrue(exception.getCause() != null);
    }

    private Path writeFixedTimestampSession(ObjectMapper objectMapper, Path path, String sessionId, String task, String timestamp, String summary) throws Exception {
        Files.createDirectories(path.getParent());
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "session");
        header.put("version", 1);
        header.put("id", sessionId);
        header.put("timestamp", timestamp);
        header.put("cwd", tempDir.toString());

        ObjectNode started = objectMapper.createObjectNode();
        started.put("type", "started");
        started.put("id", sessionId + "s");
        started.putNull("parentId");
        started.put("timestamp", "2026-06-27T00:00:01Z");
        started.put("task", task);
        started.put("maxSteps", 4);

        ObjectNode finished = objectMapper.createObjectNode();
        finished.put("type", "finished");
        finished.put("id", sessionId + "f");
        finished.put("parentId", sessionId + "s");
        finished.put("timestamp", "2026-06-27T00:00:02Z");
        finished.put("finished", true);
        finished.put("summary", summary);
        finished.put("turns", 0);

        Files.write(path, List.of(
                objectMapper.writeValueAsString(header),
                objectMapper.writeValueAsString(started),
                objectMapper.writeValueAsString(finished)
        ));
        return path;
    }
}
