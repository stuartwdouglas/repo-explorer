package com.github.stuartwouglas.repoexplorer.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class ArtifactDependency extends PanacheEntity {

    @ManyToOne(optional = false)
    public Artifact parent;

    @ManyToOne(optional = false)
    public Artifact dependency;

    @Column(nullable = true)
    public String scope;
}
