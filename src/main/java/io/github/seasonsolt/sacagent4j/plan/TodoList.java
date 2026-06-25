package io.github.seasonsolt.sacagent4j.plan;

import io.github.seasonsolt.sacagent4j.agent.Observation;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal in-memory plan/todo list for one agent run.
 *
 * <p>The list is a small domain object: callers provide plan items or status
 * updates, and this object enforces 1-based todo ids and rendering.</p>
 */
public final class TodoList {
    private final List<TodoItem> items = new ArrayList<>();

    public Observation setPlan(List<String> planItems) {
        items.clear();
        int id = 1;
        for (String item : planItems) {
            if (item != null && !item.isBlank()) {
                items.add(new TodoItem(id++, item, TodoStatus.pending));
            }
        }
        return Observation.ok("plan set with " + items.size() + " todo(s)");
    }

    public Observation updateTodo(int id, TodoStatus status) {
        int index = id - 1;
        if (index < 0 || index >= items.size()) {
            return Observation.failed("todo not found: " + id);
        }
        TodoItem current = items.get(index);
        TodoItem updated = new TodoItem(current.id(), current.content(), status);
        items.set(index, updated);
        return Observation.ok("todo " + id + " -> " + status);
    }

    public List<TodoItem> items() {
        return List.copyOf(items);
    }

    public String render() {
        if (items.isEmpty()) {
            return "No plan yet. Use set_plan before complex edits.\n";
        }
        StringBuilder out = new StringBuilder();
        for (TodoItem item : items) {
            out.append(item.id())
                    .append(". [")
                    .append(item.status())
                    .append("] ")
                    .append(item.content())
                    .append('\n');
        }
        return out.toString();
    }
}
