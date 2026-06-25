package io.github.seasonsolt.sacagent4j.agent;

public record Observation(int exitCode, String output) {
    public static Observation ok(String output) {
        return new Observation(0, output);
    }

    public static Observation failed(String output) {
        return new Observation(1, output);
    }
}
