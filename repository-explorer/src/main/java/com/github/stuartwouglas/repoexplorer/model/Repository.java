package com.github.stuartwouglas.repoexplorer.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class Repository extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String uri;

    @Column(nullable = true)
    public String name;

    @OneToMany(mappedBy = "repository")
    public List<RepositoryTag> tags;

    @Column(nullable = false)
    public boolean discoveryAttempted;

    @ManyToOne
    public GithubOrg githubOrg;

    public String language;
}
