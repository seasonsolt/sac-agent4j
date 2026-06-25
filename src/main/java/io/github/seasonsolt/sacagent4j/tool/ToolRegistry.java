package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;

import java.util.ArrayList;
import java.util.List;

/** Registry of available workspace tools. */
public final class ToolRegistry {
    private final List<Tool> tools;

    public ToolRegistry(List<Tool> tools) {
        this.tools = List.copyOf(tools);
    }

    public static ToolRegistry defaultRegistry() {
        return new ToolRegistry(List.of(
                new ReadFileTool(),
                new SearchTool(),
                new ShellTool(),
                new ApplyPatchTool(),
                new RunTestsTool()
        ));
    }

    public Tool find(Action.ToolAction action) {
        for (Tool tool : tools) {
            if (tool.supports(action)) {
                return tool;
            }
        }
        throw new IllegalArgumentException("no tool registered for action: " + action.getClass().getSimpleName());
    }

    public List<String> names() {
        List<String> names = new ArrayList<>();
        for (Tool tool : tools) {
            names.add(tool.name());
        }
        return List.copyOf(names);
    }
}
