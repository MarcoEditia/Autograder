package model;

public class Submission {
    private String studentId;
    private String name;
    private String folderPath;
    // private ArrayList<Anomaly> anomalies;
    private PlagiarismResult plagiarismResult;

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

    public String getFolderPath() {
        return folderPath;
    }
    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public PlagiarismResult getPlagiarismResult() {
        return plagiarismResult;
    }
    public void setPlagiarismResult(PlagiarismResult plagiarismResult) {
        this.plagiarismResult = plagiarismResult;
    }
}
