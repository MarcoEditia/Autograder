package reporting;

import htmlflow.HtmlFlow;
import htmlflow.HtmlView;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import config.Config;
import model.Anomaly;
import model.FinalGrade;
import model.PlagiarismResult;
import model.QuestionScore;
import model.ReportData;

public class HtmlReportBuilder extends BaseReportBuilder {
    private final HtmlView<ReportData> VIEW;

    public HtmlReportBuilder(Config config, List<String> skippedSubmissions) {
        this.VIEW = HtmlFlow.view(page -> page
            .html().attrLang("en")
                .head()
                    .meta().attrCharset("UTF-8").__()
                    .meta().attrName("viewport").attrContent("width=device-width, initial-scale=1.0").__()
                    .title().text("AutoGrader - Cohort Master Report").__()
                    .style().raw(CSS).__()
                .__()
                .body()
                    .div().attrClass("container")
                        .<ReportData>dynamic((container, data) -> {
                            List<FinalGrade> students = grades(data);
                            List<String> questions = questionNames(data);
                            List<Anomaly> anomalies = anomalies(data);
                            List<PlagiarismResult> pairs = pairs(data);
                            double max = config.getMaxScore();
                            double passMark = config.getPassMark();
                            long passed = students.stream().filter(g -> g.getTotalScore() >= passMark).count();

                            container.div().attrClass("header")
                                .div()
                                    .h1().text("AutoGrader Cohort Master Report").__()
                                    .p().attrClass("subtitle").text("Student Marks & Submission Checks").__()
                                .__()
                                .div().attrClass("meta")
                                    .div().strong().text("Generated: ").__()
                                        .text(ZonedDateTime.now(ZoneId.of("Asia/Singapore")).format(
                                            DateTimeFormatter.ofPattern("dd MMM uuuu · HH:mm 'SGT'", Locale.ENGLISH))).__()
                                    .div().strong().text("Submissions: ").__().text(students.size()).__()
                                .__()
                            .__();

                            container.h2().text("Cohort Score Summary").__();
                            String[] labels = {"Average Score", "Median Score", "Highest Score", "Lowest Score", "Passing Rate"};
                            String[] values = {marks(data.getAverageScore(), max), marks(data.getMedianScore(), max),
                                marks(data.getMaxScore(), max), marks(data.getMinScore(), max),
                                students.isEmpty() ? "—" : String.format(Locale.ENGLISH, "%.1f%%", 100.0 * passed / students.size())};
                            container.div().attrClass("metrics-grid").of(grid -> {
                                for (int i = 0; i < labels.length; i++) {
                                    final int index = i;
                                    grid.div().attrClass("metric-card highlight").of(card -> {
                                        card.div().attrClass("label").text(labels[index]).__();
                                        card.div().attrClass("value").text(values[index]).__();
                                        if (index == 4) card.span().attrClass("mini")
                                            .text(passed + " of " + students.size() + " students · Pass: ≥ " + marks(passMark, max)).__();
                                    }).__();
                                }
                            }).__();

                            Map<Anomaly.Type, Set<String>> studentsByIssue =
                                    new EnumMap<>(Anomaly.Type.class);

                            for (FinalGrade grade : data.getGrades()) {
                                for (Anomaly anomaly : grade.getAnomalies()) {
                                    studentsByIssue
                                            .computeIfAbsent(
                                                    anomaly.getType(),
                                                    type -> new HashSet<>()
                                            )
                                            .add(grade.getStudentId());
                                }
                            }

                            // Show the most widespread issues first.
                            List<Anomaly.Type> issueTypes =
                                    new ArrayList<>(studentsByIssue.keySet());

                            issueTypes.sort((a, b) -> Integer.compare(
                                    studentsByIssue.get(b).size(),
                                    studentsByIssue.get(a).size()
                            ));

                            if (!issueTypes.isEmpty()) {
                                container.div().attrClass("common-issues")
                                    .div().attrClass("common-issues-heading")
                                        .strong().text("Common issues").__()
                                        .span().text("Students affected · includes corrected issues").__()
                                    .__()
                                    .div().attrClass("common-issues-list")
                                        .of(list -> {
                                            for (Anomaly.Type type : issueTypes) {
                                                if (type == Anomaly.Type.COMPILATION_FAILURE) continue;
                                                boolean needsReview = anomalies.stream()
                                                        .anyMatch(a -> a.getType() == type
                                                                && !a.isResolved());

                                                String target = type.name().toLowerCase(Locale.ROOT)
                                                                + (needsReview ? "-review" : "-corrected");

                                                list.a()
                                                    .attrClass("common-issue")
                                                    .attrHref("#" + target)
                                                    .span().text(issueLabel(type)).__()
                                                    .strong().text(
                                                        String.valueOf(studentsByIssue.get(type).size())
                                                    ).__()
                                                .__();
                                            }
                                        })
                                    .__()
                                .__();
                            }

                            if (!skippedSubmissions.isEmpty()) {
                                container.section().attrClass("category-banner")
                                    .div().attrClass("banner-header")
                                        .span().text("Skipped submissions").__()
                                        .span().attrClass("count-pill")
                                            .text(skippedSubmissions.size() + " skipped")
                                        .__()
                                    .__()
                                    .div().attrClass("banner-body")
                                        .p().text(
                                            "These submissions were not graded and are excluded "
                                            + "from the score statistics."
                                        ).__()
                                        .ul()
                                            .of(list -> {
                                                for (String skipped : skippedSubmissions) {
                                                    list.li().text(skipped).__();
                                                }
                                            })
                                        .__()
                                    .__()
                                .__();
                            }

                            container.h2().attrId("gradebook").text("Student Gradebook").__();
                            container.p().attrClass("note")
                                .text("Select a submission category for details. Expand a flagged mark to see its compilation or execution message.").__();
                            container.div().attrClass("key")
                                .span()
                                    .i().attrClass("dot review").__()
                                    .text("Review: check the details below").__()
                                .span()
                                    .i().attrClass("dot fixed").__()
                                    .text("Corrected: no further correction needed").__()
                                .span().text("Submission checks do not indicate answer correctness.").__()
                            .__();
                            container.div().attrClass("table-container gradebook").table()
                                .thead().tr()
                                    .th().text("Student / Username").__()
                                    .of(row -> {
                                        for (String question : questions) row.th().attrClass("num").text(question).__();
                                    })
                                    .th().attrClass("num").text("Total / " + number(max)).__()
                                    .th().text("Submission Checks").__()
                                    .th().text("Plagiarism Check").__()
                                .__().__()
                                .tbody().of(tbody -> {
                                    if (students.isEmpty()) tbody.tr().td().attrColspan(questions.size() + 4)
                                        .attrClass("empty-row").text("No student results available.").__().__();
                                    for (FinalGrade student : students) {
                                        List<Anomaly> own = forStudent(anomalies, student.getStudentId());
                                        tbody.tr()
                                            .td().div().attrClass("student text-bold").text(student.getName())
                                                .small().text(student.getStudentId()).__().__().__()
                                            .of(row -> {
                                                for (String question : questions) {
                                                    List<Anomaly> issues = new ArrayList<>();
                                                    for (Anomaly a : own) {
                                                        if (isMarkIssue(a) && question.equals(questionOf(a))) issues.add(a);
                                                    }
                                                    Double score = questionScore(student, question);
                                                    row.td().attrClass("num").of(cell -> {
                                                        if (issues.isEmpty()) cell.text(score == null ? "—" : number(score));
                                                        else cell.raw("<details class=\"mark-details\"><summary>").strong().text(score == null ? "—" : number(score)).__()
                                                                .span().attrClass("mark-label").text(markLabel(issues.get(0)) + " ▾").__()
                                                            .raw("</summary>")
                                                            .div().attrClass("mark-explanation").of(explanation -> {
                                                                explanation.strong().text(question + " · Awarded " + (score == null ? "not recorded" : number(score))).__();
                                                                for (Anomaly a : issues) explanation.pre().attrClass("diagnostic").text(a.getDetails()).__();
                                                                if (issues.stream().anyMatch(a -> a.getType() != Anomaly.Type.COMPILATION_FAILURE))
                                                                    explanation.p().text("Provisional result: review any marks earned before execution stopped.").__();
                                                            }).__()
                                                        .raw("</details>");
                                                    }).__();
                                                }
                                            })
                                            .td().attrClass("num text-bold").text(number(student.getTotalScore()))
                                                .of(cell -> {
                                                    if (own.stream().anyMatch(a -> a.getType() == Anomaly.Type.EXECUTION_TIMEOUT
                                                            || a.getType() == Anomaly.Type.RUNTIME_ERROR))
                                                        cell.span().attrClass("mini").text("Provisional").__();
                                                }).__()
                                            .td().attrClass("flags").of(cell -> {
                                                Set<String> seen = new LinkedHashSet<>();
                                                for (Anomaly a : own) {
                                                    if (a.getType() == Anomaly.Type.COMPILATION_FAILURE) continue;
                                                    String key = categoryId(a);
                                                    if (!seen.add(key)) continue;
                                                    cell.div().attrClass("flag-item")
                                                        .i().attrClass("dot " + (a.isResolved() ? "fixed" : "review")).__()
                                                        .a().attrHref("#" + key).text(categoryLabel(a)).__()
                                                    .__();
                                                }
                                                // Keep a link if compilation diagnostics cannot be associated with a question.
                                                boolean unmatched = own.stream().anyMatch(a -> a.getType() == Anomaly.Type.COMPILATION_FAILURE
                                                        && !questions.contains(questionOf(a)));
                                                if (unmatched) cell.a().attrHref("#compilation-details").text("Compilation details").__();
                                                if (seen.isEmpty() && !unmatched) cell.span().attrClass("mini").text("None detected").__();
                                            }).__()
                                            .td().of(cell -> {
                                                List<PlagiarismResult> ownPairs = new ArrayList<>();
                                                for (PlagiarismResult pair : pairs) {
                                                    if (student.getStudentId().equals(pair.getStudentA())
                                                            || student.getStudentId().equals(pair.getStudentB())) ownPairs.add(pair);
                                                }
                                                if (ownPairs.isEmpty()) cell.span().attrClass("mini").text("Not flagged").__();
                                                for (PlagiarismResult pair : ownPairs) {
                                                    String other = student.getStudentId().equals(pair.getStudentA()) ? pair.getStudentB() : pair.getStudentA();
                                                    cell.a().attrClass("badge-link").attrHref("#pair-" + pairs.indexOf(pair))
                                                        .span().attrClass("badge badge-warning").text("Similarity · " + percent(pair)).__()
                                                    .__();
                                                    cell.span().attrClass("mini").text("With " + data.lookupStudentName(other)).__();
                                                }
                                            }).__()
                                        .__();
                                    }
                                }).__()
                            .__().__();
                            container.p().attrClass("note")
                                .text("A dash means no mark was recorded. Timeout/runtime results and totals containing them need review; summary statistics include those provisional scores.").__();

                            // Fallback for diagnostics whose details do not identify a scored part.
                            List<Anomaly> unmatched = new ArrayList<>();
                            for (Anomaly a : anomalies) if (a.getType() == Anomaly.Type.COMPILATION_FAILURE
                                    && !questions.contains(questionOf(a))) unmatched.add(a);
                            if (!unmatched.isEmpty()) container.div().attrId("compilation-details").attrClass("category-banner")
                                .div().attrClass("banner-header").text("Compilation Details").__()
                                .div().of(box -> {
                                    for (Anomaly a : unmatched) box.raw("<details><summary>").text(data.lookupStudentName(a.studentId()) + " · Compilation failed").raw("</summary>")
                                        .pre().attrClass("diagnostic").text(a.getDetails()).__()
                                    .raw("</details>");
                                }).__()
                            .__();

                            // Unresolved non-compilation anomalies, grouped into linked categories.
                            // Resolved anomalies are rendered in the same way after similarity checks.
                            for (boolean resolved : new boolean[] {false, true}) {
                                if (resolved) {
                                    container.h2().text("Plagiarism Check · Flagged Pairs").__();
                                    container.p().attrClass("section-intro")
                                        .text("One row per flagged pair. Similarity requires instructor review; it does not confirm plagiarism.").__();
                                    container.div().attrClass("category-banner").attrId("similarity")
                                        .div().attrClass("banner-header")
                                            .span().text("Code Similarity").__()
                                            .span().attrClass("count-pill").text(pairs.size() + (pairs.size() == 1 ? " pair" : " pairs")).__()
                                        .__()
                                        .div().attrClass("banner-body").table()
                                            .thead().tr()
                                                .th().text("Students").__().th().text("Comparison").__()
                                                .th().text("Why It Was Flagged").__().th().text("Instructor Action").__()
                                            .__().__()
                                            .tbody().of(tbody -> {
                                                if (pairs.isEmpty()) tbody.tr().td().attrColspan(4).text("No similarity flags recorded.").__().__();
                                                for (int i = 0; i < pairs.size(); i++) {
                                                    PlagiarismResult pair = pairs.get(i);
                                                    tbody.tr().attrId("pair-" + i)
                                                        .td()
                                                            .strong().text(data.lookupStudentName(pair.getStudentA())).__()
                                                            .span().attrClass("mini").text(pair.getStudentA()).__()
                                                            .br().__().strong().text(data.lookupStudentName(pair.getStudentB())).__()
                                                            .span().attrClass("mini").text(pair.getStudentB()).__()
                                                        .__()
                                                        .td().text("Combined submission code")
                                                            .span().attrClass("mini").text("A question/file breakdown is not stored in the current model.").__()
                                                        .__()
                                                        .td().strong().text(percent(pair) + " similarity").__()
                                                            .p().text("Above the configured")
                                                            .strong()
                                                                .text(String.format("%.0f%%", config.getSimilarityThreshold() * 100
                                                            ))
                                                            .__()
                                                            .text(" similarity threshold.").__()
                                                        .__()
                                                        .td().span().attrClass("badge badge-warning").text("Similarity flag").__()
                                                            .p().strong()
                                                                .text("No automatic score deduction. ").__()
                                                                .text("Compare the original sources and consider common starter code.").__()
                                                        .__()
                                                    .__();
                                                }
                                            }).__()
                                        .__().__()
                                    .__();
                                }
                                Map<Anomaly.Type, List<Anomaly>> categories = new LinkedHashMap<>();
                                for (Anomaly a : anomalies) if (a.getType() != Anomaly.Type.COMPILATION_FAILURE
                                        && a.isResolved() == resolved)
                                    categories.computeIfAbsent(a.getType(), t -> new ArrayList<>()).add(a);
                                if (categories.isEmpty()) continue;
                                container.h2().text(resolved ? "Automatically Corrected · No Further Correction Needed" : "Submission Flags · Needs Review").__();
                                container.p().attrClass("section-intro").text(resolved
                                        ? "Each row groups a student’s related corrections. Details show the affected questions and files."
                                        : "Check the recorded finding and the suggested next step before finalising marks.").__();
                                for (Map.Entry<Anomaly.Type, List<Anomaly>> entry : categories.entrySet()) {
                                    List<Anomaly> group = entry.getValue();
                                    Map<String, List<Anomaly>> byStudent = new LinkedHashMap<>();
                                    for (Anomaly a : group) byStudent.computeIfAbsent(a.studentId(), id -> new ArrayList<>()).add(a);
                                    container.div().attrClass("category-banner").attrId(categoryId(group.get(0)))
                                        .div().attrClass("banner-header")
                                            .span().text(entry.getKey().getLabel()).__()
                                            .span().attrClass("count-pill").text(group.size() + " issue(s) · " + byStudent.size() + " student(s)").__()
                                        .__()
                                        .div().attrClass("banner-body issue-table").table()
                                            .thead().tr()
                                                .th().text("Student").__().th().text("Why It Was Flagged / Affected Files").__()
                                                .th().text("System Action").__().th().text("What to Check").__()
                                            .__().__()
                                            .tbody().of(tbody -> {
                                                for (Map.Entry<String, List<Anomaly>> studentEntry : byStudent.entrySet()) {
                                                    List<Anomaly> own = studentEntry.getValue();
                                                    tbody.tr()
                                                        .td().strong().text(data.lookupStudentName(studentEntry.getKey())).__()
                                                            .span().attrClass("mini").text(studentEntry.getKey()).__()
                                                        .__()
                                                        .td().ul().attrClass("group-details").of(list -> {
                                                            for (Anomaly a : own) {
                                                                // Separates the explanation and output with a newline.
                                                                String[] parts = a.getDetails().split("\\R", 2);

                                                                list.li().of(item -> {
                                                                    item.text(parts[0]);

                                                                    if (parts.length > 1 && !parts[1].isBlank()) {
                                                                        item.raw("<details class=\"output-details\"><summary>")
                                                                            .text("View full output")
                                                                            .raw("</summary>")
                                                                            .pre().attrClass("diagnostic")
                                                                                .text(parts[1])
                                                                            .__()
                                                                            .raw("</details>");
                                                                    }
                                                                }).__();
                                                            }
                                                        }).__().__()
                                                        .td().span().attrClass(resolved ? "badge badge-success" : "badge badge-warning")
                                                            .text(resolved ? "Auto-corrected" : "Needs review").__()
                                                            .of(cell -> {
                                                                if (resolved) {
                                                                    Set<String> actions = new LinkedHashSet<>();
                                                                    for (Anomaly a : own) actions.add(a.getResolution().replaceFirst("(?i)^Auto-Repaired:\\s*", ""));
                                                                    for (String action : actions) cell.p().text(action).__();
                                                                } else cell.p().text(systemAction(entry.getKey())).__();
                                                            })
                                                        .__()
                                                        .td().text(resolved ? "No further correction needed." : nextStep(entry.getKey())).__()
                                                    .__();
                                                }
                                            }).__()
                                        .__().__()
                                        .div().attrClass("back").a().attrHref("#gradebook").text("↑ Back to student marks").__().__()
                                    .__();
                                }
                            }
                        })
                    .__()
                .__()
            .__()
        );
    }

