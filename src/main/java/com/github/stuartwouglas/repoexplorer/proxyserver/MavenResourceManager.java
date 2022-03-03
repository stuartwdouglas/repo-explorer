package com.github.stuartwouglas.repoexplorer.proxyserver;

import com.github.stuartwouglas.repoexplorer.service.HashUtil;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.blob.Blob;
import com.google.cloud.tools.jib.event.EventHandlers;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;
import com.google.cloud.tools.jib.http.FailoverHttpClient;
import com.google.cloud.tools.jib.image.json.OciManifestTemplate;
import com.google.cloud.tools.jib.registry.ManifestAndDigest;
import com.google.cloud.tools.jib.registry.RegistryClient;
import io.smallrye.common.annotation.Blocking;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

@Path("/maven2")
@Blocking
public class MavenResourceManager {


    @GET
    @Path("{group:.*?}/{artifact}/{version}/{target}")
    public byte[] get(@PathParam("group") String group, @PathParam("artifact") String artifact, @PathParam("version") String version, @PathParam("target") String target) throws Exception{
        String groupId = group.replace("/",".");
        String hash = HashUtil.sha1(groupId + ":" + artifact + ":" + version);

        ImageReference imageReference = ImageReference.of("quay.io", "stuartwdouglas0/testdeps", hash);
        CredentialRetrieverFactory credentialRetrieverFactory = CredentialRetrieverFactory.forImage(imageReference,
                (s) -> System.out.println(s.getMessage()));

        RegistryClient.Factory factory = RegistryClient.factory(new EventHandlers.Builder().build(), "quay.io", "stuartwdouglas0/testdeps", new FailoverHttpClient(false, false, s -> System.out.println(s.getMessage())));
        factory.setCredential(credentialRetrieverFactory.dockerConfig().retrieve().get());
        ManifestAndDigest<OciManifestTemplate> result = factory.newRegistryClient().pullManifest(hash, OciManifestTemplate.class);

        System.out.println(result);

        int layer = 1;
        if (target.endsWith(".pom") || target.endsWith(".pom.sha1")) {
            layer = 0;
        }

        var blob = factory.newRegistryClient().pullBlob(result.getManifest().getLayers().get(layer).getDigest(), s -> {
        }, s -> {
        });

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        blob.writeTo(out);
        String finalPath = group + "/" + artifact + "/" + version + "/" + target;
        GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(out.toByteArray()));
        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream);
        for (TarArchiveEntry entry = tarArchiveInputStream.getNextTarEntry();
             entry != null;
             entry = tarArchiveInputStream.getNextTarEntry()) {
            if (entry.getName().equals(target)) {
                return tarArchiveInputStream.readAllBytes();
            }
        }
        System.out.println("Failed to find " + finalPath);
        throw new NotFoundException();
    }


}
