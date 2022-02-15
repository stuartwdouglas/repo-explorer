package com.github.stuartwouglas.repoexplorer.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class RepositoryTag extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String ref;

    @ManyToOne
    public Repository repository;

}
