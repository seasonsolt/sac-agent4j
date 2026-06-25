package io.github.seasonsolt.sacagent4j.agent.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.AgentRun;
import io.github.seasonsolt.sacagent4j.agent.Observation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultContextManagerTest {
    @Test
    void buildsStructuredPromptSections() throws Exception {
        AgentRun run = AgentRun.start("fix the bug", 4);
        run.state().todoList().setPlan(java.util.List.of("inspect", "patch"));

        Prompt prompt = new DefaultContextManager(new ObjectMapper()).buildPrompt(run);

        assertTrue(prompt.system().contains("sac-agent4j"));
        assertTrue(prompt.actionProtocol().contains("\"type\":\"run_tests\""));
        assertTrue(prompt.task().contains("fix the bug"));
        assertTrue(prompt.agentState().contains("inspect"));
        assertTrue(prompt.history().contains("History:"));
        assertTrue(prompt.render().contains("Return exactly one JSON object"));
    }

    @Test
    void rendersOffloadedContextCompactlyInHistory() throws Exception {
        AgentRun run = AgentRun.start("compact history", 4);
        run.record(new Action.OffloadContext("log", "full log", "very large content"), Observation.ok("stored"));

        String rendered = new DefaultContextManager(new ObjectMapper()).buildPrompt(run).render();

        assertTrue(rendered.contains("contentChars"));
        assertTrue(rendered.contains("full log"));
        assertTrue(!rendered.contains("very large content"));
    }
}
