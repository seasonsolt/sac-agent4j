package io.github.seasonsolt.sacagent4j.session;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Human-facing tree view of a JSONL session's parent/child entries. */
public record SessionTree(String sessionId, String leafId, List<SessionEntry> entries) {
    public SessionTree {
        entries = List.copyOf(entries);
    }

    public String render() {
        Map<String, List<SessionEntry>> childrenByParent = new LinkedHashMap<>();
        List<SessionEntry> roots = new ArrayList<>();
        for (SessionEntry entry : entries) {
            if (entry.parentId() == null) {
                roots.add(entry);
            } else {
                childrenByParent.computeIfAbsent(entry.parentId(), ignored -> new ArrayList<>()).add(entry);
            }
        }

        StringBuilder out = new StringBuilder();
        out.append("session=").append(sessionId).append(System.lineSeparator());
        out.append("leaf=").append(leafId).append(System.lineSeparator());
        for (SessionEntry root : roots) {
            renderEntry(root, "", childrenByParent, out);
        }
        return out.toString().stripTrailing();
    }

    private void renderEntry(SessionEntry entry, String indent, Map<String, List<SessionEntry>> childrenByParent, StringBuilder out) {
        out.append(indent)
                .append("- ")
                .append(entry.id())
                .append(' ')
                .append(entry.type())
                .append(label(entry))
                .append(System.lineSeparator());
        for (SessionEntry child : childrenByParent.getOrDefault(entry.id(), List.of())) {
            renderEntry(child, indent + "  ", childrenByParent, out);
        }
    }

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

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
