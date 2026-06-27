# Super Team Session Productization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the next phased session layer so one person's agent run becomes easier for a team to find, annotate, branch, resume, and hand off.

**Architecture:** Keep the existing JSONL session format as the source of truth. Add focused read/write helpers in `io.github.seasonsolt.sacagent4j.session`, then expose them through small `session` CLI subcommands in `Main`. Each phase is independently useful: catalog/listing first, team notes second, handoff markdown third.

**Tech Stack:** Java 21, Maven, JUnit Jupiter, Jackson, picocli, GraalVM Native Image profile.

---

## Scope And Phase Gates

This plan continues the product direction from `docs/PRODUCT_DIRECTION.md`: information visibility first, capability callability later, coordination automation last.

This plan covers only **Phase 1: Session Productization**:

1. **Visibility:** list all session files in a directory as a compact catalog.
2. **Collaboration:** append team notes to a selected session entry and show them in the tree.
3. **Handoff:** generate a markdown handoff pack for a selected session leaf or branch point.

This plan does not cover Spring Boot integration, MCP adapters, approval queues, or exact checkpointing. Those are separate future plans.

Before implementation, inspect the current worktree. The repository may already contain uncommitted session work. Do not stage `AGENTS.md`; it is user-owned and unrelated.

## File Structure

Create:

- `src/main/java/io/github/seasonsolt/sacagent4j/session/SessionListItem.java`  
  One rendered row in a session catalog.
- `src/main/java/io/github/seasonsolt/sacagent4j/session/JsonlSessionCatalog.java`  
  Finds and sorts `.jsonl` session files under a directory.
- `src/test/java/io/github/seasonsolt/sacagent4j/session/SessionCatalogTest.java`  
  Tests catalog listing and ordering.
- `src/main/java/io/github/seasonsolt/sacagent4j/session/JsonlSessionAnnotator.java`  
  Appends team note entries to an existing session tree.
- `src/main/java/io/github/seasonsolt/sacagent4j/session/SessionHandoff.java`  
  Renders a markdown handoff pack for one selected ancestry path.
- `src/test/java/io/github/seasonsolt/sacagent4j/session/SessionHandoffTest.java`  
  Tests markdown handoff content.

Modify:

- `src/main/java/io/github/seasonsolt/sacagent4j/session/SessionTree.java`  
  Render `note` entries.
- `src/main/java/io/github/seasonsolt/sacagent4j/cli/Main.java`  
  Add `session list`, `session note`, and `session handoff`.
- `src/test/java/io/github/seasonsolt/sacagent4j/session/SessionReadModelTest.java`  
  Add note/tree coverage.
- `src/test/java/io/github/seasonsolt/sacagent4j/cli/MainSessionCommandTest.java`  
  Add CLI coverage for list, note, and handoff.
- `README.md`  
  Document the three new commands.
- `docs/ARCHITECTURE.md`  
  Document the new session productization helpers.
- `docs/CLASS_DIAGRAM.md`  
  Add the new session classes.

---

### Task 0: Baseline Gate For Current Session Work

**Files:**
- Read: `git status --short`
- Read: `README.md`
- Read: `docs/ARCHITECTURE.md`
- Read: `src/main/java/io/github/seasonsolt/sacagent4j/session/SessionDocument.java`
- Read: `src/main/java/io/github/seasonsolt/sacagent4j/session/SessionTree.java`
- Read: `src/main/java/io/github/seasonsolt/sacagent4j/cli/Main.java`

- [ ] **Step 1: Inspect working tree**

Run:

```bash
git status --short
```

Expected:

```text
 M README.md
 M docs/ARCHITECTURE.md
 M docs/CLASS_DIAGRAM.md
 M src/main/java/io/github/seasonsolt/sacagent4j/agent/AgentLoop.java
 M src/main/java/io/github/seasonsolt/sacagent4j/agent/AgentRun.java
 M src/main/java/io/github/seasonsolt/sacagent4j/cli/Main.java
 M src/main/java/io/github/seasonsolt/sacagent4j/session/JsonlSessionRecorder.java
 M src/test/java/io/github/seasonsolt/sacagent4j/agent/AgentLoopTest.java
?? AGENTS.md
?? src/main/java/io/github/seasonsolt/sacagent4j/session/JsonlSessionForker.java
?? src/main/java/io/github/seasonsolt/sacagent4j/session/JsonlSessionReader.java
?? src/main/java/io/github/seasonsolt/sacagent4j/session/SessionDocument.java
?? src/main/java/io/github/seasonsolt/sacagent4j/session/SessionEntry.java
?? src/main/java/io/github/seasonsolt/sacagent4j/session/SessionReplay.java
?? src/main/java/io/github/seasonsolt/sacagent4j/session/SessionSummary.java
?? src/main/java/io/github/seasonsolt/sacagent4j/session/SessionTree.java
?? src/test/java/io/github/seasonsolt/sacagent4j/cli/
?? src/test/java/io/github/seasonsolt/sacagent4j/session/SessionReadModelTest.java
```

