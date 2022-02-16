package com.github.stuartwouglas;

import com.github.stuartwouglas.repoexplorer.CloneHandler;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import javax.inject.Inject;

@QuarkusMain
public class TestRunner implements QuarkusApplication {

    final CloneHandler cloneHandler;

    @Inject
    public TestRunner(CloneHandler cloneHandler) {
        this.cloneHandler = cloneHandler;
    }

    public static void main(String ... args ) throws Exception {
        Quarkus.run(TestRunner.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        cloneHandler.doClone("file:///home/stuart/workspace/gizmo");
        return 0;
    }
}
