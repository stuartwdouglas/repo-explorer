package com.github.stuartwdouglas.quayproxy;

import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.event.EventHandlers;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;
import com.google.cloud.tools.jib.http.FailoverHttpClient;
import com.google.cloud.tools.jib.image.json.OciManifestTemplate;
import com.google.cloud.tools.jib.registry.ManifestAndDigest;
import com.google.cloud.tools.jib.registry.RegistryClient;
import com.google.cloud.tools.jib.registry.credentials.CredentialRetrievalException;
import io.smallrye.common.annotation.Blocking;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

@Path("/maven2")
@Blocking
public class MavenResourceManager {
final RegistryClient registryClient;

    public MavenResourceManager() throws CredentialRetrievalException {
        ImageReference imageReference = ImageReference.of("quay.io", "stuartwdouglas0/testdeps", "bogus");
        CredentialRetrieverFactory credentialRetrieverFactory = CredentialRetrieverFactory.forImage(imageReference,
                (s) -> System.out.println(s.getMessage()));

        RegistryClient.Factory factory = RegistryClient.factory(new EventHandlers.Builder().build(), "quay.io", "stuartwdouglas0/testdeps", new FailoverHttpClient(false, false, s -> System.out.println(s.getMessage())));
        factory.setCredential(credentialRetrieverFactory.dockerConfig().retrieve().get());
        registryClient = factory.newRegistryClient();
    }


    @GET
    @Path("{group:.*?}/{artifact}/{version}/{target}")
    public byte[] get(@PathParam("group") String group, @PathParam("artifact") String artifact, @PathParam("version") String version, @PathParam("target") String target) throws Exception{
        String groupId = group.replace("/",".");
        String hash = TagUtil.tagName(groupId, artifact, version);

        ManifestAndDigest<OciManifestTemplate> result = registryClient.pullManifest(hash, OciManifestTemplate.class);

        System.out.println(result);

        int layer = 1;
        if (target.endsWith(".pom") || target.endsWith(".pom.sha1") || result.getManifest().getLayers().size() == 1) {
            layer = 0;
        }

        var blob = registryClient.pullBlob(result.getManifest().getLayers().get(layer).getDigest(), s -> {
        }, s -> {
        });

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        blob.writeTo(out);
        String finalPath ="repo/" + group + "/" + artifact + "/" + version + "/" + target;
        GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(out.toByteArray()));
        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream);
        for (TarArchiveEntry entry = tarArchiveInputStream.getNextTarEntry();
             entry != null;
             entry = tarArchiveInputStream.getNextTarEntry()) {
            if (entry.getName().equals(finalPath)) {
                return tarArchiveInputStream.readAllBytes();
            }
        }
        System.out.println("Failed to find " + finalPath);
        throw new NotFoundException();
    }


}
