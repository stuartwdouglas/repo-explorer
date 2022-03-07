package com.github.stuartwouglas.repoexplorer.mavenparser;

import com.github.stuartwouglas.repoexplorer.discovery.RepositoryDiscoveryService;
import com.github.stuartwouglas.repoexplorer.model.Artifact;
import com.github.stuartwouglas.repoexplorer.model.ArtifactTagMapping;
import com.github.stuartwouglas.repoexplorer.model.RepositoryTag;
import com.github.stuartwouglas.repoexplorer.service.MavenProjectArtifactDiscovery;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

import javax.enterprise.context.control.ActivateRequestContext;

public class MavenDiscoveryRunner implements QuarkusApplication {

    public MavenDiscoveryRunner(MavenDiscovery mavenDiscovery, RepositoryDiscoveryService cloneHandler) {
        this.mavenDiscovery = mavenDiscovery;
        this.cloneHandler = cloneHandler;
    }

    public static void main(String ... args) {
        Quarkus.run(MavenDiscoveryRunner.class, args);
    }

    final MavenDiscovery mavenDiscovery;
    final RepositoryDiscoveryService cloneHandler;

    @Override
    @ActivateRequestContext
    public int run(String... args) throws Exception {

        cloneHandler.discoveryTask("file:///home/stuart/workspace/quarkus", (checkout, tag) -> {
            mavenDiscovery.doRepositoryDiscovery(checkout, "", tag);
        });
//        RepositoryTag tag = new RepositoryTag();
//        mavenDiscovery.doRepositoryDiscovery("/home/stuart/workspace/apicurio-registry/pom.xml", tag);
        for (Artifact artifact : Artifact.<Artifact>findAll().list()) {
                        System.out.println("\t\t\tDependency': " + artifact.groupId + ":" + artifact.artifactId + ":" + artifact.version);
        }
        for (ArtifactTagMapping artifact : ArtifactTagMapping.<ArtifactTagMapping>findAll().list()) {
            System.out.println("\t\t\tTAg': " + artifact.repositoryTag.name + " : " + artifact.artifact.artifactId);
        }
        return 0;
    }
}
