package io.github.seasonsolt.sacagent4j.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.agent.ActionDispatcher;
import io.github.seasonsolt.sacagent4j.agent.AgentLoop;
import io.github.seasonsolt.sacagent4j.agent.AgentResult;
import io.github.seasonsolt.sacagent4j.agent.AgentRun;
import io.github.seasonsolt.sacagent4j.agent.StateActionHandler;
import io.github.seasonsolt.sacagent4j.agent.context.DefaultContextManager;
import io.github.seasonsolt.sacagent4j.llm.JsonLineLlmClient;
import io.github.seasonsolt.sacagent4j.llm.LlmClient;
import io.github.seasonsolt.sacagent4j.llm.OpenAiCompatibleLlmClient;
import io.github.seasonsolt.sacagent4j.session.JsonlSessionAnnotator;
import io.github.seasonsolt.sacagent4j.session.JsonlSessionCatalog;
import io.github.seasonsolt.sacagent4j.session.JsonlSessionForker;
import io.github.seasonsolt.sacagent4j.session.JsonlSessionReader;
import io.github.seasonsolt.sacagent4j.session.JsonlSessionRecorder;
import io.github.seasonsolt.sacagent4j.session.NoopSessionRecorder;
import io.github.seasonsolt.sacagent4j.session.SessionDocument;
import io.github.seasonsolt.sacagent4j.session.SessionReplay;
import io.github.seasonsolt.sacagent4j.session.SessionRecorder;
import io.github.seasonsolt.sacagent4j.tool.DefaultPermissionGate;
import io.github.seasonsolt.sacagent4j.tool.ToolActionHandler;
import io.github.seasonsolt.sacagent4j.tool.ToolContext;
import io.github.seasonsolt.sacagent4j.tool.ToolPolicy;
import io.github.seasonsolt.sacagent4j.tool.ToolRegistry;
import io.github.seasonsolt.sacagent4j.trajectory.JsonlTrajectoryLogger;
import io.github.seasonsolt.sacagent4j.trajectory.NoopTrajectoryLogger;
import io.github.seasonsolt.sacagent4j.trajectory.TrajectoryLogger;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/** CLI entry point that wires the minimal loop with a selected LLM client. */
@CommandLine.Command(name = "sac-agent4j", mixinStandardHelpOptions = true,
        description = "A tiny handwritten SWE agent loop for learning and experimentation.",
        subcommands = Main.SessionCommand.class)
public final class Main implements Callable<Integer> {
    @CommandLine.Option(names = "--workspace", defaultValue = ".", description = "Workspace root")
    Path workspace;

    @CommandLine.Option(names = "--test-command", defaultValue = "mvn test", description = "Command used by run_tests")
    String testCommand;

    @CommandLine.Option(names = "--max-steps", defaultValue = "8", description = "Maximum agent turns")
    int maxSteps;

    @CommandLine.Option(names = "--llm", defaultValue = "json-line", description = "LLM client: json-line or openai")
    String llm;

    @CommandLine.Option(names = "--model", description = "Model name for OpenAI-compatible mode. Defaults to OPENAI_MODEL or gpt-4o-mini")
    String model;

    @CommandLine.Option(names = "--trajectory-dir", defaultValue = ".sac-agent4j/runs", description = "Directory for JSONL trajectory logs. Blank disables logging.")
    String trajectoryDir;

    @CommandLine.Option(names = "--session-dir", defaultValue = ".sac-agent4j/sessions", description = "Directory for Pi-style JSONL session files. Blank disables session recording.")
    String sessionDir;

    @CommandLine.Option(names = "--resume-session", description = "Resume from an existing JSONL session file.")
    Path resumeSession;

    @CommandLine.Option(names = "--resume-entry", description = "Entry id to resume from. Defaults to the active leaf.")
    String resumeEntry;

    @CommandLine.Parameters(index = "0..*", arity = "0..*", description = "Task for the agent")
    String[] taskWords;

