package io.github.seasonsolt.sacagent4j.agent;

import io.github.seasonsolt.sacagent4j.llm.LlmClient;
import io.github.seasonsolt.sacagent4j.plan.TodoItem;
import io.github.seasonsolt.sacagent4j.state.AgentState;
import io.github.seasonsolt.sacagent4j.tool.ToolExecutor;
import io.github.seasonsolt.sacagent4j.trajectory.NoopTrajectoryLogger;
import io.github.seasonsolt.sacagent4j.trajectory.TrajectoryLogger;

import java.util.List;

/**
 * The minimal SWE-agent control loop.
 *
 * <p>The loop now owns only time/control flow: build context, ask the LLM for
 * one {@link Action}, dispatch non-terminal actions, record the turn, and stop
 * on {@code finish} or step exhaustion.</p>
 */
public final class AgentLoop {
    private final LlmClient llmClient;
    private final ActionDispatcher actionDispatcher;
    private final ContextBuilder contextBuilder;
    private final int maxSteps;
    private final TrajectoryLogger trajectoryLogger;
    private AgentRun lastRun;

    public AgentLoop(LlmClient llmClient, ToolExecutor toolExecutor, ContextBuilder contextBuilder, int maxSteps) {
        this(llmClient, new ActionDispatcher(new StateActionHandler(), toolExecutor), contextBuilder, maxSteps, new NoopTrajectoryLogger());
    }

    public AgentLoop(LlmClient llmClient, ToolExecutor toolExecutor, ContextBuilder contextBuilder, int maxSteps, TrajectoryLogger trajectoryLogger) {
        this(llmClient, new ActionDispatcher(new StateActionHandler(), toolExecutor), contextBuilder, maxSteps, trajectoryLogger);
    }

    public AgentLoop(LlmClient llmClient, ActionDispatcher actionDispatcher, ContextBuilder contextBuilder, int maxSteps, TrajectoryLogger trajectoryLogger) {
        if (maxSteps <= 0) {
            throw new IllegalArgumentException("maxSteps must be positive");
        }
        this.llmClient = llmClient;
        this.actionDispatcher = actionDispatcher;
        this.contextBuilder = contextBuilder;
        this.maxSteps = maxSteps;
        this.trajectoryLogger = trajectoryLogger;
    }

    /**
     * Runs the agent on one task.
     *
     * @param task natural-language task from the user
     * @return final status and immutable turn history
     */
    public AgentResult run(String task) throws Exception {
        AgentRun run = AgentRun.start(task, maxSteps);
        lastRun = run;
        trajectoryLogger.started(task, maxSteps);
        try {
            while (run.hasStepsRemaining()) {
                String context = contextBuilder.build(run);
                Action action = llmClient.nextAction(context);
                if (action instanceof Action.Finish finish) {
                    AgentResult result = run.finished(finish.summary());
                    trajectoryLogger.finished(result.finished(), result.summary(), result.history().size());
                    return result;
                }
                int step = run.nextStep();
                Observation observation = actionDispatcher.dispatch(action, run);
                Turn turn = run.record(action, observation);
                trajectoryLogger.turn(step, turn.action(), turn.observation());
            }
            AgentResult result = run.stopped();
            trajectoryLogger.finished(result.finished(), result.summary(), result.history().size());
            return result;
        } finally {
            trajectoryLogger.close();
        }
    }

    public AgentState state() {
        return lastRun == null ? new AgentState() : lastRun.state();
    }

    public List<TodoItem> plan() {
        return state().plan();
    }
}
