package io.github.seasonsolt.sacagent4j.tool;

/** Result of checking whether a tool action is allowed to run. */
public record PolicyDecision(boolean allowed, String reason) {
    public static PolicyDecision allow() {
        return new PolicyDecision(true, "allowed");
    }

    public static PolicyDecision deny(String reason) {
        return new PolicyDecision(false, reason);
    }
}
