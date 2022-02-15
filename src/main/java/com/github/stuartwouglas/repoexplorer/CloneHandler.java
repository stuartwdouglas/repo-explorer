package com.github.stuartwouglas.repoexplorer;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;

import java.nio.file.Files;
import java.nio.file.Path;

public class CloneHandler {

    public static void main(String... args) throws Exception {
        doCheckout();
    }

    public static void doCheckout() throws Exception {
        Path temp = Files.createTempDirectory("test");
        Git result = new CloneCommand()
                .setDirectory(temp.toFile())
                .setURI("file:///home/stuart/workspace/gizmo")
                .call();
        for (var ref : result.tagList().call()) {
            String name = ref.getName();

            System.out.println("NAME: " + ref.getName() + " " + ref.getObjectId().getName());
        }
    }


}
