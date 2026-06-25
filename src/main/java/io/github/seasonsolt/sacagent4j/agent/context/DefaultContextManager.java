package io.github.seasonsolt.sacagent4j.agent.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.agent.AgentRun;

/** Default inspectable prompt pipeline for the minimal agent. */
public final class DefaultContextManager implements ContextManager {
    private final SystemPromptRenderer systemPromptRenderer;
    private final ActionProtocolRenderer actionProtocolRenderer;
    private final TaskRenderer taskRenderer;
    private final AgentStateRenderer agentStateRenderer;
    private final HistoryRenderer historyRenderer;

    public DefaultContextManager(ObjectMapper objectMapper) {
        this(new SystemPromptRenderer(), new ActionProtocolRenderer(objectMapper), new TaskRenderer(),
                new AgentStateRenderer(), new HistoryRenderer(objectMapper));
    }

    public DefaultContextManager(SystemPromptRenderer systemPromptRenderer,
                                 ActionProtocolRenderer actionProtocolRenderer,
                                 TaskRenderer taskRenderer,
                                 AgentStateRenderer agentStateRenderer,
                                 HistoryRenderer historyRenderer) {
        this.systemPromptRenderer = systemPromptRenderer;
        this.actionProtocolRenderer = actionProtocolRenderer;
        this.taskRenderer = taskRenderer;
        this.agentStateRenderer = agentStateRenderer;
        this.historyRenderer = historyRenderer;
    }

    @Override
    public Prompt buildPrompt(AgentRun run) throws Exception {
        return new Prompt(
                systemPromptRenderer.render(),
                actionProtocolRenderer.render(),
                taskRenderer.render(run.task()),
                agentStateRenderer.render(run.state()),
                historyRenderer.render(run.history())
        );
    }
}
