package io.github.seasonsolt.sacagent4j.session;

import java.util.List;

/** Renders a copyable markdown handoff pack for a session ancestry path. */
public final class SessionHandoff {
    private SessionHandoff() {}

    public static String render(SessionDocument document, String entryId) {
        String selectedEntryId = entryId == null || entryId.isBlank() ? document.leafId() : entryId;
        List<SessionEntry> ancestry = document.ancestryTo(selectedEntryId);
        HandoffStatus status = statusFor(ancestry);

        StringBuilder out = new StringBuilder();
        out.append("# Session Handoff").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("- Session: ").append(escapeInline(document.sessionId())).append(System.lineSeparator());
        out.append("- Task: ").append(escapeInline(status.task())).append(System.lineSeparator());
        out.append("- Status: ").append(escapeInline(status.status())).append(System.lineSeparator());
        out.append("- Selected entry: ").append(escapeInline(selectedEntryId)).append(System.lineSeparator());
        out.append("- Session file: ").append(escapeInline(document.path().toString())).append(System.lineSeparator());
        out.append(System.lineSeparator());
        out.append("## Resume Command").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("```bash").append(System.lineSeparator());
        out.append("java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar --resume-session ")
                .append(shellQuote(document.path().toString()))
                .append(" --resume-entry ")
                .append(shellQuote(selectedEntryId))
                .append(System.lineSeparator());
        out.append("```").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("## Tree").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("```text").append(System.lineSeparator());
        out.append(document.tree().render()).append(System.lineSeparator());
        out.append("```").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("## Selected Ancestry").append(System.lineSeparator()).append(System.lineSeparator());
        for (SessionEntry entry : ancestry) {
            out.append("- ")
                    .append(escapeInline(entry.id()))
                    .append(" ")
                    .append(escapeInline(entry.type()))
                    .append(label(entry))
                    .append(System.lineSeparator());
        }
        return out.toString().stripTrailing();
    }

    private static HandoffStatus statusFor(List<SessionEntry> ancestry) {
        String task = "";
        String status = "open";
        for (SessionEntry entry : ancestry) {
            if (entry.type().equals("started")) {
                task = entry.node().path("task").asText("");
            } else if (entry.type().equals("finished")) {
                status = entry.node().path("finished").asBoolean(false) ? "finished" : "stopped";
            }
        }
        return new HandoffStatus(task, status);
    }

    private static String label(SessionEntry entry) {
        return switch (entry.type()) {
            case "started" -> " task=\"" + escapeLabel(entry.node().path("task").asText("")) + "\""
                    + " maxSteps=" + entry.node().path("maxSteps").asInt();
            case "turn" -> " step=" + entry.node().path("step").asInt()
                    + " action=" + escapeInline(entry.node().path("action").path("type").asText("unknown"));
            case "finished" -> " finished=" + entry.node().path("finished").asBoolean(false)
                    + " summary=\"" + escapeLabel(entry.node().path("summary").asText("")) + "\""
                    + " turns=" + entry.node().path("turns").asInt();
            case "note" -> " title=\"" + escapeLabel(entry.node().path("title").asText("")) + "\""
                    + " bodyChars=" + entry.node().path("body").asText("").length();
            default -> "";
        };
    }

    private static String escapeInline(String value) {
        return escapeLabel(value);
    }

    private static String escapeLabel(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private record HandoffStatus(String task, String status) {}
}
