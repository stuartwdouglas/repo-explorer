package com.github.stuartwouglas.repoexplorer.model;

import javax.transaction.TransactionScoped;
import java.util.HashMap;
import java.util.Map;

@TransactionScoped
public class Cache {
    final Map<String, Map<String, Map<String, Artifact>>> cache = new HashMap<>();

    public Artifact find(String groupId, String artifactId, String version) {
        return cache.computeIfAbsent(groupId, s -> new HashMap<>()).computeIfAbsent(artifactId, s -> new HashMap<>()).get(version);
    }

    public void put(String groupId, String artifactId, String version, Artifact artifact) {
        cache.computeIfAbsent(groupId, s -> new HashMap<>()).computeIfAbsent(artifactId, s -> new HashMap<>()).put(version, artifact);
    }
}
