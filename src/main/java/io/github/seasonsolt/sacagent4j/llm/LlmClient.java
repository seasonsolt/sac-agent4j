package io.github.seasonsolt.sacagent4j.llm;

import io.github.seasonsolt.sacagent4j.agent.Action;

/**
 * Minimal seam between the agent loop and any model provider.
 *
 * <p>Implementations may call a real HTTP API, read JSON from stdin, or return
 * scripted actions for tests. The loop only depends on this single method.</p>
 */
public interface LlmClient {
    /** Returns exactly one next action for the provided context. */
    Action nextAction(String context) throws Exception;
}
