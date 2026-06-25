package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.workspace.Workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

final class ToolSupport {
    static final int MAX_OUTPUT_CHARS = 12_000;

    private ToolSupport() {}

    static String truncate(String input) {
        if (input.length() <= MAX_OUTPUT_CHARS) {
            return input;
        }
        return input.substring(0, MAX_OUTPUT_CHARS) + "\n...[truncated]";
    }

    static String literalSearch(Workspace workspace, String query) throws IOException {
        StringBuilder out = new StringBuilder();
        try (Stream<Path> paths = Files.walk(workspace.root())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> !isIgnored(workspace, path))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> appendMatches(workspace, path, query, out));
        }
        return out.isEmpty() ? "no matches" : truncate(out.toString());
    }

    private static void appendMatches(Workspace workspace, Path path, String query, StringBuilder out) {
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

    private static boolean isIgnored(Workspace workspace, Path path) {
        String p = workspace.root().relativize(path).toString();
        return p.startsWith(".git/") || p.startsWith("target/") || p.contains("/target/") || p.startsWith(".sac-agent4j/");
    }
}
