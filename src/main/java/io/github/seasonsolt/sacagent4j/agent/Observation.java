package io.github.seasonsolt.sacagent4j.agent;

/**
 * Result returned by a tool.
 *
 * <p>The shape mirrors a process exit code plus text output so all tools can be
 * fed back into the next LLM turn in a uniform way.</p>
 */
public record Observation(int exitCode, String output) {
    public static Observation ok(String output) {
        return new Observation(0, output);
    }

    public static Observation failed(String output) {
        return new Observation(1, output);
    }
}
