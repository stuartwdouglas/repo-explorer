package com.github.stuartwouglas.repoexplorer.buildexplorer;

import com.github.stuartwouglas.repoexplorer.model.Artifact;
import com.github.stuartwouglas.repoexplorer.model.ArtifactTagMapping;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Explores local maven repo, and compares the results with what the system knows how to build
 */
@Path("build-explorer")
public class BuildRepoExplorer {

    @GET
    @Transactional
    public List<Result> run(@QueryParam("path") String path) throws IOException {

        List<Artifact> found = new ArrayList<>();
        Files.walkFileTree(Paths.get(path), new FileVisitor<java.nio.file.Path>() {
            @Override
            public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(".pom")) {

                    MavenXpp3Reader reader = new MavenXpp3Reader();
                    Model model = null;
                    try {
                        model = reader.read(new FileReader(file.toFile()));
                    } catch (XmlPullParserException e) {
                        throw new RuntimeException(e);
                    }

                    String groupId = model.getGroupId();
                    if (groupId == null) {
                        groupId = model.getParent().getGroupId();
                    }
                    String artifactId = model.getArtifactId();
                    String version = model.getVersion();
                    if (version == null) {
                        version = model.getParent().getVersion();
                    }
                    if (version.endsWith("SNAPSHOT")) {
                        return FileVisitResult.CONTINUE;
                    }
                    Artifact artifact = Artifact.findOrCreate(groupId, artifactId, version);
                    found.add(artifact);
                    boolean jarPresent = false;
                    try (var stream = Files.newDirectoryStream(file.getParent())) {
                        for (var i : stream) {
                            if (i.getFileName().toString().endsWith(".jar")) {
                                jarPresent = true;
                                break;
                            }

                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(java.nio.file.Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(java.nio.file.Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        List<Result> ret = new ArrayList<>();
        for (var i : found) {
            if (ArtifactTagMapping.find("artifact", i).firstResult() == null) {
                ret.add(new Result(i.groupId, i.artifactId, i.version, false));
            }
        }
        return ret;

    }


    static class Result implements Comparable<Result> {
        final String groupId;
        final String artifactId;
        final String version;
        final boolean jarRequired;

        Result(String groupId, String artifactId, String version, boolean jarRequired) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.jarRequired = jarRequired;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getVersion() {
            return version;
        }

        public boolean isJarRequired() {
            return jarRequired;
        }

        @Override
        public int compareTo(Result o) {
            int r = groupId.compareTo(o.groupId);
            if (r != 0) {
                return r;
            }
            r = artifactId.compareTo(o.artifactId);
            if (r != 0) {
                return r;
            }
            return version.compareTo(o.version);
        }
    }
}
