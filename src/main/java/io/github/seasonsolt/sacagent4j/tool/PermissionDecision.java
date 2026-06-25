package io.github.seasonsolt.sacagent4j.tool;

/** Permission decision made before executing a tool action. */
public record PermissionDecision(boolean allowed, String reason) {
    public static PermissionDecision allow() {
        return new PermissionDecision(true, "allowed");
    }

    public static PermissionDecision deny(String reason) {
        return new PermissionDecision(false, reason);
    }
}
