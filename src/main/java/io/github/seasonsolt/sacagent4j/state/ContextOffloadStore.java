package io.github.seasonsolt.sacagent4j.state;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Stores bulky context outside the prompt while keeping a small handle visible. */
public final class ContextOffloadStore {
    public record Entry(String key, String title, String content) {}

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private int nextId = 1;

    public Observation offload(Action.OffloadContext action) {
        String key = action.key();
        if (key == null || key.isBlank()) {
            key = "ctx-" + nextId++;
        }
        if (!key.matches("[A-Za-z0-9._-]+")) {
            return Observation.failed("invalid context key: " + key);
        }
        String title = action.title() == null || action.title().isBlank() ? key : action.title();
        String content = action.content() == null ? "" : action.content();
        entries.put(key, new Entry(key, title, content));
        return Observation.ok("context offloaded: " + key + " (" + content.length() + " chars)");
    }

    public Observation read(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return Observation.failed("context not found: " + key);
        }
        return Observation.ok(entry.content());
    }

    public Map<String, String> summary() {
        Map<String, String> summary = new LinkedHashMap<>();
        entries.forEach((key, entry) -> summary.put(key, entry.title() + " (" + entry.content().length() + " chars)"));
        return summary;
    }
}
