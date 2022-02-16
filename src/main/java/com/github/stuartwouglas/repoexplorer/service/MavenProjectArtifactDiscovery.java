package com.github.stuartwouglas.repoexplorer.service;

import com.github.stuartwouglas.repoexplorer.model.Artifact;
import com.github.stuartwouglas.repoexplorer.model.ArtifactDependency;
import com.github.stuartwouglas.repoexplorer.model.RepositoryTag;

import javax.transaction.UserTransaction;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

public class MavenProjectArtifactDiscovery {

    final LocalClone clone;
    final UserTransaction userTransaction;

    public MavenProjectArtifactDiscovery(LocalClone clone, UserTransaction userTransaction) {
        this.clone = clone;
        this.userTransaction = userTransaction;
    }


    public void doDiscovery(RepositoryTag tag) {
        try {
            int result = new ProcessBuilder("mvn", "install", "-DskipTests", "dependency:list", "-DoutputFile=hack-file-discovery.txt", "-DoutputAbsoluteArtifactFilename=true", "-Dsilent=true")
                    .directory(clone.getClone().toFile())
                    .inheritIO()
                    .start().waitFor();
            if (result != 0) {
                throw new RuntimeException("Build failed");
            }
            userTransaction.begin();
            try {
                Files.walkFileTree(clone.getClone(), new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.getFileName().toString().equals("hack-file-discovery.txt")) {
                            Artifact artifact = new Artifact();
                            try (InputStream in = Files.newInputStream(file.getParent().resolve("target/maven-archiver/pom.properties"))) {
                                Properties p = new Properties();
                                p.load(in);
                                artifact.artifactId = p.getProperty("artifactId");
                                artifact.groupId = p.getProperty("groupId");
                                artifact.version = p.getProperty("version");
                                artifact.dependencies = new HashSet<>();
                                artifact.tag = tag;
                                tag.artifacts.add(artifact);
                                artifact.persistAndFlush();
                            }
                            var lines = Files.readAllLines(file);
                            for (var line : lines) {
                                var parts = line.split(":");
                                if (parts.length < 3) {
                                    continue;
                                }
                                ArtifactDependency dependency = new ArtifactDependency();
                                dependency.groupId = parts[0];
                                dependency.artifactId = parts[1];
                                dependency.version = parts[3];
                                dependency.scope = parts[4];
                                dependency.artifact = artifact;
                                artifact.dependencies.add(dependency);
                                dependency.persistAndFlush();
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
                userTransaction.commit();
            } catch (Throwable t) {
                userTransaction.rollback();
                throw new RuntimeException(t);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
