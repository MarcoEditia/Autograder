

import config.Config;
import pipeline.GradingPipeline;
import ui.ProgressUI;

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
            // ProgressUI.onProgramCompleted(); pipeline announces completition only after reports are generated.
        } catch (Exception e) {
            ProgressUI.setErrorText("An error occurred: " + e.getMessage());
        }
    }
}
