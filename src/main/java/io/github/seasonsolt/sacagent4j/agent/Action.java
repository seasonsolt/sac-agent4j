package io.github.seasonsolt.sacagent4j.agent;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
    record ReadFile(String path) implements Action {}

    record Search(String query) implements Action {}

    record Shell(String command) implements Action {}

    record ApplyPatch(String patch) implements Action {}

    record RunTests() implements Action {}

    record Finish(String summary) implements Action {}
}
