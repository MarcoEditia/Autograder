# Potential Useful Libraries

## Step 1 (Folder Arrangement):

### Validate submission structure

#### java.util.zip

- Unzips the submission

#### java.io.File

- manage metadata and file existance, also writing on it
- maybe can be used to write students name and email id incase they forgot to.

## Step 2 (Tester file)

### Tester files for each question and place them into the corresponding subfolders 

#### java.nio.file
 
- Copies Tester files into the corrsponding subfolder to prevent hardcoding

## Step 3 (Score Calculation)

### Safely terminate programs that exceed execution time

#### java.lang.ProcessBuilderProcess Isolation: 
- Spawns external OS processes to programmatically run javac (compilation) and java (execution of tester classes).  
- Crash & Exploit Protection: Prevents malicious or erroneous student code (e.g., System.exit(0)) from shutting down the autograder application.
- Infinite Loop & Hang Termination: Enforces hard execution timeouts using process.waitFor(timeout, TimeUnit.SECONDS) and forcefully terminates hanging child processes via process.destroyForcibly() without blocking the grading run.  
- Output & Error Capture: Reads standard output and error streams via process.getInputStream() and process.getErrorStream() to parse passed test cases, runtime exceptions, and compiler diagnostic messages.  

#### java.util.concurrent.ExecutorService (Optional / Concurrency)
- Manages worker threads to batch-grade multiple submissions concurrently across CPU cores, delegating the actual OS process execution to ProcessBuilder.

## Step 4 (Fill the Scoresheet)

### Export grading results in the provided CSV format.

#### OpenCSV

- Library (.jar) to read and write csv in java

# Workflow: Unzip -> Validate structure -> Score Calculation -> Fill Scoresheet | 10 Submission at a time |

``` txt
└── autograder-project/
├── compile.sh
├── run.sh
├── compile.bat
├── run.bat
├── config.properties
├── README.md
├── GX-TY.pptx
├── classes/
├── lib/
├── media/
└── src/
    ├── Main.java
    ├── analytics/
    │   └── PlagiarismEngine.java
    ├── config/
    │   └── ConfigLoader.java
    ├── execution/
    │   ├── Compiler.java
    │   ├── ProcessRunner.java
    │   └── TestInjector.java
    ├── ingestion/
    │   └── ZipService.java
    ├── io/
    │   └── CsvWriter.java
    ├── model/
    │   ├── Anomaly.java
    │   ├── AnomalyType.java
    │   ├── FinalGrade.java
    │   ├── PlagiarismResult.java
    │   ├── QuestionScore.java
    │   └── Submission.java
    ├── pipeline/
    │   └── GradingPipeline.java
    ├── reporting/
    │   └── HtmlAnomalyBuilder.java
    ├── scoring/
    │   └── Evaluator.java
    ├── ui/
    │   ├── ConsoleCLI.java
    │   └── InputValidator.java
    └── validation/
        ├── Sanitizer.java
        └── Validator.java
```

PROGRAM FLOW
1. Initialization: Member 1's `Main` starts the app. `ConfigLoader` reads config.properties, and `Main` boots up the `GradingPipeline`.
2. Ingestion: `GradingPipeline` calls Member 2's `ZipService` to unzip the massive batch of student submissions.
3. Validation & Sanitization: `GradingPipeline` passes the folder paths to Member 2's `Validator` and `Sanitizer`, wrapping valid folders into clean `Submission` objects.
4. Test Injection: `GradingPipeline` loops through the `Submission` objects and sends them to Member 3's `TestInjector` to overwrite any student tester files with the professor's master tester files.
5. Compilation: `GradingPipeline` passes the `Submission` to Member 3's `Compiler`, which compiles the `.java` files into a `classes/` folder.
6. Execution: `GradingPipeline` constructs the terminal command (e.g., `["java", "Q1aTester"]`) and passes it to Member 3's `ProcessRunner.run()`. The runner executes the code, captures all `stdout`, and returns a `ProcessResult` object containing that raw text.
7. Evaluation: `GradingPipeline` takes the raw output string from `ProcessResult` and feeds it into Member 4's `Evaluator`. The Evaluator parses the text to determine passed/failed tests and returns a `QuestionScore` object.
8. Analytics: After all submissions are evaluated, `GradingPipeline` bundles every student's raw source code into a Map and passes it to Member 4's `PlagiarismEngine.checkAll()`, which returns a list of `PlagiarismResult`s.
9. Export: `GradingPipeline` hands the compiled scores and plagiarism results to Member 5's `CsvWriter` and `HtmlAnomalyBuilder` to generate the final files.
10. UI Wrapping: Throughout steps 1–9, Member 6's `ConsoleCLI` receives status updates from `GradingPipeline` and renders a clean progress bar to the user.

