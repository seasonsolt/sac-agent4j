package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;

/**
 * Backward-compatible facade over the tool registry pipeline.
 *
 * <p>The real tool architecture is now {@link ToolRegistry} +
 * {@link PermissionGate} + {@link ToolActionHandler}. This facade preserves the
 * simple API used by early tests and CLI wiring.</p>
 */
public final class ToolExecutor {
    private final ToolContext context;
    private final ToolActionHandler handler;

    public ToolExecutor(Workspace workspace, String testCommand) {
        this(workspace, testCommand, ToolPolicy.defaultPolicy());
    }

    public ToolExecutor(Workspace workspace, String testCommand, ToolPolicy toolPolicy) {
        this(new ToolContext(workspace, testCommand, toolPolicy),
                new ToolActionHandler(ToolRegistry.defaultRegistry(), new DefaultPermissionGate()));
    }

    public ToolExecutor(ToolContext context, ToolActionHandler handler) {
        this.context = context;
        this.handler = handler;
    }

    /** Dispatches one workspace tool action through registry lookup and permission checking. */
    public Observation execute(Action.ToolAction action) throws Exception {
        return handler.execute(action, context);
    }

    /** Reads a UTF-8 text file after workspace boundary validation. */
    public Observation readFile(String path) throws Exception {
        return execute(new Action.ReadFile(path));
    }

    /** Performs a tiny literal search without depending on ripgrep. */
    public Observation search(String query) throws Exception {
        return execute(new Action.Search(query));
    }

    /** Runs a shell command in the workspace and returns stdout/stderr as one observation. */
    public Observation shell(String command) throws Exception {
        return execute(new Action.Shell(command));
    }

    /** Applies a model-provided unified diff using git apply. */
    public Observation applyPatch(String patch) throws Exception {
        return execute(new Action.ApplyPatch(patch));
    }
}
