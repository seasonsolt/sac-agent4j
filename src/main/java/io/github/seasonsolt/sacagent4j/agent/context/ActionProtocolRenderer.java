package io.github.seasonsolt.sacagent4j.agent.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.agent.ActionCatalog;

/** Renders the JSON action protocol visible to the model. */
public final class ActionProtocolRenderer {
    private final ObjectMapper objectMapper;

    public ActionProtocolRenderer() {
        this(new ObjectMapper());
    }

    public ActionProtocolRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String render() throws Exception {
        StringBuilder out = new StringBuilder();
        out.append("Actions:\n");
        for (String example : ActionCatalog.exampleJsonLines(objectMapper)) {
            out.append(example).append('\n');
        }
        return out.toString();
    }
}
