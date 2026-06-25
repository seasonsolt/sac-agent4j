package io.github.seasonsolt.sacagent4j.state;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;
import io.github.seasonsolt.sacagent4j.plan.TodoItem;
import io.github.seasonsolt.sacagent4j.plan.TodoList;

import java.util.List;
import java.util.Map;

/**
 * Mutable state for a single agent run.
 *
 * <p>This is sac-agent4j's minimal counterpart to deepagents-style state: it
 * centralizes the plan, virtual files, and context offloads so the prompt can
 * stay small while the agent still has durable handles for intermediate work.</p>
 */
public final class AgentState {
    private final TodoList todoList = new TodoList();
    private final VirtualFileSystem virtualFileSystem = new VirtualFileSystem();
    private final ContextOffloadStore contextOffloads = new ContextOffloadStore();

    public Observation setPlan(Action.SetPlan action) {
        return todoList.setPlan(action);
    }

    public Observation updateTodo(Action.UpdateTodo action) {
        return todoList.updateTodo(action);
    }

    public Observation writeVirtualFile(Action.WriteVirtualFile action) {
        return virtualFileSystem.write(action.path(), action.content());
    }

    public Observation readVirtualFile(Action.ReadVirtualFile action) {
        return virtualFileSystem.read(action.path());
    }

    public Observation offloadContext(Action.OffloadContext action) {
        return contextOffloads.offload(action);
    }

    public Observation readContext(Action.ReadContext action) {
        return contextOffloads.read(action.key());
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
