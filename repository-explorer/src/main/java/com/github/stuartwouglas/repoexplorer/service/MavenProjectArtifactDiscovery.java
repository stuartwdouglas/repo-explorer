package com.github.stuartwouglas.repoexplorer.service;

import com.github.stuartwouglas.repoexplorer.model.Artifact;
import com.github.stuartwouglas.repoexplorer.model.ArtifactDependency;
import com.github.stuartwouglas.repoexplorer.model.Repository;
import com.github.stuartwouglas.repoexplorer.model.RepositoryTag;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.transaction.UserTransaction;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
//                                artifact.tag = tag;
//                                tag.artifacts.add(artifact);
                                artifact.persistAndFlush();
                            }
                            var lines = Files.readAllLines(file);
                            for (var line : lines) {
                                var parts = line.split(":");
                                if (parts.length < 3) {
                                    continue;
                                }
                                ArtifactDependency dependency = new ArtifactDependency();
//                                dependency.groupId = parts[0];
//                                dependency.artifactId = parts[1];
//                                dependency.version = parts[3];
                                dependency.scope = parts[4];
//                                dependency.artifact = artifact;
                                artifact.dependencies.add(dependency);
                                dependency.persistAndFlush();
                                System.out.println(Arrays.toString(parts));

                                String jarFileName = parts[5];
                                String pomFileName = jarFileName.substring(0, jarFileName.lastIndexOf(".jar")) + ".pom";
                                System.out.println("POM:" + pomFileName);
                                if (Files.isRegularFile(Paths.get(pomFileName))) {
                                    handlePomFile(pomFileName);
                                }
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

    private void handlePomFile(String pomFileName) {

        try (InputStream in = Files.newInputStream(Paths.get(pomFileName))) {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(in);
            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
            NodeList nList = doc.getElementsByTagName("scm");

            for (int i = 0; i < nList.getLength(); ++i) {
                var node = nList.item(i);
                for (int j = 0; j < node.getChildNodes().getLength(); ++j) {
                    Node conNode = node.getChildNodes().item(j);
                    if (conNode.getNodeName().equals("connection")) {
                        String text = conNode.getTextContent();
                        if (text.startsWith("scm:git:")) {
                            String uri = text.substring("scm:git:".length());
                            if (Repository.find("uri", uri).list().isEmpty()) {
                                Repository r = new Repository();
                                r.uri = uri;
                                r.persistAndFlush();
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
