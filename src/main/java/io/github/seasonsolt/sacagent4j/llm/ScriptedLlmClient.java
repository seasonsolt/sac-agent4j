package io.github.seasonsolt.sacagent4j.llm;

import io.github.seasonsolt.sacagent4j.agent.Action;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/** Deterministic LLM fake used by tests and examples. */
public final class ScriptedLlmClient implements LlmClient {
    private final Queue<Action> actions;

    public ScriptedLlmClient(List<Action> actions) {
        this.actions = new ArrayDeque<>(actions);
    }

    @Override
    public Action nextAction(String context) {
        Action action = actions.poll();
        if (action == null) {
            return new Action.Finish("script exhausted");
        }
        return action;
    }
}
