package io.github.seasonsolt.sacagent4j.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;
import io.github.seasonsolt.sacagent4j.session.JsonlSessionReader;
import io.github.seasonsolt.sacagent4j.session.JsonlSessionRecorder;
import io.github.seasonsolt.sacagent4j.session.SessionDocument;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MainSessionCommandTest {
    @TempDir
    Path tempDir;

    @Test
    void printsSessionSummary() throws Exception {
        Path sessionPath = writeSession();
        StringWriter output = new StringWriter();
        CommandLine commandLine = new CommandLine(new Main());
        commandLine.setOut(new PrintWriter(output, true));

        int exitCode = commandLine.execute("session", "summary", sessionPath.toString());

        assertEquals(0, exitCode);
        assertTrue(output.toString().contains("task=fix tests"));
        assertTrue(output.toString().contains("status=finished"));
        assertTrue(output.toString().contains("actions=read_file:1"));
    }

    @Test
    void createsSessionForkFromCli() throws Exception {
        Path sessionPath = writeSession();
        Path outputDir = tempDir.resolve("forks");
        StringWriter output = new StringWriter();
        CommandLine commandLine = new CommandLine(new Main());
        commandLine.setOut(new PrintWriter(output, true));

        int exitCode = commandLine.execute("session", "fork", sessionPath.toString(), "--output-dir", outputDir.toString());

        assertEquals(0, exitCode);
        String pathLine = output.toString().trim();
        assertTrue(pathLine.startsWith("forkedSession="));
        Path forkPath = Path.of(pathLine.substring("forkedSession=".length()));
        assertTrue(Files.exists(forkPath));

        SessionDocument fork = JsonlSessionReader.read(new ObjectMapper(), forkPath);
        assertTrue(fork.header().has("forkedFrom"));
    }

    @Test
    void printsSessionTree() throws Exception {
        Path sessionPath = writeSession();
        StringWriter output = new StringWriter();
        CommandLine commandLine = new CommandLine(new Main());
        commandLine.setOut(new PrintWriter(output, true));

        int exitCode = commandLine.execute("session", "tree", sessionPath.toString());

        assertEquals(0, exitCode);
        assertTrue(output.toString().contains("session="));
        assertTrue(output.toString().contains("leaf="));
        assertTrue(output.toString().contains("started"));
        assertTrue(output.toString().contains("turn"));
        assertTrue(output.toString().contains("action=read_file"));
    }

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

    @Test
    void resumesSessionFromCliAndAppendsContinuation() throws Exception {
        Path sessionPath = writeSession();
        ObjectMapper objectMapper = new ObjectMapper();
        String oldLeafId = JsonlSessionReader.read(objectMapper, sessionPath).leafId();
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setIn(new ByteArrayInputStream("{\"type\":\"finish\",\"summary\":\"resumed\"}\n".getBytes(StandardCharsets.UTF_8)));
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        try {
            int exitCode = new CommandLine(new Main()).execute(
                    "--workspace", tempDir.toString(),
                    "--trajectory-dir", "",
                    "--resume-session", sessionPath.toString()
            );

            assertEquals(0, exitCode);
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }

        SessionDocument resumed = JsonlSessionReader.read(objectMapper, sessionPath);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("summary=resumed"));
        assertEquals("started", resumed.entries().get(resumed.entries().size() - 2).type());
        assertEquals(oldLeafId, resumed.entries().get(resumed.entries().size() - 2).parentId());
        assertEquals("finished", resumed.entries().get(resumed.entries().size() - 1).type());
    }

    private Path writeSession() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonlSessionRecorder recorder = new JsonlSessionRecorder(objectMapper, new Workspace(tempDir), ".sac-agent4j/sessions");
        recorder.started("fix tests", 4);
        recorder.turn(0, new Action.ReadFile("README.md"), Observation.ok("readme"));
        recorder.finished(true, "done", 1);
        recorder.close();
        return recorder.path().orElseThrow();
    }
}
