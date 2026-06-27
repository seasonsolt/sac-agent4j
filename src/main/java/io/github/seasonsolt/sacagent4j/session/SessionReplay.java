package io.github.seasonsolt.sacagent4j.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.AgentRun;
import io.github.seasonsolt.sacagent4j.agent.Observation;
import io.github.seasonsolt.sacagent4j.agent.StateActionHandler;
import io.github.seasonsolt.sacagent4j.agent.Turn;
import io.github.seasonsolt.sacagent4j.state.AgentState;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Rebuilds a resumable run view from a JSONL session ancestry path. */
public record SessionReplay(String task, String leafId, List<Turn> history, AgentState state) {
    public SessionReplay {
        history = List.copyOf(history);
    }

    public static SessionReplay from(ObjectMapper objectMapper, Path path, String entryId) throws Exception {
        SessionDocument document = JsonlSessionReader.read(objectMapper, path);
        String leafId = entryId == null || entryId.isBlank() ? document.leafId() : entryId;
        List<SessionEntry> ancestry = document.ancestryTo(leafId);
        String task = "";
        List<Turn> history = new ArrayList<>();
        AgentState state = new AgentState();
        StateActionHandler stateActionHandler = new StateActionHandler();

        for (SessionEntry entry : ancestry) {
            if (entry.type().equals("started")) {
                task = entry.node().path("task").asText("");
            } else if (entry.type().equals("turn")) {
                Turn turn = turnFromEntry(objectMapper, entry);
                history.add(turn);
                replayStateAction(turn.action(), state, stateActionHandler);
            }
        }

        return new SessionReplay(task, leafId, history, state);
    }

    public AgentRun toAgentRun(int maxSteps) {
        return AgentRun.resume(task, maxSteps, state, history);
    }

    private static Turn turnFromEntry(ObjectMapper objectMapper, SessionEntry entry) throws Exception {
        Action action = actionFromSession(objectMapper, entry.node().path("action"));
        Observation observation = objectMapper.treeToValue(entry.node().path("observation"), Observation.class);
        return new Turn(action, observation);
    }

    private static Action actionFromSession(ObjectMapper objectMapper, JsonNode actionNode) throws Exception {
        if (actionNode.path("type").asText("").equals("offload_context") && !actionNode.has("content")) {
            return new Action.OffloadContext(
                    actionNode.path("key").asText(null),
                    actionNode.path("title").asText(null),
                    null
            );
        }
        return objectMapper.treeToValue(actionNode, Action.class);
    }

    private static void replayStateAction(Action action, AgentState state, StateActionHandler stateActionHandler) {
        if (action instanceof Action.OffloadContext offloadContext && offloadContext.content() == null) {
            return;
        }
        if (action instanceof Action.StateAction stateAction) {
            stateActionHandler.execute(stateAction, state);
        }
    }
}
