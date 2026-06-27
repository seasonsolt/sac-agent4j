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
  -> wires composition root explicitly
       -> ToolContext
       -> ToolRegistry.defaultRegistry()
       -> DefaultPermissionGate
       -> ToolActionHandler
       -> ActionDispatcher
       -> DefaultContextManager
  -> AgentLoop.run(AgentRun)
       -> AgentRun.start(task, maxSteps)
          or SessionReplay.from(...).toAgentRun(maxSteps)
       -> ContextManager.buildPrompt(AgentRun).render()
       -> LlmClient.nextAction(context)
       -> AgentLoop handles finish
       -> ActionDispatcher.dispatch(action, AgentRun)
            -> StateActionHandler for StateAction
            -> ToolActionHandler for ToolAction
                 -> ToolRegistry.find(action)
                 -> PermissionGate.check(tool, action, context)
                 -> Tool.execute(action, context)
       -> AgentRun.record(Action, Observation)
       -> TrajectoryLogger.turn(...)
       -> SessionRecorder.turn(...)
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
| `ActionCatalog` | Keeps runtime JSON subtype names and model-visible action examples synchronized. |
| `Action` | Sealed action protocol between model and Java runtime. |
| `Observation` | Tool/state execution result. |
| `Turn` | One action/observation pair in the trajectory. |
| `AgentRun` | One run lifecycle: task, step budget, history, and state. |
| `AgentState` | Per-run inner-world state root. Holds todo list, virtual files, and context offloads. |
| `ActionDispatcher` | Routes non-terminal actions by action family. |
| `StateActionHandler` | Applies state action semantics to `AgentState`. |
| `ToolActionHandler` | Routes tool actions through registry lookup and permission checking. |
| `ToolRegistry` | Registry of concrete workspace tools. |
| `Tool` | One executable workspace capability with name and risk level. |
| `PermissionGate` | Boundary that can approve/reject risky tool actions before execution. |
| `ToolPolicy` | Shell safety policy used by the default permission gate. |
| `Workspace` | Path boundary and workspace file resolution. |
| `TrajectoryLogger` | Port for recording run events. |
| `SessionRecorder` | Pi-style append-only session tree for human resume/fork workflows. |
| `JsonlSessionReader` | Read model for inspecting session trees without replaying the agent runtime. |
| `JsonlSessionForker` | Creates a new session file from an existing session ancestry path. |
| `SessionTree` | Renders parent/child entry lineage with copyable entry ids. |
| `SessionReplay` | Reconstructs prompt history and replayable `AgentState` from a session ancestry path. |

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
{"type":"offload_context","key":"failure-log","title":"full failure log","content":"...large text..."}
{"type":"read_context","key":"failure-log"}
```

This is the smallest current equivalent of Deep Agents-style filesystem/context-management behavior.

## Current action groups

The current `Action` sealed interface is explicitly grouped:

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

This lets the runtime route by action family instead of branching over every concrete record in `AgentLoop`.

## Action protocol catalog

The model-visible protocol is now derived from `ActionCatalog` instead of being hand-written in `ActionProtocolRenderer`.

```text
Action.java @JsonSubTypes
  -> ActionCatalog.runtimeTypesByClass()
  -> ActionCatalog.examples()
  -> ActionProtocolRenderer
```

Tests assert that every runtime JSON subtype has a catalog example, and every catalog example round-trips through Jackson. Adding a new action therefore requires updating `Action.java` and adding a deliberate example; otherwise the test suite fails before prompt/runtime drift can ship.

## Pi-style session recording

In addition to `TrajectoryLogger`, the CLI now wires a lightweight `SessionRecorder` by default:

```text
.sac-agent4j/sessions/--workspace-path--/<timestamp>_<sessionId>.jsonl
```

The first line is a session header. Later entries are append-only tree entries with `id` and `parentId`:

```text
session header
  -> started
      -> turn
          -> turn
              -> finished
```

This deliberately follows the useful part of Pi's coding-agent design: keep a human-readable session history that can later grow into resume, fork, tree navigation, compaction, and branch summaries. It is not a LangGraph-style checkpoint. It does not currently persist enough runtime state to resume from an exact suspended instruction; it records the action/observation path for human/session workflows.

Session files now have a small read/fork surface:

```text
session summary <session.jsonl>
  -> task, status, action counts, active leaf

session tree <session.jsonl>
  -> parent/child lineage with copyable entry ids

session fork <session.jsonl> [--entry-id <id>]
  -> new JSONL file with copied ancestry and fork metadata

JsonlSessionRecorder.resume(path, leafId)
  -> appends future entries with parentId = leafId

--resume-session <session.jsonl> [--resume-entry <id>]
  -> rebuilds AgentRun history/state from ancestry and continues the loop
```

This is the first project step from "super individual" logs toward "super team" memory: a teammate can inspect what happened, branch from a specific decision point, and preserve the lineage as JSONL instead of relying on chat screenshots or implicit local context.

Runtime resume is deliberately narrower than full checkpointing. It restores
the selected task, prior turns, plans/todos, and virtual files by replaying
state actions from the ancestry path. Tool actions are not re-executed.
Compact `offload_context` records remain visible in history, but their full
payload is not restored because the session recorder stores key/title/size
rather than large content bodies.

## Tool boundary

Tool execution is no longer a single switch or facade. The active shape is:

```text
ActionDispatcher
  -> ToolActionHandler
       -> ToolRegistry
            -> ReadFileTool / SearchTool / ShellTool / ApplyPatchTool / RunTestsTool
       -> PermissionGate
            -> DefaultPermissionGate
                 -> ToolPolicy for command-backed medium/high-risk tools
