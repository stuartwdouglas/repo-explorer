package com.github.stuartwouglas.repoexplorer.mavenparser;

import com.github.stuartwouglas.repoexplorer.discovery.RepositoryDiscoveryService;
import com.github.stuartwouglas.repoexplorer.github.RepositoryAddedEvent;
import com.github.stuartwouglas.repoexplorer.model.Repository;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Parameters;
import io.quarkus.runtime.ExecutorRecorder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.transaction.UserTransaction;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class MavenDiscoveryRunner {

    final MavenDiscovery mavenDiscovery;
    final RepositoryDiscoveryService cloneHandler;
    final UserTransaction userTransaction;
    final Lock lock = new ReentrantLock();

    public MavenDiscoveryRunner(MavenDiscovery mavenDiscovery, RepositoryDiscoveryService cloneHandler, UserTransaction userTransaction) {
        this.mavenDiscovery = mavenDiscovery;
        this.cloneHandler = cloneHandler;
        this.userTransaction = userTransaction;
    }
    void handleAdded(@Observes(during = TransactionPhase.AFTER_SUCCESS) RepositoryAddedEvent event) throws Exception {
        ExecutorRecorder.getCurrent().execute(this::run);
    }

    @ActivateRequestContext
    public void run() {
        if (!lock.tryLock()) {
            return;
        }
        try {

            for (; ; ) {
                List<Repository> unprocessed = Repository.list("discoveryAttempted=false and (language is null or language='Java')");
                if (unprocessed.isEmpty()) {
                    break;
                }
                for (var r : unprocessed) {
                    try {
                        Log.infof("Processing repository %s", r.uri);
                        cloneHandler.discoveryTask(r, (checkout, tag) -> {
                            mavenDiscovery.doRepositoryDiscovery(checkout, "", tag);
                        });
                    } catch (Throwable t) {
                        Log.error("Failed to process repo " + r.uri, t);
                    }
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        Log.infof("Processing done");
    }
}
