package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReportData {
    private final List<PlagiarismResult> plagiarismResults;
    private final List<Anomaly> anomalies;
    private final List<FinalGrade> grades;

    private final double averageScore;
    private final double medianScore;
    private final double maxScore;
    private final double minScore;

    public ReportData(List<FinalGrade> grades, List<Anomaly> anomalies, List<PlagiarismResult> plagiarismResults) {
        this.anomalies = anomalies;
        this.grades = grades;
        this.plagiarismResults = plagiarismResults;

        List<Double> scores = new ArrayList<>();
        for (FinalGrade grade : this.grades){
            scores.add(grade.getTotalScore());
        }
        Collections.sort(scores);

        if (scores.isEmpty()){
            this.minScore = 0.0;
            this.maxScore = 0.0;
            this.averageScore = 0.0;
            this.medianScore = 0.0;
        } else {
            this.minScore = scores.get(0);
            this.maxScore = scores.get(scores.size() - 1);

            double sum = 0.0;
            for (double s : scores) {
                sum += s;
            }
            this.averageScore = sum / scores.size();

            int size = scores.size();
            if (size % 2 == 0) {
                this.medianScore = (scores.get(size / 2 - 1) + scores.get(size / 2)) / 2.0;
            } else {
                this.medianScore = scores.get(size / 2);
            }
        }
    }

    public String lookupStudentName(String studentId) {
        if (studentId == null) {
            return "Unknown";
        }
        for (FinalGrade grade : grades) {
            if (studentId.equalsIgnoreCase(grade.getStudentId())) {
                return grade.getName();
            }
        }
        return "Unknown";
    }

    public List<FinalGrade> getGrades() {
        return grades;
    }

    public List<Anomaly> getAnomalies() {
        return anomalies;
    }

    public List<PlagiarismResult> getPlagiarismResults() {
        return plagiarismResults;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public double getMedianScore() {
        return medianScore;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public double getMinScore() {
        return minScore;
    }
}