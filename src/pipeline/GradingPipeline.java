package pipeline;

import analytics.PlagiarismEngine;
import config.Config;
import execution.CompilationResult;
import execution.Compiler;
import execution.ProcessRunner;
import execution.TestInjector;
import ingestion.IngestionResult;
import ingestion.SubmissionCleaner;
import io.CsvWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import model.*;
import reporting.BaseReportBuilder;
import reporting.HtmlReportBuilder;
import scoring.Evaluator;
import ui.ProgressUI;
import util.FileUtils;

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
            // Sort 
            Arrays.sort(
                    testerFiles,
                    Comparator.comparing(File::getName)
            );

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
                
                // 
                System.out.println();
                System.out.println("Student: " + ingestionResult.getStudentId());
                System.out.println();

                int correctedHeaders = 0;
                int unresolvedHeaders = 0;

                for (Anomaly anomaly : ingestionResult.getAnomalies()) {
                    if (anomaly.getType() == Anomaly.Type.MISSING_HEADER) {
                        if (anomaly.isResolved()) {
                            correctedHeaders++;
                        } else {
                            unresolvedHeaders++;
                        }
                        continue;
                    }

                    System.out.printf(
                            "  [%-7s] %s%n",
                            anomaly.isResolved() ? "FIXED" : "REVIEW",
                            anomaly.getType().getLabel()
                    );
                }

                if (correctedHeaders > 0) {
                    System.out.printf(
                            "  [FIXED]   Headers corrected in %d file(s)%n",
                            correctedHeaders
                    );
                }

                if (unresolvedHeaders > 0) {
                    System.out.printf(
                            "  [REVIEW]  Headers need checking in %d file(s)%n",
                            unresolvedHeaders
                    );
                }
                
                Submission submission = new Submission(ingestionResult.getStudentId(), ingestionResult.getStudentName(), ingestionResult.getCleanSubmissionDirectory().toString());
                FinalGrade grade = new FinalGrade(submission.getStudentId(), submission.getName());
                grade.addAnomalies(ingestionResult.getAnomalies());

                // ============ Execution ============
                for (File testerFile : testerFiles) {
                    if (!testerFile.isFile() || !testerFile.getName().endsWith("Tester.java")) continue;

                    File questionDir = new File(submission.getFolderPath(), testerFile.getName().substring(0, 2));
                    // The name part of the tester files and the submission files (e.g. Q1a, QlaTester)
                    String testerFileName = testerFile.getName().replace(".java", "");
                    String questionName = testerFileName.replace("Tester", "");

                    if (!new File(questionDir, questionName + ".java").isFile()){
                        grade.addQuestionScore(new QuestionScore(questionName, 0.0));
                        System.out.printf(
                                "  [MISSING] %s source missing — 0 marks%n",
                                questionName
                        );
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
                    CompilationResult result = this.compiler.compile(submission, questionDir.getName(), questionName);
                    if (!result.isSuccess()) {
                        Anomaly anomaly = new Anomaly(submission.getStudentId(), 
                                Anomaly.Type.COMPILATION_FAILURE, 
                                questionName + " received 0 marks; tests were not run. Compiler line numbers refer to the cleaned source" 
                                + System.lineSeparator() + result.getOutput());
                        anomalyList.add(anomaly);
                        grade.addAnomaly(anomaly);
                        grade.addQuestionScore(new QuestionScore(questionName, 0.0));
                        System.out.printf("  [COMPILE] %s failed — 0 marks%n", questionName);
                        continue;
                    } else {
                        ProgressUI.onComplieFileSuccess();
                    }
                    
                    ProcessResult runResult = ProcessRunner.run(new ArrayList<>(List.of(
                        "java", "-cp", 
                        new File(questionDir, "classes/" + questionName).getAbsolutePath() + File.pathSeparator + questionDir.getAbsolutePath(), 
                        testerFileName
                    )), config.getTimeout(), questionDir);

                    if (runResult.isTimedOut()){
                        long passedTests = runResult.getOutput()
                                .lines()
                                .filter(line -> line.trim().equals("Passed"))
                                .count();
                        double partialScore = passedTests;
                        
                        Anomaly anomaly = new Anomaly(submission.getStudentId(), 
                                Anomaly.Type.EXECUTION_TIMEOUT, 
                                questionName + " exceeded " + config.getTimeout() + " seconds. "
                                + "Retained " + partialScore + " marks from completed tests. Remaining tests in this tester were not run; manual review required."
                                + System.lineSeparator() + runResult.getOutput());
                        anomalyList.add(anomaly);
                        grade.addAnomaly(anomaly);

                        grade.addQuestionScore(new QuestionScore(questionName, partialScore));
                        System.out.printf(
                                "  [TIMEOUT] %s stopped — %.1f marks retained; manual review required%n",
                                questionName,
                                partialScore
                        );
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
                        System.out.printf(
                                "  [ERROR]   %s execution failed — provisional 0; review required%n",
                                questionName
                        );
                        continue;
                    }

                    ProgressUI.onEvaluate(questionName);
                    QuestionScore questionScore = Evaluator.evaluate(questionName, runResult.getOutput());
                    grade.addQuestionScore(questionScore);
                    System.out.printf(
                            "  [RESULT]  %-4s %.1f marks%n",
                            questionName,
                            questionScore.getScore()
                    );
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

                boolean needsMarkReview = grade.getAnomalies().stream()
                        .anyMatch(a ->
                                a.getType() == Anomaly.Type.EXECUTION_TIMEOUT
                                || a.getType() == Anomaly.Type.RUNTIME_ERROR
                        );

                System.out.printf(
                        "%n  Total: %.1f / %.1f%s%n",
                        grade.getTotalScore(),
                        (double) config.getMaxScore(),
                        needsMarkReview ? " — provisional; manual review required" : ""
                );

                System.out.println(
                        "  Full details are available in the HTML report."
                );
                } catch (IOException | SecurityException e){
                    // A failed archive/submission does not abort the remaining batch.
                    // It is skipped, rather than exported as an earned zero.
                    skippedSubmissions.add(zip.getFileName() + ": " + e.getMessage());
                    System.out.println();
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
                CsvWriter csvWriter = new CsvWriter(config.getScoresheetTemplatePath());
                csvWriter.export(
                    new ReportData(grades, anomalyList, flags), 
                    config.getGeneratedReportPath().toString() + "/" + config.getScoresReportFilename()
                );
                BaseReportBuilder htmlAnomalyBuilder = new HtmlReportBuilder(config, skippedSubmissions);
                htmlAnomalyBuilder.generateReport(
                    grades, config.getGeneratedReportPath().toString() + "/" + config.getAnomalyReportFilename()
                );
                ProgressUI.onGenerateReportSuccess(config.getGeneratedReportPath().toString(), 
                config.getGeneratedReportPath().toString() + "/" + config.getAnomalyReportFilename(),
                config.getGeneratedReportPath().toString() + "/" + config.getScoresReportFilename());
                if (skippedSubmissions.isEmpty()) {
                    ProgressUI.onProgramCompleted();
                } else {
                    Path skippedFile = config.getGeneratedReportPath().resolve("skipped-submissions.txt");
                    Files.write(skippedFile, skippedSubmissions);
                    ProgressUI.setErrorText(
                            "Reports generated for " + grades.size()
                            + " submissions. Skipped " + skippedSubmissions.size()
                            + "; see " + skippedFile
                            + ". These students have blank scores in the exported template."
                    );
                }
            } catch (IOException e) {
                ProgressUI.setErrorText("IOError while generating reports: " + e.getMessage());
            }
            
        } catch (IOException e) {
            ProgressUI.setErrorText("IOError while unzipping student submissions: " + e.getMessage());
        }
    }
}
