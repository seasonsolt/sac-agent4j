package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.workspace.Workspace;

/** Runtime dependencies shared by local tools. */
public record ToolContext(Workspace workspace, String testCommand, ToolPolicy toolPolicy) {}
