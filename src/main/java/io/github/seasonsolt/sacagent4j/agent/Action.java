package io.github.seasonsolt.sacagent4j.agent;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.seasonsolt.sacagent4j.plan.TodoStatus;

import java.util.List;

/**
 * The only language the model is allowed to use when it wants the agent to act.
 *
 * <p>The protocol is still one JSON object per turn, but its Java type shape is
 * split by intent: control actions stop the loop, state actions mutate/read the
 * agent's inner world, and tool actions touch the workspace.</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Action.SetPlan.class, name = "set_plan"),
        @JsonSubTypes.Type(value = Action.UpdateTodo.class, name = "update_todo"),
        @JsonSubTypes.Type(value = Action.WriteVirtualFile.class, name = "write_virtual_file"),
        @JsonSubTypes.Type(value = Action.ReadVirtualFile.class, name = "read_virtual_file"),
        @JsonSubTypes.Type(value = Action.OffloadContext.class, name = "offload_context"),
        @JsonSubTypes.Type(value = Action.ReadContext.class, name = "read_context"),
        @JsonSubTypes.Type(value = Action.ReadFile.class, name = "read_file"),
        @JsonSubTypes.Type(value = Action.Search.class, name = "search"),
        @JsonSubTypes.Type(value = Action.Shell.class, name = "shell"),
        @JsonSubTypes.Type(value = Action.ApplyPatch.class, name = "apply_patch"),
        @JsonSubTypes.Type(value = Action.RunTests.class, name = "run_tests"),
        @JsonSubTypes.Type(value = Action.Finish.class, name = "finish")
})
public sealed interface Action permits Action.ControlAction, Action.StateAction, Action.ToolAction {
    /** Actions that change loop control rather than state or tools. */
    sealed interface ControlAction extends Action permits Finish {}

    /** Actions that operate only on {@link io.github.seasonsolt.sacagent4j.state.AgentState}. */
    sealed interface StateAction extends Action permits SetPlan, UpdateTodo, WriteVirtualFile, ReadVirtualFile, OffloadContext, ReadContext {}

    /** Actions that touch the real workspace through the tool boundary. */
    sealed interface ToolAction extends Action permits ReadFile, Search, Shell, ApplyPatch, RunTests {}

    /** Replace the current plan with a short ordered todo list. */
    record SetPlan(List<String> items) implements StateAction {}

    /** Update a 1-based todo id to pending, in_progress, completed, or cancelled. */
    record UpdateTodo(int id, TodoStatus status) implements StateAction {}

    /** Write an in-memory note/draft without touching the real workspace. */
    record WriteVirtualFile(String path, String content) implements StateAction {}

    /** Read an in-memory note/draft from agent state. */
    record ReadVirtualFile(String path) implements StateAction {}

    /** Store bulky context behind a small key so future prompts can stay compact. */
    record OffloadContext(String key, String title, String content) implements StateAction {}

    /** Retrieve content previously stored with offload_context. */
    record ReadContext(String key) implements StateAction {}

    /** Read one file from the workspace. */
    record ReadFile(String path) implements ToolAction {}

    /** Search plain-text files in the workspace for a literal query. */
    record Search(String query) implements ToolAction {}

    /** Run a shell command in the workspace. */
    record Shell(String command) implements ToolAction {}

    /** Apply a unified diff through git apply. */
    record ApplyPatch(String patch) implements ToolAction {}

    /** Run the configured verification command, usually the project's test command. */
    record RunTests() implements ToolAction {}

    /** Stop the loop and return a human-readable summary. */
    record Finish(String summary) implements ControlAction {}
}
