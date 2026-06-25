package io.github.seasonsolt.sacagent4j.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.agent.ActionDispatcher;
import io.github.seasonsolt.sacagent4j.agent.AgentLoop;
import io.github.seasonsolt.sacagent4j.agent.AgentResult;
import io.github.seasonsolt.sacagent4j.agent.StateActionHandler;
import io.github.seasonsolt.sacagent4j.agent.context.DefaultContextManager;
import io.github.seasonsolt.sacagent4j.llm.JsonLineLlmClient;
import io.github.seasonsolt.sacagent4j.llm.LlmClient;
import io.github.seasonsolt.sacagent4j.llm.OpenAiCompatibleLlmClient;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/** CLI entry point that wires the minimal loop with a selected LLM client. */
@CommandLine.Command(name = "sac-agent4j", mixinStandardHelpOptions = true,
        description = "A tiny handwritten SWE agent loop for learning and experimentation.")
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

    @CommandLine.Parameters(index = "0..*", arity = "1..*", description = "Task for the agent")
    String[] taskWords;

    @Override
    public Integer call() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Workspace ws = new Workspace(workspace);
        LlmClient llmClient = switch (llm) {
            case "json-line" -> new JsonLineLlmClient(objectMapper, new BufferedReader(new InputStreamReader(System.in)), System.out);
            case "openai" -> OpenAiCompatibleLlmClient.fromEnv(objectMapper, model);
            default -> throw new IllegalArgumentException("unsupported --llm: " + llm);
        };
        TrajectoryLogger trajectoryLogger = trajectoryDir == null || trajectoryDir.isBlank()
                ? new NoopTrajectoryLogger()
                : new JsonlTrajectoryLogger(objectMapper, ws, trajectoryDir);

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
                trajectoryLogger
        );
        AgentResult result = loop.run(String.join(" ", taskWords));
        System.out.println("finished=" + result.finished());
        System.out.println("summary=" + result.summary());
        System.out.println("turns=" + result.history().size());
        return result.finished() ? 0 : 2;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
