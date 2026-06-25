package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Observation;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Shared process execution adapter for command-backed tools. */
public final class ProcessRunner {
    private ProcessRunner() {}

    public static Observation runShell(Path workingDirectory, String command, Duration timeout) throws Exception {
        return run(List.of("sh", "-lc", command), workingDirectory, timeout, null,
                "command timed out after " + timeout.toSeconds() + "s: " + command);
    }

    public static Observation run(Path workingDirectory, List<String> command, Duration timeout, String stdin) throws Exception {
        return run(command, workingDirectory, timeout, stdin,
                "process timed out after " + timeout.toSeconds() + "s: " + String.join(" ", command));
    }

    private static Observation run(List<String> command, Path workingDirectory, Duration timeout, String stdin, String timeoutMessage) throws Exception {
        Process process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(false)
                .start();

        if (stdin != null) {
            try (OutputStream processStdin = process.getOutputStream()) {
                processStdin.write(stdin.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            process.getOutputStream().close();
        }

        boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            return Observation.failed(timeoutMessage);
        }

        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Observation(process.exitValue(), ToolSupport.truncate("stdout:\n" + stdout + "\nstderr:\n" + stderr));
    }
}
