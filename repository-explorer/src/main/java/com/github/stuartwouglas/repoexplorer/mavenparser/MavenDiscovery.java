package com.github.stuartwouglas.repoexplorer.mavenparser;

import com.github.stuartwouglas.repoexplorer.model.Artifact;
import com.github.stuartwouglas.repoexplorer.model.ArtifactDependency;
import com.github.stuartwouglas.repoexplorer.model.ArtifactTagMapping;
import com.github.stuartwouglas.repoexplorer.model.RepositoryTag;
import com.github.stuartwouglas.repoexplorer.service.LocalClone;
import io.quarkus.logging.Log;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class MavenDiscovery {
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void doRepositoryDiscovery(LocalClone localClone, String rootPomDirectory, RepositoryTag repositoryTag) {
        doDiscovery(localClone, rootPomDirectory, repositoryTag, new HashMap<>());
    }

    private void doDiscovery(LocalClone localClone, String pomDirectory, RepositoryTag repositoryTag, HashMap<String, String> properties) {
        try {
            Path pomPath = localClone.getClone().resolve(pomDirectory);
            File pomFilePath = pomPath.resolve("pom.xml").toFile();
            if (!pomFilePath.isFile()) {
                return;
            }
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(pomFilePath));

            for (var i : model.getProperties().entrySet()) {
                properties.put(i.getKey().toString(), i.getValue().toString());
            }

            if (repositoryTag != null) {
                for (var module : model.getModules()) {
                    doDiscovery(localClone, (pomDirectory.isEmpty() ? "" : pomDirectory + File.separator) + module, repositoryTag, new HashMap<>(properties));
                }
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
                return;
            }
            properties.put("project.version", version);
            Artifact artifact = Artifact.findOrCreate(groupId, artifactId, version);
            if (repositoryTag != null) {
                ArtifactTagMapping existing = ArtifactTagMapping.find("artifact", artifact).firstResult();
                if (existing == null) {
                    ArtifactTagMapping mapping = new ArtifactTagMapping();
                    mapping.path = pomDirectory;
                    mapping.artifact = artifact;
                    mapping.repositoryTag = repositoryTag;
                    mapping.persistAndFlush();
                } else {
                    Log.error("Unable to add " + artifact + " from " + repositoryTag.repository.uri + ":" + repositoryTag.name + " because it is already owned by " + existing.repositoryTag.repository.uri + ":" + existing.repositoryTag.name);
                    return;
                }
            }
            for (var dependency : model.getDependencies()) {
                String iv = null;
                if (dependency.getVersion() != null) {
                    Matcher matcher = Pattern.compile("\\$\\{(a:.*?)}").matcher(dependency.getVersion());
                    iv = matcher.replaceAll(s -> {
                        String result = properties.get(s.group(1));
                        if (result == null) {
                            return "MISSING";
                        }
                        return result;
                    });
                    if (iv.equals("MISSING")) {
                        continue;
                    }
                    Artifact dep = Artifact.findOrCreate(dependency.getGroupId(), dependency.getArtifactId(), iv);
                    ArtifactDependency artifactDependency = new ArtifactDependency();
                    artifactDependency.scope = dependency.getScope();
                    artifactDependency.dependency = dep;
                }
            }
            if (model.getDependencyManagement() != null) {
                for (var dependency : model.getDependencyManagement().getDependencies()) {
                    String iv = null;
                    if (dependency.getVersion() != null) {
                        Matcher matcher = Pattern.compile("\\$\\{(.*?)}").matcher(dependency.getVersion());
                        iv = matcher.replaceAll(s -> {
                            String result = properties.get(s.group(1));
                            if (result == null) {
                                return "MISSING";
                            }
                            return result;
                        });
                        if (iv.equals("MISSING")) {
                            continue;
                        }
                        Artifact dep = Artifact.findOrCreate(dependency.getGroupId(), dependency.getArtifactId(), iv);
                        ArtifactDependency artifactDependency = new ArtifactDependency();
                        artifactDependency.scope = dependency.getScope();
                        artifactDependency.dependency = dep;
                    }
                }
            }


        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }
}
