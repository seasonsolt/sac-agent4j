package io.github.seasonsolt.sacagent4j.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void resolvesNewNestedFileForWriteInsideWorkspace() throws Exception {
        Workspace workspace = new Workspace(tempDir);
        Path target = workspace.resolveForWrite("nested/dir/new.txt");
        assertTrue(target.startsWith(tempDir.toRealPath()));
        assertTrue(Files.isDirectory(tempDir.resolve("nested/dir")));
    }

    @Test
    void rejectsWriteThroughSymlinkEscapingWorkspace() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("workspace"));
        Path outside = Files.createDirectory(tempDir.resolve("outside"));
        Files.createSymbolicLink(root.resolve("link"), outside);
        Workspace workspace = new Workspace(root);

        assertThrows(SecurityException.class, () -> workspace.resolveForWrite("link/evil.txt"));
    }
}
