# sac-agent4j

`sac-agent4j` is a tiny handwritten Java SWE agent for learning how coding agents work.

It intentionally avoids Spring AI, LangChain4j, and large agent frameworks in the first version. The goal is to expose the core loop directly:

```text
build context -> ask for one JSON action -> execute tool -> record observation -> repeat
```

## Minimal philosophy

- One agent loop.
- One JSON action per turn.
- Tiny explicit tools.
- Local workspace boundary checks.
- Test feedback as a first-class action.
- No framework magic in the MVP.

## Actions

```json
{"type":"set_plan","items":["inspect failure","patch bug","run tests"]}
{"type":"update_todo","id":1,"status":"in_progress"}
{"type":"write_virtual_file","path":"notes/root-cause.md","content":"..."}
{"type":"read_virtual_file","path":"notes/root-cause.md"}
{"type":"offload_context","key":"test-output","title":"full failing test log","content":"..."}
{"type":"read_context","key":"test-output"}
{"type":"read_file","path":"README.md"}
{"type":"search","query":"TODO"}
{"type":"shell","command":"mvn test"}
{"type":"apply_patch","patch":"...unified diff..."}
{"type":"run_tests"}
{"type":"finish","summary":"done"}
```

## Build and test

```bash
mvn test
mvn package
```

## Native binary with GraalVM

The regular JVM jar remains the default build. If GraalVM with `native-image` is
installed, build an opt-in native CLI binary with:

```bash
mvn -Pnative package
```

The executable is written to:

```text
target/sac-agent4j
```

Run it the same way as the jar:

```bash
target/sac-agent4j --workspace . "inspect this repo"
```

Native Image mainly improves command startup time and memory footprint. It does
not remove the dominant latency in `--llm openai` mode, which is usually the
network round trip and model response time.

This project uses Jackson polymorphic records and picocli annotations. Picocli
native metadata is generated during compilation by `picocli-codegen`; the
Jackson record reflection metadata is checked in under
`src/main/resources/META-INF/native-image/` and covered by a unit test.

If a future native build reports missing reflection metadata for new code paths,
collect metadata by running representative CLI flows under the native-image
agent and merge the generated entries into the checked-in metadata:

```bash
mvn package
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
  -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar \
  --trajectory-dir "" \
  --workspace . \
  "inspect this repo" <<'JSON'
{"type":"finish","summary":"metadata warmup"}
JSON
```

Then rebuild with `mvn -Pnative package`.

## Demo: fix a toy Java bug

Run the end-to-end demo:

```bash
examples/run-toy-demo.sh
```

The script copies `examples/toy-java-bug` to `/tmp/sac-agent4j-toy-demo`, verifies the baseline test fails, feeds a JSON-line action sequence into `sac-agent4j`, applies a patch, runs tests again, and prints the trajectory log.

Expected final signal:

```text
CalculatorTest passed
```

The demo trajectory shows the full loop:

```text
set_plan         -> create explicit todo list
update_todo      -> mark current step in_progress/completed
shell ./test.sh  -> failing AssertionError
offload_context  -> store bulky failure notes behind a key
read_file        -> inspect Calculator.java
write_virtual_file -> save root-cause notes in AgentState
apply_patch      -> change left - right to left + right
run_tests        -> CalculatorTest passed
finish
```

## Plan / todo list

For non-trivial tasks, the model can make progress explicit before editing:

```json
{"type":"set_plan","items":["reproduce failure","inspect code","apply fix","run tests"]}
{"type":"update_todo","id":1,"status":"in_progress"}
{"type":"update_todo","id":1,"status":"completed"}
```

Todo ids are 1-based and statuses are:

```text
pending | in_progress | completed | cancelled
```

The current plan is rendered into every subsequent prompt, and plan actions are also recorded in the JSONL trajectory.


## AgentState: virtual files and context offload

`sac-agent4j` keeps a single in-memory `AgentState` for each run. It currently contains:

```text
AgentState
  ├── TodoList
  ├── VirtualFileSystem
  └── ContextOffloadStore
```

Virtual files are scratch notes/drafts that do not touch the real workspace:

```json
{"type":"write_virtual_file","path":"notes/root-cause.md","content":"..."}
{"type":"read_virtual_file","path":"notes/root-cause.md"}
```

