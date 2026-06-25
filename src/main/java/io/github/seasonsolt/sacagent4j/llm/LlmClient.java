package io.github.seasonsolt.sacagent4j.llm;

import io.github.seasonsolt.sacagent4j.agent.Action;

public interface LlmClient {
    Action nextAction(String context) throws Exception;
}
