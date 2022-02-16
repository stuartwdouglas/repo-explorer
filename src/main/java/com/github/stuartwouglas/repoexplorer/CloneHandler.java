package com.github.stuartwouglas.repoexplorer;

import com.github.stuartwouglas.repoexplorer.model.Repository;
import com.github.stuartwouglas.repoexplorer.model.RepositoryTag;
import com.github.stuartwouglas.repoexplorer.service.LocalClone;
import com.github.stuartwouglas.repoexplorer.service.MavenProjectArtifactDiscovery;

import javax.inject.Singleton;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class CloneHandler {

    public static final String REFS_TAGS = "refs/tags/";

    final UserTransaction userTransaction;

    public CloneHandler(UserTransaction userTransaction) {
        this.userTransaction = userTransaction;
    }

    public void doClone(String uri) throws Exception {
        try (LocalClone checkout = LocalClone.clone(uri)) {
            Repository existing = Repository.find("uri", uri).firstResult();
            Map<String, String> knownTags = new HashMap<>();
            userTransaction.begin();
            try {
                if (existing == null) {
                    existing = new Repository();
                    existing.uri = uri;
                    existing.tags = new ArrayList<>();
                    existing.persist();
                } else {
                    for (var tag : existing.tags) {
                        knownTags.put(tag.name, tag.ref);
                    }
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
                    }
                    System.out.println("NAME: " + ref.getName() + " " + ref.getObjectId().getName());
                }
                userTransaction.commit();
                for (var tag : existing.tags) {
                    checkout.getGit().checkout().setName(tag.ref).call();
                    MavenProjectArtifactDiscovery d = new MavenProjectArtifactDiscovery(checkout, userTransaction);
                    d.doDiscovery(tag);
                    break; //do one tag for now
                }
            } catch (Throwable t) {
                userTransaction.rollback();
                throw new RuntimeException(t);
            }
        }
    }


}
