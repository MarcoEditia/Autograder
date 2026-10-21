package autograder.pipeline;

import autograder.config.Config;
import autograder.execution.CompilationResult;
import autograder.execution.Compiler;
import autograder.execution.ProcessRunner;
import autograder.execution.TestInjector;
import autograder.util.FileUtils;
import ingestion.IngestionResult;
import ingestion.SubmissionCleaner;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import model.Anomaly;
import model.ProcessResult;
import model.Submission;
import scoring.Evaluator;

public class GradingPipeline {
    private final Config config;
    private final Compiler compiler;

    private final File testerFilesFolder;
    private final TestInjector testInjector;

    public GradingPipeline(Config config) {
        this.config = config;
        this.compiler = new Compiler();

        this.testerFilesFolder = this.config.getTesterFilesPath().toFile();
        this.testInjector = new TestInjector(this.testerFilesFolder);
    }

    public void run() {
        try {
            List<Path> zipFiles = FileUtils.listFilePaths(config.getStudentSubmissionPath());
            List<Anomaly> anomalyList = new ArrayList<>();

            for (Path zip : zipFiles) {
                // Ensures file is a zip file
                if (!zip.getFileName().toString().toLowerCase().endsWith(".zip")) continue;

                // ============ Ingestion ============
                IngestionResult ingestionResult = SubmissionCleaner.processSubmission(zip, config.getCleanStudentSubmissionPath());
                anomalyList.addAll(ingestionResult.getAnomalies());

                // System.out.println("Ingested submission for: " + ingestionResult.getStudentId());
                for (Anomaly anomaly : ingestionResult.getAnomalies()) {
                    System.out.println(anomaly.toString());
                }

                // ============ Execution ============
                Submission submission = new Submission(ingestionResult.getStudentId(), ingestionResult.getStudentName(), ingestionResult.getCleanSubmissionDirectory().toString());
                
                for (File testerFile : testerFilesFolder.listFiles()) {
                    if (!testerFile.getName().endsWith(".java")) continue;

                    File questionDir = new File(submission.getFolderPath(), testerFile.getName().substring(0, 2));
                    // The name part of the tester files and the submission files (e.g. Q1a, QlaTester)
                    String testerFileName = testerFile.getName().replace(".java", "");
                    String questionName = testerFileName.replace("Tester", "");

                    // Injecting and overriding tester files in the submissions
                    this.testInjector.injectTesters(testerFile.getName(), questionDir);

                    // Compile results
                    CompilationResult result = this.compiler.compile(submission, questionDir.getName());
                    if (!result.isSuccess()) {
                        System.out.println("Compilation failed: " + result.getOutput());
                    } else {
                        System.out.println("[SUCCESS] Compiled " + questionDir.getName() + " for " + submission.getStudentId());
                    }

                    ProcessResult runResult = ProcessRunner.run(new ArrayList<>(List.of(
                        "java", "-cp", 
                        new File(questionDir, "classes").getAbsolutePath() + File.pathSeparator + questionDir.getAbsolutePath(), 
                        testerFileName
                    )), config.getTimeout());

                    Evaluator.evaluate(questionName, runResult.getOutput());
                }
            }
        } catch (IOException e) {
            System.out.println("Error while unzipping student submissions: " + e.getMessage());
        }
    }
}
