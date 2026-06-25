package io.github.seasonsolt.sacagent4j.agent;

import io.github.seasonsolt.sacagent4j.tool.ToolExecutor;

/**
 * Routes model actions to the correct execution boundary.
 *
 * <p>The loop should not know every concrete action. This dispatcher keeps the
 * taxonomy visible: state actions mutate/read {@link AgentRun#state()}, tool
 * actions go through the workspace boundary, and control actions are handled by
 * {@link AgentLoop} before dispatch.</p>
 */
public final class ActionDispatcher {
    private final StateActionHandler stateActionHandler;
    private final ToolExecutor toolExecutor;

    public ActionDispatcher(StateActionHandler stateActionHandler, ToolExecutor toolExecutor) {
        this.stateActionHandler = stateActionHandler;
        this.toolExecutor = toolExecutor;
    }

    public Observation dispatch(Action action, AgentRun run) throws Exception {
        return switch (action) {
            case Action.ControlAction ignored -> Observation.failed("control actions are handled by AgentLoop");
            case Action.StateAction stateAction -> stateActionHandler.execute(stateAction, run.state());
            case Action.ToolAction toolAction -> toolExecutor.execute(toolAction);
        };
    }
}
