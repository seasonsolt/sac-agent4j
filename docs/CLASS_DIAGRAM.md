# sac-agent4j Class Diagram

This diagram records the class shape after introducing `AgentRun`, `ActionDispatcher`, `StateActionHandler`, the `ControlAction` / `StateAction` / `ToolAction` hierarchy, and the `ContextManager` prompt seam.

For architectural commentary, see [`ARCHITECTURE.md`](./ARCHITECTURE.md).

```mermaid
classDiagram
    direction LR

    class Main {
      +call() Integer
      +main(args) void
    }

    class AgentLoop {
      -LlmClient llmClient
      -ActionDispatcher actionDispatcher
      -ContextManager contextManager
      -TrajectoryLogger trajectoryLogger
      -int maxSteps
      -AgentRun lastRun
      +run(task) AgentResult
      +state() AgentState
      +plan() List~TodoItem~
    }

    class AgentRun {
      -String task
      -int maxSteps
      -AgentState state
      -List~Turn~ history
      -int nextStep
      +start(task, maxSteps) AgentRun
      +hasStepsRemaining() boolean
      +record(action, observation) Turn
      +finished(summary) AgentResult
      +stopped() AgentResult
    }

    class ActionDispatcher {
      -StateActionHandler stateActionHandler
      -ToolExecutor toolExecutor
      +dispatch(action, run) Observation
    }

    class StateActionHandler {
      +execute(stateAction, state) Observation
    }

    class ContextManager {
      <<interface>>
      +buildPrompt(run) Prompt
    }

    class DefaultContextManager {
      -SystemPromptRenderer systemPromptRenderer
      -ActionProtocolRenderer actionProtocolRenderer
      -TaskRenderer taskRenderer
      -AgentStateRenderer agentStateRenderer
      -HistoryRenderer historyRenderer
      +buildPrompt(run) Prompt
    }

    class Prompt {
      +String system
      +String actionProtocol
      +String task
      +String agentState
      +String history
      +render() String
    }

    class ContextBuilder {
      +build(run) String
      +build(task, history, agentState) String
    }

    class SystemPromptRenderer
    class ActionProtocolRenderer
    class TaskRenderer
    class AgentStateRenderer
    class HistoryRenderer

    class LlmClient {
      <<interface>>
      +nextAction(context) Action
    }

    class JsonLineLlmClient
    class OpenAiCompatibleLlmClient
    class ScriptedLlmClient

    class Action {
      <<sealed interface>>
    }

    class ControlAction {
      <<sealed interface>>
    }

    class StateAction {
      <<sealed interface>>
    }

    class ToolAction {
      <<sealed interface>>
    }

    class SetPlan
    class UpdateTodo
    class WriteVirtualFile
    class ReadVirtualFile
    class OffloadContext
    class ReadContext
    class ReadFile
    class Search
    class Shell
    class ApplyPatch
    class RunTests
    class Finish

    class Observation {
      +int exitCode
      +String output
      +ok(output) Observation
      +failed(output) Observation
    }

    class Turn {
      +Action action
      +Observation observation
    }

    class AgentResult {
      +boolean finished
      +String summary
      +List~Turn~ history
    }

    class AgentState {
      -TodoList todoList
      -VirtualFileSystem virtualFileSystem
      -ContextOffloadStore contextOffloads
      +todoList() TodoList
      +virtualFileSystem() VirtualFileSystem
      +contextOffloads() ContextOffloadStore
      +plan() List~TodoItem~
      +renderStateSummary() String
    }

    class TodoList {
      -List~TodoItem~ items
      +setPlan(items) Observation
      +updateTodo(id, status) Observation
      +items() List~TodoItem~
      +render() String
    }

    class TodoItem {
      +int id
      +String content
      +TodoStatus status
    }

    class TodoStatus {
      <<enum>>
      pending
      in_progress
      completed
      cancelled
    }

    class VirtualFileSystem {
      -Map~String,String~ files
      +write(path, content) Observation
      +read(path) Observation
      +summary() Map~String,Integer~
    }

    class ContextOffloadStore {
      -Map~String,Entry~ entries
      -int nextId
      +offload(key, title, content) Observation
      +read(key) Observation
      +summary() Map~String,String~
    }

    class ToolExecutor {
      -Workspace workspace
      -String testCommand
      -ToolPolicy toolPolicy
      +execute(toolAction) Observation
      +readFile(path) Observation
      +search(query) Observation
      +shell(command) Observation
      +applyPatch(patch) Observation
    }

    class ToolPolicy {
      -List~Pattern~ deniedShellPatterns
      +defaultPolicy() ToolPolicy
      +allowAll() ToolPolicy
      +checkShell(command) PolicyDecision
    }

    class PolicyDecision {
      +boolean allowed
      +String reason
    }

    class Workspace {
      -Path root
      +root() Path
      +resolveExisting(path) Path
      +resolveForWrite(path) Path
    }

    class TrajectoryLogger {
      <<interface>>
      +started(task, maxSteps)
      +turn(step, action, observation)
      +finished(finished, summary, turns)
      +close()
    }

    class JsonlTrajectoryLogger
    class NoopTrajectoryLogger

    Main --> AgentLoop : wires
    AgentLoop --> AgentRun : starts/advances
    AgentLoop --> LlmClient : asks next action
    AgentLoop --> ContextManager : builds prompt
    ContextManager <|.. DefaultContextManager
    DefaultContextManager --> Prompt
    DefaultContextManager --> SystemPromptRenderer
    DefaultContextManager --> ActionProtocolRenderer
    DefaultContextManager --> TaskRenderer
    DefaultContextManager --> AgentStateRenderer
    DefaultContextManager --> HistoryRenderer
    ContextBuilder --> DefaultContextManager : compatibility facade
    AgentLoop --> ActionDispatcher : dispatches non-terminal actions
    AgentLoop --> TrajectoryLogger : records events

    AgentRun *-- AgentState
    AgentRun *-- Turn
    AgentRun --> AgentResult

    ActionDispatcher --> StateActionHandler
    ActionDispatcher --> ToolExecutor
    StateActionHandler --> AgentState

    LlmClient <|.. JsonLineLlmClient
    LlmClient <|.. OpenAiCompatibleLlmClient
    LlmClient <|.. ScriptedLlmClient

    Action <|-- ControlAction
    Action <|-- StateAction
    Action <|-- ToolAction
    ControlAction <|.. Finish
    StateAction <|.. SetPlan
    StateAction <|.. UpdateTodo
    StateAction <|.. WriteVirtualFile
    StateAction <|.. ReadVirtualFile
    StateAction <|.. OffloadContext
    StateAction <|.. ReadContext
    ToolAction <|.. ReadFile
    ToolAction <|.. Search
    ToolAction <|.. Shell
    ToolAction <|.. ApplyPatch
    ToolAction <|.. RunTests

    Turn --> Action
    Turn --> Observation

    AgentState *-- TodoList
    AgentState *-- VirtualFileSystem
    AgentState *-- ContextOffloadStore
    TodoList *-- TodoItem
    TodoItem --> TodoStatus

    ToolExecutor --> Workspace
    ToolExecutor --> ToolPolicy
    ToolPolicy --> PolicyDecision

    TrajectoryLogger <|.. JsonlTrajectoryLogger
    TrajectoryLogger <|.. NoopTrajectoryLogger
```

## Responsibility split

```text
AgentLoop          = time/control flow
AgentRun           = one run's lifecycle state
AgentState         = agent's inner world
Action             = typed model/runtime protocol
ContextManager     = attention/context boundary
Prompt             = structured prompt sections
ActionDispatcher   = action family routing
StateActionHandler = state mutation/read semantics
ToolExecutor       = external workspace/tool side effects
LlmClient          = model boundary
TrajectoryLogger   = trace boundary
```

## Why this is more OO than the previous shape

- `AgentLoop` no longer branches over every concrete action.
- `AgentLoop` depends on `ContextManager`, not a string-building concrete class.
- Prompt sections are represented by `Prompt` and independent renderers.
- `ToolExecutor` only accepts `ToolAction`; it no longer knows about state actions.
- `AgentState` no longer accepts action records; state mutation semantics moved to `StateActionHandler`.
- `AgentRun` owns history, state, step budget, and run result construction.
- The Java sealed hierarchy now expresses action ontology directly.
