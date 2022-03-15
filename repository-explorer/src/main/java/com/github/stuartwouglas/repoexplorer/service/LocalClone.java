package com.github.stuartwouglas.repoexplorer.service;

import com.github.stuartwouglas.repoexplorer.utils.DirectoryUtils;
import io.quarkus.logging.Log;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.TagOpt;

import java.io.File;
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

    public static LocalClone clone(String repository, Path baseCheckoutDir) {
        return clone(repository, null, baseCheckoutDir);
    }

    public static LocalClone clone(String repository, String existingCheckout, Path baseCheckoutDir) {
        int retryCount = 0;
        long backoffTime = 2000;
        for (; ; ) {
            try {
                if (existingCheckout == null) {
                    Path temp;
                    if (baseCheckoutDir == null) {
                        temp = Files.createTempDirectory("local-checkout");
                    } else {
                        temp = Files.createTempDirectory(baseCheckoutDir, "checkout-");
                    }
                    Git result = new CloneCommand()
                            .setDirectory(temp.toFile())
                            .setURI(repository)
                            .call();
                    return new LocalClone(result, temp, repository);
                } else {
                    File dir = new File(existingCheckout);
                    Git result = Git.open(dir);
                    var fetchCommand = result.fetch()
                            .setTagOpt(TagOpt.FETCH_TAGS)
                            .call();
                    return new LocalClone(result, dir.toPath(), repository);
                }
            } catch (Exception e) {
                Log.error("Failed to clone " + repository + " backing off " + backoffTime);
                if (retryCount++ == 10) {
                    throw new RuntimeException(e);
                }
                try {
                    Thread.sleep(backoffTime);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                backoffTime *= 2;
            }
        }
    }

    @Override
    public void close() throws Exception {
        git.close();
        closed = true;
    }
}
