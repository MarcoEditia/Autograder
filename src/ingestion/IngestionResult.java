package ingestion;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import model.Anomaly;

public final class IngestionResult {
    private final String studentId;
    private final String studentName;
    private final Path cleanSubmissionDirectory;
    private final List<Anomaly> anomalies;

    public IngestionResult(String studentId, String studentName, Path cleanSubmissionDirectory, List<Anomaly> anomalies) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.cleanSubmissionDirectory = cleanSubmissionDirectory;
        this.anomalies = anomalies != null ? List.copyOf(anomalies) : Collections.emptyList();
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName(){
        return studentName;
    }

    public Path getCleanSubmissionDirectory() {
        return cleanSubmissionDirectory;
    }

    public List<Anomaly> getAnomalies() {
        return anomalies;
    }
}