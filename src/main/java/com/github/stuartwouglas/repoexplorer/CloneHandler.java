package com.github.stuartwouglas.repoexplorer;

import com.github.stuartwouglas.repoexplorer.model.Repository;
import com.github.stuartwouglas.repoexplorer.model.RepositoryTag;
import com.github.stuartwouglas.repoexplorer.service.LocalClone;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class CloneHandler {

    public static final String REFS_TAGS = "refs/tags/";

    @Transactional
    public void doClone(String uri) throws Exception {
        try (LocalClone checkout = LocalClone.clone(uri)) {
            Repository existing = Repository.find("uri", uri).firstResult();
            Map<String, String> knownTags = new HashMap<>();
            if (existing == null) {
                Repository r = new Repository();
                r.uri = uri;
                r.persist();
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
                    tag.persist();
                }
                System.out.println("NAME: " + ref.getName() + " " + ref.getObjectId().getName());
            }
        }
    }


}
