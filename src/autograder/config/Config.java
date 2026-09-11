package autograder.config;

import java.util.Properties;
import java.io.InputStream;
import java.nio.file.Path;

import java.io.FileNotFoundException;
import java.io.IOException;


public class Config {
    private final Properties configProperties = new Properties();

    public Config(){
        loadConfiguration();
    }

    private void loadConfiguration(){
        String configPropertiesFileName = "config.properties";

        try (InputStream is = Config.class.getResourceAsStream(configPropertiesFileName)){
            if(is == null){
                throw new FileNotFoundException("Could not find " + configPropertiesFileName + " in autograder.config");
            }
            configProperties.load(is);
        
        } catch (IOException error){
            throw new IllegalStateException("Failed to load config.properties", error);
        }
    }

    public Path getTesterFilesPath(){
        String rawTesterFilesPath = configProperties.getProperty("TESTER_FILES_PATH").trim();
        Path TesterFilesPath = Path.of(rawTesterFilesPath).toAbsolutePath().normalize();

        return TesterFilesPath;
    }

    public Path getStudentSubmissionPath(){
        String rawStudentSubmissionPath = configProperties.getProperty("STUDENT_SUBMISSION_PATH").trim();
        Path studentSubmissionPath = Path.of(rawStudentSubmissionPath).toAbsolutePath().normalize();

        return studentSubmissionPath;
    }
}
