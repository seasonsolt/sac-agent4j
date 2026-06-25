package io.github.seasonsolt.sacagent4j.plan;

/** Small state machine for a task in the agent's current plan. */
public enum TodoStatus {
    pending,
    in_progress,
    completed,
    cancelled
}
