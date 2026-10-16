package io;

import model.FinalGrade;
import model.QuestionScore;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CsvWriter {
    
    public void exportToCsv(List<FinalGrade> grades, String fileName) throws IOException{
        File outputFile = new File("reports/" + fileName); // hardcoded reports/ as parent dir
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()){
            parentDir.mkdirs();
        }

        Set<String> allQuestions = new TreeSet<>();
        for (FinalGrade grade : grades){
            for (QuestionScore score : grade.getQuestionScores()){
                allQuestions.add(score.getQuestionName());
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))){
            // Write CSV header row: Student_ID,Name,Q1a,...,Total_Score
            StringBuilder header = new StringBuilder("Studen_ID,Name");
            for(String qName : allQuestions){
                header.append(",").append(escapeCsv(qName));
            }
            header.append(",Total_Score");
            writer.write(header.toString());
            writer.newLine();

            // Write wor per student
            for (FinalGrade grade : grades){
                StringBuilder row = new StringBuilder();
                row.append(escapeCsv(grade.getStudentId()))
                   .append(",")
                   .append(escapeCsv(grade.getName()));
                for (String qName : allQuestions){
                    double score = 0.0;
                    for (QuestionScore qs : grade.getQuestionScores()){
                        if (qs.getQuestionName().equals(qName)){
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
