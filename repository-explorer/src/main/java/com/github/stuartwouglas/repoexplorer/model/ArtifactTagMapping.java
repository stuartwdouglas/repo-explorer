package com.github.stuartwouglas.repoexplorer.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

@Entity
public class ArtifactTagMapping extends PanacheEntity {

    /**
     * TODO: should be many to one?
     */
    @OneToOne(optional = false)
    @JoinColumn
    public Artifact artifact;
    /**
     * The tag
     */
    @ManyToOne(optional = false)
    public RepositoryTag repositoryTag;
    /**
     * The path to the module in the repository
     */
    public String path;

}
