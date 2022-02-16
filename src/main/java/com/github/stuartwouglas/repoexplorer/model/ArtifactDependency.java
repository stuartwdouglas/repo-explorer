package com.github.stuartwouglas.repoexplorer.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class ArtifactDependency extends PanacheEntity {

    @ManyToOne(optional = false)
    public Artifact artifact;

    @Column(nullable = false)
    public String artifactId;

    @Column(nullable = false)
    public String groupId;

    @Column(nullable = false)
    public String version;

    @Column(nullable = false)
    public String scope;
}
