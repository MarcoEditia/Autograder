package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FinalGrade {
    private final String studentId;
    private final String name;
    private final List<QuestionScore> questionScore;
    private final List<Anomaly> anomalies;
    private final List<PlagiarismResult> plagiarismFlags;

    public FinalGrade(String studentId, String name){
        this.studentId = studentId;
        this.name = name;
        this.questionScore = new ArrayList<>();
        this.anomalies = new ArrayList<>();
        this.plagiarismFlags = new ArrayList<>();
    }

    // Getters
    public String getStudentId(){
        return studentId;
    }
    public String getName(){
        return name;
    }
    public List<QuestionScore> getQuestionScores(){
        return Collections.unmodifiableList(questionScore);
    }
    public List<Anomaly> getAnomalies(){
        return Collections.unmodifiableList(anomalies);
    }
    public List<PlagiarismResult> getPlagiarismFlags(){
        return Collections.unmodifiableList(plagiarismFlags);
    }

    // Add to List
    public void addQuestionScore(QuestionScore score){
        this.questionScore.add(score);
    }

    public void addAnomaly(Anomaly anomaly){
        this.anomalies.add(anomaly);
    }
    public void addPlagiarismFlag(PlagiarismResult flag){
        this.plagiarismFlags.add(flag);
    }

    public double getTotalScore(){
        double total = 0.0;
        for (QuestionScore qs : questionScore){
            total += qs.getScore();
        }
        return total;
    }
}   

