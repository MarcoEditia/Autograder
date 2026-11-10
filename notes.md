# Libraries Used

## Step 1 (Folder Arrangement)

### Validate submission structure

#### java.util.zip

- Unzips the submission

#### java.nio.file

- Manages file paths and existence, copies files, and writes the cleaned submission

## Step 2 (Tester file)

### Copy the tester files into the corresponding question folders

#### java.nio.file

- Copies the instructor's tester over any tester the student submitted, to prevent tampering

## Step 3 (Score Calculation)

### Safely terminate programs that exceed execution time

#### java.lang.ProcessBuilder

- Process Isolation: runs `javac` (compilation) and `java` (execution of tester classes) as separate OS processes.
- Crash & Exploit Protection: student code such as `System.exit(0)` cannot shut down the autograder.
- Infinite Loop & Hang Termination: `process.waitFor(timeout, TimeUnit.SECONDS)` enforces the time limit, and `process.destroyForcibly()` kills hanging processes.
- Output Capture: stderr is merged into stdout (`redirectErrorStream(true)`) and read by a background thread, so the output can be parsed.

## Step 4 (Plagiarism Check)

#### java.util (HashSet, HashMap)

- Each submission becomes a Set of normalised lines. Pairs are compared with Jaccard similarity.

## Step 5 (Fill the Scoresheet and Report)

#### CsvWriter (standard Java)

- Reads the provided CSV template and writes each student's total into it

#### HtmlFlow 5.0.4 + htmlApiFaster 1.0.17

- Libraries (.jar in `lib/`) to build the HTML instructor report

# Workflow: Unzip -> Validate structure -> Inject testers -> Compile and Run -> Score Calculation -> Plagiarism check -> Fill Scoresheet and Report | one submission at a time |

``` txt
└── Autograder/
├── compile.sh
├── run.sh
├── compile.bat
├── run.bat
├── README.md
├── notes.md
├── IS442-ScoreSheet.csv
├── lib/
├── RenameToYourUsername/
├── Tester-Files/
├── student-submission/
├── clean-student-submission/
├── reports/
├── classes/
└── src/
    ├── analytics/
    │   └── PlagiarismEngine.java
    ├── autograder/
    │   ├── Main.java
    │   ├── config/
    │   │   ├── Config.java
    │   │   └── config.properties
    │   ├── execution/
    │   │   ├── CompilationResult.java
    │   │   ├── Compiler.java
    │   │   ├── ProcessRunner.java
    │   │   └── TestInjector.java
    │   ├── pipeline/
    │   │   └── GradingPipeline.java
    │   ├── ui/
    │   │   └── ProgressUI.java
    │   └── util/
    │       └── FileUtils.java
    ├── ingestion/
    │   ├── IngestionResult.java
    │   ├── SubmissionCleaner.java
    │   └── ZipHandler.java
    ├── io/
    │   └── CsvWriter.java
    ├── model/
    │   ├── Anomaly.java
    │   ├── FinalGrade.java
    │   ├── PlagiarismResult.java
    │   ├── ProcessResult.java
    │   ├── QuestionScore.java
    │   ├── ReportData.java
    │   └── Submission.java
    ├── reporting/
    │   ├── BaseReportBuilder.java
    │   └── HtmlReportBuilder.java
    └── scoring/
        └── Evaluator.java
```

PROGRAM FLOW
1. Initialization: `Main` opens the `ProgressUI` window, `Config` reads config.properties, and `Main` runs the `GradingPipeline`.
2. Ingestion: for each ZIP, `GradingPipeline` calls `SubmissionCleaner`, which uses `ZipHandler` to unzip it, fixes folder and header problems, and records them as `Anomaly` objects in an `IngestionResult`. A ZIP that cannot be processed is skipped and listed in the report.
3. Test Injection: for each question, `TestInjector` overwrites any student tester with the instructor's tester.
4. Compilation: `Compiler` compiles that question's files into a `classes/` folder. A failure is recorded and the question scores 0.
5. Execution: `ProcessRunner` runs the tester with the timeout from config and returns a `ProcessResult` containing all the output text. A timeout keeps the marks from tests that already passed and is flagged for review.
6. Evaluation: `GradingPipeline` feeds the output into `Evaluator`. It reads the last line as the score, and gives 0 if that line is not a number or does not match the number of `Passed` lines. It returns a `QuestionScore`.
7. Analytics: after all submissions are graded, `GradingPipeline` bundles every student's source code into a Map and passes it to `PlagiarismEngine.checkAll()`. It removes the starter code lines, compares every pair and returns a list of `PlagiarismResult`s.
8. Export: `GradingPipeline` hands the grades and plagiarism results to `CsvWriter` and `HtmlReportBuilder` to generate the final files.
9. UI: throughout, `GradingPipeline` sends status updates to `ProgressUI`, which shows a progress bar.
