package io.github.seasonsolt.sacagent4j.plan;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal in-memory plan/todo list for one agent run.
 *
 * <p>The model updates this list with {@code set_plan} and {@code update_todo}
 * actions. The current list is then printed into the next prompt so planning is
 * explicit instead of hidden in model context.</p>
 */
public final class TodoList {
    private final List<TodoItem> items = new ArrayList<>();

    public Observation setPlan(Action.SetPlan action) {
        items.clear();
        int id = 1;
        for (String item : action.items()) {
            if (item != null && !item.isBlank()) {
                items.add(new TodoItem(id++, item, TodoStatus.pending));
            }
        }
        return Observation.ok("plan set with " + items.size() + " todo(s)");
    }

    public Observation updateTodo(Action.UpdateTodo action) {
        int index = action.id() - 1;
        if (index < 0 || index >= items.size()) {
            return Observation.failed("todo not found: " + action.id());
        }
        TodoItem current = items.get(index);
        TodoItem updated = new TodoItem(current.id(), current.content(), action.status());
        items.set(index, updated);
        return Observation.ok("todo " + action.id() + " -> " + action.status());
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
