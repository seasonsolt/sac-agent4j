package io.github.seasonsolt.sacagent4j.agent.context;

import io.github.seasonsolt.sacagent4j.agent.AgentRun;

/** Builds the prompt for the next model call from the current run. */
public interface ContextManager {
    Prompt buildPrompt(AgentRun run) throws Exception;
}
