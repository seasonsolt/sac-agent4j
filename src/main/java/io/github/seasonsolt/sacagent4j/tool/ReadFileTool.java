package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;

import java.nio.file.Files;
import java.nio.file.Path;

/** Reads UTF-8 files inside the workspace boundary. */
public final class ReadFileTool implements Tool {
    @Override public String name() { return "read_file"; }
    @Override public RiskLevel risk() { return RiskLevel.LOW; }
    @Override public boolean supports(Action.ToolAction action) { return action instanceof Action.ReadFile; }

    @Override
    public Observation execute(Action.ToolAction action, ToolContext context) throws Exception {
        Action.ReadFile readFile = (Action.ReadFile) action;
        Path resolved = context.workspace().resolveExisting(readFile.path());
        return Observation.ok(ToolSupport.truncate(Files.readString(resolved)));
    }
}
