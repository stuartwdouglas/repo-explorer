package com.github.stuartwouglas.repoexplorer.service;

import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.event.EventHandlers;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;
import com.google.cloud.tools.jib.http.FailoverHttpClient;
import com.google.cloud.tools.jib.registry.RegistryClient;
import com.google.cloud.tools.jib.registry.credentials.CredentialRetrievalException;

public class JibClientUtil {

    public static RegistryClient client() throws CredentialRetrievalException {
        ImageReference imageReference = ImageReference.of("quay.io", "stuartwdouglas0/testdeps", "bogus");
        CredentialRetrieverFactory credentialRetrieverFactory = CredentialRetrieverFactory.forImage(imageReference,
                (s) -> System.out.println(s.getMessage()));

        RegistryClient.Factory factory = RegistryClient.factory(new EventHandlers.Builder().build(), "quay.io", "stuartwdouglas0/testdeps", new FailoverHttpClient(false, false, s -> System.out.println(s.getMessage())));
        factory.setCredential(credentialRetrieverFactory.dockerConfig().retrieve().get());
        return factory.newRegistryClient();
    }

}
