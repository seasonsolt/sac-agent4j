package io.github.seasonsolt.sacagent4j.agent;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.plan.TodoStatus;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionJsonTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesReadFileAction() throws Exception {
        Action action = objectMapper.readValue("{\"type\":\"read_file\",\"path\":\"README.md\"}", Action.class);
        Action.ReadFile readFile = assertInstanceOf(Action.ReadFile.class, action);
        assertTrue(action instanceof Action.ToolAction);
        assertEquals("README.md", readFile.path());
    }

    @Test
    void parsesPlanActions() throws Exception {
        Action setPlanAction = objectMapper.readValue("{\"type\":\"set_plan\",\"items\":[\"inspect\",\"patch\"]}", Action.class);
        Action.SetPlan setPlan = assertInstanceOf(Action.SetPlan.class, setPlanAction);
        assertTrue(setPlanAction instanceof Action.StateAction);
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
        assertTrue(new Action.Finish("done") instanceof Action.ControlAction);
        assertEquals("{\"type\":\"finish\",\"summary\":\"done\"}", json);
    }

    @Test
    void actionCatalogExamplesCoverEveryJsonSubtype() throws Exception {
        Map<Class<? extends Action>, String> runtimeTypes = runtimeTypesByClass();
        Map<Class<? extends Action>, String> catalogTypes = ActionCatalog.examples().stream()
                .collect(Collectors.toMap(
                        ActionCatalog.Example::actionClass,
                        ActionCatalog.Example::type,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        assertEquals(runtimeTypes, catalogTypes);
        for (ActionCatalog.Example example : ActionCatalog.examples()) {
            String json = objectMapper.writeValueAsString(example.action());
            Action parsed = objectMapper.readValue(json, Action.class);
            assertEquals(example.actionClass(), parsed.getClass());
            assertTrue(json.contains("\"type\":\"" + example.type() + "\""));
        }
    }

    private static Map<Class<? extends Action>, String> runtimeTypesByClass() {
        JsonSubTypes subTypes = Action.class.getAnnotation(JsonSubTypes.class);
        Map<Class<? extends Action>, String> result = new LinkedHashMap<>();
        for (JsonSubTypes.Type type : subTypes.value()) {
            @SuppressWarnings("unchecked")
            Class<? extends Action> actionClass = (Class<? extends Action>) type.value();
            result.put(actionClass, type.name());
        }
        return result;
    }
}