Context offload stores bulky text behind a small key. The prompt shows only the key/title/size until the model explicitly asks to read it:

```json
{"type":"offload_context","key":"failure-log","title":"full test output","content":"...large text..."}
{"type":"read_context","key":"failure-log"}
```

This follows the same broad idea as LangChain deepagents' filesystem/context-management features, but keeps the Java MVP framework-free and inspectable.


## Architecture docs

- [Class diagram](docs/CLASS_DIAGRAM.md)
- [Architecture notes](docs/ARCHITECTURE.md)

## Run

The current CLI is deliberately primitive: it prints the context and reads one JSON action per line from stdin.

```bash
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar --workspace . "inspect this repo"
```

Example action input:

```json
{"type":"read_file","path":"README.md"}
{"type":"finish","summary":"README inspected"}
```


## OpenAI-compatible mode

The default `json-line` mode is still the simplest way to inspect the loop manually. To let a real model choose actions, use `--llm openai` with standard environment variables:

```bash
export OPENAI_API_KEY=...
export OPENAI_BASE_URL=https://api.openai.com/v1   # optional
export OPENAI_MODEL=gpt-4o-mini                    # optional

java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar \
  --llm openai \
  --workspace /path/to/repo \
  --test-command "mvn test" \
  "fix the failing tests"
```

Any provider that implements the OpenAI `/chat/completions` response shape can be used by changing `OPENAI_BASE_URL`.

## Safety and trajectory logs

`sac-agent4j` now includes a minimal `ToolPolicy` for shell commands. It is not a sandbox, but it blocks obvious foot-guns such as:

- `rm -rf ...`
- `sudo ...`
- `chmod -R ...` / `chown -R ...`
- `git push ...`
- `curl ... | sh` / `wget ... | sh`
- shutdown/reboot/disk-format style commands

Every run also writes a JSONL trajectory by default:

```text
.sac-agent4j/runs/<timestamp>.jsonl
```

Each line is an event:

```json
{"event":"started","task":"...","maxSteps":8}
{"event":"turn","step":0,"action":{...},"observation":{...}}
{"event":"finished","finished":true,"summary":"...","turns":1}
```

Disable logging by passing a blank trajectory directory:

```bash
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar --trajectory-dir "" "inspect this repo"
```

## Team memory: sessions

Runs also write Pi-style JSONL session files by default:

```text
.sac-agent4j/sessions/<workspace>/<timestamp>_<sessionId>.jsonl
```

Session files are append-only trees with `id` / `parentId` links. They are meant
to turn one person's agent run into team-reviewable memory: what task was run,
which actions were taken, which tools returned what, and where a future branch
can start.

Inspect a previous session:

```bash
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar \
  session summary .sac-agent4j/sessions/<workspace>/<session>.jsonl
```

Print the parent/child tree with entry ids:

```bash
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar \
  session tree .sac-agent4j/sessions/<workspace>/<session>.jsonl
```

Create a fork file from the active leaf:

```bash
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar \
  session fork .sac-agent4j/sessions/<workspace>/<session>.jsonl
```

Create a fork from a specific entry:

```bash
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar \
  session fork .sac-agent4j/sessions/<workspace>/<session>.jsonl \
  --entry-id <entryId>
```

Resume from a session or fork:

```bash
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar \
  --resume-session .sac-agent4j/sessions/<workspace>/<session>.jsonl
```

Resume from a specific entry:

```bash
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar \
  --resume-session .sac-agent4j/sessions/<workspace>/<session>.jsonl \
  --resume-entry <entryId>
```

Resume rebuilds the prompt history and replayable `AgentState` actions from
the selected ancestry path, then appends the new continuation to the same
session file. `--max-steps` is counted as additional turns after the resume
point. Compact `offload_context` entries keep their history record, but their
full payload is not restored because session files store only key/title/size.
Use `session tree` when you need to copy an entry id for `--entry-id` or
`--resume-entry`.

Disable session recording by passing a blank session directory:

```bash
java -jar target/sac-agent4j-0.1.0-SNAPSHOT.jar --session-dir "" "inspect this repo"
```

## Why not Spring AI / LangChain4j yet?

Those frameworks are useful after the core seams are stable. For the first version, this project keeps the agent mechanics visible: action protocol, tool execution, observations, and stopping conditions.
