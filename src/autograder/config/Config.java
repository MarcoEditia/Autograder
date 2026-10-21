package autograder.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public final class Config {
    private static final String CONFIG_FILE = "config.properties";

    private final Path testerFilesPath;
    private final Path studentSubmissionPath;
    private final Path cleanStudentSubmissionPath;

    private final Path generatedReportPath;
    private final String scoresReportFilename;
    private final String anomalyReportFilename;

    private final long timeout;

    public Config() {
        Properties configProperties = loadProperties(CONFIG_FILE);

        this.testerFilesPath = getPath(configProperties, "TESTER_FILES_PATH");
        this.studentSubmissionPath = getPath(configProperties, "STUDENT_SUBMISSION_PATH");
        this.cleanStudentSubmissionPath = getPath(configProperties, "CLEAN_STUDENT_SUBMISSION_PATH");

        this.generatedReportPath = getPath(configProperties, "GENERATED_REPORT_PATH");
        this.scoresReportFilename = getString(configProperties, "SCORES_REPORT_FILENAME");
        this.anomalyReportFilename = getString(configProperties, "ANOMALY_REPORT_FILENAME");

        this.timeout = getLong(configProperties, "TIMEOUT");
    }

    private Properties loadProperties(String fileName) {
        Properties props = new Properties();

        try (InputStream is = Config.class.getResourceAsStream(fileName)) {
            if (is == null) {
                throw new FileNotFoundException("Could not find " + fileName + " in autograder.config");
            }
            props.load(is);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to load " + fileName, error);
        }

        return props;
    }

    private String getString(Properties props, String key) {
        String string = props.getProperty(key);

        if (string == null || string.trim().isEmpty()) {
            throw new IllegalArgumentException(key + " configuration key is missing or empty!");
        }
        return string;
    }

    private Path getPath(Properties props, String key) {
        String rawPath = props.getProperty(key);

        if (rawPath == null || rawPath.trim().isEmpty()) {
            throw new IllegalArgumentException(key + " configuration key is missing or empty!");
        }
        Path cleanPath = Path.of(rawPath.trim()).toAbsolutePath().normalize();

        return cleanPath;
    }

    private long getLong(Properties props, String key) {
        String rawTimeout = props.getProperty(key);

        if (rawTimeout == null || rawTimeout.trim().isEmpty()) {
            throw new IllegalArgumentException(key + " configuration key is missing or empty!");
        }
        try {
            return Long.parseLong(rawTimeout);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " configuration key is not a valid long type!");
        }
    }

    public Path getTesterFilesPath() {
        return testerFilesPath;
    }

    public Path getStudentSubmissionPath() {
        return studentSubmissionPath;
    }

    public Path getCleanStudentSubmissionPath() {
        return cleanStudentSubmissionPath;
    }

    public Path getGeneratedReportPath() {
        return generatedReportPath;
    }

    public String getScoresReportFilename() {
        return scoresReportFilename;
    }

    public String getAnomalyReportFilename() {
        return anomalyReportFilename;
    }

    public long getTimeout() {
        return timeout;
    }

    public static String getConfigFile() {
        return CONFIG_FILE;
    }
}
