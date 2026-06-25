package io.github.seasonsolt.sacagent4j.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkspaceTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesFilesInsideWorkspace() throws Exception {
        Files.writeString(tempDir.resolve("a.txt"), "hello");
        Workspace workspace = new Workspace(tempDir);
        assertEquals(tempDir.resolve("a.txt").toRealPath(), workspace.resolveExisting("a.txt"));
    }

    @Test
    void rejectsPathTraversal() throws Exception {
        Workspace workspace = new Workspace(tempDir);
        assertThrows(SecurityException.class, () -> workspace.resolveForWrite("../evil.txt"));
    }
}
