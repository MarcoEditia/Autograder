# Java AutoGrader

A Java application that grades a batch of programming submissions using instructor-provided testers. It validates and prepares submission folders, compiles and executes question parts, calculates marks, checks code similarity, and exports a CSV scoresheet and an HTML instructor report.

## 1. Requirements

- JDK 17 or later. Both `java` and `javac` must be available on PATH.
- A desktop environment for the Swing progress window.
- The supplied `lib/` folder, including HtmlFlow and its supporting htmlApiFaster library.

Check your installation:

```sh
java -version
javac -version
```

Keep the project resources together. Run all commands from the `Autograder` folder containing the compile and run scripts.

## 2. Quick start

1. Place one ZIP per student directly in `student-submission/`.
2. Put all students who should appear in the output into the roster template, `IS442-ScoreSheet.csv`.
3. Keep the official testers in `Tester-Files/` and the original starter resources in `RenameToYourUsername/`.
4. Review `src/config/config.properties`.
5. Compile and run using the commands below.
6. Wait for completion, then open the files in `reports/`.

You do not need to extract each student ZIP or start a separate grading run for each student. If an LMS download contains all submissions inside one outer ZIP, extract that outer ZIP first and put the individual student ZIPs into the input folder.

### macOS / Linux

```sh
sh compile.sh
sh run.sh
```

Alternatively:

```sh
chmod +x compile.sh run.sh
./compile.sh
./run.sh
```

### Windows Command Prompt

```bat
compile.bat
run.bat
```

### Windows PowerShell

```powershell
.\compile.bat
.\run.bat
```

The compile script builds `src/Main.java` and its dependencies into `classes/`, then copies the configuration into `classes/config/`. The run script starts `Main` with the supplied libraries on the classpath. Recompile after changing Java source files or `src/config/config.properties`.

The application starts grading immediately when launched. The Swing window shows progress; the terminal shows concise results and errors.

## 3. Files and folders

| Path | Purpose |
| --- | --- |
| `student-submission/` | Input student ZIPs. |
| `Tester-Files/` | Official Java testers, such as `Q1aTester.java`. |
| `RenameToYourUsername/` | Original starter resources used to remove shared starter lines from similarity comparisons. |
| `IS442-ScoreSheet.csv` | Input roster template. Keep this separate from generated output. |
| `clean-student-submission/` | Extracted working copies, injected testers and compiled question classes. |
| `reports/` | Generated HTML, CSV and, when needed, skipped-submissions log. |
| `src/` | Java source code and configuration. |
| `classes/` | Compiled application and copied configuration. |
| `lib/` | Required external JAR files. |
| `groundtruth.txt` | Reference results supplied with the project. The application does not read this file when awarding marks. |

The compile script creates `classes/` if missing. During grading, the application creates `clean-student-submission/`, its student working folders and question compilation folders, and `reports/` as needed. Input folders and resources must be supplied before running.

The original ZIPs remain unchanged.

## 4. Preparing submissions and the roster

### ZIP files and contents

Place one ZIP per student in `student-submission/`. The `2023-2024-` prefix is optional. For example, both filenames produce the student ID `alex.goh.2024`:

```text
alex.goh.2024.zip
2023-2024-alex.goh.2024.zip
```

The application derives the ID from the final filename segment after a hyphen or underscore, excluding `.zip`. Other ZIP filenames can still be extracted, but their derived IDs must match the CSV roster for marks to be exported correctly.

Submissions normally contain `Q1`, `Q2` and `Q3` inside a username folder. Supported incorrect root-folder names and missing username folders are handled automatically. The current assessment expects:

| Folder | Required files |
| --- | --- |
| `Q1/` | `Q1a.java`, `Q1b.java` |
| `Q2/` | `Q2a.java`, `Q2b.java`, `persons.txt`, `personstester.txt`, `students.txt`, `studentstester.txt` |
| `Q3/` | `Q3.java`, `ShapeComparator.java` |

Retain required support classes, including the supplied Q3 shape classes and Q2 `DataException.class`.

Automatic folder and header corrections are recorded in the report. Missing question folders and required files are flagged for review.

Use only one ZIP per student and remove submissions from unrelated assessments or previous batches.

### CSV roster

The exporter matches the ID derived from each ZIP filename to the template’s `Username` field, ignoring one leading `#`. Matching is case-sensitive.

```text
ZIP:       alex.goh.2024.zip
Username:  #alex.goh.2024
```

Keep the supplied column headings unchanged. Append new student rows without replacing existing students, use unique usernames, set the denominator to the assessment maximum and leave the numerator blank.

The exporter fills only `Calculated Final Grade Numerator`, preserving other field values and student row order. It does not add students missing from the template. Students without a matching processed result receive a blank mark, not an assumed zero.

Use UTF-8 CSV with one physical line per student. Quoted commas and escaped quotation marks are supported; multiline quoted fields are not.

## 5. Configuration

Edit `src/config/config.properties`, then rerun the compile script. Relative paths resolve from the directory where you launch the application, so launch from the project root.

