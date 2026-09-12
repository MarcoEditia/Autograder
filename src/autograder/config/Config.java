package autograder.config;

import java.util.Properties;
import java.io.InputStream;
import java.nio.file.Path;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Config {
    private final Path testerFilesPath;
    private final Path studentSubmissionPath;
    private final Path cleanStudentSubmissionPath;

    public Config() {
        this("config.properties");
    }

    public Config(String configPropertiesFileName) {
        Properties configProperties = loadProperties(configPropertiesFileName);

        this.testerFilesPath = getPath(configProperties, "TESTER_FILES_PATH");
        this.studentSubmissionPath = getPath(configProperties, "STUDENT_SUBMISSION_PATH");
        this.cleanStudentSubmissionPath = getPath(configProperties, "CLEAN_STUDENT_SUBMISSION_PATH");
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

    private Path getPath(Properties props, String key) {
        String rawPath = props.getProperty(key);

        if (rawPath == null || rawPath.trim().isEmpty()) {
            throw new IllegalArgumentException(key + " configuration key is missing or empty!");
        }
        Path cleanPath = Path.of(rawPath.trim()).toAbsolutePath().normalize();

        return cleanPath;
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
}
