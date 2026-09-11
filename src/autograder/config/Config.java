package autograder.config;

import java.util.Properties;
import java.io.InputStream;

import java.io.FileNotFoundException;
import java.io.IOException;


public class Config {
    private final Properties properties = new Properties();

    public Config(){
        loadConfiguration();
    }

    private void loadConfiguration(){
        String configPropertiesFileName = "config.properties";

        try (InputStream is = Config.class.getResourceAsStream(configPropertiesFileName)){
            if(is == null){
                throw new FileNotFoundException("Could not find " + configPropertiesFileName + "in autograder.config");
            }
            properties.load(is);
        } catch (IOException error){
            throw new IllegalStateException("Failed to load config.properties", error);
        }
    }

    public get
    
}
