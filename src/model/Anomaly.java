package model;

import java.util.Objects;

/**
 * Encapsulates an anomaly detected during ingestion, compilation, or execution.
 */
public final class Anomaly {

    public enum Severity {
        WARNING,
        ERROR    
    }

    public enum Type {
        // Ingestion and Structural issues
        UNRENAMED_FOLDER("Unrenamed Root Directory", Severity.WARNING),
        INCORRECT_FOLDER("Incorrect Root Directory", Severity.WARNING),
        FLATTENED_HIERARCHY("Missing Username Root Directory", Severity.WARNING),
        MISSING_QUESTION_FOLDER("Missing Question Directory", Severity.ERROR),
        MISSING_SOURCE_FILE("Missing Java Source File", Severity.ERROR),
        MISSING_HEADER("Missing or Malformed Header Comment", Severity.WARNING),
        UNMODIFIED_TEMPLATE("Unmodified Resource Folder Submitted", Severity.WARNING),

        // Compilation & Execution issues
        COMPILATION_FAILURE("Java Compilation Failed", Severity.ERROR),
        EXECUTION_TIMEOUT("Execution Timed Out (Infinite Loop)", Severity.ERROR),
        RUNTIME_ERROR("Subprocess Exited With Error", Severity.ERROR);

        private final String label;
        private final Severity severity;

        Type(String label, Severity severity) {
            this.label = label;
            this.severity = severity;
        }

        public String getLabel() {
            return label;
        }

        public Severity getSeverity() {
            return severity;
        }
    }

    private final String studentId;
    private final Type type;
    private final String details;

    public Anomaly(String studentId, Type type, String details) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.studentId = Objects.requireNonNull(studentId, "studentId cannot be null");
        this.details = details == null ? "" : details.trim();
    }

    public String studentId(){
        return studentId;
    }

    public Type getType() {
        return type;
    }

    public Severity getSeverity() {
        return type.getSeverity();
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return String.format("[%s] [%s] StudentId: [%s] | %s - %s",
                getSeverity(),
                type.name(),
                studentId,
                type.getLabel(),
                details);
    }
}

// // Usage
// Ingestion
// anomalies.add(new Anomaly(
//     Anomaly.Type.UNRENAMED_FOLDER, 
//     "Submission contained 'RenameToYourUsername' root directory"
// ));

// // Execution
// anomalies.add(new Anomaly(
//     Anomaly.Type.EXECUTION_TIMEOUT, 
//     "Question 1b exceeded 3-second limit and was forcefully destroyed"
// ));

// // Exporting
// List<Anomaly> fatalErrors = anomalies.stream()
//     .filter(a -> a.getSeverity() == Anomaly.Severity.ERROR)
//     .toList();