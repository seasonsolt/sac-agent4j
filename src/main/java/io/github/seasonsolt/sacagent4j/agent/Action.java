package io.github.seasonsolt.sacagent4j.agent;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.seasonsolt.sacagent4j.plan.TodoStatus;

import java.util.List;

/**
 * The only language the model is allowed to use when it wants the agent to act.
 *
 * <p>Keeping this protocol small is intentional: sac-agent4j is meant to show
 * the core SWE-agent loop before adding richer framework abstractions.</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Action.SetPlan.class, name = "set_plan"),
        @JsonSubTypes.Type(value = Action.UpdateTodo.class, name = "update_todo"),
        @JsonSubTypes.Type(value = Action.ReadFile.class, name = "read_file"),
        @JsonSubTypes.Type(value = Action.Search.class, name = "search"),
        @JsonSubTypes.Type(value = Action.Shell.class, name = "shell"),
        @JsonSubTypes.Type(value = Action.ApplyPatch.class, name = "apply_patch"),
        @JsonSubTypes.Type(value = Action.RunTests.class, name = "run_tests"),
        @JsonSubTypes.Type(value = Action.Finish.class, name = "finish")
})
public sealed interface Action permits Action.SetPlan, Action.UpdateTodo, Action.ReadFile, Action.Search, Action.Shell, Action.ApplyPatch, Action.RunTests, Action.Finish {
    /** Replace the current plan with a short ordered todo list. */
    record SetPlan(List<String> items) implements Action {}

    /** Update a 1-based todo id to pending, in_progress, completed, or cancelled. */
    record UpdateTodo(int id, TodoStatus status) implements Action {}

    /** Read one file from the workspace. */
    record ReadFile(String path) implements Action {}

    /** Search plain-text files in the workspace for a literal query. */
    record Search(String query) implements Action {}

    /** Run a shell command in the workspace. This is powerful and should later gain policy controls. */
    record Shell(String command) implements Action {}

    /** Apply a unified diff through git apply. */
    record ApplyPatch(String patch) implements Action {}

    /** Run the configured verification command, usually the project's test command. */
    record RunTests() implements Action {}

    /** Stop the loop and return a human-readable summary. */
    record Finish(String summary) implements Action {}
}