    @Override
    public Integer call() throws Exception {
        boolean hasTask = taskWords != null && taskWords.length > 0;
        if (!hasTask && resumeSession == null) {
            System.err.println("missing task");
            return 2;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        SessionReplay replay = resumeSession == null ? null : SessionReplay.from(objectMapper, resumeSession, resumeEntry);
        String task = hasTask ? String.join(" ", taskWords) : replay.task();
        Workspace ws = new Workspace(workspace);
        LlmClient llmClient = switch (llm) {
            case "json-line" -> new JsonLineLlmClient(objectMapper, new BufferedReader(new InputStreamReader(System.in)), System.out);
            case "openai" -> OpenAiCompatibleLlmClient.fromEnv(objectMapper, model);
            default -> throw new IllegalArgumentException("unsupported --llm: " + llm);
        };
        TrajectoryLogger trajectoryLogger = trajectoryDir == null || trajectoryDir.isBlank()
                ? new NoopTrajectoryLogger()
                : new JsonlTrajectoryLogger(objectMapper, ws, trajectoryDir);
        SessionRecorder sessionRecorder = sessionDir == null || sessionDir.isBlank()
                ? new NoopSessionRecorder()
                : resumeSession == null
                        ? new JsonlSessionRecorder(objectMapper, ws, sessionDir)
                        : JsonlSessionRecorder.resume(objectMapper, resumeSession, replay.leafId());

        ToolContext toolContext = new ToolContext(ws, testCommand, ToolPolicy.defaultPolicy());
        ActionDispatcher actionDispatcher = new ActionDispatcher(
                new StateActionHandler(),
                new ToolActionHandler(ToolRegistry.defaultRegistry(), new DefaultPermissionGate()),
                toolContext
        );
        AgentLoop loop = new AgentLoop(
                llmClient,
                actionDispatcher,
                new DefaultContextManager(objectMapper),
                maxSteps,
                trajectoryLogger,
                sessionRecorder
        );
        AgentRun run = replay == null
                ? AgentRun.start(task, maxSteps)
                : AgentRun.resume(task, maxSteps, replay.state(), replay.history());
        AgentResult result = loop.run(run);
        System.out.println("finished=" + result.finished());
        System.out.println("summary=" + result.summary());
        System.out.println("turns=" + result.history().size());
        return result.finished() ? 0 : 2;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @CommandLine.Command(name = "session", description = "Inspect and fork JSONL session files.",
            subcommands = {
                    SessionSummaryCommand.class,
                    SessionTreeCommand.class,
                    SessionListCommand.class,
                    SessionNoteCommand.class,
                    SessionForkCommand.class
            })
    static final class SessionCommand implements Runnable {
        @CommandLine.Spec
        CommandSpec spec;

        @Override
        public void run() {
            spec.commandLine().usage(spec.commandLine().getOut());
        }
    }

    @CommandLine.Command(name = "summary", description = "Print a compact session summary.")
    static final class SessionSummaryCommand implements Callable<Integer> {
        @CommandLine.Spec
        CommandSpec spec;

        @CommandLine.Parameters(index = "0", description = "Session JSONL file")
        Path session;

        @Override
        public Integer call() throws Exception {
            SessionDocument document = JsonlSessionReader.read(new ObjectMapper(), session);
            spec.commandLine().getOut().println(document.summary().render());
            return 0;
        }
    }

    @CommandLine.Command(name = "tree", description = "Print a session parent/child tree.")
    static final class SessionTreeCommand implements Callable<Integer> {
        @CommandLine.Spec
        CommandSpec spec;

        @CommandLine.Parameters(index = "0", description = "Session JSONL file")
        Path session;

        @Override
        public Integer call() throws Exception {
            SessionDocument document = JsonlSessionReader.read(new ObjectMapper(), session);
            spec.commandLine().getOut().println(document.tree().render());
            return 0;
        }
    }

    @CommandLine.Command(name = "list", description = "List session files under a directory.")
    static final class SessionListCommand implements Callable<Integer> {
        @CommandLine.Spec
        CommandSpec spec;

        @CommandLine.Parameters(index = "0", description = "Session root directory")
        Path directory;

        @Override
        public Integer call() throws Exception {
            ObjectMapper objectMapper = new ObjectMapper();
            for (var item : JsonlSessionCatalog.list(objectMapper, directory)) {
                spec.commandLine().getOut().println(item.render());
            }
            return 0;
        }
    }

    @CommandLine.Command(name = "note", description = "Append a team note to a selected session entry.")
    static final class SessionNoteCommand implements Callable<Integer> {
        @CommandLine.Spec
        CommandSpec spec;

        @CommandLine.Parameters(index = "0", description = "Session JSONL file")
        Path session;

        @CommandLine.Option(names = "--entry-id", required = true, description = "Entry id to annotate.")
        String entryId;

        @CommandLine.Option(names = "--title", required = true, description = "Note title.")
        String title;

        @CommandLine.Option(names = "--body", required = true, description = "Note body.")
        String body;

        @Override
        public Integer call() throws Exception {
            String noteId = JsonlSessionAnnotator.note(new ObjectMapper(), session, entryId, title, body);
            spec.commandLine().getOut().println("note=" + noteId);
            return 0;
        }
    }

    @CommandLine.Command(name = "fork", description = "Create a new session file forked from a selected entry.")
    static final class SessionForkCommand implements Callable<Integer> {
        @CommandLine.Spec
        CommandSpec spec;

        @CommandLine.Parameters(index = "0", description = "Session JSONL file")
        Path session;

        @CommandLine.Option(names = "--entry-id", description = "Entry id to fork from. Defaults to the active leaf.")
        String entryId;

        @CommandLine.Option(names = "--output-dir", description = "Directory for the forked session. Defaults to a forks directory beside the source session.")
        Path outputDir;

        @Override
        public Integer call() throws Exception {
            ObjectMapper objectMapper = new ObjectMapper();
            SessionDocument document = JsonlSessionReader.read(objectMapper, session);
            String forkEntryId = entryId == null || entryId.isBlank() ? document.leafId() : entryId;
            Path forkDirectory = outputDir == null ? session.toAbsolutePath().normalize().getParent().resolve("forks") : outputDir;
            Path forkPath = JsonlSessionForker.fork(objectMapper, session, forkEntryId, forkDirectory);
            spec.commandLine().getOut().println("forkedSession=" + forkPath.toAbsolutePath().normalize());
            return 0;
        }
    }
}
