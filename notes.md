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
src/
└── autograder/
    ├── Main.java                             // Minimal bootstrap entry point only
    │
    ├── config/
    │   ├── GradingConfig.java                // Loads key-values from external properties
    │   └── config.properties                 // External config file (timeouts, paths, weights)
    │
    ├── controller/
    │   └── GradingOrchestrator.java          // Coordinates pipeline flow between model and view
    │
    ├── model/                                // Pure entities & value objects
    │   ├── Submission.java                   // Student ID, original path, sanitized path
    │   ├── QuestionResult.java               // Points earned, stdout, compiler diagnostics
    │   ├── StudentGrade.java                 // Final total score, breakdown per question
    │   ├── Anomaly.java                      // Anomaly record (type, severity, description)
    │   └── AnomalyType.java                  // ENUM: UNRENAMED_FOLDER, MISSING_FILE, TIMEOUT, etc.
    │
    ├── pipeline/
    │   ├── ingestion/
    │   │   ├── ZipService.java               // Safe archive unzipping
    │   │   └── SubmissionSanitizer.java      // Folder hierarchy restructuring & header repair
    │   ├── execution/
    │   │   ├── ProcessRunner.java            // Low-level ProcessBuilder & timeout enforcement
    │   │   ├── JavaCompilerService.java      // javac execution & compile error capture
    │   │   └── TestRunnerService.java        // Injects tester files & executes java processes
    │   ├── scoring/
    │   │   └── RubricCalculator.java         // Computes marks based on exit code or stdout
    │   └── reporting/
    │       ├── CsvReportExporter.java        // Writes final scoresheet CSV
    │       └── AnomalyReportExporter.java    // Writes detailed anomaly/audit log (Bonus)
    │
    ├── ui/                                   // View layer (Console or GUI)
    │   ├── View.java                         // Interface for user feedback
    │   └── ConsoleView.java                  // Progress bar, terminal alerts, status tables
    │
    └── util/
        └── FileUtils.java                    // Pure filesystem path & file manipulation helpers
```