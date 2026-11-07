package autograder.pipeline;

import analytics.PlagiarismEngine;
import autograder.config.Config;
import autograder.execution.CompilationResult;
import autograder.execution.Compiler;
import autograder.execution.ProcessRunner;
import autograder.execution.TestInjector;
import autograder.ui.ProgressUI;
import autograder.util.FileUtils;
import ingestion.IngestionResult;
import ingestion.SubmissionCleaner;
import io.CsvWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.*;
import reporting.HtmlReportBuilder;
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
            List<FinalGrade> grades = new ArrayList<>();
            Map<String, String> codeById = new HashMap<>();
            List<String> skippedSubmissions = new ArrayList<>();
            File[] testerFiles = testerFilesFolder.listFiles();
            
            if (testerFiles == null) throw new IOException("Cannot read the tester folder");
            boolean hasTester = false;
            for (File file : testerFiles){
                if (file.isFile() && file.getName().endsWith("Tester.java")){
                    hasTester = true;
                }
            }
            if (!hasTester) throw new IOException("No official tester files found");

            // Setup UI: Base unzip progress bar
            int zipCount = 0;
            for (Path zip : zipFiles) {
                if (zip.getFileName().toString().toLowerCase().endsWith(".zip")) zipCount++;
            }
            ProgressUI.onProgramStart(zipCount);
            
            for (Path zip : zipFiles) {
                // Ensures file is a zip file
                if (!zip.getFileName().toString().toLowerCase().endsWith(".zip")) continue;

                ProgressUI.onStudentStart(zip.getFileName().toString());
                ProgressUI.onUnzip();

                try {
                // ============ Ingestion ============
                IngestionResult ingestionResult = SubmissionCleaner.processSubmission(zip, config.getCleanStudentSubmissionPath());
                ProgressUI.onUnzipSuccess();
                ProgressUI.onStudentIdKnown(ingestionResult.getStudentId());

                anomalyList.addAll(ingestionResult.getAnomalies());

                System.out.println("Ingested submission for: " + ingestionResult.getStudentId());
                for (Anomaly anomaly : ingestionResult.getAnomalies()) {
                    System.out.println(anomaly.toString());
                }
                
                Submission submission = new Submission(ingestionResult.getStudentId(), ingestionResult.getStudentName(), ingestionResult.getCleanSubmissionDirectory().toString());
                FinalGrade grade = new FinalGrade(submission.getStudentId(), submission.getName());
                grade.addAnomalies(ingestionResult.getAnomalies());

                // ============ Execution ============
                for (File testerFile : testerFilesFolder.listFiles()) {
                    if (!testerFile.isFile() || !testerFile.getName().endsWith(".java")) continue;

                    File questionDir = new File(submission.getFolderPath(), testerFile.getName().substring(0, 2));
                    // The name part of the tester files and the submission files (e.g. Q1a, QlaTester)
                    String testerFileName = testerFile.getName().replace(".java", "");
                    String questionName = testerFileName.replace("Tester", "");

                    if (!new File(questionDir, questionName + ".java").isFile()){
                        grade.addQuestionScore(new QuestionScore(questionName, 0.0));
                        continue;
                    }

                    // Injecting and overriding tester files in the submissions
                    ProgressUI.onCopyTestFile(testerFile.getName());
                    if (!this.testInjector.injectTesters(testerFile.getName(), questionDir)){
                        throw new IOException("Could not copy official tester " + testerFile.getName());
                    }
                    ProgressUI.onCopyTestFileSuccess();

                    // Compile results
                    ProgressUI.onComplieFile(testerFileName);
                    CompilationResult result = this.compiler.compile(submission, questionDir.getName());
                    if (!result.isSuccess()) {
                        Anomaly anomaly = new Anomaly(submission.getStudentId(), 
                                Anomaly.Type.COMPILATION_FAILURE, 
                                questionName + " received 0 marks; tests were not run. Compiler line numbers refer to the cleaned source" 
                                + System.lineSeparator() + result.getOutput());
                        anomalyList.add(anomaly);
                        grade.addAnomaly(anomaly);
                        grade.addQuestionScore(new QuestionScore(questionName, 0.0));
                        System.out.println("Compilation failed: " + questionName + " — " + submission.getStudentId());
                        continue;
                    } else {
                        System.out.println("[SUCCESS] Compiled " + questionDir.getName() + " for " + submission.getStudentId());
                        ProgressUI.onComplieFileSuccess();
                    }
                    
                    ProcessResult runResult = ProcessRunner.run(new ArrayList<>(List.of(
                        "java", "-cp", 
                        new File(questionDir, "classes").getAbsolutePath() + File.pathSeparator + questionDir.getAbsolutePath(), 
                        testerFileName
                    )), config.getTimeout(), questionDir);

                    if (runResult.isTimedOut()){
                        Anomaly anomaly = new Anomaly(submission.getStudentId(), 
                                Anomaly.Type.EXECUTION_TIMEOUT, 
                                questionName + " exceeded " + config.getTimeout() + " seconds."
                                + "The tester was stopped. Partial marks were not calculated automatically. Score awarded was 0.0; needs manual review"
                                + System.lineSeparator() + runResult.getOutput());
                        anomalyList.add(anomaly);
                        grade.addAnomaly(anomaly);
                        grade.addQuestionScore(new QuestionScore(questionName, 0.0));
                        continue;
                    }

                    if (!runResult.isSuccess()){
                        Anomaly anomaly = new Anomaly(submission.getStudentId(), 
                                Anomaly.Type.RUNTIME_ERROR, 
                                questionName + " did not finish successfully. Awarded 0.0 score; needs manual review. "
                                + "Exit code: " + runResult.getExitCode() + System.lineSeparator() + runResult.getOutput());
                        anomalyList.add(anomaly);
                        grade.addAnomaly(anomaly);
                        grade.addQuestionScore(new QuestionScore(questionName, 0.0));
                        continue;
                    }

                    ProgressUI.onEvaluate(questionName);
                    QuestionScore questionScore = Evaluator.evaluate(questionName, runResult.getOutput());
                    grade.addQuestionScore(questionScore);
                    ProgressUI.onEvaluateSuccess();
                }

                // Attaching all source codes into one chunk of string to process plagiarism
                String sourceCode = "";
                for (Path file : FileUtils.listFilePaths(ingestionResult.getCleanSubmissionDirectory())) {
                    String fileName = file.getFileName().toString();
                    if (!fileName.endsWith(".java") || fileName.endsWith("Tester.java")) continue;

                    sourceCode += Files.readString(file) + "\n";
                }

                codeById.put(submission.getStudentId(), sourceCode);
                grades.add(grade);
                } catch (IOException | SecurityException e){
                    // A failed archive/submission does not abort the remaining batch.
                    // It is skipped, rather than exported as an earned zero.
                    skippedSubmissions.add(zip.getFileName() + ": " + e.getMessage());
                    System.out.println("Skipped submission: " + zip.getFileName() + ": " + e.getMessage()); 
                }
                ProgressUI.onStudentDone();
            }
            
            // numOfComparison pairs = n(n-1)/2
            ProgressUI.onPlagiarismCheck(grades.size() * (grades.size() - 1) / 2); 
            List<PlagiarismResult> flags = PlagiarismEngine.checkAll(codeById, config.getSimilarityThreshold());
            ProgressUI.onPlagiarismSuccess();

            // Adding plagiarism flags to each grade
            for (FinalGrade grade : grades) {
                // Only check A to avoid double counting on subsequent grades where they already have
                // a previous occurence with another grade
                for (PlagiarismResult flag : flags) {
                    if (grade.getStudentId().equals(flag.getStudentA())) {
                        grade.addPlagiarismFlag(flag);
                    }
                }
            }

            // Generate report files
            ProgressUI.onGenerateReport();
            try {
                CsvWriter csvWriter = new CsvWriter();
                csvWriter.export(
                    new ReportData(grades, anomalyList, flags), 
                    config.getGeneratedReportPath().toString() + "/" + config.getScoresReportFilename()
                );
                HtmlReportBuilder htmlAnomalyBuilder = new HtmlReportBuilder(config);
                htmlAnomalyBuilder.generateReport(
                    grades, config.getGeneratedReportPath().toString() + "/" + config.getAnomalyReportFilename()
                );
                Path skippedFile = config.getGeneratedReportPath().resolve("skipped-submissions.txt");
                Files.write(skippedFile, skippedSubmissions);
                ProgressUI.onGenerateReportSuccess(config.getGeneratedReportPath().toString(), 
                config.getGeneratedReportPath().toString() + "/" + config.getAnomalyReportFilename(),
                config.getGeneratedReportPath().toString() + "/" + config.getScoresReportFilename());
                if (skippedSubmissions.isEmpty()) {
                    ProgressUI.onProgramCompleted();
                } else {
                    ProgressUI.setErrorText("Reports generated for " + grades.size()
                            + " submissions. Skipped " + skippedSubmissions.size()
                            + "; see reports/skipped-submissions.txt. These students are absent from the scoresheet.");
                }
            } catch (IOException e) {
                ProgressUI.setErrorText("IOError while generating reports: " + e.getMessage());
            }
            
        } catch (IOException e) {
            ProgressUI.setErrorText("IOError while unzipping student submissions: " + e.getMessage());
        }
    }
}
