# sac-agent4j Class Diagram

This diagram records the current class shape after adding `AgentState`, virtual files, context offload, plan/todo actions, tool policy, and trajectory logging.

For architectural commentary and the next proposed OO refinement, see [`ARCHITECTURE.md`](./ARCHITECTURE.md).

```mermaid
classDiagram
    direction LR

    class Main {
      +call() Integer
      +main(args) void
    }

    class AgentLoop {
      -LlmClient llmClient
      -ToolExecutor toolExecutor
      -ContextBuilder contextBuilder
      -TrajectoryLogger trajectoryLogger
      -AgentState agentState
      -List~Turn~ history
      -int maxSteps
      +run(task) AgentResult
      +state() AgentState
      +plan() List~TodoItem~
      -execute(action) Observation
    }

    class ContextBuilder {
      -ObjectMapper objectMapper
      +build(task, history, agentState) String
      -renderTurnForPrompt(turn) String
    }

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
      +setPlan(action) Observation
      +updateTodo(action) Observation
      +writeVirtualFile(action) Observation
      +readVirtualFile(action) Observation
      +offloadContext(action) Observation
      +readContext(action) Observation
      +plan() List~TodoItem~
      +renderStateSummary() String
    }

    class TodoList {
      -List~TodoItem~ items
      +setPlan(action) Observation
      +updateTodo(action) Observation
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
      -normalize(path) String
    }

    class ContextOffloadStore {
      -Map~String,Entry~ entries
      -int nextId
      +offload(action) Observation
      +read(key) Observation
      +summary() Map~String,String~
    }

    class ContextOffloadEntry {
      +String key
      +String title
      +String content
    }

    class ToolExecutor {
      -Workspace workspace
      -String testCommand
      -ToolPolicy toolPolicy
      +execute(action) Observation
      -readFile(path) Observation
      -search(query) Observation
      -shell(command) Observation
      -applyPatch(patch) Observation
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
      +allow() PolicyDecision
      +deny(reason) PolicyDecision
    }

    class Workspace {
      -Path root
      +root() Path
      +resolveExisting(path) Path
      +resolveForWrite(path) Path
      -resolveInside(path) Path
    }

    class TrajectoryLogger {
      <<interface>>
      +started(task, maxSteps)
      +turn(step, action, observation)
      +finished(finished, summary, turns)
      +close()
    }

    class JsonlTrajectoryLogger {
      -ObjectMapper objectMapper
      -BufferedWriter writer
      -Path path
      +path() Path
      -actionForLog(action) ObjectNode
    }

    class NoopTrajectoryLogger

    Main --> AgentLoop : wires
    AgentLoop --> LlmClient : asks next action
    AgentLoop --> ContextBuilder : builds prompt
    AgentLoop --> ToolExecutor : executes workspace tools
    AgentLoop --> AgentState : executes state actions
    AgentLoop --> TrajectoryLogger : records events
    AgentLoop --> Turn : appends
    AgentLoop --> AgentResult : returns

    LlmClient <|.. JsonLineLlmClient
    LlmClient <|.. OpenAiCompatibleLlmClient
    LlmClient <|.. ScriptedLlmClient

    Action <|.. SetPlan
    Action <|.. UpdateTodo
    Action <|.. WriteVirtualFile
    Action <|.. ReadVirtualFile
    Action <|.. OffloadContext
    Action <|.. ReadContext
    Action <|.. ReadFile
    Action <|.. Search
    Action <|.. Shell
    Action <|.. ApplyPatch
    Action <|.. RunTests
    Action <|.. Finish

    Turn --> Action
    Turn --> Observation

    AgentState *-- TodoList
    AgentState *-- VirtualFileSystem
    AgentState *-- ContextOffloadStore
    TodoList *-- TodoItem
    TodoItem --> TodoStatus
    ContextOffloadStore *-- ContextOffloadEntry

    ToolExecutor --> Workspace
    ToolExecutor --> ToolPolicy
    ToolPolicy --> PolicyDecision

    TrajectoryLogger <|.. JsonlTrajectoryLogger
    TrajectoryLogger <|.. NoopTrajectoryLogger
```

## Proposed next class diagram

This is the recommended next OO refinement before adding heavier features such as subagents, skills, HITL, or checkpointing.

```mermaid
classDiagram
    direction LR

    class AgentLoop {
      -LlmClient llmClient
      -ContextBuilder contextBuilder
      -ActionDispatcher actionDispatcher
      -TrajectoryLogger trajectoryLogger
      +run(task) AgentResult
    }

    class AgentRun {
      -String task
      -AgentState state
      -List~Turn~ history
      -int maxSteps
      -int currentStep
      +state() AgentState
      +history() List~Turn~
      +append(turn)
      +hasStepsRemaining() boolean
      +finish(summary) AgentResult
      +stopped() AgentResult
    }

    class AgentState {
      -TodoList todoList
      -VirtualFileSystem virtualFileSystem
      -ContextOffloadStore contextOffloads
      +renderStateSummary() String
    }

    class ActionDispatcher {
      -StateActionHandler stateActionHandler
      -ToolExecutor toolExecutor
      +execute(action, run) Observation
    }

    class StateActionHandler {
      +execute(action, state) Observation
    }

    class ToolExecutor {
      +execute(toolAction) Observation
    }

    class ContextBuilder {
      +build(run) String
    }

    class LlmClient {
      <<interface>>
      +nextAction(context) Action
    }

    class TrajectoryLogger {
      <<interface>>
    }

    AgentLoop --> AgentRun
    AgentLoop --> LlmClient
    AgentLoop --> ContextBuilder
    AgentLoop --> ActionDispatcher
    AgentLoop --> TrajectoryLogger

    AgentRun *-- AgentState
    AgentRun *-- Turn

    ActionDispatcher --> StateActionHandler
    ActionDispatcher --> ToolExecutor
    StateActionHandler --> AgentState
    ContextBuilder --> AgentRun
```

The intended responsibility split is:

```text
AgentLoop        = time/control flow
AgentRun         = one run's lifecycle state
AgentState       = agent's inner world
ActionDispatcher = action routing
StateActionHandler = state mutation/read semantics
ToolExecutor     = external workspace/tool side effects
```
