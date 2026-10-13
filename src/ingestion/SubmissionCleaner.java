package ingestion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SubmissionCleaner {
    private SubmissionCleaner(){
        // Clean submission
    }

    public static Path createStudentSubmissionDirectory(Path submissionDirectory, Path targetDirectory) throws IOException{

        String name = submissionDirectory.getFileName().toString().replace(".zip", "").split("-")[2];

        Path destination = targetDirectory.toAbsolutePath().normalize();
        Path submissionDestination =  destination.resolve(name);

        if (Files.exists(submissionDestination)) {
            System.out.println("Directory already exists: " + submissionDestination.toAbsolutePath());
            return submissionDestination; 
        }

        try {
            Files.createDirectories(submissionDestination);
            System.out.println("Directory created and ready: " + submissionDestination.toAbsolutePath());
            return submissionDestination;
        } catch (IOException e) {
            System.err.println("Failed to create directory: " + e.getMessage());
        }
        return submissionDestination;
    }

    public static void fixSubmissionNameAndEmail(Path cleanSubmissionDirectory) throws IOException {
        String emailId = cleanSubmissionDirectory.getFileName().toString();
        String name = inferStudentName(emailId);
        String header = "/*\n * Name: " + name + "\n * Email ID: " + emailId + "\n */";
        Pattern existingHeader = Pattern.compile("(?s)^\\s*/\\*.*?Name\\s*:.*?Email ID\\s*:.*?\\*/");

        System.out.println("Renaming: " + cleanSubmissionDirectory);
        System.out.println("...");

        try (Stream<Path> files = Files.walk(cleanSubmissionDirectory)) {
            List<Path> javaFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .toList();

            for (Path javaFile : javaFiles) {
                String contents = Files.readString(javaFile);
                Matcher matcher = existingHeader.matcher(contents);
                String updatedContents;

                if (matcher.find()) {
                    updatedContents = header + contents.substring(matcher.end());
                } else {
                    updatedContents = header + "\n\n" + contents;
                }

                Files.writeString(javaFile, updatedContents);
            }
        }
    
        System.out.println("Finish renaming: " + cleanSubmissionDirectory);
        System.out.println("...");
    }

    private static String inferStudentName(String emailId) {
        String[] parts = emailId.split("\\.");
        if (parts.length >= 3 && isNamePart(parts[0]) && isNamePart(parts[1])) {
            return capitalize(parts[1]) + " " + capitalize(parts[0]);
        }
        if (parts.length >= 1 && isNamePart(parts[0])) {
            return capitalize(parts[0]);
        }
        return emailId;
    }

    private static boolean isNamePart(String value) {
        return value.matches("[A-Za-z]+");
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }
}