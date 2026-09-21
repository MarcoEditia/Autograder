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
Initialization: Member 1's Main starts the app. ConfigLoader reads config.properties into memory.
Ingestion: Member 1's GradingPipeline calls Member 2's ZipService.unzip(), returning raw file paths.
Validation: Paths are passed to Member 2's Validator, returning a list of Submission objects containing attached Anomaly logs for structural errors.
Sanitization: Broken Submission objects are routed to Member 2's Sanitizer, which auto-fixes directories and missing headers.
Preparation: Fixed Submission objects are sent to Member 3's TestInjector to receive tester files, then passed to Compiler for bytecode generation.
Execution: Compiled files go to Member 3's ProcessRunner, which captures stdout and safely terminates timeouts.
Evaluation: Raw stdout is handed to Member 4's Evaluator to generate QuestionScore objects based on passed test cases.
Analytics: All source code is passed to Member 4's PlagiarismEngine to identify heavily duplicated logic.
Export: Scores and anomalies are passed to Member 5's CsvWriter for scoresheet.csv and HtmlAnomalyBuilder for the bonus report.
UI Wrapping: Throughout steps 1–9, Member 6's ConsoleCLI receives status updates from GradingPipeline and renders clean progress indicators to the user without exposing internal logic.
