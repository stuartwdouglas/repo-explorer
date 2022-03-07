package com.github.stuartwouglas.repoexplorer.discovery;

import com.github.stuartwouglas.repoexplorer.model.Repository;
import com.github.stuartwouglas.repoexplorer.model.RepositoryTag;
import com.github.stuartwouglas.repoexplorer.service.LocalClone;
import com.github.stuartwouglas.repoexplorer.service.MavenProjectArtifactDiscovery;
import io.quarkus.logging.Log;
import org.jboss.logging.Logger;

import javax.inject.Singleton;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Singleton
public class RepositoryDiscoveryService {

    public static final String REFS_TAGS = "refs/tags/";

    final UserTransaction userTransaction;

    public RepositoryDiscoveryService(UserTransaction userTransaction) {
        this.userTransaction = userTransaction;
    }

    public void discoveryTask(String uri, BiConsumer<LocalClone, RepositoryTag> task) throws Exception {
        Repository existing = Repository.find("uri", uri).firstResult();
        if (existing == null) {
            userTransaction.begin();
            try {
                existing = new Repository();
                existing.uri = uri;
                existing.tags = new ArrayList<>();
                existing.persistAndFlush();
                userTransaction.commit();
            } catch (Throwable t){
                userTransaction.rollback();
                throw new RuntimeException(t);
            }
        }
        discoveryTask(existing, task);
    }

    public void discoveryTask(Repository existing, BiConsumer<LocalClone, RepositoryTag> task) throws Exception {
        try (LocalClone checkout = LocalClone.clone(existing.uri)) {
            Map<String, String> knownTags = new HashMap<>();
            List<RepositoryTag> newTags = new ArrayList<>();
            userTransaction.begin();
            existing = Repository.findById(existing.id);
            existing.discoveryAttempted = true;
            try {
                for (var tag : existing.tags) {
                    knownTags.put(tag.name, tag.ref);
                }

                for (var ref : checkout.getGit().tagList().call()) {
                    String name = ref.getName();
                    if (name.startsWith(REFS_TAGS)) {
                        name = name.substring(REFS_TAGS.length());
                    }
                    if (!knownTags.containsKey(name)) {
                        RepositoryTag tag = new RepositoryTag();
                        tag.name = name;
                        tag.ref = ref.getObjectId().getName();
                        tag.repository = existing;
                        tag.artifacts = new ArrayList<>();
                        tag.persist();
                        existing.tags.add(tag);
                        newTags.add(tag);
                    }
                    System.out.println("NAME: " + ref.getName() + " " + ref.getObjectId().getName());
                }
                userTransaction.commit();
                for (var tag : newTags) {
                    checkout.getGit().checkout().setName(tag.ref).call();
                    try {
                        task.accept(checkout, tag);
                    } catch (Throwable t) {
                        Log.error("Failed to process " + tag.name, t);
                    }
                }
            } catch (Throwable t) {
                try {
                    userTransaction.rollback();
                } catch (Throwable tx) {
                    Log.error("Failed to roll back tx on " + existing.uri, tx);
                }
                throw new RuntimeException(t);
            }
        }
    }


}
