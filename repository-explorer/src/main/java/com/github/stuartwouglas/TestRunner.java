package com.github.stuartwouglas;

import com.github.stuartwouglas.repoexplorer.discovery.RepositoryDiscoveryService;
import com.github.stuartwouglas.repoexplorer.model.Repository;
import com.github.stuartwouglas.repoexplorer.service.MavenProjectArtifactDiscovery;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.List;

//@QuarkusMain
public class TestRunner implements QuarkusApplication {

    final RepositoryDiscoveryService cloneHandler;
    final UserTransaction userTransaction;

    @Inject
    public TestRunner(RepositoryDiscoveryService cloneHandler, UserTransaction userTransaction) {
        this.cloneHandler = cloneHandler;
        this.userTransaction = userTransaction;
    }

    public static void main(String... args) throws Exception {
        Quarkus.run(TestRunner.class, args);
    }

    @Override
    @ActivateRequestContext
    public int run(String... args) throws Exception {
        cloneHandler.discoveryTask("file:///home/stuart/workspace/gizmo", (checkout, tag) -> {
            MavenProjectArtifactDiscovery d = new MavenProjectArtifactDiscovery(checkout, userTransaction);
            d.doDiscovery(tag);
        });
        for (; ; ) {
            List<Repository> unprocessed = Repository.list("discoveryAttempted", false);
            if (unprocessed.isEmpty()) {
                break;
            }
            for (var r : unprocessed) {
                try {
                    cloneHandler.discoveryTask(r, (checkout, tag) -> {
                        MavenProjectArtifactDiscovery d = new MavenProjectArtifactDiscovery(checkout, userTransaction);
                        d.doDiscovery(tag);
                    });
                } catch (Throwable t) {
                    Log.error("Failed to process repo " + r.uri, t);
                    userTransaction.begin();
                    try {
                        Repository loaded = Repository.findById(r.id);
                        loaded.discoveryAttempted = true;
                        userTransaction.commit();
                    } catch (Throwable tx) {
                        userTransaction.rollback();
                        throw new RuntimeException(tx);
                    }
                }
            }
        }
        for (Repository r : Repository.<Repository>listAll()) {
            System.out.println("Repository: " + r.uri);
            for (var t : r.tags) {
                System.out.println("\tTag: " + t.name);
                for (var a : t.artifacts) {
//                    System.out.println("\t\tArtifact: " + a.groupId + ":" + a.artifactId + ":" + a.version);
//                    for (var d : a.dependencies) {
//                        System.out.println("\t\t\tDependency': " + d.dependency.groupId + ":" + d.dependency.artifactId + ":" + d.dependency.version);
//                    }
                }
            }
        }
        return 0;
    }
}
