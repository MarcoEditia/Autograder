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
import reporting.HtmlAnomalyBuilder;
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
                for (Anomaly anomaly : ingestionResult.getAnomalies()) {
                    grade.addAnomaly(anomaly);
                }

                // ============ Execution ============
                for (File testerFile : testerFilesFolder.listFiles()) {
                    if (!testerFile.getName().endsWith(".java")) continue;

                    File questionDir = new File(submission.getFolderPath(), testerFile.getName().substring(0, 2));
                    // The name part of the tester files and the submission files (e.g. Q1a, QlaTester)
                    String testerFileName = testerFile.getName().replace(".java", "");
                    String questionName = testerFileName.replace("Tester", "");

                    // Injecting and overriding tester files in the submissions
                    ProgressUI.onCopyTestFile(testerFile.getName());
                    this.testInjector.injectTesters(testerFile.getName(), questionDir);
                    ProgressUI.onCopyTestFileSuccess();

                    // Compile results
                    ProgressUI.onComplieFile(testerFileName);
                    CompilationResult result = this.compiler.compile(submission, questionDir.getName());
                    if (!result.isSuccess()) {
                        System.out.println("Compilation failed: " + result.getOutput());
                    } else {
                        System.out.println("[SUCCESS] Compiled " + questionDir.getName() + " for " + submission.getStudentId());
                        ProgressUI.onComplieFileSuccess();
                    }
                    
                    ProcessResult runResult = ProcessRunner.run(new ArrayList<>(List.of(
                        "java", "-cp", 
                        new File(questionDir, "classes").getAbsolutePath() + File.pathSeparator + questionDir.getAbsolutePath(), 
                        testerFileName
                    )), config.getTimeout());

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
                ProgressUI.onStudentDone();
            }
            
            // numOfComparison pairs = n(n-1)/2
            ProgressUI.onPlagiarismCheck(grades.size() * (grades.size() - 1) / 2); 
            List<PlagiarismResult> flags = PlagiarismEngine.checkAll(codeById);
            ProgressUI.completeBar();
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
                HtmlAnomalyBuilder htmlAnomalyBuilder = new HtmlAnomalyBuilder();
                htmlAnomalyBuilder.generateReport(
                    grades, config.getGeneratedReportPath().toString() + "/" + config.getAnomalyReportFilename()
                );
                ProgressUI.onGenerateReportSuccess();
            } catch (IOException e) {
                ProgressUI.setErrorText("IOError while generating reports: " + e.getMessage());
            }
            
        } catch (IOException e) {
            ProgressUI.setErrorText("IOError while unzipping student submissions: " + e.getMessage());
        }
    }
}
