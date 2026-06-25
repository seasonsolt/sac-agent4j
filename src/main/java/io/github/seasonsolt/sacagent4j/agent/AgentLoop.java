package io.github.seasonsolt.sacagent4j.agent;

import io.github.seasonsolt.sacagent4j.llm.LlmClient;
import io.github.seasonsolt.sacagent4j.plan.TodoList;
import io.github.seasonsolt.sacagent4j.tool.ToolExecutor;
import io.github.seasonsolt.sacagent4j.trajectory.NoopTrajectoryLogger;
import io.github.seasonsolt.sacagent4j.trajectory.TrajectoryLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * The minimal SWE-agent control loop.
 *
 * <p>The loop is deliberately boring: build context, ask the LLM for one
 * {@link Action}, execute it, append the {@link Observation}, repeat until
 * {@code finish} or {@code maxSteps}. This is the core mechanism that larger
 * coding agents hide behind planners, event streams, or framework callbacks.</p>
 */
public final class AgentLoop {
    private final LlmClient llmClient;
    private final ToolExecutor toolExecutor;
    private final ContextBuilder contextBuilder;
    private final int maxSteps;
    private final TrajectoryLogger trajectoryLogger;
    private final List<Turn> history = new ArrayList<>();
    private final TodoList todoList = new TodoList();

    public AgentLoop(LlmClient llmClient, ToolExecutor toolExecutor, ContextBuilder contextBuilder, int maxSteps) {
        this(llmClient, toolExecutor, contextBuilder, maxSteps, new NoopTrajectoryLogger());
    }

    public AgentLoop(LlmClient llmClient, ToolExecutor toolExecutor, ContextBuilder contextBuilder, int maxSteps, TrajectoryLogger trajectoryLogger) {
        if (maxSteps <= 0) {
            throw new IllegalArgumentException("maxSteps must be positive");
        }
        this.llmClient = llmClient;
        this.toolExecutor = toolExecutor;
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
        trajectoryLogger.started(task, maxSteps);
        try {
            for (int step = 0; step < maxSteps; step++) {
                String context = contextBuilder.build(task, history, todoList);
                Action action = llmClient.nextAction(context);
                if (action instanceof Action.Finish finish) {
                    AgentResult result = AgentResult.finished(finish.summary(), history);
                    trajectoryLogger.finished(result.finished(), result.summary(), result.history().size());
                    return result;
                }
                Observation observation = execute(action);
                history.add(new Turn(action, observation));
                trajectoryLogger.turn(step, action, observation);
            }
            AgentResult result = AgentResult.stopped(history);
            trajectoryLogger.finished(result.finished(), result.summary(), result.history().size());
            return result;
        } finally {
            trajectoryLogger.close();
        }
    }

    public List<io.github.seasonsolt.sacagent4j.plan.TodoItem> plan() {
        return todoList.items();
    }

    private Observation execute(Action action) throws Exception {
        if (action instanceof Action.SetPlan setPlan) {
            return todoList.setPlan(setPlan);
        }
        if (action instanceof Action.UpdateTodo updateTodo) {
            return todoList.updateTodo(updateTodo);
        }
        return toolExecutor.execute(action);
    }
}
