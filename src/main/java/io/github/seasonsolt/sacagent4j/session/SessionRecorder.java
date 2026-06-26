package io.github.seasonsolt.sacagent4j.session;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;

import java.nio.file.Path;
import java.util.Optional;

/** Records a Pi-style append-only session tree for human resume/fork workflows. */
public interface SessionRecorder extends AutoCloseable {
    default void started(String task, int maxSteps) throws Exception {}

    default void turn(int step, Action action, Observation observation) throws Exception {}

    default void finished(boolean finished, String summary, int turns) throws Exception {}

    default Optional<Path> path() {
        return Optional.empty();
    }

    @Override
    default void close() throws Exception {}
}
