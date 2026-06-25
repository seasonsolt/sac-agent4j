# sac-agent4j Architecture Notes

This document records the current architecture and the next OO refactoring direction for `sac-agent4j`.

The project is intentionally a small, framework-free Java SWE-agent harness. It borrows core ideas from LangChain Deep Agents / similar coding agents — planning, filesystem-like state, context offload, tool execution, and tracing — but keeps the implementation explicit enough to study.

## Design intent

`sac-agent4j` is not trying to be a production framework first. It is a learning-oriented Java design whose core loop should remain visible:

```text
build context -> ask model for one typed action -> execute action -> append observation -> repeat
```

The architectural goal is object-oriented clarity:

- Each class should have one clear reason to exist.
- Runtime state should be explicit, not scattered.
- External side effects should be isolated behind small ports/adapters.
- The agent loop should express the time/control flow, not every tool detail.
- The action protocol should remain typed and inspectable.

## Current runtime flow

```text
Main
  -> AgentLoop.run(task)
       -> AgentRun.start(task, maxSteps)
       -> ContextManager.buildPrompt(AgentRun).render()
       -> LlmClient.nextAction(context)
       -> AgentLoop handles finish
       -> ActionDispatcher.dispatch(action, AgentRun)
            -> StateActionHandler for StateAction
            -> ToolExecutor for ToolAction
       -> AgentRun.record(Action, Observation)
       -> TrajectoryLogger.turn(...)
       -> finish or continue
```

## Current major abstractions

| Abstraction | Responsibility |
|---|---|
| `AgentLoop` | Owns only the step loop and terminal control flow. |
| `LlmClient` | Port for model decision-making. |
| `ContextManager` | Port for building structured prompts from `AgentRun`. |
| `Prompt` | Structured prompt sections with final string rendering. |
| `DefaultContextManager` | Composes prompt section renderers. |
| `ContextBuilder` | Backward-compatible string facade over the new context seam. |
| `Action` | Sealed action protocol between model and Java runtime. |
| `Observation` | Tool/state execution result. |
| `Turn` | One action/observation pair in the trajectory. |
| `AgentRun` | One run lifecycle: task, step budget, history, and state. |
| `AgentState` | Per-run inner-world state root. Holds todo list, virtual files, and context offloads. |
| `ActionDispatcher` | Routes non-terminal actions by action family. |
| `StateActionHandler` | Applies state action semantics to `AgentState`. |
| `ToolExecutor` | Executes real workspace tools: read/search/shell/patch/tests. |
| `ToolPolicy` | Shell safety policy. |
| `Workspace` | Path boundary and workspace file resolution. |
| `TrajectoryLogger` | Port for recording run events. |

## AgentState

`AgentState` is the current state root:

```text
AgentState
  ├── TodoList
  ├── VirtualFileSystem
  └── ContextOffloadStore
```

### TodoList

Captures explicit model planning:

```json
{"type":"set_plan","items":["reproduce failure","inspect code","patch bug","run tests"]}
{"type":"update_todo","id":1,"status":"in_progress"}
```

### VirtualFileSystem

An in-memory filesystem for notes, drafts, and intermediate artifacts. It does not touch the real workspace:

```json
{"type":"write_virtual_file","path":"notes/root-cause.md","content":"..."}
{"type":"read_virtual_file","path":"notes/root-cause.md"}
```

### ContextOffloadStore

Stores bulky text behind a key. Prompts and trajectories record only key/title/size unless the model explicitly reads the content:

```json
{"type":"offload_context","key":"failure-log","title":"full test output","content":"...large text..."}
{"type":"read_context","key":"failure-log"}
```

This is the smallest current equivalent of Deep Agents-style filesystem/context-management behavior.

## Current action groups

The current `Action` sealed interface is now explicitly grouped:

```text
Action
  ├── ControlAction
  │   └── finish
  ├── StateAction
  │   ├── set_plan / update_todo
  │   ├── write_virtual_file / read_virtual_file
  │   └── offload_context / read_context
  └── ToolAction
      ├── read_file / search
      └── shell / apply_patch / run_tests
```

This lets `ToolExecutor` accept only `ToolAction`, while `StateActionHandler` accepts only `StateAction`.

## Current design strengths

