package model;

import java.util.List;

public class ReportData {
    private final int totalSubmissions;
    private final List<PlagiarismResult> plagiarismResults;

    public ReportData(int totalSubmissions, List<PlagiarismResult> plagiarismResults) {
        this.totalSubmissions = totalSubmissions;
        this.plagiarismResults = plagiarismResults;
    }

    public int getTotalSubmissions() {
        return totalSubmissions;
    }

    public List<PlagiarismResult> getPlagiarismResults() {
        return plagiarismResults;
    }
}