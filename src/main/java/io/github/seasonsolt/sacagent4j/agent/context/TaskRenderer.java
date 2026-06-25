package io.github.seasonsolt.sacagent4j.agent.context;

/** Renders the user's task as a distinct prompt section. */
public final class TaskRenderer {
    public String render(String task) {
        return "Task:\n" + task + "\n";
    }
}
