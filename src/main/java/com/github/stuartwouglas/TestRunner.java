package com.github.stuartwouglas;

import com.github.stuartwouglas.repoexplorer.CloneHandler;
import com.github.stuartwouglas.repoexplorer.model.Artifact;
import com.github.stuartwouglas.repoexplorer.model.Repository;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.UserTransaction;

@QuarkusMain
public class TestRunner implements QuarkusApplication {

    final CloneHandler cloneHandler;
    final UserTransaction userTransaction;

    @Inject
    public TestRunner(CloneHandler cloneHandler, UserTransaction userTransaction) {
        this.cloneHandler = cloneHandler;
        this.userTransaction = userTransaction;
    }

    public static void main(String ... args ) throws Exception {
        Quarkus.run(TestRunner.class, args);
    }

    @Override
    @ActivateRequestContext
    public int run(String... args) throws Exception {
        cloneHandler.doClone("file:///home/stuart/workspace/gizmo");
        for (Repository r : Repository.<Repository>listAll()) {
            System.out.println("Repository: " + r.uri);
            for (var t : r.tags) {
                System.out.println("\tTag: " + t.name);
                for (var a : t.artifacts) {
                    System.out.println("\t\tArtifact: " + a.groupId + ":" + a.artifactId + ":" + a.version);
                    for (var d : a.dependencies) {
                        System.out.println("\t\t\tDependency': " + d.groupId + ":" + d.artifactId + ":" + d.version);
                    }
                }
            }
        }
        return 0;
    }
}
