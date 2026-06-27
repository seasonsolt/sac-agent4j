package io.github.seasonsolt.sacagent4j.session;

import java.nio.file.Path;
import java.time.Instant;

/** One compact row in a human-facing session catalog. */
public record SessionListItem(
        Path path,
        String sessionId,
        Instant timestamp,
        String task,
        String status,
        String finalSummary,
        int turns,
        String leafId
) {
    public String render() {
        return "timestamp=" + timestamp
                + " status=" + status
                + " turns=" + turns
                + " task=\"" + escape(task) + "\""
                + " summary=\"" + escape(finalSummary) + "\""
                + " leaf=" + leafId
                + " session=" + sessionId
                + " path=" + path;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