1. **Explicit loop** — the agent mechanism is easy to read.
2. **Typed protocol** — actions are Java records under a sealed interface.
3. **Small model seam** — `LlmClient` makes model providers replaceable.
4. **State root exists** — `AgentState` prevents state from scattering further.
5. **Tool boundary exists** — real side effects are mostly inside `ToolExecutor` / `Workspace` / `ToolPolicy`.
6. **Tracing exists** — `TrajectoryLogger` records inspectable JSONL events.
7. **Offline demo exists** — scripted toy Java bug demo verifies the end-to-end loop without API keys.

## Current design smells

These are intentional review targets, not emergencies.

### 1. AgentState still renders itself

`AgentState` now no longer handles action records, but it still renders its own prompt summary. A future `AgentStateRenderer` could make state purely data.

### 2. Action taxonomy is improved but still nested in one file

The sealed hierarchy is now explicit, but all action records still live in `Action.java`. This keeps the MVP compact; later, each action family could move into its own package.

## Current refined class shape

The first OO refinement has been applied:

```text
AgentLoop
  ├── AgentRun
  ├── ContextManager
  ├── LlmClient
  ├── ActionDispatcher
  └── TrajectoryLogger

AgentRun
  ├── AgentState
  └── List<Turn>

ActionDispatcher
  ├── StateActionHandler
  └── ToolExecutor
```

### AgentRun

Represents one execution lifetime:

```text
AgentRun = task metadata + max steps + AgentState + history + next step
```

### ActionDispatcher

Owns non-terminal action routing:

```text
ActionDispatcher.dispatch(action, run) -> Observation
```

### StateActionHandler

Owns state action semantics:

```text
StateActionHandler.execute(StateAction, AgentState) -> Observation
```

## Desired future loop

The ideal `AgentLoop.run()` should be close to this:

```java
while (run.hasStepsRemaining()) {
    String context = contextManager.buildPrompt(run).render();
    Action action = llmClient.nextAction(context);

    if (action instanceof Action.Finish finish) {
        return run.finish(finish.summary());
    }

    Observation observation = actionDispatcher.execute(action, run);
    run.append(new Turn(action, observation));
    trajectoryLogger.turn(run.step(), action, observation);
}
return run.stopped();
```

This preserves the philosophical center:

```text
AgentLoop = time and control flow
ActionDispatcher = action routing
AgentRun = run lifecycle state
AgentState = agent inner world
ToolExecutor = outside world side effects
```

## Review criteria for future class diagrams

Use these questions before adding new features:

1. **Single reason to exist** — can each class be explained in one sentence?
2. **State ownership** — does all run state live under `AgentRun` / `AgentState`?
3. **Side-effect boundary** — are external effects isolated behind ports/adapters?
4. **Action taxonomy** — does a new action belong to control, state, or tool?
5. **Loop clarity** — can `AgentLoop.run()` still be understood at a glance?
6. **No framework gravity** — does the design stay Java-native and inspectable?

## Immediate refactor backlog

| Priority | Change | Why |
|---|---|---|
| Done | Introduce `AgentRun` | Centralized per-run lifecycle state. |
| Done | Introduce `ActionDispatcher` | Removed action routing from `AgentLoop`. |
| Done | Introduce `StateActionHandler` | Made `AgentState` closer to pure state. |
| Done | Split conceptual action groups | Added `ControlAction`, `StateAction`, and `ToolAction`. |
| Done | Split prompt rendering sections | Added `ContextManager`, structured `Prompt`, and section renderers. |
| P2 | Add checkpoint/persistence seam | Move beyond in-memory state when needed. |

## Relationship to LangChain Deep Agents / pi-style coding agents

`sac-agent4j` currently covers the minimal versions of these ideas:

| Deep-agent concept | sac-agent4j equivalent |
|---|---|
| Agent loop | `AgentLoop` |
| Model abstraction | `LlmClient` |
| Tool protocol | `Action` sealed interface |
| Filesystem/state | `VirtualFileSystem` under `AgentState` |
| Context offload | `ContextOffloadStore` |
| Planning | `TodoList` |
| Shell/tool execution | `ToolExecutor` |
| Safety policy | `ToolPolicy` |
| Tracing | `TrajectoryLogger` |

Missing or intentionally deferred:

- subagents with isolated context windows
- human-in-the-loop approval/edit/reject
- persistent checkpoints and resumability
- skill loading
- streaming events
- permission profiles
- richer evaluation harness

Those should be added only after the core OO shape is clean.
