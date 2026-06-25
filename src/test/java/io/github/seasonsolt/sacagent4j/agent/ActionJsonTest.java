package io.github.seasonsolt.sacagent4j.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.plan.TodoStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ActionJsonTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesReadFileAction() throws Exception {
        Action action = objectMapper.readValue("{\"type\":\"read_file\",\"path\":\"README.md\"}", Action.class);
        Action.ReadFile readFile = assertInstanceOf(Action.ReadFile.class, action);
        assertEquals("README.md", readFile.path());
    }

    @Test
    void parsesPlanActions() throws Exception {
        Action setPlanAction = objectMapper.readValue("{\"type\":\"set_plan\",\"items\":[\"inspect\",\"patch\"]}", Action.class);
        Action.SetPlan setPlan = assertInstanceOf(Action.SetPlan.class, setPlanAction);
        assertEquals(2, setPlan.items().size());

        Action updateAction = objectMapper.readValue("{\"type\":\"update_todo\",\"id\":1,\"status\":\"in_progress\"}", Action.class);
        Action.UpdateTodo updateTodo = assertInstanceOf(Action.UpdateTodo.class, updateAction);
        assertEquals(1, updateTodo.id());
        assertEquals(TodoStatus.in_progress, updateTodo.status());
    }

    @Test
    void parsesAgentStateActions() throws Exception {
        Action writeVirtual = objectMapper.readValue("{\"type\":\"write_virtual_file\",\"path\":\"notes/a.md\",\"content\":\"hello\"}", Action.class);
        Action.WriteVirtualFile writeVirtualFile = assertInstanceOf(Action.WriteVirtualFile.class, writeVirtual);
        assertEquals("notes/a.md", writeVirtualFile.path());

        Action offload = objectMapper.readValue("{\"type\":\"offload_context\",\"key\":\"log\",\"title\":\"test log\",\"content\":\"long\"}", Action.class);
        Action.OffloadContext offloadContext = assertInstanceOf(Action.OffloadContext.class, offload);
        assertEquals("log", offloadContext.key());

        Action readContext = objectMapper.readValue("{\"type\":\"read_context\",\"key\":\"log\"}", Action.class);
        assertInstanceOf(Action.ReadContext.class, readContext);
    }

    @Test
    void writesFinishAction() throws Exception {
        String json = objectMapper.writeValueAsString(new Action.Finish("done"));
        assertEquals("{\"type\":\"finish\",\"summary\":\"done\"}", json);
    }
}
