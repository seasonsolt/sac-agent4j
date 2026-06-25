package io.github.seasonsolt.sacagent4j.state;

import io.github.seasonsolt.sacagent4j.agent.Observation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory virtual filesystem inspired by deepagents-style agent state.
 *
 * <p>These files are notes and intermediate artifacts for the agent. They are
 * not written to the real workspace unless a later tool/action decides to do so.</p>
 */
public final class VirtualFileSystem {
    private final Map<String, String> files = new LinkedHashMap<>();

    public Observation write(String path, String content) {
        String normalized = normalize(path);
        if (normalized == null) {
            return Observation.failed("invalid virtual file path: " + path);
        }
        files.put(normalized, content == null ? "" : content);
        return Observation.ok("virtual file written: " + normalized + " (" + files.get(normalized).length() + " chars)");
    }

    public Observation read(String path) {
        String normalized = normalize(path);
        if (normalized == null || !files.containsKey(normalized)) {
            return Observation.failed("virtual file not found: " + path);
        }
        return Observation.ok(files.get(normalized));
    }

    public Map<String, Integer> summary() {
        Map<String, Integer> summary = new LinkedHashMap<>();
        files.forEach((path, content) -> summary.put(path, content.length()));
        return summary;
    }

    private String normalize(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank() || normalized.contains("..") || normalized.contains("//")) {
            return null;
        }
        return normalized;
    }
}
