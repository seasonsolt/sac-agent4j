package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Executes the small tool set exposed to the model.
 *
 * <p>This class is intentionally local and synchronous. It is the right place to
 * add policy checks later, for example shell command allow/deny lists or separate
 * approval for destructive tools.</p>
 */
public final class ToolExecutor {
    private static final int MAX_OUTPUT_CHARS = 12_000;

    private final Workspace workspace;
    private final String testCommand;

    public ToolExecutor(Workspace workspace, String testCommand) {
        this.workspace = workspace;
        this.testCommand = testCommand;
    }

    /** Dispatches one model action to its concrete tool implementation. */
    public Observation execute(Action action) throws Exception {
        return switch (action) {
            case Action.ReadFile readFile -> readFile(readFile.path());
            case Action.Search search -> search(search.query());
            case Action.Shell shell -> shell(shell.command());
            case Action.ApplyPatch applyPatch -> applyPatch(applyPatch.patch());
            case Action.RunTests ignored -> shell(testCommand);
            case Action.Finish ignored -> Observation.ok("");
        };
    }

    /** Reads a UTF-8 text file after workspace boundary validation. */
    public Observation readFile(String path) throws IOException {
        Path resolved = workspace.resolveExisting(path);
        return Observation.ok(truncate(Files.readString(resolved)));
    }

    /** Performs a tiny literal search without depending on ripgrep. */
    public Observation search(String query) throws IOException {
        StringBuilder out = new StringBuilder();
        try (Stream<Path> paths = Files.walk(workspace.root())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> !isIgnored(path))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> appendMatches(path, query, out));
        }
        if (out.isEmpty()) {
            return Observation.ok("no matches");
        }
        return Observation.ok(truncate(out.toString()));
    }

    /** Runs a shell command in the workspace and returns stdout/stderr as one observation. */
    public Observation shell(String command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("sh", "-lc", command)
                .directory(workspace.root().toFile())
                .redirectErrorStream(false)
                .start();
        boolean completed = process.waitFor(Duration.ofSeconds(60).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroyForcibly();
            return Observation.failed("command timed out after 60s: " + command);
        }
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Observation(process.exitValue(), truncate("stdout:\n" + stdout + "\nstderr:\n" + stderr));
    }

    /** Applies a model-provided unified diff using git apply. */
    public Observation applyPatch(String patch) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("git", "apply", "--whitespace=nowarn", "-")
                .directory(workspace.root().toFile())
                .redirectErrorStream(false)
                .start();
        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(patch.getBytes(StandardCharsets.UTF_8));
        }
        int exit = process.waitFor();
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Observation(exit, truncate("stdout:\n" + stdout + "\nstderr:\n" + stderr));
    }

    private void appendMatches(Path path, String query, StringBuilder out) {
        try {
            int lineNo = 1;
            for (String line : Files.readAllLines(path)) {
                if (line.contains(query)) {
                    Path rel = workspace.root().relativize(path);
                    out.append(rel).append(':').append(lineNo).append(':').append(line).append('\n');
                }
                lineNo++;
            }
        } catch (Exception ignored) {
            // Binary or unreadable files are irrelevant for the minimal search tool.
        }
    }

    private boolean isIgnored(Path path) {
        String p = workspace.root().relativize(path).toString();
        return p.startsWith(".git/") || p.startsWith("target/") || p.contains("/target/");
    }

    private static String truncate(String input) {
        if (input.length() <= MAX_OUTPUT_CHARS) {
            return input;
        }
        return input.substring(0, MAX_OUTPUT_CHARS) + "\n...[truncated]";
    }
}
