package io.github.seasonsolt.sacagent4j.agent;

import io.github.seasonsolt.sacagent4j.state.AgentState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One agent execution lifetime: task, state, history, and step budget.
 *
 * <p>{@link AgentLoop} advances an {@code AgentRun}; it no longer needs to own
 * per-run mutable state directly.</p>
 */
public final class AgentRun {
    private final String task;
    private final int maxSteps;
    private final AgentState state;
    private final List<Turn> history = new ArrayList<>();
    private int nextStep;

    private AgentRun(String task, int maxSteps, AgentState state) {
        this(task, maxSteps, state, List.of(), 0);
    }

    private AgentRun(String task, int maxSteps, AgentState state, List<Turn> history, int nextStep) {
        if (maxSteps <= 0) {
            throw new IllegalArgumentException("maxSteps must be positive");
        }
        this.task = task == null ? "" : task;
        this.maxSteps = maxSteps;
        this.state = Objects.requireNonNull(state, "state");
        this.history.addAll(history == null ? List.of() : history);
        this.nextStep = nextStep;
    }

    public static AgentRun start(String task, int maxSteps) {
        return new AgentRun(task, maxSteps, new AgentState());
    }

    public static AgentRun resume(String task, int maxSteps, AgentState state, List<Turn> history) {
        return new AgentRun(task, maxSteps, state, history, 0);
    }

    public String task() {
        return task;
    }

    public int maxSteps() {
        return maxSteps;
    }

    public int nextStep() {
        return nextStep;
    }

    public AgentState state() {
        return state;
    }

    public List<Turn> history() {
        return List.copyOf(history);
    }

    public boolean hasStepsRemaining() {
        return nextStep < maxSteps;
    }

    public Turn record(Action action, Observation observation) {
        Turn turn = new Turn(action, observation);
        history.add(turn);
        nextStep++;
        return turn;
    }

    public AgentResult finished(String summary) {
        return AgentResult.finished(summary, history);
    }

    public AgentResult stopped() {
        return AgentResult.stopped(history);
    }
}
