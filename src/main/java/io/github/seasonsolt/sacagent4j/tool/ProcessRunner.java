package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Observation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

        // Drain stdout/stderr on dedicated threads so a child that writes more than
        // the OS pipe buffer (~64KB) cannot block on write and stall waitFor. Reading
        // only after waitFor would deadlock such commands into a false timeout.
        StreamGobbler outGobbler = new StreamGobbler(process.getInputStream());
        StreamGobbler errGobbler = new StreamGobbler(process.getErrorStream());
        Thread outThread = startDaemon(outGobbler, "process-stdout");
        Thread errThread = startDaemon(errGobbler, "process-stderr");

        if (stdin != null) {
            try (OutputStream processStdin = process.getOutputStream()) {
                processStdin.write(stdin.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            process.getOutputStream().close();
        }

        boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!completed) {
            killTree(process);
            return Observation.failed(timeoutMessage);
        }

        outThread.join();
        errThread.join();
        return new Observation(process.exitValue(),
                ToolSupport.truncate("stdout:\n" + outGobbler.text() + "\nstderr:\n" + errGobbler.text()));
    }

    private static Thread startDaemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /** Kills the process and any descendants, since destroying only the shell leaves its children running. */
    private static void killTree(Process process) throws InterruptedException {
        List<ProcessHandle> descendants = process.descendants().collect(Collectors.toList());
        process.destroyForcibly();
        descendants.forEach(ProcessHandle::destroyForcibly);
        process.waitFor(5, TimeUnit.SECONDS);
    }

    private static final class StreamGobbler implements Runnable {
        private final InputStream stream;
        private volatile String text = "";

        StreamGobbler(InputStream stream) {
            this.stream = stream;
        }

        @Override
        public void run() {
            try (InputStream in = stream) {
                text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                // Pipe closed early (e.g. the process was killed); keep whatever was read.
            }
        }

        String text() {
            return text;
        }
    }
}
