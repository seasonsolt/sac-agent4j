package io.github.seasonsolt.sacagent4j.agent.context;

/** Renders the JSON action protocol visible to the model. */
public final class ActionProtocolRenderer {
    public String render() {
        StringBuilder out = new StringBuilder();
        out.append("Actions:\n");
        out.append("{\"type\":\"set_plan\",\"items\":[\"inspect failure\",\"patch bug\",\"run tests\"]}\n");
        out.append("{\"type\":\"update_todo\",\"id\":1,\"status\":\"in_progress\"}\n");
        out.append("{\"type\":\"write_virtual_file\",\"path\":\"notes/root-cause.md\",\"content\":\"...\"}\n");
        out.append("{\"type\":\"read_virtual_file\",\"path\":\"notes/root-cause.md\"}\n");
        out.append("{\"type\":\"offload_context\",\"key\":\"test-output\",\"title\":\"full failing test log\",\"content\":\"...\"}\n");
        out.append("{\"type\":\"read_context\",\"key\":\"test-output\"}\n");
        out.append("{\"type\":\"read_file\",\"path\":\"README.md\"}\n");
        out.append("{\"type\":\"search\",\"query\":\"TODO\"}\n");
        out.append("{\"type\":\"shell\",\"command\":\"mvn test\"}\n");
        out.append("{\"type\":\"apply_patch\",\"patch\":\"...unified diff...\"}\n");
        out.append("{\"type\":\"run_tests\"}\n");
        out.append("{\"type\":\"finish\",\"summary\":\"...\"}\n");
        return out.toString();
    }
}
