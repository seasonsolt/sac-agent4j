package io.github.seasonsolt.sacagent4j.agent.context;

import io.github.seasonsolt.sacagent4j.agent.AgentRun;
import io.github.seasonsolt.sacagent4j.agent.ContextBuilder;

/** Factory helpers for bridging older string builders into the new context seam. */
public final class ContextManagers {
    private ContextManagers() {}

    public static ContextManager fromContextBuilder(ContextBuilder contextBuilder) {
        return new ContextManager() {
            @Override
            public Prompt buildPrompt(AgentRun run) throws Exception {
                return new Prompt(contextBuilder.build(run), "", "", "", "");
            }
        };
    }
}
