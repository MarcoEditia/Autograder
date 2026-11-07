package io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.FinalGrade;
import model.ReportData;

public class CsvWriter {

    private final Path templatePath;

    public CsvWriter(Path templatePath) {
        this.templatePath = templatePath;
    }

    public void export(ReportData data, String outputPath) throws IOException {
        Path outputFile = Path.of(outputPath).toAbsolutePath().normalize();

        if (outputFile.equals(templatePath.toAbsolutePath().normalize())) {
            throw new IOException("Output must be different from the template.");
        }

        // Read the template before opening the output file.
        List<String> lines = Files.readAllLines(
                templatePath,
                StandardCharsets.UTF_8
        );

        if (lines.isEmpty()) {
            throw new IOException("The CSV template is empty.");
        }

        // Remove an optional UTF-8 BOM.
        String headerLine = lines.get(0).replaceFirst("^\uFEFF", "");
        List<String> headers = parseCsvLine(headerLine);

        int usernameColumn = headers.indexOf("Username");
        int scoreColumn = headers.indexOf(
                "Calculated Final Grade Numerator"
        );

        if (usernameColumn == -1 || scoreColumn == -1) {
            throw new IOException("Required CSV columns were not found.");
        }

        // Student ID -> grading result.
        Map<String, FinalGrade> gradesById = new HashMap<>();

        for (FinalGrade grade : data.getGrades()) {
            gradesById.put(grade.getStudentId(), grade);
        }

        // Prepare the updated rows.
        List<List<String>> updatedRows = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }

            List<String> columns = parseCsvLine(lines.get(i));

            if (columns.size() != headers.size()) {
                throw new IOException(
                        "Incorrect number of columns on CSV line " + (i + 1)
                );
            }

            String studentId = columns.get(usernameColumn).trim();

            if (studentId.startsWith("#")) {
                studentId = studentId.substring(1);
            }

            FinalGrade grade = gradesById.get(studentId);

            columns.set(
                    scoreColumn,
                    grade == null
                            ? ""
                            : Double.toString(grade.getTotalScore())
            );

            updatedRows.add(columns);
        }

        Path parent = outputFile.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                outputFile,
                StandardCharsets.UTF_8)) {

            // Keep the template's original header.
            writer.write(headerLine);
            writer.newLine();

            for (List<String> columns : updatedRows) {
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) {
                        writer.write(",");
                    }

                    writer.write(escapeCsv(columns.get(i)));
                }

                writer.newLine();
            }
        }
    }

    // Supports quoted commas and escaped quotation marks.
    private List<String> parseCsvLine(String line) throws IOException {
        List<String> columns = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);

            if (current == '"') {
                if (insideQuotes
                        && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {

                    value.append('"');
                    i++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (current == ',' && !insideQuotes) {
                columns.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }

        if (insideQuotes) {
            throw new IOException("Unclosed quotation mark in CSV row.");
        }

        columns.add(value.toString());
        return columns;
    }

    private String escapeCsv(String field) {
        if (field == null) {
            return "";
        }

        if (field.contains(",")
                || field.contains("\"")
                || field.contains("\n")
                || field.contains("\r")) {

            return "\"" + field.replace("\"", "\"\"") + "\"";
        }

        return field;
    }
}