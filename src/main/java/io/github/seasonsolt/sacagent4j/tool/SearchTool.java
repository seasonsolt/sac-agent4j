package io.github.seasonsolt.sacagent4j.tool;

import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Observation;

/** Performs tiny literal search over workspace text files. */
public final class SearchTool implements Tool {
    @Override public String name() { return "search"; }
    @Override public RiskLevel risk() { return RiskLevel.LOW; }
    @Override public boolean supports(Action.ToolAction action) { return action instanceof Action.Search; }

    @Override
    public Observation execute(Action.ToolAction action, ToolContext context) throws Exception {
        Action.Search search = (Action.Search) action;
        return Observation.ok(ToolSupport.literalSearch(context.workspace(), search.query()));
    }
}
