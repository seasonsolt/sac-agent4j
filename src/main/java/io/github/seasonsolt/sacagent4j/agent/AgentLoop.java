package io.github.seasonsolt.sacagent4j.agent;

import io.github.seasonsolt.sacagent4j.llm.LlmClient;
import io.github.seasonsolt.sacagent4j.tool.ToolExecutor;

import java.util.ArrayList;
import java.util.List;

public final class AgentLoop {
    private final LlmClient llmClient;
    private final ToolExecutor toolExecutor;
    private final ContextBuilder contextBuilder;
    private final int maxSteps;
    private final List<Turn> history = new ArrayList<>();

    public AgentLoop(LlmClient llmClient, ToolExecutor toolExecutor, ContextBuilder contextBuilder, int maxSteps) {
        if (maxSteps <= 0) {
            throw new IllegalArgumentException("maxSteps must be positive");
        }
        this.llmClient = llmClient;
        this.toolExecutor = toolExecutor;
        this.contextBuilder = contextBuilder;
        this.maxSteps = maxSteps;
    }

    public AgentResult run(String task) throws Exception {
        for (int step = 0; step < maxSteps; step++) {
            String context = contextBuilder.build(task, history);
            Action action = llmClient.nextAction(context);
            if (action instanceof Action.Finish finish) {
                return AgentResult.finished(finish.summary(), history);
            }
            Observation observation = toolExecutor.execute(action);
            history.add(new Turn(action, observation));
        }
        return AgentResult.stopped(history);
    }
}