| Setting | Supplied value | Purpose |
| --- | --- | --- |
| `TESTER_FILES_PATH` | `Tester-Files/` | Official tester folder. |
| `STUDENT_SUBMISSION_PATH` | `student-submission/` | Input ZIP folder. |
| `CLEAN_STUDENT_SUBMISSION_PATH` | `clean-student-submission/` | Extracted working folder. |
| `STARTER_FILES_PATH` | `RenameToYourUsername/` | Starter source folder for similarity filtering. |
| `GENERATED_REPORT_PATH` | `reports/` | Output folder. |
| `SCORES_REPORT_FILENAME` | `scoresheet.csv` | Output CSV filename. |
| `SCORESHEET_TEMPLATE_PATH` | `./IS442-ScoreSheet.csv` | Input roster template. |
| `ANOMALY_REPORT_FILENAME` | `anomaly_report.html` | Output HTML filename. |
| `MAX_SCORE` | `20` | Assessment maximum displayed in reports. |
| `PASS_MARK` | `10` | Passing mark; a score equal to this value passes. |
| `SIMILARITY_THRESHOLD` | `0.75` | Flag pairs whose similarity is strictly greater than 75%. |
| `TIMEOUT` | `10` | Execution limit in seconds per tester process. |

Maximum score and pass mark are in marks. Similarity threshold is a fraction from 0 to 1. Updating `MAX_SCORE` does not change the tester's marking rules or the CSV template's denominator; keep all three consistent.

The CSV output path must differ from the template path. The exporter rejects attempts to overwrite the template at the same normalized path.

## 6. Grading behaviour

The pipeline processes submissions sequentially and sorts tester filenames before execution. A background reader captures subprocess output.

1. **Ingestion:** inspect and extract the ZIP, validate required folders/files and record automatic corrections.
2. **Testing:** overwrite student tester copies with official testers, compile each question part into its own output folder, and run it from its question directory so relative input files can be found.
3. **Scoring:** record each question score and the student's total. A failed part does not stop later parts.
4. **Similarity:** compare unique pairs of processed students after removing normalized starter lines.
5. **Reporting:** fill the roster-based CSV and generate the HTML report.

### How marks are awarded

| Situation | Handling |
| --- | --- |
| Missing question source | Award zero for that part and continue. |
| Compilation fails | Award zero for that part, skip its tester, and record compiler diagnostics. |
| Tester completes successfully | Parse the final output line as a numeric score and check that it equals the count of lines containing exactly `Passed` after trimming. A mismatch receives zero. |
| Tester exceeds the execution limit | Stop it, count completed `Passed` lines as partial marks, and flag the result for manual review. |
| Process exits unsuccessfully | Record a runtime anomaly and a provisional zero for the part. |
| Submission-level I/O or permission failure | Skip that submission and continue with the remaining batch. |

The current marking convention assumes one mark per `Passed` line. In a timed-out tester, remaining test cases do not run automatically. A timeout means the time limit was exceeded; it does not by itself prove an infinite loop.

An exception caught by the official tester may lead to failed test cases with a normal process exit, rather than a separate runtime anomaly.

## 7. Reading the results

### HTML report: `reports/anomaly_report.html`

Open the file in a browser. No web server is required. You can also open the report directly from the application’s UI after grading finishes.

- **Cohort Score Summary:** average, median, highest/lowest marks and passing rate.
- **Common issues:** compact categories showing the number of distinct affected students, including corrected issues. Several missing headers from one student count as one affected student in that category.
- **Skipped submissions:** submissions that were not graded, with their recorded reasons.
- **Student Gradebook:** question marks, totals, submission-check links and similarity flags. Expand a flagged mark to inspect its diagnostics.
- **Issue details:** affected students/files, why the issue was flagged, the system action and what to check. Long output can be expanded when needed.
- **Similarity pairs:** flagged student pairs and similarity percentages for instructor review.

**Corrected** means an automatic resolution was recorded for the submission issue. It does not mean the answer is correct. **Needs review** identifies an unresolved issue. Compiler line numbers refer to cleaned source files, where inserted headers may shift line numbers relative to the original ZIP.

Skipped submissions are excluded from score statistics. Timeout/runtime results and totals containing them need review; the displayed statistics include those provisional scores.

### CSV scoresheet: `reports/scoresheet.csv`

Open the file in a spreadsheet application. The exporter fills the existing roster rows rather than creating a separate question-column layout.

A blank mark means no processed result matched that roster username. It is not a confirmed zero. Check skipped submissions, username spelling and roster coverage before importing grades elsewhere. Review any provisional totals using the HTML report.

### Skipped-submission log

When the current run skips submissions, it writes `reports/skipped-submissions.txt`. Check the completion message and HTML report as well.

The current code does not delete an older skipped-submissions log when a later run skips none. Remove or archive the old log before each run, or check its timestamp before treating it as current.

## 8. Similarity checking

The engine combines a student's Java source, excluding filenames ending in `Tester.java`. It then:

