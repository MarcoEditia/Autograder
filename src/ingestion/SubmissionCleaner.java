package ingestion;

import model.Anomaly;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class SubmissionCleaner {

    private static final Pattern HEADER_PATTERN = Pattern.compile(
            "(?s)^\\s*/\\*.*?Name\\s*:.*?Email ID\\s*:.*?\\*/"
    );
    private static final Pattern HEADER_NAME_PATTERN = Pattern.compile(
            "(?m)^[ \\t]*\\*?[ \\t]*Name[ \\t]*:[ \\t]*(.*?)[ \\t]*$"
    );
    private static final Pattern HEADER_EMAIL_PATTERN = Pattern.compile(
            "(?m)^[ \\t]*\\*?[ \\t]*Email ID[ \\t]*:[ \\t]*(.*?)[ \\t]*$"
    );

    // Expected exam structure
    private static final List<String> EXPECTED_QUESTION_DIRECTORIES = List.of("Q1", "Q2", "Q3");
    private static final List<String> EXPECTED_SOURCE_FILES = List.of(
            "Q1/Q1a.java",
            "Q1/Q1b.java",
            "Q2/Q2a.java",
            "Q2/Q2b.java",
            "Q2/persons.txt",
            "Q2/personstester.txt",
            "Q2/students.txt",
            "Q2/studentstester.txt",
            "Q3/Q3.java",
            "Q3/ShapeComparator.java"
    );

    private SubmissionCleaner() {
        // Utility class
    }

    /**
     * Executes the entire ingestion pipeline: inspects, extracts, detects anomalies, and repairs headers.
     */
    public static IngestionResult processSubmission(Path zipFilePath, Path targetDirectory) throws IOException {
        String studentId = extractStudentId(zipFilePath);
        String studentName = inferStudentName(studentId);

        List<Anomaly> anomalies = new ArrayList<>();

        // Detect structural anomalies from zip entries before unizipping
        anomalies.addAll(ZipHandler.inspectZip(zipFilePath, studentId));

        // Prepare student directory in clean-student-submission and unzip
        Path studentDirectory = targetDirectory.toAbsolutePath().normalize().resolve(studentId);
        Files.createDirectories(studentDirectory);
        ZipHandler.unzip(zipFilePath, studentDirectory);

        // Inspect missing directories and required source files
        anomalies.addAll(validateRequiredFiles(studentDirectory, studentId));

        // Inspect headers, record anomalies, and clean/inject standard headers
        anomalies.addAll(inspectAndSanitizeHeaders(studentDirectory, studentId, studentName));

        return new IngestionResult(studentId, studentName, studentDirectory, anomalies);
    }

    public static String extractStudentId(Path zipFilePath) {
        String filename = zipFilePath.getFileName().toString();
        String baseName = filename.toLowerCase().endsWith(".zip")
                ? filename.substring(0, filename.length() - 4)
                : filename;

        String[] parts = baseName.split("[-_]");
        return parts[parts.length - 1].trim();
    }

    private static List<Anomaly> validateRequiredFiles(Path studentDirectory, String studentId) {
        List<Anomaly> anomalies = new ArrayList<>();

        for (String questionDirName : EXPECTED_QUESTION_DIRECTORIES) {
            Path questionDirPath = studentDirectory.resolve(questionDirName);
            if (!Files.isDirectory(questionDirPath)) {
                anomalies.add(new Anomaly(
                    studentId,
                    Anomaly.Type.MISSING_QUESTION_FOLDER,
                    "Missing expected question directory: " + questionDirName
                ));
            }
        }

        for (String sourceFile : EXPECTED_SOURCE_FILES) {
            Path sourcefilePath = studentDirectory.resolve(sourceFile);
            if (!Files.isRegularFile(sourcefilePath)) {
                anomalies.add(new Anomaly(
                    studentId,
                    Anomaly.Type.MISSING_SOURCE_FILE,
                    "Missing expected source file: " + sourceFile
                ));
            }
        }

        return anomalies;
    }

    private static List<Anomaly> inspectAndSanitizeHeaders(Path studentDirectory, String studentId, String studentName) throws IOException {
        List<Anomaly> anomalies = new ArrayList<>();
        String standardHeader = String.format("/*\n * Name: %s\n * Email ID: %s\n */", studentName, studentId);

        try (Stream<Path> stream = Files.walk(studentDirectory)) {
            List<Path> javaFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> !isMacOsMetadata(path))
                    .toList();

            for (Path javaFile : javaFiles) {
                String content = Files.readString(javaFile);
                Matcher matcher = HEADER_PATTERN.matcher(content);
                boolean hasHeaderBlock = matcher.find();
                boolean hasValidHeader = hasHeaderBlock&& hasHeaderValue(HEADER_NAME_PATTERN, matcher.group()) && hasHeaderValue(HEADER_EMAIL_PATTERN, matcher.group());

                // Detect anomaly before modifying
                if (!hasValidHeader) {
                    anomalies.add(new Anomaly(
                        studentId,
                        Anomaly.Type.MISSING_HEADER,
                        "Missing or malformed header comment in " + studentDirectory.relativize(javaFile)
                    ));

                    // Inject standard header
                    Files.writeString(javaFile, standardHeader + "\n\n" + content);
                }

                String updatedContent;
                if (hasHeaderBlock) {
                    // Strip the matched header block (valid, partial, or completely empty)
                    updatedContent = standardHeader + "\n" + content.substring(matcher.end()).stripLeading();
                } else {
                    // No comment block existed at all
                    updatedContent = standardHeader + "\n\n" + content;
                }

                Files.writeString(javaFile, updatedContent);
            }
        }

        return anomalies;
    }

    private static boolean hasHeaderValue(Pattern pattern, String header) {
        Matcher matcher = pattern.matcher(header);
        return matcher.find() && !matcher.group(1).trim().isEmpty();
    }

    private static boolean isMacOsMetadata(Path path) {
        for (Path component : path) {
            if (component.toString().equals("__MACOSX")) {
                return true;
            }
        }

        String fileName = path.getFileName().toString();
        return fileName.equals(".DS_Store") || fileName.startsWith("._");
    }

    public static String inferStudentName(String emailId) {
        String[] parts = emailId.split("\\.");
        if (parts.length >= 2 && isAlpha(parts[0]) && isAlpha(parts[1])) {
            return capitalize(parts[0]) + " " + capitalize(parts[1]);
        }
        if (parts.length >= 1 && isAlpha(parts[0])) {
            return capitalize(parts[0]);
        }
        return emailId;
    }

    private static boolean isAlpha(String str) {
        return str != null && str.matches("[A-Za-z]+");
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }
}