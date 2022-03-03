package com.github.stuartwouglas.repoexplorer.service;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.FilePermissions;
import com.google.cloud.tools.jib.api.buildplan.ImageFormat;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageMaker {


    public static void main(String... args) throws Exception {
        Path base = Paths.get("/home/stuart/workspace/quarkus-quickstarts/getting-started/repo");
        AtomicInteger count = new AtomicInteger();
        Files.walkFileTree(base, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(".jar")) {
                    Path parent = file.getParent();
                    String version = parent.getFileName().toString();
                    String artifactId = parent.getParent().getFileName().toString();
                    String groupId = base.relativize(parent.getParent().getParent()).toString().replace("/", ":");
                    System.out.println("G:" + groupId + " A:" + artifactId + " V:" + version);
                    try {
                        doBuild(file.getParent(), groupId, artifactId, version, base);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
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
    }

    public static void doBuild(Path path, String group, String artifact, String version, Path base) throws Exception {


        String gavString = group + ":" + artifact + ":" + version;
        String tag = HashUtil.sha1(gavString);

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
