package io.github.seasonsolt.sacagent4j.trajectory;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;

/** Records the agent trajectory for debugging and replay. */
public interface TrajectoryLogger extends AutoCloseable {
    default void started(String task, int maxSteps) throws Exception {}

    default void turn(int step, Action action, Observation observation) throws Exception {}

    default void finished(boolean finished, String summary, int turns) throws Exception {}

    @Override
    default void close() throws Exception {}
}
