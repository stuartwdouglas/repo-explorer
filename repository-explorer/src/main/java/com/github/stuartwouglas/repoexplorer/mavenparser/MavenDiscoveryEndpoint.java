package com.github.stuartwouglas.repoexplorer.mavenparser;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/trigger-discovery")
public class MavenDiscoveryEndpoint {

    final MavenDiscoveryRunner mavenDiscoveryRunner;

    public MavenDiscoveryEndpoint(MavenDiscoveryRunner mavenDiscoveryRunner) {
        this.mavenDiscoveryRunner = mavenDiscoveryRunner;
    }

    @POST
    public void post() {
        mavenDiscoveryRunner.trigger();
    }
}