1. Removes comments.
2. Replaces alphabetic/underscore sequences with `X` and removes whitespace from each line.
3. Stores distinct normalized lines in a set.
4. Removes lines that also occur in the configured starter sources.
5. Compares pairs only when each student has at least five remaining distinct lines.
6. Uses Jaccard similarity: shared entries divided by all distinct entries across the pair.

A pair is flagged only when its score exceeds `SIMILARITY_THRESHOLD`. The HTML report deduplicates pairs and shows them for both participating students.

If starter files cannot be read, the engine prints a message and continues without starter-line removal. Check this message when similarity results are unexpectedly high.

Similarity is a signal for review, not proof of plagiarism. The normalization is coarse and sets discard order and repeated lines. Shared solution patterns can raise scores; a student with too few remaining lines may not be compared. A “not flagged” result is not a guarantee of originality.

## 9. Running another batch

1. Stop the application and save any previous reports you want to retain. Output filenames are reused.
2. Replace the input ZIPs with the intended batch and reconcile duplicates.
3. Clear the contents of the dedicated `clean-student-submission/` working folder. The application reuses student-ID folders, so old files can otherwise remain. Do not delete original ZIPs, testers or starter resources.
4. Remove or archive the previous `skipped-submissions.txt`.
5. Update the roster and configuration as needed. Recompile if source or configuration changed.
6. Run the application and reconcile the number of input ZIPs with graded and skipped submissions. Check that every graded username appears in the roster.

## 10. Using a different assessment

More students for the same assessment require additional ZIPs and matching roster rows. A different assessment also requires adapting its resources and rules:

- Supply matching official testers whose filenames end in `Tester.java`.
- Review the expected folders and required files in `SubmissionCleaner`.
- Review the pipeline's question-folder mapping: it currently takes the first two characters of the tester filename, matching `Q1`, `Q2` and `Q3`.
- Keep each tester's final numeric score consistent with its number of `Passed` lines. Weighted tests require a change to the evaluator and timeout partial-mark logic.
- Supply required input data and helper classes.
- Update the original starter sources used for similarity filtering.
- Set the maximum mark, pass mark, timeout and similarity threshold, and update the roster denominator.
- Check a known-correct submission, a failing submission and a timeout case before grading the cohort.

This version uses assessment-specific naming and required-file conventions. Changing configuration paths alone does not configure every part of a new assessment.

## 11. Troubleshooting

| Problem | What to check |
| --- | --- |
| `java` or `javac` not found | Install a JDK and add its `bin` folder to PATH. |
| Script permission denied | Use `sh compile.sh` / `sh run.sh`, or grant execute permission. |
| Configuration missing or changes ignored | Run from the project root, edit `src/config/config.properties` and recompile. |
| Cannot read tester folder / no official testers | Check `TESTER_FILES_PATH` and that files end in `Tester.java`. |
| No submissions processed | Check the input path and place individual `.zip` files there. |
| Missing text input or helper class | Check the original archive and required assessment resources in the affected question folder. |
| Unexpected zero | Check missing sources, compiler diagnostics, process errors, final tester score and `Passed` count. |
| Timeout | Inspect the output and affected method, and check the limit against a known-correct submission. |
| Unexpectedly high similarity | Check starter-folder availability, threshold and the flagged source code. |
| Blank CSV mark | Check whether the submission was skipped and whether the roster username matches the derived student ID. |
| Graded student missing from CSV | Add their row to the input roster; the exporter only fills existing template rows. |
| Required CSV columns not found | Restore the exact supplied headings, especially `Username` and `Calculated Final Grade Numerator`. |
| Old files affect results | Clear the dedicated extracted working folder before rerunning. |
| Old skipped log remains | Check the current completion message/HTML report and remove the previous log. |

## 12. Code structure and libraries

| Package / class | Responsibility |
| --- | --- |
| `Main` | Initialise the UI, configuration and pipeline. |
| `pipeline.GradingPipeline` | Coordinate the grading stages. |
| `ingestion` | Inspect ZIPs, prepare working folders and validate/correct submissions. |
| `execution` | Inject testers, compile question parts and run subprocesses with time limits. |
| `scoring.Evaluator` | Parse and check marks from tester output. |
| `analytics.PlagiarismEngine` | Compare normalized submission source sets. |
| `model.FinalGrade` | One student's scores, anomalies and similarity references. |
| `model.ReportData` | Cohort report data and statistics. |
| `reporting.BaseReportBuilder` | Prepare shared report data and write the report. |
| `reporting.HtmlReportBuilder` | Render the HTML report using HtmlFlow. |
| `io.CsvWriter` | Match results to the roster and export total marks. |
| `config.Config` | Load settings from `config.properties`. |
| `ui.ProgressUI` | Show grading progress and completion/error messages. |

External libraries are supplied in `lib/`: HtmlFlow 5.0.4 for HTML generation and htmlApiFaster 1.0.17 as its supporting dependency. Files, ZIPs, processes, collections and the Swing UI use Java standard APIs.
