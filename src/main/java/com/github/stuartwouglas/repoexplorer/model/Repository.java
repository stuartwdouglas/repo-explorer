package com.github.stuartwouglas.repoexplorer.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class Repository extends PanacheEntity {

    @Column(nullable = false)
    public String url;

    @Column(nullable = true)
    public String name;

    @OneToMany
    public List<RepositoryTag> tags;
}
