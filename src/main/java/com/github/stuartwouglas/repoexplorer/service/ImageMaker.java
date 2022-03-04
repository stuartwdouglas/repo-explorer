package com.github.stuartwouglas.repoexplorer.service;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.Credential;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.FilePermissions;
import com.google.cloud.tools.jib.api.buildplan.ImageFormat;
import com.google.cloud.tools.jib.event.EventHandlers;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;
import com.google.cloud.tools.jib.http.FailoverHttpClient;
import com.google.cloud.tools.jib.image.json.ManifestTemplate;
import com.google.cloud.tools.jib.registry.ManifestAndDigest;
import com.google.cloud.tools.jib.registry.RegistryClient;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ImageMaker {

    static Credential credential;

    public static void main(String... args) throws Exception {


        ImageReference imageReference = ImageReference.of("quay.io", "stuartwdouglas0/testdeps", "03cda3f4d6bc8f3aaa6fa526c1739ef772b77b45");
        CredentialRetrieverFactory credentialRetrieverFactory = CredentialRetrieverFactory.forImage(imageReference,
                (s) -> System.out.println(s.getMessage()));

        credential = credentialRetrieverFactory.dockerConfig().retrieve().get();
        RegistryClient.Factory factory = RegistryClient.factory(new EventHandlers.Builder().build(), "quay.io", "stuartwdouglas0/testdeps", new FailoverHttpClient(false, false, s -> System.out.println(s.getMessage())));
        factory.setCredential(credential);
        var client = factory.newRegistryClient();

        ExecutorService executorService = Executors.newFixedThreadPool(8);

        Set<String> seen = new HashSet<>();
        Path base = Paths.get("/home/stuart/workspace/quarkus-quickstarts/getting-started/repo");
        List<Future<?>> results = new ArrayList<>();
        Files.walkFileTree(base, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(".pom") || file.getFileName().toString().endsWith(".jar") || file.getFileName().toString().endsWith(".properties")) {
                    results.add(executorService.submit(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            Path parent = file.getParent();
                            if (seen.contains(parent.toString())) {
                                return FileVisitResult.CONTINUE;
                            }
                            seen.add(parent.toString());
                            String version = parent.getFileName().toString();
                            String artifactId = parent.getParent().getFileName().toString();
                            //TODO: FIXME: wrong replacement
                            String groupId = base.relativize(parent.getParent().getParent()).toString().replace("/", ".");
                            System.out.println("G:" + groupId + " A:" + artifactId + " V:" + version);
                            try {
                                doBuild(file.getParent(), groupId, artifactId, version, base, client);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            return null;
                        }
                    }));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        for (var i : results) {
            i.get();
        }
    }

    public static void doBuild(Path path, String group, String artifact, String version, Path base, RegistryClient client) throws Exception {


        String gavString = group + ":" + artifact + ":" + version;
        String tag = gavString;
        String deleteTag = null;
        if (gavString.length() > 120) {
            tag = HashUtil.sha1(gavString);
        } else {
            tag = TagUtil.tagName(group, artifact, version);
            deleteTag = HashUtil.sha1(gavString);
        }

        Optional<ManifestAndDigest<ManifestTemplate>> result = client.checkManifest(tag);
        if (result.isPresent()) {
            return;
        }

        Containerizer containerizer;
        ImageReference imageReference = ImageReference.of("quay.io",
                "stuartwdouglas0/testdeps", tag);


        RegistryImage registryImage = toRegistryImage(imageReference);
        containerizer = Containerizer.to(registryImage);
        containerizer.setToolName("Test");
        containerizer.setToolVersion("0.1");
        containerizer.addEventHandler(LogEvent.class, (e) -> {
            if (!e.getMessage().isEmpty()) {
                System.out.println(e.getMessage());
            }
        });
        containerizer.setAllowInsecureRegistries(false);
        containerizer.setAlwaysCacheBaseImage(true);
        containerizer.setOfflineMode(false);

        FileEntriesLayer.Builder layer = FileEntriesLayer.builder().setName("Content Layer");
        FileEntriesLayer.Builder pomLayer = FileEntriesLayer.builder().setName("Pom Layer");

        Files.newDirectoryStream(path).forEach(f -> {
            AbsoluteUnixPath pathInContainer = AbsoluteUnixPath.fromPath(Paths.get("/repo").resolve(base.relativize(f)));
            if (f.getFileName().toString().endsWith(".pom") || f.getFileName().toString().endsWith(".pom.sha1")) {
                pomLayer.addEntry(f, pathInContainer, FilePermissions.DEFAULT_FILE_PERMISSIONS, Instant.EPOCH);
            } else {
                layer.addEntry(f, pathInContainer, FilePermissions.DEFAULT_FILE_PERMISSIONS, Instant.EPOCH);
            }
        });

        JibContainer container = Jib.fromScratch()
                .setFormat(ImageFormat.OCI)
                .addFileEntriesLayer(pomLayer.build())
                .addFileEntriesLayer(layer.build())
                .addLabel("groupId", group)
                .addLabel("artifactId", artifact)
                .addLabel("version", version)
                .containerize(containerizer);

        if (deleteTag != null) {
            CloseableHttpClient closeableHttpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(new CredentialsProvider() {
                        @Override
                        public void setCredentials(AuthScope authscope, Credentials credentials) {

                        }

                        @Override
                        public Credentials getCredentials(AuthScope authscope) {
                            return new Credentials() {
                                @Override
                                public Principal getUserPrincipal() {
                                    return new Principal() {
                                        @Override
                                        public String getName() {
                                            return credential.getUsername();
                                        }
                                    };
                                }

                                @Override
                                public String getPassword() {
                                    return credential.getPassword();
                                }
                            };
                        }

                        @Override
                        public void clear() {

                        }
                    }).build();
            HttpDelete request = new HttpDelete("https://quay.io/v2/stuartwdouglas0/testdeps/manifests/" + deleteTag);
            System.out.println("DELETE:" + closeableHttpClient.execute(request).getStatusLine().getStatusCode());
        }

        System.out.println("built " + tag);

    }


    private static RegistryImage toRegistryImage(ImageReference imageReference) {
        CredentialRetrieverFactory credentialRetrieverFactory = CredentialRetrieverFactory.forImage(imageReference,
                (s) -> System.out.println(s.getMessage()));
        RegistryImage registryImage = RegistryImage.named(imageReference);
        registryImage.addCredentialRetriever(credentialRetrieverFactory.wellKnownCredentialHelpers());
        registryImage.addCredentialRetriever(credentialRetrieverFactory.dockerConfig());
        return registryImage;
    }

}
