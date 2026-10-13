package ingestion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


public final class ZipHandler {
    private ZipHandler() {
        // Zip handler class
    }   
    public static void unzip(Path zipFilePath, Path targetDirectory) throws IOException {
        Path destination = targetDirectory.toAbsolutePath().normalize();

        if (!Files.exists(destination)) {
            Files.createDirectories(destination);
        }

        System.out.println("Unzipping: " + zipFilePath);
        System.out.println("...");

        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            List<? extends ZipEntry> entries = new ArrayList<>(zipFile.stream().toList());
            String wrapperDirectory = findWrapperDirectory(entries);

            for (ZipEntry entry : entries) {
                String entryName = entry.getName().replace('\\', '/');
                if (wrapperDirectory != null && entryName.startsWith(wrapperDirectory + "/")) {
                    entryName = entryName.substring(wrapperDirectory.length() + 1);
                }

                if (entryName.isEmpty()) {
                    continue;
                }

                Path targetPath = destination.resolve(entryName).normalize();

                if (!targetPath.startsWith(destination)) {
                    throw new SecurityException("Blocked Zip Slip attempt from entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    if (targetPath.getParent() != null) {
                        Files.createDirectories(targetPath.getParent());
                    }

                    // Open stream directly for this specific entry
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            System.out.println("Successfully Unzipped: " + zipFilePath + " to " + targetDirectory);
            System.out.println("...");
        }
    }

    private static String findWrapperDirectory(List<? extends ZipEntry> entries) {
        Set<String> topLevelNames = new HashSet<>();
        boolean hasNestedEntry = false;

        for (ZipEntry entry : entries) {
            String entryName = entry.getName().replace('\\', '/');
            while (entryName.startsWith("/")) {
                entryName = entryName.substring(1);
            }
            if (entryName.isEmpty()) {
                continue;
            }

            int separator = entryName.indexOf('/');
            if (separator < 0) {
                topLevelNames.add(entryName);
            } else {
                hasNestedEntry = true;
                topLevelNames.add(entryName.substring(0, separator));
            }
        }

        if (hasNestedEntry && topLevelNames.size() == 1) {
            String topLevelName = topLevelNames.iterator().next();
            if (!topLevelName.equals("Q1") && !topLevelName.equals("Q2") && !topLevelName.equals("Q3")) {
                return topLevelName;
            }
        }
        return null;
    }
    
} 