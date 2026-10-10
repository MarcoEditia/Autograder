package autograder;
// TODO: Import all other java files if required

import autograder.config.Config;
import autograder.pipeline.GradingPipeline;

public class Main {
    public static void main(String args[]){
        System.out.println("Starting Application...");
        Config config = new Config();
        GradingPipeline pipeline = new GradingPipeline(config);
    }
}
