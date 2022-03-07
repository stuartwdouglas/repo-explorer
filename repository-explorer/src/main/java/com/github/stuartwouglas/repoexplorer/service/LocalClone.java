package com.github.stuartwouglas.repoexplorer.service;

import com.github.stuartwouglas.repoexplorer.utils.DirectoryUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;

import java.nio.file.Files;
import java.nio.file.Path;

public class LocalClone implements AutoCloseable {

    private final Git git;
    private final Path clone;
    private final String repository;
    boolean closed = false;

    public LocalClone(Git git, Path checkout, String repository) {
        this.git = git;
        this.clone = checkout;
        this.repository = repository;
    }

    public Git getGit() {
        if (closed) {
            throw new IllegalStateException("closed");
        }
        return git;
    }

    public Path getClone() {
        return clone;
    }

    public String getRepository() {
        return repository;
    }

    public static LocalClone clone(String repository) {
        try {
            Path temp = Files.createTempDirectory("local-checkout");
            Git result = new CloneCommand()
                    .setDirectory(temp.toFile())
                    .setURI(repository)
                    .call();
            return new LocalClone(result, temp, repository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        git.close();
        DirectoryUtils.delete(clone);
        closed = true;
    }
}
