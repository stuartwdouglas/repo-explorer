package com.github.stuartwouglas.repoexplorer.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.Set;

@Entity
public class Artifact extends PanacheEntity {

    @Column(nullable = false)
    public String artifactId;

    @Column(nullable = false)
    public String groupId;

    @Column(nullable = false)
    public String version;

    @OneToMany(mappedBy = "artifact")
    public Set<ArtifactDependency> dependencies;

    @ManyToOne(optional = false)
    public RepositoryTag tag;

}