If this exact output has changed, continue only after identifying which changes belong to the user. Do not revert user changes.

- [ ] **Step 2: Run baseline verification**

Run:

```bash
mvn test
```

Expected:

```text
[INFO] Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 3: Run whitespace check**

Run:

```bash
git diff --check
```

Expected: no output and exit code `0`.

- [ ] **Step 4: Commit current verified session baseline**

Run:

```bash
git add README.md \
  docs/ARCHITECTURE.md \
  docs/CLASS_DIAGRAM.md \
  src/main/java/io/github/seasonsolt/sacagent4j/agent/AgentLoop.java \
  src/main/java/io/github/seasonsolt/sacagent4j/agent/AgentRun.java \
  src/main/java/io/github/seasonsolt/sacagent4j/cli/Main.java \
  src/main/java/io/github/seasonsolt/sacagent4j/session/JsonlSessionRecorder.java \
  src/main/java/io/github/seasonsolt/sacagent4j/session/JsonlSessionForker.java \
  src/main/java/io/github/seasonsolt/sacagent4j/session/JsonlSessionReader.java \
  src/main/java/io/github/seasonsolt/sacagent4j/session/SessionDocument.java \
  src/main/java/io/github/seasonsolt/sacagent4j/session/SessionEntry.java \
  src/main/java/io/github/seasonsolt/sacagent4j/session/SessionReplay.java \
  src/main/java/io/github/seasonsolt/sacagent4j/session/SessionSummary.java \
  src/main/java/io/github/seasonsolt/sacagent4j/session/SessionTree.java \
  src/test/java/io/github/seasonsolt/sacagent4j/agent/AgentLoopTest.java \
  src/test/java/io/github/seasonsolt/sacagent4j/cli/MainSessionCommandTest.java \
  src/test/java/io/github/seasonsolt/sacagent4j/session/SessionReadModelTest.java
git commit -m "Add branchable session resume workflow"
```

Expected:

```text
[main <hash>] Add branchable session resume workflow
```

`AGENTS.md` must remain unstaged.

---

### Task 1: Session Catalog Read Model

**Files:**
- Create: `src/main/java/io/github/seasonsolt/sacagent4j/session/SessionListItem.java`
- Create: `src/main/java/io/github/seasonsolt/sacagent4j/session/JsonlSessionCatalog.java`
- Create: `src/test/java/io/github/seasonsolt/sacagent4j/session/SessionCatalogTest.java`

- [ ] **Step 1: Write the failing catalog test**

Create `src/test/java/io/github/seasonsolt/sacagent4j/session/SessionCatalogTest.java`:

```java
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
```

- [ ] **Step 2: Run the catalog test to verify it fails**

Run:

```bash
mvn -Dtest=SessionCatalogTest test
```

Expected:

```text
[ERROR] COMPILATION ERROR
cannot find symbol
  symbol:   class SessionListItem
cannot find symbol
  symbol:   variable JsonlSessionCatalog
```

- [ ] **Step 3: Add `SessionListItem`**

Create `src/main/java/io/github/seasonsolt/sacagent4j/session/SessionListItem.java`:

```java
package io.github.seasonsolt.sacagent4j.session;

import java.nio.file.Path;
import java.time.Instant;

