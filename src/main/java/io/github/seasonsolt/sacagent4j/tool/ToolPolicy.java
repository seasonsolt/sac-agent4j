package io.github.seasonsolt.sacagent4j.tool;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Minimal safety policy for model-triggered tools.
 *
 * <p>The first version is intentionally conservative only around obvious foot
 * guns. It does not try to be a sandbox; it is a readable guardrail that can be
 * replaced by a stricter policy later.</p>
 */
public final class ToolPolicy {
    private final List<Pattern> deniedShellPatterns;

    private ToolPolicy(List<Pattern> deniedShellPatterns) {
        this.deniedShellPatterns = deniedShellPatterns;
    }

    /** Default policy: allow normal inspection/test commands, block obvious destructive commands. */
    public static ToolPolicy defaultPolicy() {
        return new ToolPolicy(List.of(
                Pattern.compile("(?i)(^|[;&|\\s])sudo(\\s|$)"),
                Pattern.compile("(?i)(^|[;&|\\s])rm\\s+(-[^\\s]*[rf][^\\s]*|-r|-f)"),
                Pattern.compile("(?i)(^|[;&|\\s])chmod\\s+-R(\\s|$)"),
                Pattern.compile("(?i)(^|[;&|\\s])chown\\s+-R(\\s|$)"),
                Pattern.compile("(?i)(^|[;&|\\s])mkfs(\\.|\\s|$)"),
                Pattern.compile("(?i)(^|[;&|\\s])dd\\s+"),
                Pattern.compile("(?i)(^|[;&|\\s])(shutdown|reboot|poweroff)(\\s|$)"),
                Pattern.compile("(?i)(^|[;&|\\s])git\\s+push(\\s|$)"),
                Pattern.compile("(?i)(curl|wget)[^|;&]*\\|\\s*(sh|bash)"),
                Pattern.compile("\":\\(\\)\\s*\\{\\s*:\\|:\\s*&\\s*}\\s*;\\s*:\"")
        ));
    }

    /** No-op policy for tests or deliberately unsafe local experiments. */
    public static ToolPolicy allowAll() {
        return new ToolPolicy(List.of());
    }

    public PermissionDecision checkShell(String command) {
        if (command == null || command.isBlank()) {
            return PermissionDecision.deny("empty shell command");
        }
        for (Pattern pattern : deniedShellPatterns) {
            if (pattern.matcher(command).find()) {
                return PermissionDecision.deny("shell command rejected by policy: " + command);
            }
        }
        return PermissionDecision.allow();
    }
}
