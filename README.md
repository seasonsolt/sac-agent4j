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

## Why not Spring AI / LangChain4j yet?

Those frameworks are useful after the core seams are stable. For the first version, this project keeps the agent mechanics visible: action protocol, tool execution, observations, and stopping conditions.