/** One compact row in a human-facing session catalog. */
public record SessionListItem(
        Path path,
        String sessionId,
        Instant timestamp,
        String task,
        String status,
        String finalSummary,
        int turns,
        String leafId
) {
    public String render() {
        return "timestamp=" + timestamp
                + " status=" + status
                + " turns=" + turns
                + " task=\"" + escape(task) + "\""
                + " summary=\"" + escape(finalSummary) + "\""
                + " leaf=" + leafId
                + " session=" + sessionId
                + " path=" + path;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
```

- [ ] **Step 4: Add `JsonlSessionCatalog`**

Create `src/main/java/io/github/seasonsolt/sacagent4j/session/JsonlSessionCatalog.java`:

```java
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
                    .sorted(Comparator.comparing(SessionListItem::timestamp).reversed())
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
```

- [ ] **Step 5: Run the catalog test to verify it passes**

Run:

```bash
mvn -Dtest=SessionCatalogTest test
```

Expected:

```text
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 6: Commit catalog read model**

Run:

```bash
git add src/main/java/io/github/seasonsolt/sacagent4j/session/SessionListItem.java \
  src/main/java/io/github/seasonsolt/sacagent4j/session/JsonlSessionCatalog.java \
  src/test/java/io/github/seasonsolt/sacagent4j/session/SessionCatalogTest.java
git commit -m "Add session catalog read model"
```

Expected:

```text
[main <hash>] Add session catalog read model
```

---

### Task 2: `session list` CLI

**Files:**
- Modify: `src/main/java/io/github/seasonsolt/sacagent4j/cli/Main.java`
- Modify: `src/test/java/io/github/seasonsolt/sacagent4j/cli/MainSessionCommandTest.java`

- [ ] **Step 1: Write the failing CLI test**

Add this test method to `src/test/java/io/github/seasonsolt/sacagent4j/cli/MainSessionCommandTest.java`:

```java
@Test
void listsSessionsFromCli() throws Exception {
    Path sessionPath = writeSession();
    Path sessionRoot = sessionPath.getParent().getParent();
    StringWriter output = new StringWriter();
    CommandLine commandLine = new CommandLine(new Main());
    commandLine.setOut(new PrintWriter(output, true));

    int exitCode = commandLine.execute("session", "list", sessionRoot.toString());

    assertEquals(0, exitCode);
    assertTrue(output.toString().contains("task=\"fix tests\""));
    assertTrue(output.toString().contains("status=finished"));
    assertTrue(output.toString().contains("path=" + sessionPath.toAbsolutePath().normalize()));
}
```

- [ ] **Step 2: Run the CLI test to verify it fails**

Run:

```bash
mvn -Dtest=MainSessionCommandTest#listsSessionsFromCli test
```

Expected:

```text
Unmatched arguments from index 1: 'list', '<path>'
[ERROR] Failures:
```

- [ ] **Step 3: Add imports and subcommand registration**

Modify `src/main/java/io/github/seasonsolt/sacagent4j/cli/Main.java`.

Add import:

```java
import io.github.seasonsolt.sacagent4j.session.JsonlSessionCatalog;
```

Change the `SessionCommand` annotation to:

```java
@CommandLine.Command(name = "session", description = "Inspect and fork JSONL session files.",
        subcommands = {
                SessionSummaryCommand.class,
                SessionTreeCommand.class,
                SessionListCommand.class,
                SessionForkCommand.class
        })
```

- [ ] **Step 4: Add the `SessionListCommand` class**

Add this nested class in `Main`, after `SessionTreeCommand`:

```java
@CommandLine.Command(name = "list", description = "List session files under a directory.")
static final class SessionListCommand implements Callable<Integer> {
    @CommandLine.Spec
    CommandSpec spec;

    @CommandLine.Parameters(index = "0", description = "Session root directory")
    Path directory;

    @Override
    public Integer call() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        for (var item : JsonlSessionCatalog.list(objectMapper, directory)) {
            spec.commandLine().getOut().println(item.render());
        }
        return 0;
    }
}
```

- [ ] **Step 5: Run CLI tests**

Run:

```bash
mvn -Dtest=MainSessionCommandTest test
```

Expected:

```text
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 6: Commit `session list`**

Run:

```bash
git add src/main/java/io/github/seasonsolt/sacagent4j/cli/Main.java \
  src/test/java/io/github/seasonsolt/sacagent4j/cli/MainSessionCommandTest.java
git commit -m "Add session list command"
```

Expected:

```text
[main <hash>] Add session list command
```

---

### Task 3: Team Notes On Session Entries

**Files:**
- Create: `src/main/java/io/github/seasonsolt/sacagent4j/session/JsonlSessionAnnotator.java`
- Modify: `src/main/java/io/github/seasonsolt/sacagent4j/session/SessionTree.java`
- Modify: `src/test/java/io/github/seasonsolt/sacagent4j/session/SessionReadModelTest.java`

- [ ] **Step 1: Write failing note/tree test**

Add this test method to `src/test/java/io/github/seasonsolt/sacagent4j/session/SessionReadModelTest.java`:

```java
@Test
void rendersTeamNotesUnderSelectedEntries() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    Path sessionPath = writeSampleSession(objectMapper);
    SessionDocument document = JsonlSessionReader.read(objectMapper, sessionPath);
    String firstTurnId = document.entries().stream()
            .filter(entry -> entry.type().equals("turn"))
            .findFirst()
            .orElseThrow()
            .id();

    String noteId = JsonlSessionAnnotator.note(
            objectMapper,
            sessionPath,
            firstTurnId,
            "review",
            "This read established the branch point."
    );

    SessionDocument annotated = JsonlSessionReader.read(objectMapper, sessionPath);
    String tree = annotated.tree().render();

    assertTrue(tree.contains(noteId + " note title=\"review\" bodyChars=39"));
    assertTrue(tree.indexOf(firstTurnId + " turn step=0 action=read_file") < tree.indexOf(noteId + " note"));
}
```

- [ ] **Step 2: Run the note test to verify it fails**

Run:

```bash
mvn -Dtest=SessionReadModelTest#rendersTeamNotesUnderSelectedEntries test
```

Expected:

```text
[ERROR] COMPILATION ERROR
cannot find symbol
  symbol:   variable JsonlSessionAnnotator
```

- [ ] **Step 3: Add `JsonlSessionAnnotator`**

Create `src/main/java/io/github/seasonsolt/sacagent4j/session/JsonlSessionAnnotator.java`:

```java
package io.github.seasonsolt.sacagent4j.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;

/** Appends human/team annotations to existing session entries. */
public final class JsonlSessionAnnotator {
    private JsonlSessionAnnotator() {}

    public static String note(ObjectMapper objectMapper, Path sessionPath, String entryId, String title, String body) throws Exception {
        SessionDocument document = JsonlSessionReader.read(objectMapper, sessionPath);
        if (document.entries().stream().noneMatch(entry -> entry.id().equals(entryId))) {
            throw new IllegalArgumentException("session entry not found: " + entryId);
        }
        String noteId = shortId();
        ObjectNode note = objectMapper.createObjectNode();
        note.put("type", "note");
        note.put("id", noteId);
        note.put("parentId", entryId);
        note.put("timestamp", Instant.now().toString());
        note.put("title", title == null ? "" : title);
        note.put("body", body == null ? "" : body);

        try (BufferedWriter writer = Files.newBufferedWriter(
                sessionPath.toAbsolutePath().normalize(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            writer.write(objectMapper.writeValueAsString(note));
            writer.newLine();
        }
        return noteId;
    }

    private static String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
```

- [ ] **Step 4: Render notes in `SessionTree`**

Modify the `label(SessionEntry entry)` switch in `src/main/java/io/github/seasonsolt/sacagent4j/session/SessionTree.java` to include the `note` case:

```java
private String label(SessionEntry entry) {
    return switch (entry.type()) {
        case "started" -> " task=\"" + escape(entry.node().path("task").asText("")) + "\""
                + " maxSteps=" + entry.node().path("maxSteps").asInt();
        case "turn" -> " step=" + entry.node().path("step").asInt()
                + " action=" + entry.node().path("action").path("type").asText("unknown");
        case "finished" -> " finished=" + entry.node().path("finished").asBoolean(false)
                + " summary=\"" + escape(entry.node().path("summary").asText("")) + "\""
                + " turns=" + entry.node().path("turns").asInt();
        case "forked" -> " fromEntry=" + entry.node().path("from").path("entryId").asText("");
        case "note" -> " title=\"" + escape(entry.node().path("title").asText("")) + "\""
                + " bodyChars=" + entry.node().path("body").asText("").length();
        default -> "";
    };
}
```

- [ ] **Step 5: Run note/tree tests**

Run:

```bash
mvn -Dtest=SessionReadModelTest test
```

Expected:

```text
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 6: Commit note read/write support**

Run:

```bash
git add src/main/java/io/github/seasonsolt/sacagent4j/session/JsonlSessionAnnotator.java \
  src/main/java/io/github/seasonsolt/sacagent4j/session/SessionTree.java \
  src/test/java/io/github/seasonsolt/sacagent4j/session/SessionReadModelTest.java
git commit -m "Add session entry notes"
```

Expected:

```text
[main <hash>] Add session entry notes
```

---

### Task 4: `session note` CLI

**Files:**
- Modify: `src/main/java/io/github/seasonsolt/sacagent4j/cli/Main.java`
- Modify: `src/test/java/io/github/seasonsolt/sacagent4j/cli/MainSessionCommandTest.java`

- [ ] **Step 1: Write failing CLI note test**

Add this test method to `src/test/java/io/github/seasonsolt/sacagent4j/cli/MainSessionCommandTest.java`:

```java
@Test
void appendsSessionNoteFromCli() throws Exception {
    Path sessionPath = writeSession();
    ObjectMapper objectMapper = new ObjectMapper();
    String leafId = JsonlSessionReader.read(objectMapper, sessionPath).leafId();
    StringWriter output = new StringWriter();
    CommandLine commandLine = new CommandLine(new Main());
    commandLine.setOut(new PrintWriter(output, true));

    int exitCode = commandLine.execute(
            "session",
            "note",
            sessionPath.toString(),
            "--entry-id",
            leafId,
            "--title",
            "handoff",
            "--body",
            "Ready for teammate review."
    );

    assertEquals(0, exitCode);
    assertTrue(output.toString().startsWith("note="));
    String tree = JsonlSessionReader.read(objectMapper, sessionPath).tree().render();
    assertTrue(tree.contains("note title=\"handoff\" bodyChars=26"));
}
```

- [ ] **Step 2: Run the CLI note test to verify it fails**

Run:

```bash
mvn -Dtest=MainSessionCommandTest#appendsSessionNoteFromCli test
```

Expected:

```text
Unmatched arguments from index 1: 'note', '<path>', '--entry-id', '<id>', '--title', 'handoff', '--body', 'Ready for teammate review.'
[ERROR] Failures:
```

- [ ] **Step 3: Add import and subcommand registration**

Modify `src/main/java/io/github/seasonsolt/sacagent4j/cli/Main.java`.

Add import:

```java
import io.github.seasonsolt.sacagent4j.session.JsonlSessionAnnotator;
```

Update `SessionCommand` subcommands:

```java
@CommandLine.Command(name = "session", description = "Inspect and fork JSONL session files.",
        subcommands = {
                SessionSummaryCommand.class,
                SessionTreeCommand.class,
                SessionListCommand.class,
                SessionNoteCommand.class,
                SessionForkCommand.class
        })
```

- [ ] **Step 4: Add the `SessionNoteCommand` class**

Add this nested class in `Main`, after `SessionListCommand`:

```java
@CommandLine.Command(name = "note", description = "Append a human note to a session entry.")
static final class SessionNoteCommand implements Callable<Integer> {
    @CommandLine.Spec
    CommandSpec spec;

    @CommandLine.Parameters(index = "0", description = "Session JSONL file")
    Path session;

    @CommandLine.Option(names = "--entry-id", required = true, description = "Entry id to attach the note to.")
    String entryId;

    @CommandLine.Option(names = "--title", required = true, description = "Short note title.")
    String title;

    @CommandLine.Option(names = "--body", required = true, description = "Note body.")
    String body;

    @Override
    public Integer call() throws Exception {
        String noteId = JsonlSessionAnnotator.note(new ObjectMapper(), session, entryId, title, body);
        spec.commandLine().getOut().println("note=" + noteId);
        return 0;
    }
}
```

- [ ] **Step 5: Run CLI tests**

Run:

```bash
mvn -Dtest=MainSessionCommandTest test
```

Expected:

```text
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 6: Commit note CLI**

Run:

```bash
git add src/main/java/io/github/seasonsolt/sacagent4j/cli/Main.java \
  src/test/java/io/github/seasonsolt/sacagent4j/cli/MainSessionCommandTest.java
git commit -m "Add session note command"
```

Expected:

```text
[main <hash>] Add session note command
```

---

### Task 5: Session Handoff Markdown

**Files:**
- Create: `src/main/java/io/github/seasonsolt/sacagent4j/session/SessionHandoff.java`
- Create: `src/test/java/io/github/seasonsolt/sacagent4j/session/SessionHandoffTest.java`

- [ ] **Step 1: Write failing handoff renderer test**

Create `src/test/java/io/github/seasonsolt/sacagent4j/session/SessionHandoffTest.java`:

```java
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
```

- [ ] **Step 2: Run handoff test to verify it fails**

Run:

```bash
mvn -Dtest=SessionHandoffTest test
```

Expected:

```text
[ERROR] COMPILATION ERROR
cannot find symbol
  symbol:   variable SessionHandoff
```

- [ ] **Step 3: Add `SessionHandoff`**

Create `src/main/java/io/github/seasonsolt/sacagent4j/session/SessionHandoff.java`:

```java
package io.github.seasonsolt.sacagent4j.session;

import java.util.List;

/** Renders a copyable markdown handoff pack for a session ancestry path. */
public final class SessionHandoff {
    private SessionHandoff() {}

    public static String render(SessionDocument document, String entryId) {
        String selectedEntryId = entryId == null || entryId.isBlank() ? document.leafId() : entryId;
        List<SessionEntry> ancestry = document.ancestryTo(selectedEntryId);
        SessionSummary summary = document.summary();
        StringBuilder out = new StringBuilder();
        out.append("# Session Handoff").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("- Session: ").append(document.sessionId()).append(System.lineSeparator());
        out.append("- Task: ").append(summary.task()).append(System.lineSeparator());
        out.append("- Status: ").append(summary.status()).append(System.lineSeparator());
        out.append("- Selected entry: ").append(selectedEntryId).append(System.lineSeparator());
        out.append("- Session file: ").append(document.path()).append(System.lineSeparator());
        out.append(System.lineSeparator());
        out.append("## Resume Command").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("```bash").append(System.lineSeparator());
        out.append("java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar --resume-session ")
                .append(document.path())
                .append(" --resume-entry ")
                .append(selectedEntryId)
                .append(System.lineSeparator());
        out.append("```").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("## Tree").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("```text").append(System.lineSeparator());
        out.append(document.tree().render()).append(System.lineSeparator());
        out.append("```").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("## Selected Ancestry").append(System.lineSeparator()).append(System.lineSeparator());
        for (SessionEntry entry : ancestry) {
            out.append("- ")
                    .append(entry.id())
                    .append(" ")
                    .append(entry.type())
                    .append(label(entry))
                    .append(System.lineSeparator());
        }
        return out.toString().stripTrailing();
    }

    private static String label(SessionEntry entry) {
        return switch (entry.type()) {
            case "started" -> " task=\"" + escape(entry.node().path("task").asText("")) + "\"";
            case "turn" -> " action=" + entry.node().path("action").path("type").asText("unknown");
            case "finished" -> " summary=\"" + escape(entry.node().path("summary").asText("")) + "\"";
            case "note" -> " note=\"" + escape(entry.node().path("title").asText("")) + "\"";
            default -> "";
        };
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
```

- [ ] **Step 4: Run handoff test to verify it passes**

Run:

```bash
mvn -Dtest=SessionHandoffTest test
```

Expected:

```text
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 5: Commit handoff renderer**

Run:

```bash
git add src/main/java/io/github/seasonsolt/sacagent4j/session/SessionHandoff.java \
  src/test/java/io/github/seasonsolt/sacagent4j/session/SessionHandoffTest.java
git commit -m "Add session handoff renderer"
```

Expected:

```text
[main <hash>] Add session handoff renderer
```

---

### Task 6: `session handoff` CLI

**Files:**
- Modify: `src/main/java/io/github/seasonsolt/sacagent4j/cli/Main.java`
- Modify: `src/test/java/io/github/seasonsolt/sacagent4j/cli/MainSessionCommandTest.java`

- [ ] **Step 1: Write failing CLI handoff test**

Add this test method to `src/test/java/io/github/seasonsolt/sacagent4j/cli/MainSessionCommandTest.java`:

```java
@Test
void printsSessionHandoffFromCli() throws Exception {
    Path sessionPath = writeSession();
    StringWriter output = new StringWriter();
    CommandLine commandLine = new CommandLine(new Main());
    commandLine.setOut(new PrintWriter(output, true));

    int exitCode = commandLine.execute("session", "handoff", sessionPath.toString());

    assertEquals(0, exitCode);
    assertTrue(output.toString().contains("# Session Handoff"));
    assertTrue(output.toString().contains("Task: fix tests"));
    assertTrue(output.toString().contains("## Resume Command"));
}
```

- [ ] **Step 2: Run CLI handoff test to verify it fails**

Run:

```bash
mvn -Dtest=MainSessionCommandTest#printsSessionHandoffFromCli test
```

Expected:

```text
Unmatched arguments from index 1: 'handoff', '<path>'
[ERROR] Failures:
```

- [ ] **Step 3: Add import and subcommand registration**

Modify `src/main/java/io/github/seasonsolt/sacagent4j/cli/Main.java`.

Add imports:

```java
import io.github.seasonsolt.sacagent4j.session.SessionHandoff;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
```

Update `SessionCommand` subcommands:

```java
@CommandLine.Command(name = "session", description = "Inspect and fork JSONL session files.",
        subcommands = {
                SessionSummaryCommand.class,
                SessionTreeCommand.class,
                SessionListCommand.class,
                SessionNoteCommand.class,
                SessionHandoffCommand.class,
                SessionForkCommand.class
        })
```

- [ ] **Step 4: Add `SessionHandoffCommand`**

Add this nested class in `Main`, after `SessionNoteCommand`:

```java
@CommandLine.Command(name = "handoff", description = "Render a markdown handoff pack for a session entry.")
static final class SessionHandoffCommand implements Callable<Integer> {
    @CommandLine.Spec
    CommandSpec spec;

    @CommandLine.Parameters(index = "0", description = "Session JSONL file")
    Path session;

    @CommandLine.Option(names = "--entry-id", description = "Entry id to hand off. Defaults to the active leaf.")
    String entryId;

    @CommandLine.Option(names = "--output", description = "Output markdown file. Defaults to stdout.")
    Path output;

    @Override
    public Integer call() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SessionDocument document = JsonlSessionReader.read(objectMapper, session);
        String selectedEntryId = entryId == null || entryId.isBlank() ? document.leafId() : entryId;
        String markdown = SessionHandoff.render(document, selectedEntryId);
        if (output == null) {
            spec.commandLine().getOut().println(markdown);
        } else {
            Files.createDirectories(output.toAbsolutePath().normalize().getParent());
            Files.writeString(output, markdown + System.lineSeparator(), StandardCharsets.UTF_8);
            spec.commandLine().getOut().println("handoff=" + output.toAbsolutePath().normalize());
        }
        return 0;
    }
}
```

- [ ] **Step 5: Run CLI tests**

Run:

```bash
mvn -Dtest=MainSessionCommandTest test
```

Expected:

```text
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 6: Commit handoff CLI**

Run:

```bash
git add src/main/java/io/github/seasonsolt/sacagent4j/cli/Main.java \
  src/test/java/io/github/seasonsolt/sacagent4j/cli/MainSessionCommandTest.java
git commit -m "Add session handoff command"
```

Expected:

```text
[main <hash>] Add session handoff command
```

---

### Task 7: Documentation And Native Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/ARCHITECTURE.md`
- Modify: `docs/CLASS_DIAGRAM.md`

- [ ] **Step 1: Update README session commands**

In `README.md`, extend the "Team memory: sessions" section with these command blocks:

````markdown
List sessions under a session root:

```bash
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar \
  session list .sac-agent4j/sessions
```

Attach a team note to an entry:

```bash
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar \
  session note .sac-agent4j/sessions/<workspace>/<session>.jsonl \
  --entry-id <entryId> \
  --title "handoff" \
  --body "Ready for teammate review."
```

Generate a markdown handoff pack:

```bash
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar \
  session handoff .sac-agent4j/sessions/<workspace>/<session>.jsonl \
  --entry-id <entryId>
```
````

- [ ] **Step 2: Update architecture notes**

In `docs/ARCHITECTURE.md`, add these rows to the current major abstractions table:

```markdown
| `JsonlSessionCatalog` | Lists session files under a root for team discovery. |
| `JsonlSessionAnnotator` | Appends human/team notes to existing session entries. |
| `SessionHandoff` | Renders a markdown handoff pack for a selected ancestry path. |
```

In the "Session files now have a small read/fork surface" block, add:

```text
session list <session-root>
  -> compact catalog of session files

session note <session.jsonl> --entry-id <id> --title <title> --body <body>
  -> appends a team note under a selected entry

session handoff <session.jsonl> [--entry-id <id>]
  -> markdown handoff pack with resume command, tree, and selected ancestry
```

- [ ] **Step 3: Update class diagram**

In `docs/CLASS_DIAGRAM.md`, add class declarations:

```mermaid
    class JsonlSessionCatalog {
      +list(objectMapper, root) List~SessionListItem~
    }

    class SessionListItem {
      +render() String
    }

    class JsonlSessionAnnotator {
      +note(objectMapper, sessionPath, entryId, title, body) String
    }

    class SessionHandoff {
      +render(document, entryId) String
    }
```

Add relationships:

```mermaid
    JsonlSessionCatalog --> JsonlSessionReader
    JsonlSessionCatalog --> SessionListItem
    JsonlSessionAnnotator --> JsonlSessionReader
    SessionHandoff --> SessionDocument
```

- [ ] **Step 4: Run full tests**

Run:

```bash
mvn test
```

Expected:

```text
[INFO] Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

The exact test count is expected to be `46` after this plan: current `40`, plus one catalog test, one note read-model test, one handoff renderer test, and three CLI tests.

- [ ] **Step 5: Build shaded jar**

Run:

```bash
mvn -DskipTests package
```

Expected:

```text
[INFO] Building jar: /Users/Thin/Source/git/seasonsolt/sac-agent4j/target/sac-agent4j-0.1.0-SNAPSHOT.jar
[INFO] BUILD SUCCESS
```

- [ ] **Step 6: Run jar smoke for list, note, handoff**

Run:

```bash
set -euo pipefail
smoke_dir="$(pwd)/target/session-productization-smoke-$(date +%Y%m%d%H%M%S)"
workspace="$smoke_dir/workspace"
mkdir -p "$workspace"
printf 'session productization smoke\n' > "$workspace/README.md"
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar --workspace "$workspace" --trajectory-dir "" --session-dir ".sessions" "inspect readme" <<'JSON' >/tmp/sac-agent4j-productization-first.out
{"type":"read_file","path":"README.md"}
{"type":"finish","summary":"first"}
JSON
session_file="$(find "$workspace/.sessions" -name '*.jsonl' -print | head -n 1)"
session_root="$(dirname "$(dirname "$session_file")")"
entry_id="$(java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar session tree "$session_file" | awk '/ action=read_file/{print $2; exit}')"
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar session list "$session_root"
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar session note "$session_file" --entry-id "$entry_id" --title "handoff" --body "Ready for teammate review."
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar session handoff "$session_file" --entry-id "$entry_id" | sed -n '1,24p'
```

Expected output contains:

```text
task="inspect readme"
note=<8-character-id>
# Session Handoff
Task: inspect readme
```

- [ ] **Step 7: Build native binary**

Run:

```bash
mvn -DskipTests -Pnative package
```

Expected:

```text
Build artifacts:
 /Users/Thin/Source/git/seasonsolt/sac-agent4j/target/sac-agent4j (executable)
[INFO] BUILD SUCCESS
```

- [ ] **Step 8: Run native smoke for list, note, handoff**

Run:

```bash
set -euo pipefail
smoke_dir="$(pwd)/target/native-session-productization-smoke-$(date +%Y%m%d%H%M%S)"
workspace="$smoke_dir/workspace"
mkdir -p "$workspace"
printf 'native session productization smoke\n' > "$workspace/README.md"
target/sac-agent4j --workspace "$workspace" --trajectory-dir "" --session-dir ".sessions" "inspect readme" <<'JSON' >/tmp/sac-agent4j-native-productization-first.out
{"type":"read_file","path":"README.md"}
{"type":"finish","summary":"first native"}
JSON
session_file="$(find "$workspace/.sessions" -name '*.jsonl' -print | head -n 1)"
session_root="$(dirname "$(dirname "$session_file")")"
entry_id="$(target/sac-agent4j session tree "$session_file" | awk '/ action=read_file/{print $2; exit}')"
target/sac-agent4j session list "$session_root"
target/sac-agent4j session note "$session_file" --entry-id "$entry_id" --title "handoff" --body "Ready for native teammate review."
target/sac-agent4j session handoff "$session_file" --entry-id "$entry_id" | sed -n '1,24p'
```

Expected output contains:

```text
task="inspect readme"
note=<8-character-id>
# Session Handoff
Task: inspect readme
```

- [ ] **Step 9: Run whitespace check**

Run:

```bash
git diff --check
```

Expected: no output and exit code `0`.

- [ ] **Step 10: Commit docs and verification-aligned changes**

Run:

```bash
git add README.md docs/ARCHITECTURE.md docs/CLASS_DIAGRAM.md
git commit -m "Document session productization workflow"
```

Expected:

```text
[main <hash>] Document session productization workflow
```

## Self-Review

- **Spec coverage:** The plan covers the requested staged improvement approach and maps directly to the product-direction ladder: visibility through `session list`, collaboration through `session note`, and handoff through `session handoff`.
- **Placeholder scan:** The plan contains concrete file paths, test code, implementation code, commands, expected failures, expected passing output, and commit commands.
- **Type consistency:** Names are consistent across tasks: `SessionListItem`, `JsonlSessionCatalog`, `JsonlSessionAnnotator`, `SessionHandoff`, `session list`, `session note`, and `session handoff`.
