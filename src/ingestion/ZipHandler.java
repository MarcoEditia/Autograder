package ingestion;

import model.Anomaly;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ZipHandler {

    private static final String DEFAULT_RESOURCE_FOLDER = "RenameToYourUsername";
    private static final Pattern QUESTION_FOLDER_PATTERN = Pattern.compile("^Q\\d+.*", Pattern.CASE_INSENSITIVE);

    private ZipHandler() {
        // Utility class
    }

    /**
     * Inspects the zip file structure and records any packaging anomalies.
     */
    public static List<Anomaly> inspectZip(Path zipFilePath, String studentId) throws IOException {
        List<Anomaly> detectedAnomalies = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            List<? extends ZipEntry> entries = zipFile.stream().toList();
            Set<String> topLevelNames = getTopLevelNames(entries);

            // Anomaly 1: Flattened structure (Q1, Q2 directly at root)
            boolean missingRootUsernameFolder = topLevelNames.stream().anyMatch(name -> QUESTION_FOLDER_PATTERN.matcher(name).matches());
            // Anomaly 2: has unrenamed template folder name
            boolean unrenamedRootFolder = topLevelNames.stream().anyMatch(name -> name.equalsIgnoreCase(DEFAULT_RESOURCE_FOLDER) || name.equalsIgnoreCase("RenameToYourStudentID"));
            // Anomaly 3: Random / wrong root folder name
            boolean incorrectRootFolder = topLevelNames.stream().anyMatch(name -> !(name.equalsIgnoreCase(studentId)));
            
            if (missingRootUsernameFolder) {
                detectedAnomalies.add(new Anomaly(
                    Anomaly.Type.FLATTENED_HIERARCHY,
                    " " + topLevelNames + "Zip contains question folders directly at root without student username folder"
                ));
            } else if (unrenamedRootFolder) {
                detectedAnomalies.add(new Anomaly(
                    Anomaly.Type.UNRENAMED_FOLDER,
                    "Zip contains default unrenamed template folder '" + DEFAULT_RESOURCE_FOLDER + "'"
                ));
            } else if (incorrectRootFolder) {
                detectedAnomalies.add(new Anomaly(
                    Anomaly.Type.INCORRECT_FOLDER,
                    "Zip contains default incorrect root folder '" + topLevelNames +  "'"
                ));
            }
        }

        return detectedAnomalies;
    }

    /**
     * Unzips the archive into targetDirectory, automatically stripping any top-level wrapper.
     */
    public static void unzip(Path zipFilePath, Path targetDirectory) throws IOException {
        Path destination = targetDirectory.toAbsolutePath().normalize();
        Files.createDirectories(destination);

        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            List<? extends ZipEntry> entries = new ArrayList<>(zipFile.stream().toList());
            String wrapperDirectory = findWrapperDirectory(entries);

            for (ZipEntry entry : entries) {
                String entryName = entry.getName().replace('\\', '/');

                // Strip wrapper directory if present (e.g. "RenameToYourUsername/" or "jiagu.wen.2025/")
                if (wrapperDirectory != null && entryName.startsWith(wrapperDirectory + "/")) {
                    entryName = entryName.substring(wrapperDirectory.length() + 1);
                }

                if (entryName.isBlank()) {
                    continue;
                }

                Path targetPath = destination.resolve(entryName).normalize();

                // Prevent Zip Slip vulnerability
                if (!targetPath.startsWith(destination)) {
                    throw new SecurityException("Blocked Zip Slip attempt: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    if (targetPath.getParent() != null) {
                        Files.createDirectories(targetPath.getParent());
                    }
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private static Set<String> getTopLevelNames(List<? extends ZipEntry> entries) {
        Set<String> topLevelNames = new HashSet<>();
        for (ZipEntry entry : entries) {
            String entryName = entry.getName().replace('\\', '/');
            while (entryName.startsWith("/")) {
                entryName = entryName.substring(1);
            }
            if (entryName.isBlank()) {
                continue;
            }
            int separator = entryName.indexOf('/');
            topLevelNames.add(separator < 0 ? entryName : entryName.substring(0, separator));
        }
        return topLevelNames;
    }

    private static String findWrapperDirectory(List<? extends ZipEntry> entries) {
        Set<String> topLevelNames = new HashSet<>();
        boolean hasNestedEntry = false;

        for (ZipEntry entry : entries) {
            String entryName = entry.getName().replace('\\', '/');
            while (entryName.startsWith("/")) {
                entryName = entryName.substring(1);
            }
            if (entryName.isBlank()) {
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
            String candidate = topLevelNames.iterator().next();
            if (!QUESTION_FOLDER_PATTERN.matcher(candidate).matches()) {
                return candidate;
            }
        }
        return null;
    }
}