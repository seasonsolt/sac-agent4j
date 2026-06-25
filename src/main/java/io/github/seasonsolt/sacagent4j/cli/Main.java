package io.github.seasonsolt.sacagent4j.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seasonsolt.sacagent4j.agent.AgentLoop;
import io.github.seasonsolt.sacagent4j.agent.AgentResult;
import io.github.seasonsolt.sacagent4j.agent.ContextBuilder;
import io.github.seasonsolt.sacagent4j.llm.JsonLineLlmClient;
import io.github.seasonsolt.sacagent4j.tool.ToolExecutor;
import io.github.seasonsolt.sacagent4j.workspace.Workspace;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "sac-agent4j", mixinStandardHelpOptions = true,
        description = "A tiny handwritten SWE agent loop for learning and experimentation.")
public final class Main implements Callable<Integer> {
    @CommandLine.Option(names = "--workspace", defaultValue = ".", description = "Workspace root")
    Path workspace;

    @CommandLine.Option(names = "--test-command", defaultValue = "mvn test", description = "Command used by run_tests")
    String testCommand;

    @CommandLine.Option(names = "--max-steps", defaultValue = "8", description = "Maximum agent turns")
    int maxSteps;

    @CommandLine.Parameters(index = "0..*", arity = "1..*", description = "Task for the agent")
    String[] taskWords;

    @Override
    public Integer call() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Workspace ws = new Workspace(workspace);
        AgentLoop loop = new AgentLoop(
                new JsonLineLlmClient(objectMapper, new BufferedReader(new InputStreamReader(System.in)), System.out),
                new ToolExecutor(ws, testCommand),
                new ContextBuilder(objectMapper),
                maxSteps
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
