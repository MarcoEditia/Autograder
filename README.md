# Java AutoGrader

A Java application for grading a batch of programming submissions using instructor-provided testers. It prepares submissions, compiles and runs question parts, calculates marks, checks code similarity, and generates CSV and HTML reports.

## Requirements

- JDK 17 or later, with both `java` and `javac` available on your PATH.
- A desktop environment for the Swing progress window.
- The supplied JAR files in `lib/`. Keep these with the application.

Check your Java installation:

```sh
java -version
javac -version
```

## Quick start for instructors

1. Extract the project and open a terminal in its root folder, where `compile.sh` and `run.sh` are located.
2. Place one ZIP per student in `student-submission/`. Remove ZIPs from previous batches first, or move them to a separate archive folder.
3. Keep the matching instructor testers in `Tester-Files/`.
4. Check the configuration described below.
5. Compile and run using the commands for your operating system.
6. Wait for grading to finish, then open the HTML report and CSV in `reports/`.

There is no need to unzip each student submission or run the application separately for each student.

### macOS / Linux

```sh
sh compile.sh
sh run.sh
```

Alternatively, make the scripts executable once and run them directly:

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

In PowerShell, use `./compile.bat` followed by `./run.bat`.

Always run from the project root. Relative input and output paths resolve from the current working directory. Run the compile step again after changing Java source files or the configuration file.

## Project folders

| Path | Purpose |
| --- | --- |
| `student-submission/` | Input ZIP files, one per student. |
| `Tester-Files/` | Instructor-controlled Java testers for the assessment. |
| `RenameToYourUsername/` | The starter code given to students. Its lines are ignored by the similarity check. |
| `clean-student-submission/` | Working copies extracted and prepared for grading. |
| `reports/` | Generated HTML report and CSV scoresheet. |
| `IS442-ScoreSheet.csv` | Provided roster/scoresheet template, when using template-based CSV export. |
| `src/` | Application source code and configuration. |
| `classes/` | Compiled application classes. |
| `lib/` | Required external libraries. |

Keep original ZIPs as the source of record. Inspect working copies when investigating a flag, because automatic corrections may change their folder structure or source headers.

## Preparing student submissions

Use the student's username as the ZIP filename, following the assessment's naming convention, for example `student.one.2025.zip`.

A normal ZIP contains a username folder, question folders such as `Q1`, `Q2` and `Q3`, and the Java source files required by that assessment. Keep question data files, such as text inputs, in their expected question folders.

- Place the individual student ZIPs directly in the configured input folder.
- Extract an LMS batch download first if it wraps all student ZIPs inside one larger archive.
- Include only the students and assessment intended for this run.
- Avoid multiple ZIPs for the same student. Resolve resubmissions before grading.
- Use the supplied naming conventions, such as `Q1a.java` and `Q1aTester.java`.

The cleaner can correct supported folder and header mistakes. A missing source file, corrupt archive or other serious issue still needs instructor attention.

## Configuration

Edit:

```text
src/autograder/config/config.properties
```

The compile script copies this file into the application classes folder. Recompile after editing it so the next run uses the new values.

The following settings are defined:

| Setting | Meaning |
| --- | --- |
| `TESTER_FILES_PATH` | Folder containing instructor testers. |
| `STUDENT_SUBMISSION_PATH` | Folder containing student ZIPs. |
| `CLEAN_STUDENT_SUBMISSION_PATH` | Folder for extracted working copies. |
| `GENERATED_REPORT_PATH` | Destination for generated reports. |
| `SCORES_REPORT_FILENAME` | CSV output filename. |
| `ANOMALY_REPORT_FILENAME` | HTML output filename. |
| `STARTER_FILES_PATH` | Folder containing the starter code given to students. |
| `SCORESHEET_TEMPLATE_PATH` | Provided CSV scoresheet template to fill in. |
| `MAX_SCORE` | Maximum marks for the assessment. |
| `PASS_MARK` | Passing mark, in marks (not a percentage). |
| `SIMILARITY_THRESHOLD` | A pair of students above this similarity is flagged, as a fraction (`0.75` is 75%). |
| `TIMEOUT` | Execution time limit in seconds for each tester process. |
| `COMPILE_TIMEOUT` | Time limit in seconds for compiling one question part. |

Example paths using the supplied layout:

```properties
TESTER_FILES_PATH=Tester-Files/
STUDENT_SUBMISSION_PATH=student-submission/
CLEAN_STUDENT_SUBMISSION_PATH=clean-student-submission/
STARTER_FILES_PATH=RenameToYourUsername/
GENERATED_REPORT_PATH=reports/
SCORES_REPORT_FILENAME=scoresheet.csv
SCORESHEET_TEMPLATE_PATH=IS442-ScoreSheet.csv
ANOMALY_REPORT_FILENAME=anomaly_report.html
MAX_SCORE = 20
PASS_MARK = 10
SIMILARITY_THRESHOLD = 0.75
TIMEOUT = 10
COMPILE_TIMEOUT = 15
```

Choose a timeout suitable for the supplied tests and machine. A timeout means execution exceeded the limit; it does not by itself prove an infinite loop. Long limits also make batches with hanging submissions take longer.

The report uses `MAX_SCORE` and `PASS_MARK` for the passing rate. Set them in marks, not percentages.

## What happens during grading

