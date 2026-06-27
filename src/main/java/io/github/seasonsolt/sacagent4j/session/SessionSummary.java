package io.github.seasonsolt.sacagent4j.session;

import java.util.Map;
import java.util.stream.Collectors;

/** Compact human-facing summary of a session file. */
public record SessionSummary(
        String sessionId,
        String cwd,
        String task,
        String status,
        boolean finished,
        String finalSummary,
        int turns,
        String leafId,
        Map<String, Integer> actionCounts
) {
    public String render() {
        String actions = actionCounts.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(", "));
        return String.join(System.lineSeparator(),
                "session=" + sessionId,
                "cwd=" + cwd,
                "task=" + task,
                "status=" + status,
                "finished=" + finished,
                "summary=" + finalSummary,
                "turns=" + turns,
                "leaf=" + leafId,
                "actions=" + actions
        );
    }
}
