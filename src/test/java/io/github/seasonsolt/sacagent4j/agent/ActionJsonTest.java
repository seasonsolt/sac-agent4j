package io.github.seasonsolt.sacagent4j.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ActionJsonTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesReadFileAction() throws Exception {
        Action action = objectMapper.readValue("{\"type\":\"read_file\",\"path\":\"README.md\"}", Action.class);
        Action.ReadFile readFile = assertInstanceOf(Action.ReadFile.class, action);
        assertEquals("README.md", readFile.path());
    }

    @Test
    void writesFinishAction() throws Exception {
        String json = objectMapper.writeValueAsString(new Action.Finish("done"));
        assertEquals("{\"type\":\"finish\",\"summary\":\"done\"}", json);
    }
}
