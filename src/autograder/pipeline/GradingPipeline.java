package autograder.pipeline;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import autograder.config.Config;
import autograder.execution.CompilationResult;
import autograder.util.FileUtils;
import ingestion.IngestionResult;
import ingestion.SubmissionCleaner;

import autograder.execution.Compiler;

import model.Submission;
import model.Anomaly;

public class GradingPipeline {
    private Config config;

    public GradingPipeline(Config config) {
        this.config = config;
    }

    public void run() throws IOException {
        // ArrayList<String> filePaths = ZipService.unzip();
        List<Path> zipFiles = FileUtils.listFilePaths(config.getStudentSubmissionPath());
        List<Anomaly> anomalyList = new ArrayList<>();

        for (Path zip : zipFiles) {
            if (zip.getFileName().toString().toLowerCase().endsWith(".zip")) {
                // Ingestion
                IngestionResult ingestionResult = SubmissionCleaner.processSubmission(zip, config.getCleanStudentSubmissionPath());
                
                anomalyList.addAll(ingestionResult.getAnomalies());

                System.out.println("Ingested submission for: " + ingestionResult.getStudentId());
                for (Anomaly anomaly : ingestionResult.getAnomalies()) {
                    System.out.println(anomaly.toString());
                }
                
                // Execution
                Submission submission = new Submission(ingestionResult.getStudentId(), ingestionResult.getStudentName(), ingestionResult.getCleanSubmissionDirectory().toString());
                
                Compiler compiler = new Compiler();
                CompilationResult result = compiler.compile(submission);
                if (!result.isSuccess()) {
                    System.out.println("Compilation failed: " + result.getOutput());
                }
            }     
        }
    }
}
