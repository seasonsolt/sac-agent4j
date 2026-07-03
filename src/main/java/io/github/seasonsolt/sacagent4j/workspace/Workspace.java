package io.github.seasonsolt.sacagent4j.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Workspace boundary guard for all file-oriented tools.
 *
 * <p>The model should not be able to read or write outside the selected project
 * directory. This class centralizes path normalization and traversal checks.</p>
 */
public final class Workspace {
    private final Path root;

    public Workspace(Path root) throws IOException {
        this.root = root.toRealPath();
    }

    public Path root() {
        return root;
    }

    /** Resolves an existing path and rejects attempts to escape the workspace. */
    public Path resolveExisting(String relativePath) throws IOException {
        Path resolved = root.resolve(relativePath).normalize().toRealPath();
        if (!resolved.startsWith(root)) {
            throw new SecurityException("path escapes workspace: " + relativePath);
        }
        return resolved;
    }

    /** Resolves a path intended for writing and creates parent directories inside the workspace. */
    public Path resolveForWrite(String relativePath) throws IOException {
        Path resolved = root.resolve(relativePath).normalize().toAbsolutePath();

        // A lexical startsWith check is not enough: a symlink that lives inside the
        // workspace can point outside it, so the write would follow the link and
        // escape the boundary. Canonicalize the deepest already-existing ancestor
        // (symlinks and all) and re-check before creating anything.
        Path existing = resolved;
        while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
        }
        if (existing == null) {
            throw new SecurityException("path escapes workspace: " + relativePath);
        }
        Path realExisting = existing.toRealPath();
        if (!realExisting.startsWith(root)) {
            throw new SecurityException("path escapes workspace: " + relativePath);
        }

        // The not-yet-created remainder cannot contain existing symlinks, so
        // grafting it onto the canonical ancestor yields the true write target.
        Path realResolved = realExisting.resolve(existing.relativize(resolved)).normalize();
        if (!realResolved.startsWith(root)) {
            throw new SecurityException("path escapes workspace: " + relativePath);
        }

        Path parent = realResolved.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return realResolved;
    }
}
