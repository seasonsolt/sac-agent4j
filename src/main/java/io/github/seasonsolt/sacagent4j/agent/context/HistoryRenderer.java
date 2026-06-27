package io.github.seasonsolt.sacagent4j.agent.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.seasonsolt.sacagent4j.agent.Action;
import io.github.seasonsolt.sacagent4j.agent.Turn;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Renders prior turns while preserving context-offload compactness. */
public final class HistoryRenderer {
    private static final Pattern COMPACT_CONTENT_CHARS = Pattern.compile("\\((\\d+) chars\\)");

    private final ObjectMapper objectMapper;

    public HistoryRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String render(List<Turn> history) throws Exception {
        StringBuilder out = new StringBuilder("History:\n");
        for (Turn turn : history) {
            out.append(renderTurn(turn)).append('\n');
        }
        return out.toString();
    }

    private String renderTurn(Turn turn) throws Exception {
        if (turn.action() instanceof Action.OffloadContext offloadContext) {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode action = root.putObject("action");
            action.put("type", "offload_context");
            action.put("key", offloadContext.key());
            action.put("title", offloadContext.title());
            action.put("contentChars", contentChars(turn, offloadContext));
            root.set("observation", objectMapper.valueToTree(turn.observation()));
            return objectMapper.writeValueAsString(root);
        }
        return objectMapper.writeValueAsString(turn);
    }

    private int contentChars(Turn turn, Action.OffloadContext offloadContext) {
        if (offloadContext.content() != null) {
            return offloadContext.content().length();
        }
        String output = turn.observation().output();
        if (output == null) {
            return 0;
        }
        Matcher matcher = COMPACT_CONTENT_CHARS.matcher(output);
        if (!matcher.find()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }
}
