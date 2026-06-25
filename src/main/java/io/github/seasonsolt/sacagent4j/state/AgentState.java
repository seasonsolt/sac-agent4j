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
 * class as a pure state model rather than a dispatcher or renderer.</p>
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

    public Map<String, Integer> virtualFileSummary() {
        return virtualFileSystem.summary();
    }

    public Map<String, String> contextSummary() {
        return contextOffloads.summary();
    }
}
