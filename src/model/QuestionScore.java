package model;

public class QuestionScore {
    private String questionName;
    private double score;

    public QuestionScore(String questionName, double score) {
        this.questionName = questionName;
        this.score = score;
    }

    public String getQuestionName() {
        return questionName;
    }

    public double getScore() {
        return score;
    }
}