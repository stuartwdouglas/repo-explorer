package com.github.stuartwouglas.repoexplorer.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class RepositoryTag extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String ref;

    @ManyToOne(optional = false)
    public Repository repository;

    @OneToMany(mappedBy = "repositoryTag")
    public List<ArtifactTagMapping> artifacts;

    public String buildTool;

}
