package model;

import java.util.List;

public class Submission {
    private String studentId;
    private String name;
    private String folderPath;
    // private ArrayList<Anomaly> anomalies;
    
    // CHANGED: Now accepts a List of results to track multiple cheating offenses
    private List<PlagiarismResult> plagiarismResults;

    public Submission(String studentId, String name, String folderPath) {
        this.studentId = studentId;
        this.name = name;
        this.folderPath = folderPath;
    }

    public String getStudentId() {
        return studentId;
    }
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getFolderPath() {
        return folderPath;
    }
    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public List<PlagiarismResult> getPlagiarismResults() {
        return plagiarismResults;
    }
    public void setPlagiarismResults(List<PlagiarismResult> plagiarismResults) {
        this.plagiarismResults = plagiarismResults;
    }
}