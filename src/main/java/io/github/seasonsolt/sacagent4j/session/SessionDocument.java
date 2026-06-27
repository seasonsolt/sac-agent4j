package io.github.seasonsolt.sacagent4j.session;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Parsed read model for one JSONL session file. */
public record SessionDocument(Path path, ObjectNode header, List<SessionEntry> entries) {
    public SessionDocument {
        entries = List.copyOf(entries);
    }

    public String sessionId() {
        return header.path("id").asText();
    }

    public String leafId() {
        for (int index = entries.size() - 1; index >= 0; index--) {
            SessionEntry entry = entries.get(index);
            if (!entry.type().equals("forked")) {
                return entry.id();
            }
        }
        return "";
    }

    public List<SessionEntry> ancestryTo(String entryId) {
        Map<String, SessionEntry> byId = entries.stream()
                .collect(Collectors.toMap(SessionEntry::id, Function.identity(), (left, right) -> right, LinkedHashMap::new));
        SessionEntry cursor = byId.get(entryId);
        if (cursor == null) {
            throw new IllegalArgumentException("session entry not found: " + entryId);
        }

        List<SessionEntry> ancestry = new ArrayList<>();
        while (cursor != null) {
            ancestry.add(0, cursor);
            String parentId = cursor.parentId();
            cursor = parentId == null ? null : byId.get(parentId);
        }
        return List.copyOf(ancestry);
    }

    public SessionTree tree() {
        return new SessionTree(sessionId(), leafId(), entries);
    }

    public SessionSummary summary() {
        String task = "";
        String status = "open";
        boolean finished = false;
        String finalSummary = "";
        int turns = 0;
        Map<String, Integer> actionCounts = new LinkedHashMap<>();

        for (SessionEntry entry : entries) {
            if (entry.type().equals("started")) {
                task = entry.node().path("task").asText("");
            } else if (entry.type().equals("turn")) {
                turns++;
                String actionType = entry.node().path("action").path("type").asText("unknown");
                actionCounts.merge(actionType, 1, Integer::sum);
            } else if (entry.type().equals("finished")) {
                finished = entry.node().path("finished").asBoolean(false);
                finalSummary = entry.node().path("summary").asText("");
                status = finished ? "finished" : "stopped";
            }
        }

        return new SessionSummary(
                sessionId(),
                header.path("cwd").asText(""),
                task,
                status,
                finished,
                finalSummary,
                turns,
                leafId(),
                Collections.unmodifiableMap(new LinkedHashMap<>(actionCounts))
        );
    }
}