    @Override
    protected String buildContent(ReportData data) {
         return VIEW.render(data); 
    }

    private static List<FinalGrade> grades(ReportData data) {
        return data.getGrades() == null ? Collections.emptyList() : data.getGrades();
    }
    private static List<Anomaly> anomalies(ReportData data) {
        return data.getAnomalies() == null ? Collections.emptyList() : data.getAnomalies();
    }
    private static List<String> questionNames(ReportData data) {
        Set<String> names = new TreeSet<>();
        for (FinalGrade grade : grades(data)) for (QuestionScore score : grade.getQuestionScores()) names.add(score.getQuestionName());
        return new ArrayList<>(names);
    }
    private static Double questionScore(FinalGrade grade, String question) {
        for (QuestionScore score : grade.getQuestionScores()) if (question.equals(score.getQuestionName())) return score.getScore();
        return null;
    }
    private static List<Anomaly> forStudent(List<Anomaly> anomalies, String id) {
        List<Anomaly> own = new ArrayList<>();
        for (Anomaly anomaly : anomalies) if (id.equals(anomaly.studentId())) own.add(anomaly);
        return own;
    }
    private static List<PlagiarismResult> pairs(ReportData data) {
        Map<String, PlagiarismResult> unique = new LinkedHashMap<>();
        if (data.getPlagiarismResults() != null) for (PlagiarismResult pair : data.getPlagiarismResults()) {
            String a = pair.getStudentA(), b = pair.getStudentB();
            String key = (a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a) + "|" + pair.getSimilarityScore();
            unique.putIfAbsent(key, pair);
        }
        return new ArrayList<>(unique.values());
    }
    private static boolean isMarkIssue(Anomaly a) {
        return a.getType() == Anomaly.Type.COMPILATION_FAILURE || a.getType() == Anomaly.Type.EXECUTION_TIMEOUT
                || a.getType() == Anomaly.Type.RUNTIME_ERROR;
    }
    private static String questionOf(Anomaly a) {
        String details = a.getDetails();
        Matcher tester = Pattern.compile("\\b(Q\\d+[a-zA-Z]?)Tester\\b").matcher(details);
        if (tester.find()) return tester.group(1);
        Matcher question = Pattern.compile("\\b(Q\\d+[a-zA-Z]?)\\b").matcher(details);
        return question.find() ? question.group(1) : "";
    }
    private static String markLabel(Anomaly a) {
        if (a.getType() == Anomaly.Type.COMPILATION_FAILURE) return "Compilation failed";
        if (a.getType() == Anomaly.Type.EXECUTION_TIMEOUT) return "Execution timeout";
        return "Runtime error";
    }
    private static String categoryId(Anomaly a) {
        return a.getType().name().toLowerCase(Locale.ROOT) + (a.isResolved() ? "-corrected" : "-review");
    }
    private static String categoryLabel(Anomaly a) {
        String suffix = a.isResolved() ? " corrected" : " issue";
        switch (a.getType()) {
            case MISSING_HEADER: return a.isResolved() ? "Header corrected" : "Header issue";
            case UNRENAMED_FOLDER: case INCORRECT_FOLDER: return "Root folder" + suffix;
            case FLATTENED_HIERARCHY: return "Username folder" + suffix;
            case MISSING_QUESTION_FOLDER: return "Folder missing";
            case MISSING_SOURCE_FILE: return "File missing";
            case EXECUTION_TIMEOUT: return "Execution timeout";
            case RUNTIME_ERROR: return "Runtime error";
            default: return a.getType().getLabel();
        }
    }
    private static String systemAction(Anomaly.Type type) {
        if (type == Anomaly.Type.EXECUTION_TIMEOUT) return "The tester was stopped. See the recorded marks and execution message; partial marks may need manual review.";
        if (type == Anomaly.Type.RUNTIME_ERROR) return "Execution did not finish successfully. See the recorded marks and error message.";
        return "The finding was recorded. Its details and the gradebook show the available result.";
    }
    private static String nextStep(Anomaly.Type type) {
        if (type == Anomaly.Type.MISSING_SOURCE_FILE || type == Anomaly.Type.MISSING_QUESTION_FOLDER)
            return "Check the original ZIP for a missing or misplaced file before confirming the marks.";
        if (type == Anomaly.Type.EXECUTION_TIMEOUT) return "Inspect the affected method for non-termination and check any completed tests for partial marks.";
        if (type == Anomaly.Type.RUNTIME_ERROR) return "Inspect the recorded runtime message and the affected question.";
        return "Check the recorded details against the original submission.";
    }
    private static String issueLabel(Anomaly.Type type) {
        return switch (type) {
            case MISSING_HEADER -> "Missing / malformed header";
            case UNRENAMED_FOLDER -> "Default folder name";
            case INCORRECT_FOLDER -> "Incorrect root folder";
            case FLATTENED_HIERARCHY -> "Missing username folder";
            case MISSING_QUESTION_FOLDER -> "Missing question folder";
            case MISSING_SOURCE_FILE -> "Missing Java file";
            case COMPILATION_FAILURE -> "Compilation failed";
            case EXECUTION_TIMEOUT -> "Execution timeout";
            case RUNTIME_ERROR -> "Runtime error";
        };
    }
    private static String number(double value) { return String.format(Locale.ENGLISH, "%.1f", value); }
    private static String marks(double value, double max) { return number(value) + " / " + number(max); }
    private static String percent(PlagiarismResult pair) { return String.format(Locale.ENGLISH, "%.2f%%", pair.getSimilarityScore() * 100); }

