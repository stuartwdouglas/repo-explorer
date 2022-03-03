package com.github.stuartwouglas.repoexplorer.service;

import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.blob.Blob;
import com.google.cloud.tools.jib.event.EventHandlers;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;
import com.google.cloud.tools.jib.http.FailoverHttpClient;
import com.google.cloud.tools.jib.image.json.OciManifestTemplate;
import com.google.cloud.tools.jib.registry.ManifestAndDigest;
import com.google.cloud.tools.jib.registry.RegistryClient;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

public class ImageGrabber {


    public static void main(String... test) throws Exception {

        ImageReference imageReference = ImageReference.of("quay.io", "stuartwdouglas0/testdeps", "03cda3f4d6bc8f3aaa6fa526c1739ef772b77b45");
        CredentialRetrieverFactory credentialRetrieverFactory = CredentialRetrieverFactory.forImage(imageReference,
                (s) -> System.out.println(s.getMessage()));

        RegistryClient.Factory factory = RegistryClient.factory(new EventHandlers.Builder().build(), "quay.io", "stuartwdouglas0/testdeps", new FailoverHttpClient(false, false, s -> System.out.println(s.getMessage())));
        factory.setCredential(credentialRetrieverFactory.dockerConfig().retrieve().get());
        ManifestAndDigest<OciManifestTemplate> result = factory.newRegistryClient().pullManifest("03cda3f4d6bc8f3aaa6fa526c1739ef772b77b45", OciManifestTemplate.class);

        System.out.println(result);
        var blob = factory.newRegistryClient().pullBlob(result.getManifest().getLayers().get(0).getDigest(), s -> {
        }, s -> {
        });

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        blob.writeTo(out);
        System.out.println(out.size());
        GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(out.toByteArray()));
        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream);
        for (TarArchiveEntry entry = tarArchiveInputStream.getNextTarEntry();
             entry != null;
             entry = tarArchiveInputStream.getNextTarEntry()) {
            System.out.println(entry.getName());
        }
        System.out.println(result);

    }
}
