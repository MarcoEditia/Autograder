package model;

public class PlagiarismResult {
    private String studentA;
    private String studentB;
    private double similarityScore;

    public PlagiarismResult(String studentA, String studentB, double similarityScore) {
        this.studentA = studentA;
        this.studentB = studentB;
        this.similarityScore = similarityScore;
    }

    public String getStudentA() { return studentA; }
    public String getStudentB() { return studentB; }
    public double getSimilarityScore() { return similarityScore; }
}