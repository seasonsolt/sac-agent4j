package io.github.seasonsolt.sacagent4j.agent;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The only language the model is allowed to use when it wants the agent to act.
 *
 * <p>Keeping this protocol small is intentional: sac-agent4j is meant to show
 * the core SWE-agent loop before adding richer framework abstractions.</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Action.ReadFile.class, name = "read_file"),
        @JsonSubTypes.Type(value = Action.Search.class, name = "search"),
        @JsonSubTypes.Type(value = Action.Shell.class, name = "shell"),
        @JsonSubTypes.Type(value = Action.ApplyPatch.class, name = "apply_patch"),
        @JsonSubTypes.Type(value = Action.RunTests.class, name = "run_tests"),
        @JsonSubTypes.Type(value = Action.Finish.class, name = "finish")
})
public sealed interface Action permits Action.ReadFile, Action.Search, Action.Shell, Action.ApplyPatch, Action.RunTests, Action.Finish {
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