```

## Current design strengths

1. **Explicit loop** — the agent mechanism is easy to read.
2. **Typed protocol** — actions are Java records under a sealed interface.
3. **Small model seam** — `LlmClient` makes model providers replaceable.
4. **State root exists** — `AgentState` prevents state from scattering further.
5. **Tool boundary exists** — real side effects pass through `ToolRegistry` / `PermissionGate` / `Workspace` / `ToolPolicy`.
6. **Tracing exists** — `TrajectoryLogger` records inspectable JSONL events.
7. **Offline demo exists** — scripted toy Java bug demo verifies the end-to-end loop without API keys.

## Current design smells

These are intentional review targets, not emergencies.

### 1. AgentState is pure state

`AgentState` owns the todo list, virtual filesystem, and context offload store, but prompt-facing formatting lives in `AgentStateRenderer`. This keeps the state model free of `StringBuilder`/presentation logic while preserving a compact prompt summary.

### 2. Action taxonomy is improved but still nested in one file

The sealed hierarchy is now explicit, but all action records still live in `Action.java`. This keeps the MVP compact; later, each action family could move into its own package.

### 3. Process execution is centralized

`ShellTool`, `RunTestsTool`, and `ApplyPatchTool` are separated by capability and now share `ProcessRunner` for timeout handling, stdin, and stdout/stderr capture. Future work can still add richer process events or streaming output, but command-backed tool execution now has one adapter boundary.

## Current refined class shape

The OO refinement now matches the runtime path:

```text
AgentLoop
  ├── AgentRun
  ├── ContextManager
  ├── LlmClient
  ├── ActionDispatcher
  ├── TrajectoryLogger
  └── SessionRecorder

AgentRun
  ├── AgentState
  └── List<Turn>

ActionDispatcher
  ├── StateActionHandler
  └── ToolActionHandler
        ├── ToolRegistry
        └── PermissionGate
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

## Current loop shape

`AgentLoop.run()` is intentionally close to:

```java
while (run.hasStepsRemaining()) {
    String context = contextManager.buildPrompt(run).render();
    Action action = llmClient.nextAction(context);

    if (action instanceof Action.Finish finish) {
        return run.finished(finish.summary());
    }

    int step = run.nextStep();
    Observation observation = actionDispatcher.dispatch(action, run);
    Turn turn = run.record(action, observation);
    trajectoryLogger.turn(step, turn.action(), turn.observation());
    sessionRecorder.turn(step, turn.action(), turn.observation());
}
return run.stopped();
```

This preserves the philosophical center:

```text
AgentLoop = time and control flow
ActionDispatcher = action-family routing
AgentRun = run lifecycle state
AgentState = agent inner world
ToolActionHandler = outside-world side-effect boundary
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
| Done | Add `ToolRegistry` + `PermissionGate` | Tool growth and risk gating now have explicit seams. |
| Done | Remove tool facade from main runtime path | `AgentLoop`/CLI now wire `ToolActionHandler` directly. |
| Done | Remove prompt compatibility facade | Deleted `ContextBuilder` / `ContextManagers`; `ContextManager` is the only prompt seam. |
| Done | Merge decision types | `ToolPolicy` now returns `PermissionDecision`; `PolicyDecision` is gone. |
| Done | Add `ProcessRunner` | Shared timeout/stdout/stderr/stdin handling across shell, tests, and patch tools. |
| Done | Add `ActionCatalog` | Runtime JSON subtypes and prompt examples are covered by one tested catalog. |
| Done | Move state rendering out of `AgentState` | `AgentStateRenderer` owns prompt formatting; state remains data. |
| Done | Remove `ToolExecutor` | Tests and examples use `ToolActionHandler` directly; no facade remains. |
| Done | Report unreadable search files | `ToolSupport.literalSearch` counts skipped unreadable files instead of swallowing exceptions silently. |
| Done | Add Pi-style session recorder | JSONL session files now use id/parentId entries as the first step toward resume/fork/tree workflows. |
| Done | Add JSONL session replay | `--resume-session` can continue from a session/fork ancestry path. |
| P2 | Add checkpoint/persistence seam | Move beyond replayed session history to exact suspended runtime state when needed. |

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
| Tool registry / side-effect boundary | `ToolActionHandler` + `ToolRegistry` + `PermissionGate` |
| Safety policy | `ToolPolicy` |
| Tracing | `TrajectoryLogger` |
| Pi-style session history | `SessionRecorder` + `JsonlSessionRecorder` |
| Session tree/resume/fork | `JsonlSessionReader` + `SessionTree` + `JsonlSessionForker` + `SessionReplay` |

Missing or intentionally deferred:

- subagents with isolated context windows
- human-in-the-loop approval/edit/reject
- exact persistent checkpoints
- skill loading
- streaming events
- permission profiles
- richer evaluation harness

Those should be added only after the core OO shape is clean.
