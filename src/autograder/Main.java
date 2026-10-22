package autograder;
// TODO: Import all other java files if required

import autograder.config.Config;
import autograder.pipeline.GradingPipeline;
import autograder.ui.ProgressUI;

public class Main {
    public static void main(String args[]){
        System.out.println("Starting Application...");
        
        try {
            // Init the Ui window
            ProgressUI.setupUI();
            ProgressUI.setFrameVisibility(true);

            Config config = new Config();

            GradingPipeline pipeline = new GradingPipeline(config);
            pipeline.run();
            ProgressUI.onProgramCompleted();
        } catch (Exception e) {
            ProgressUI.setErrorText("An error occurred: " + e.getMessage());
            ProgressUI.onProgramError();
        }
    }
}
