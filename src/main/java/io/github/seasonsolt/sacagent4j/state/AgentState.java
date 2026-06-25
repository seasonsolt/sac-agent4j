package io.github.seasonsolt.sacagent4j.state;

import io.github.seasonsolt.sacagent4j.plan.TodoItem;
import io.github.seasonsolt.sacagent4j.plan.TodoList;

import java.util.List;
import java.util.Map;

/**
 * Mutable inner world for a single agent run.
 *
 * <p>State owns the data structures. Action semantics live in
 * {@link io.github.seasonsolt.sacagent4j.agent.StateActionHandler}, keeping this
 * class closer to a state model than a dispatcher.</p>
 */
public final class AgentState {
    private final TodoList todoList = new TodoList();
    private final VirtualFileSystem virtualFileSystem = new VirtualFileSystem();
    private final ContextOffloadStore contextOffloads = new ContextOffloadStore();

    public TodoList todoList() {
        return todoList;
    }

    public VirtualFileSystem virtualFileSystem() {
        return virtualFileSystem;
    }

    public ContextOffloadStore contextOffloads() {
        return contextOffloads;
    }

    public List<TodoItem> plan() {
        return todoList.items();
    }

    public String renderPlan() {
        return todoList.render();
    }

    public Map<String, Integer> virtualFileSummary() {
        return virtualFileSystem.summary();
    }

    public Map<String, String> contextSummary() {
        return contextOffloads.summary();
    }

    public String renderStateSummary() {
        StringBuilder out = new StringBuilder();
        out.append("Current plan:\n").append(renderPlan()).append('\n');
        out.append("Virtual files:\n");
        if (virtualFileSummary().isEmpty()) {
            out.append("No virtual files. Use write_virtual_file for notes or drafts.\n");
        } else {
            virtualFileSummary().forEach((path, chars) -> out.append("- ").append(path).append(" (").append(chars).append(" chars)\n"));
        }
        out.append("Context offloads:\n");
        if (contextSummary().isEmpty()) {
            out.append("No offloaded context. Use offload_context for bulky snippets.\n");
        } else {
            contextSummary().forEach((key, summary) -> out.append("- ").append(key).append(": ").append(summary).append('\n'));
        }
        return out.toString();
    }
}