1. **Ingestion and cleaning:** extract each ZIP, validate its structure and record supported automatic corrections.
2. **Testing:** inject the instructor tester, compile the affected question part and run the tester with a time limit.
3. **Scoring:** read each tester's final score line and collect per-question marks and the student's total. A score that does not match the tester's number of `Passed` lines is set to 0.
4. **Similarity checking:** remove the starter code lines, then compare unique pairs of student submissions.
5. **Reporting:** export scores and explanations for instructor review.

Grading proceeds sequentially. A background output reader captures the subprocess logs. Compilation failures and timeouts should allow the pipeline to continue to later question parts and students.

## Reading the results

### HTML instructor report

Open `reports/anomaly_report.html` in a browser, or the filename selected in configuration.

- **Cohort Score Summary:** overall marks and passing percentage.
- **Student Gradebook:** per-question marks, total score, submission-check categories and similarity flags.
- **Issue details:** follow a category link to see the affected student, question or file, why it was flagged, the system action and what to check.
- **Full output:** expand the available compiler or execution log for technical details.
- **Similarity:** inspect flagged student pairs and their similarity percentages separately from submission checks.

**Corrected** means the application recorded an automatic resolution. **Needs review** means an instructor must inspect the issue. A corrected folder or header does not establish that the student's answers are correct.

### Compilation failures

The affected question part receives zero and its tester does not run. Review the compiler message and the reported filename/line. Automatic header insertion can shift line numbers relative to the original source file, so inspect the cleaned working copy used for compilation.

### Execution timeouts

The application stops the tester after its configured time limit. Review the retained output and the recorded question score.

It will retain partial marks from completed tests before the timeout. Counting `Passed` lines assumes one mark per passing test. Remaining tests within that stopped tester do not run automatically. Any retained marks need instructor review.

A timeout in one question part does not automatically imply zero for the student's entire submission.

### Similarity flags

The basic engine compares sets of normalized source lines using Jaccard similarity. It removes comments, masks alphabetic text and removes whitespace before comparison. Lines that also appear in the starter code (`RenameToYourUsername/`) are removed first, and a submission with fewer than 5 lines of its own is not compared.

Similarity is a review signal. Short solutions and coarse normalization can still produce high scores. Review both students' original work before reaching a conclusion. The percentage does not represent the percentage of code proven to be copied.

### CSV scoresheet

Open `reports/scoresheet.csv`, or the configured CSV filename, in a spreadsheet application.

The exporter reads the supplied scoresheet template (`IS442-ScoreSheet.csv`), matches students by username and fills in their total marks. It preserves the existing column headings, student information and row order.

Students without a processed result remain blank and require manual checking; a blank grade does not mean zero.

Check student identities, totals and any provisional marks before importing the scoresheet into another system. Keep the original template separate from the generated output.


### Skipped submissions

A batch with skipped ZIPs creates `reports/skipped-submissions.txt`. Read the completion message and this file to identify submissions that need attention. A skipped submission has not been graded and must not be treated as a confirmed zero.

## Running another batch

1. Save the previous reports under a different folder or filename if you need to keep them.
2. Replace the input ZIPs with the new batch.
3. With the application stopped, remove old generated contents from the dedicated `clean-student-submission/` working folder to prevent stale files or compiled classes affecting the next run. Do not delete original submissions or instructor resources.
4. Check the assessment configuration and run again.
5. Reconcile the input ZIP count with graded and skipped submissions.

## Using a different assessment

Adding more students to the same assessment only requires adding their ZIPs to the input folder. A different assessment also requires matching resources and grading rules:

- Replace the instructor testers and any required input data.
- Check the expected question folders and required source-file list in the cleaner.
- Follow the naming convention used by the pipeline to select question folders and tester classes.
- Ensure each tester prints `Passed` for each passed test, then its final numeric score on the last line. `Evaluator` gives 0 if that score does not equal the number of `Passed` lines.
- Replace the starter code in `RenameToYourUsername/`.
- Check maximum marks, pass mark, timeouts and similarity threshold.
- Supply the correct roster template if using template-based export.
- Run a known-correct submission and a deliberately failing submission before grading the cohort.

The current framework still depends on assessment-specific file conventions and tester output. Changing the input folder alone does not configure a new assessment.

## Troubleshooting

| Problem | What to check |
| --- | --- |
| `java` or `javac` is not found | Install a JDK and add its `bin` folder to PATH. |
| Script permission denied | Use `sh compile.sh` / `sh run.sh`, or grant execute permission. |
| Configuration not found or changes have no effect | Run from the project root and rerun the compile script. |
| No submissions appear | Check the configured input path and place individual `.zip` files there. |
| A question cannot find its text input | Confirm the input file exists in the question folder and the subprocess uses that folder as its working directory. |
| Compilation fails | Expand the compiler output in the report and inspect the affected cleaned source. |
| Execution times out | Inspect the method and log. Check the limit against a known-correct solution. |
| A score is unexpectedly zero | Check compilation, runtime errors, timeout flags and the tester's final numeric output. A score that does not match the number of `Passed` lines is also set to 0. |
| Results contain old files | Clear the dedicated generated working folder before rerunning. |
| A student is absent from the scoresheet | Check skipped submissions and, for template export, the username match. |

## Code overview

`GradingPipeline` coordinates the workflow. Ingestion classes prepare submissions; `Compiler`, `ProcessRunner` and `TestInjector` handle testing; `Evaluator` calculates question scores; `PlagiarismEngine` checks similarity; and the CSV/HTML builders produce reports. Model classes carry results between stages. `ProgressUI` shows progress.

The project includes HtmlFlow 5.0.4 and htmlApiFaster 1.0.17 for HTML generation. File handling, ZIP extraction, process execution and the progress window use Java standard APIs.
