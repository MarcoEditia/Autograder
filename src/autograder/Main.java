package autograder;
// TODO: Import all other java files if required

import autograder.config.Config;
import autograder.pipeline.GradingPipeline;
import autograder.ui.ProgressUI;

public class Main {
    public static void main(String args[]){
        System.out.println("Starting Application...");
        
        try {
            Config config = new Config();

            ProgressUI.setupUI();
            ProgressUI.setFrameVisibility(true);

            GradingPipeline pipeline = new GradingPipeline(config);
            pipeline.run();
            ProgressUI.onProgramCompleted();
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
            ProgressUI.onProgramError();
        }
    }
}
