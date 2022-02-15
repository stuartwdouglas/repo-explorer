package com.github.stuartwouglas;

import com.github.stuartwouglas.repoexplorer.CloneHandler;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class TestRunner implements QuarkusApplication {

    public static void main(String ... args ) throws Exception {
        Quarkus.run(TestRunner.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        CloneHandler.doCheckout();
        return 0;
    }
}
