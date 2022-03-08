package com.github.stuartwouglas.repoexplorer.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Set;

@Entity
public class GithubOrg extends PanacheEntity {


    @Column(unique = true, nullable = false)
    public String name;

    public String includes;

    public String allowedGroups;

}
