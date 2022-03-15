package com.github.stuartwouglas.repoexplorer.scm;

import com.github.stuartwouglas.repoexplorer.model.GithubOrg;
import com.github.stuartwouglas.repoexplorer.model.Repository;
import io.quarkus.narayana.jta.runtime.TransactionConfiguration;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("github")
public class GithubOrgEndpoint {

    @Inject
    Event<RepositoryAddedEvent> added;

    @POST
    @Transactional
    @TransactionConfiguration(timeout = 6000)
    public void addOrg(Org data) throws Exception {
        GithubOrg existing = GithubOrg.find("name", data.name).firstResult();
        if (existing == null) {
            GithubOrg org = new GithubOrg();
            org.name = data.name;
            org.includes = data.includes;
            org.persistAndFlush();
        }
        PagedIterable<GHRepository> repos = GitHub.connect().getOrganization(data.name).listRepositories(1000);
        repos.forEach(s -> {
            if (s.isFork()) {
                return;
            }
            String httpTransportUrl = s.getHttpTransportUrl();
            Repository existingRepo = Repository.find("uri", httpTransportUrl).firstResult();
            if (existingRepo == null) {
                System.out.println(httpTransportUrl);
                //System.out.println(s.getFileContent("pom.xml"));
                Repository r = new Repository();
                r.uri = httpTransportUrl;
                r.name = s.getName();
                r.language = s.getLanguage();
                r.persistAndFlush();
            }
        });
        added.fire(new RepositoryAddedEvent());
    }

    public static class Org {
        private String name;
        private String includes;

        public String getName() {
            return name;
        }

        public Org setName(String name) {
            this.name = name;
            return this;
        }

        public String getIncludes() {
            return includes;
        }

        public Org setIncludes(String includes) {
            this.includes = includes;
            return this;
        }
    }

}
