package io.github.seasonsolt.sacagent4j.agent;

import io.github.seasonsolt.sacagent4j.state.AgentState;

/** Applies state-only actions to the run's {@link AgentState}. */
public final class StateActionHandler {
    public Observation execute(Action.StateAction action, AgentState state) {
        return switch (action) {
            case Action.SetPlan setPlan -> state.todoList().setPlan(setPlan.items());
            case Action.UpdateTodo updateTodo -> state.todoList().updateTodo(updateTodo.id(), updateTodo.status());
            case Action.WriteVirtualFile writeVirtualFile -> state.virtualFileSystem().write(writeVirtualFile.path(), writeVirtualFile.content());
            case Action.ReadVirtualFile readVirtualFile -> state.virtualFileSystem().read(readVirtualFile.path());
            case Action.OffloadContext offloadContext -> state.contextOffloads().offload(offloadContext.key(), offloadContext.title(), offloadContext.content());
            case Action.ReadContext readContext -> state.contextOffloads().read(readContext.key());
        };
    }
}
