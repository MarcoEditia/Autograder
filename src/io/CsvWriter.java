package io;

import model.FinalGrade;
import model.QuestionScore;
import model.ReportData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generates a detailed CSV scoresheet export containing individual 
 * question marks and total scores for the cohort.
 */
public class CsvWriter implements ReportExporter {

    @Override
    public void export(ReportData data, String outputPath) throws IOException {
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Collect all unique question names across the cohort (automatically sorted)
        Set<String> allQuestions = new TreeSet<>();
        for (FinalGrade grade : data.getGrades()) {
            for (QuestionScore score : grade.getQuestionScores()) {
                allQuestions.add(score.getQuestionName());
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            // Write CSV Header: Student_ID,Name,Q1a,Q1b,...,Total_Score
            StringBuilder header = new StringBuilder("Student_ID,Name");
            for (String qName : allQuestions) {
                header.append(",").append(escapeCsv(qName));
            }
            header.append(",Total_Score");
            writer.write(header.toString());
            writer.newLine();

            // Write Row per Student
            for (FinalGrade grade : data.getGrades()) {
                StringBuilder row = new StringBuilder();
                row.append(escapeCsv(grade.getStudentId()))
                   .append(",")
                   .append(escapeCsv(grade.getName()));

                // Match score for each collected question name
                for (String qName : allQuestions) {
                    double score = 0.0;
                    for (QuestionScore qs : grade.getQuestionScores()) {
                        if (qs.getQuestionName().equals(qName)) {
                            score = qs.getScore();
                            break;
                        }
                    }
                    row.append(",").append(score);
                }

                row.append(",").append(grade.getTotalScore());
                writer.write(row.toString());
                writer.newLine();
            }
        }
    }
    // Escape fields containing commas, quotes, or newlines for CSV format.
    private String escapeCsv(String field) {
        if (field == null) {
            return "\"\"";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

}
