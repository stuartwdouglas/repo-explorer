package com.github.stuartwouglas.repoexplorer.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class GithubOrg extends PanacheEntity {


    @Column(unique = true, nullable = false)
    public String name;

    public String includes;

    public String getName() {
        return name;
    }

    public GithubOrg setName(String name) {
        this.name = name;
        return this;
    }

    public String getIncludes() {
        return includes;
    }

    public GithubOrg setIncludes(String includes) {
        this.includes = includes;
        return this;
    }
}
