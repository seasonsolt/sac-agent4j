package io.github.seasonsolt.sacagent4j.agent;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.plan.TodoStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Source of truth for model-visible action examples. */
public final class ActionCatalog {
    private ActionCatalog() {}

    public record Example(String type, Class<? extends Action> actionClass, Action action) {}

    public static List<Example> examples() {
        Map<Class<? extends Action>, String> runtimeTypes = runtimeTypesByClass();
        return List.of(
                example(runtimeTypes, Action.SetPlan.class, new Action.SetPlan(List.of("inspect failure", "patch bug", "run tests"))),
                example(runtimeTypes, Action.UpdateTodo.class, new Action.UpdateTodo(1, TodoStatus.in_progress)),
                example(runtimeTypes, Action.WriteVirtualFile.class, new Action.WriteVirtualFile("notes/root-cause.md", "...")),
                example(runtimeTypes, Action.ReadVirtualFile.class, new Action.ReadVirtualFile("notes/root-cause.md")),
                example(runtimeTypes, Action.OffloadContext.class, new Action.OffloadContext("test-output", "full failing test log", "...")),
                example(runtimeTypes, Action.ReadContext.class, new Action.ReadContext("test-output")),
                example(runtimeTypes, Action.ReadFile.class, new Action.ReadFile("README.md")),
                example(runtimeTypes, Action.Search.class, new Action.Search("TODO")),
                example(runtimeTypes, Action.Shell.class, new Action.Shell("mvn test")),
                example(runtimeTypes, Action.ApplyPatch.class, new Action.ApplyPatch("...unified diff...")),
                example(runtimeTypes, Action.RunTests.class, new Action.RunTests()),
                example(runtimeTypes, Action.Finish.class, new Action.Finish("..."))
        );
    }

    public static List<String> exampleJsonLines(ObjectMapper objectMapper) throws Exception {
        return examples().stream()
                .map(example -> writeExample(objectMapper, example.action()))
                .toList();
    }

    public static Map<Class<? extends Action>, String> runtimeTypesByClass() {
        JsonSubTypes subTypes = Action.class.getAnnotation(JsonSubTypes.class);
        if (subTypes == null) {
            throw new IllegalStateException("Action must declare @JsonSubTypes");
        }
        Map<Class<? extends Action>, String> types = new LinkedHashMap<>();
        for (JsonSubTypes.Type type : subTypes.value()) {
            Class<?> rawClass = type.value();
            if (!Action.class.isAssignableFrom(rawClass)) {
                throw new IllegalStateException("Json subtype is not an Action: " + rawClass.getName());
            }
            @SuppressWarnings("unchecked")
            Class<? extends Action> actionClass = (Class<? extends Action>) rawClass;
            types.put(actionClass, type.name());
        }
        return Map.copyOf(types);
    }

    private static Example example(Map<Class<? extends Action>, String> runtimeTypes, Class<? extends Action> actionClass, Action action) {
        String type = runtimeTypes.get(actionClass);
        if (type == null) {
            throw new IllegalStateException("No @JsonSubTypes entry for action example: " + actionClass.getName());
        }
        return new Example(type, actionClass, action);
    }

    private static String writeExample(ObjectMapper objectMapper, Action action) {
        try {
            return objectMapper.writeValueAsString(action);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize action example: " + action.getClass().getName(), e);
        }
    }
}
