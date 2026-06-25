package io.github.seasonsolt.sacagent4j.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Workspace {
    private final Path root;

    public Workspace(Path root) throws IOException {
        this.root = root.toRealPath();
    }

    public Path root() {
        return root;
    }

    public Path resolveExisting(String relativePath) throws IOException {
        Path resolved = root.resolve(relativePath).normalize().toRealPath();
        if (!resolved.startsWith(root)) {
            throw new SecurityException("path escapes workspace: " + relativePath);
        }
        return resolved;
    }

    public Path resolveForWrite(String relativePath) throws IOException {
        Path resolved = root.resolve(relativePath).normalize().toAbsolutePath();
        if (!resolved.startsWith(root)) {
            throw new SecurityException("path escapes workspace: " + relativePath);
        }
        Path parent = resolved.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return resolved;
    }
}
