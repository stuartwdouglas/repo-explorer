package com.github.stuartwouglas.repoexplorer.model;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Parameters;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.transaction.TransactionScoped;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a maven artifact
 */
@Entity
public class Artifact extends PanacheEntity {

    @Column(nullable = false)
    public String artifactId;

    @Column(nullable = false)
    public String groupId;

    @Column(nullable = false)
    public String version;

    @OneToMany(mappedBy = "parent")
    public Set<ArtifactDependency> dependencies;

    public static Artifact find(String groupId, String artifactId, String version) {
        return Artifact.find("artifactId=:a and groupId=:g and version=:v", Parameters.with("a", artifactId).and("g", groupId).and("v", version).map()).firstResult();
    }

    public static Artifact findOrCreate(String groupId, String artifactId, String version) {
        var existing = Arc.container().instance(Cache.class).get().find(groupId, artifactId, version);
        if (existing != null) {
            return existing;
        }
        Artifact artifact = find(groupId, artifactId, version);
        if (artifact == null) {
            artifact = new Artifact();
            artifact.artifactId = artifactId;
            artifact.groupId = groupId;
            artifact.version = version;
            artifact.persistAndFlush();
        }
        Arc.container().instance(Cache.class).get().put(groupId, artifactId, version, artifact);
        return artifact;
    }

}