    private static final String CSS = """

    :root {
      --bg-main: #f8fafc;
      --text-main: #0f172a;
      --text-muted: #64748b;
      --card-bg: #ffffff;
      --border-color: #cbd5e1;
      
      --danger-bg: #fee2e2;
      --danger-text: #991b1b;
      --warning-bg: #fef3c7;
      --warning-text: #92400e;
      --success-bg: #dcfce7;
      --success-text: #166534;
      --info-bg: #e0f2fe;
      --info-text: #075985;
    }

    body {
      font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;
      margin: 0;
      padding: 40px;
      background-color: var(--bg-main);
      color: var(--text-main);
      line-height: 1.5;
    }

    .container {
      max-width: 98%;
      margin: 0 auto;
    }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      margin-bottom: 30px;
      padding-bottom: 20px;
      border-bottom: 2px solid var(--border-color);
    }

    .header h1 {
      margin: 0;
      font-size: 1.8rem;
      font-weight: 700;
    }

    .header .meta {
      font-size: 0.875rem;
      color: var(--text-muted);
      text-align: right;
    }

    /* KPI Metrics Grid */
    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 15px;
      margin-bottom: 35px;
    }

    .metric-card {
      background: var(--card-bg);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      padding: 16px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    }

    .metric-card .label {
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
      margin-bottom: 6px;
    }

    .metric-card .value {
      font-size: 1.6rem;
      font-weight: 700;
    }

    .metric-card.highlight .value { color: #2563eb; }

    h2 {
      font-size: 1.3rem;
      margin-top: 40px;
      margin-bottom: 15px;
      color: var(--text-main);
    }

    /* Tables */
    .table-container {
      background: var(--card-bg);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
      margin-bottom: 30px;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
      font-size: 0.95rem;
    }

    th {
      background-color: #1e293b;
      color: #ffffff;
      padding: 12px 18px;
      font-weight: 600;
      font-size: 0.85rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    td {
      padding: 12px 18px;
      border-bottom: 1px solid var(--border-color);
      vertical-align: middle;
    }

    tr:last-child td {
      border-bottom: none;
    }

    tr:hover {
      background-color: #f8fafc;
    }

    /* Category Banners */
    .category-banner {
      background: var(--card-bg);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      margin-bottom: 20px;
      overflow: hidden;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    }

    .banner-header {
      background-color: #1e293b;
      color: #ffffff;
      padding: 12px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: 600;
      font-size: 0.95rem;
    }

    .banner-header .count-pill {
      background-color: rgba(255, 255, 255, 0.15);
      color: #ffffff;
      padding: 3px 10px;
      border-radius: 12px;
      font-size: 0.8rem;
      font-weight: 700;
    }

    .banner-header.clean {
      background-color: #334155;
    }

    .no-errors {
      padding: 18px 20px;
      color: var(--text-muted);
      font-style: italic;
    }

    /* Badges & Text Utilities */
    .badge {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }

    .output-details {
    margin-top: 8px;
    }

    .output-details summary {
        cursor: pointer;
        color: #234979;
        font-size: 13px;
        font-weight: 600;
    }

    .diagnostic {
        white-space: pre-wrap;
        overflow-wrap: anywhere;
        max-height: 300px;
        overflow-y: auto;
    }

    .badge-danger { background-color: var(--danger-bg); color: var(--danger-text); }
    .badge-warning { background-color: var(--warning-bg); color: var(--warning-text); }
    .badge-success { background-color: var(--success-bg); color: var(--success-text); }
    .badge-info { background-color: var(--info-bg); color: var(--info-text); }

    .code-snippet {
      font-family: 'Courier New', Courier, monospace;
      font-size: 0.85rem;
      background: #f1f5f9;
      padding: 2px 6px;
      border-radius: 4px;
    }

    /* Compact common-issues summary */
    .common-issues {
        margin: -12px 0 24px;
        padding: 14px 16px;
        background: var(--card-bg);
        border: 1px solid var(--border-color);
        border-radius: 8px;
    }

    .common-issues-heading {
        display: flex;
        align-items: baseline;
        flex-wrap: wrap;
        gap: 6px 14px;
        margin-bottom: 10px;
        font-size: 14px;
    }

    .common-issues-heading > span {
        color: var(--text-muted);
        font-size: 12px;
    }

    .common-issues-list {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
    }

    .common-issue {
        display: inline-flex;
        align-items: center;
        gap: 12px;
        padding: 6px 10px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 6px;
        color: #234979;
        font-size: 13px;
        text-decoration: none;
    }

    .common-issue strong {
        color: var(--text-main);
        font-variant-numeric: tabular-nums;
    }

    .common-issue:hover {
        background: #eff6ff;
        border-color: #93b4e4;
    }

    .common-issue:hover span {
        text-decoration: underline;
        text-underline-offset: 3px;
    }

    .text-bold { font-weight: 700; }
  
    .table-container,.banner-body{overflow-x:auto}.num{text-align:right;font-variant-numeric:tabular-nums}.student small{display:block;color:var(--text-muted);font-size:12px;font-weight:400}.note{font-size:13px;color:var(--text-muted);margin:8px 0 18px}.flags{font-size:13px;min-width:205px}.flags a{display:block;color:inherit;margin:4px 0;text-decoration:underline;text-underline-offset:3px}.status-line{padding:13px 17px;border:1px solid #bbd9c5;background:#edf7ef;border-radius:8px;color:#24533a;font-size:14px;margin-bottom:24px}.banner-body td{vertical-align:top}.badge{white-space:nowrap}.category-banner:target{outline:3px solid #93b4e4;outline-offset:4px}a:focus-visible{outline:3px solid #2563eb;outline-offset:3px}.sample{font-size:12px;color:var(--text-muted);margin-bottom:14px}footer{margin-top:28px;padding-top:15px;border-top:1px solid var(--border-color);font-size:12px;color:var(--text-muted)}@media(max-width:750px){body{padding:20px 14px}.header{align-items:flex-start;flex-direction:column;gap:15px}.header .meta{text-align:left}.metrics-grid{grid-template-columns:repeat(2,minmax(0,1fr))}th,td{padding:11px 13px}.table-container table{min-width:780px}.banner-body table{min-width:650px}.banner-header{gap:10px}}@media print{body{padding:0;background:white}.table-container,.banner-body{overflow:visible}.category-banner{break-inside:avoid}.sample{display:none}th{print-color-adjust:exact}a{color:inherit;text-decoration:none!important}}

    html{scroll-behavior:smooth;scroll-padding-top:20px}.section-intro{color:#526173;font-size:14px;margin:-5px 0 16px}.flag-item{display:flex;align-items:center;gap:7px;margin:5px 0}.flag-item a{color:#234979}.gradebook th{white-space:nowrap}.gradebook td{vertical-align:top}.gradebook .num{font-size:15px}.table-container .num{min-width:60px}.key{display:flex;gap:20px;flex-wrap:wrap;margin:14px 0 22px;font-size:13px;color:#475569}.dot{width:9px;height:9px;display:inline-block;border-radius:50%;flex-shrink:0}.dot.review{background:#c07a12}.dot.fixed{background:#248347}.issue-table th:nth-child(1){width:19%}.issue-table th:nth-child(2){width:30%}.issue-table th:nth-child(3){width:23%}.issue-table th:nth-child(4){width:28%}.issue-table p{margin:5px 0;font-size:14px}.mini{font-size:12px;color:#526173;display:block;margin-top:5px}.jump{color:#294d7d;text-decoration:none;font-size:13px}.back{padding:10px 20px;border-top:1px solid #e2e8f0;font-size:12px}.back a{color:#38516e}.reason{background:#fff7e7;border:1px solid #ebd5a7;padding:11px 15px;margin:0 0 18px;border-radius:6px;font-size:13px}.metrics-grid{grid-template-columns:repeat(5,minmax(0,1fr))}@media(max-width:750px){.metrics-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.issue-table{min-width:800px}}

    .mark-details{min-width:110px;max-width:260px}.mark-details summary{cursor:pointer;list-style:none;text-align:right}.mark-details summary::-webkit-details-marker{display:none}.mark-details summary .mark-label{display:block;font-size:11px;font-weight:600;color:#991b1b;text-decoration:underline;text-underline-offset:3px;margin-top:5px}.mark-details[open]{min-width:240px}.mark-details[open] summary{margin-bottom:12px}.mark-explanation{text-align:left;background:#fff5f5;border:1px solid #efcccc;border-radius:6px;padding:12px;font-size:12px;font-weight:400;line-height:1.55}.mark-explanation p{margin:6px 0}.mark-explanation pre{white-space:pre-wrap;overflow-wrap:anywhere;background:white;border:1px solid #eadada;padding:8px;font:11px/1.6 ui-monospace,monospace;border-radius:4px}.mark-details summary:focus-visible{outline:3px solid #2563eb;outline-offset:4px}.gradebook th,.gradebook td{padding:12px 13px}.gradebook table{min-width:1050px}.example-help{background:#eff6ff;border:1px solid #c5daf3;border-radius:7px;color:#254d77;padding:12px 16px;font-size:13px;margin-bottom:20px}@media print{.mark-details{max-width:none}}

    .gradebook table{min-width:1050px}.gradebook td{vertical-align:top}.gradebook .flags{min-width:160px}.meta{min-width:200px}.mark-details summary{white-space:normal}.diagnostic{white-space:pre-wrap;overflow-wrap:anywhere;font:12px/1.6 Consolas,monospace;background:#f8fafc;border:1px solid #e2e8f0;border-radius:5px;padding:12px;margin:8px 0}.group-details{margin:0;padding-left:18px;font-size:14px}.group-details li{margin-bottom:7px}.category-banner:target,tr:target{outline:2px solid #93b4e4;outline-offset:-2px}.badge-link{text-decoration:none}.mini{font-weight:400}.unmatched{margin:0 0 20px}.issue-table td{vertical-align:top}@media print{details{break-inside:avoid}.diagnostic{font-size:10px}}

    """;

}
