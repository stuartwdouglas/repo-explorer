package com.github.stuartwouglas.repoexplorer.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class DirectoryUtils {

    public static void delete(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (Stream<Path> pathStream = Files.list(path)) {
                    pathStream.forEach(DirectoryUtils::delete);
                }
                Files.delete(path);
            } else {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete " + path);
        }
    }
}
