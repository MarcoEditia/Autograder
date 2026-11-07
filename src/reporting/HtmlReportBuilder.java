package reporting;

import htmlflow.HtmlFlow;
import htmlflow.HtmlView;

import model.Anomaly;
import model.FinalGrade;
import model.PlagiarismResult;
import model.ReportData;

import autograder.config.Config;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java.util.EnumMap;

public class HtmlReportBuilder extends BaseReportBuilder{

    private final Config config;
    private final HtmlView<ReportData> VIEW;

    public HtmlReportBuilder(Config config){
        this.config = config;

        this.VIEW = HtmlFlow.view(page -> page
        .html().attrLang("en")
            .head()
                .meta().attrCharset("UTF-8").__()
                .meta().attrName("viewport").attrContent("width=device-width, initial-scale=1.0").__()
                .title().text("AutoGrader - Cohort Master Report").__()
                .style()
                    .raw(":root {\n"
                        + "      --bg-main: #f8fafc;\n"
                        + "      --text-main: #0f172a;\n"
                        + "      --text-muted: #64748b;\n"
                        + "      --card-bg: #ffffff;\n"
                        + "      --border-color: #cbd5e1;\n"
                        + "      \n"
                        + "      --danger-bg: #fee2e2;\n"
                        + "      --danger-text: #991b1b;\n"
                        + "      --warning-bg: #fef3c7;\n"
                        + "      --warning-text: #92400e;\n"
                        + "      --success-bg: #dcfce7;\n"
                        + "      --success-text: #166534;\n"
                        + "      --info-bg: #e0f2fe;\n"
                        + "      --info-text: #075985;\n"
                        + "    }\n"
                        + "\n"
                        + "    body {\n"
                        + "      font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;\n"
                        + "      margin: 0;\n"
                        + "      padding: 40px;\n"
                        + "      background-color: var(--bg-main);\n"
                        + "      color: var(--text-main);\n"
                        + "      line-height: 1.5;\n"
                        + "    }\n"
                        + "\n"
                        + "    .container {\n"
                        + "      max-width: 1200px;\n"
                        + "      margin: 0 auto;\n"
                        + "    }\n"
                        + "\n"
                        + "    .header {\n"
                        + "      display: flex;\n"
                        + "      justify-content: space-between;\n"
                        + "      align-items: flex-end;\n"
                        + "      margin-bottom: 30px;\n"
                        + "      padding-bottom: 20px;\n"
                        + "      border-bottom: 2px solid var(--border-color);\n"
                        + "    }\n"
                        + "\n"
                        + "    .header h1 {\n"
                        + "      margin: 0;\n"
                        + "      font-size: 1.8rem;\n"
                        + "      font-weight: 700;\n"
                        + "    }\n"
                        + "\n"
                        + "    .header .meta {\n"
                        + "      font-size: 0.875rem;\n"
                        + "      color: var(--text-muted);\n"
                        + "      text-align: right;\n"
                        + "    }\n"
                        + "\n"
                        + "    /* KPI Metrics Grid */\n"
                        + "    .metrics-grid {\n"
                        + "      display: grid;\n"
                        + "      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));\n"
                        + "      gap: 15px;\n"
                        + "      margin-bottom: 35px;\n"
                        + "    }\n"
                        + "\n"
                        + "    .metric-card {\n"
                        + "      background: var(--card-bg);\n"
                        + "      border: 1px solid var(--border-color);\n"
                        + "      border-radius: 8px;\n"
                        + "      padding: 16px;\n"
                        + "      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);\n"
                        + "    }\n"
                        + "\n"
                        + "    .metric-card .label {\n"
                        + "      font-size: 0.75rem;\n"
                        + "      font-weight: 700;\n"
                        + "      text-transform: uppercase;\n"
                        + "      letter-spacing: 0.05em;\n"
                        + "      color: var(--text-muted);\n"
                        + "      margin-bottom: 6px;\n"
                        + "    }\n"
                        + "\n"
                        + "    .metric-card .value {\n"
                        + "      font-size: 1.6rem;\n"
                        + "      font-weight: 700;\n"
                        + "    }\n"
                        + "\n"
                        + "    .metric-card.highlight .value { color: #2563eb; }\n"
                        + "\n"
                        + "    h2 {\n"
                        + "      font-size: 1.3rem;\n"
                        + "      margin-top: 40px;\n"
                        + "      margin-bottom: 15px;\n"
                        + "      color: var(--text-main);\n"
                        + "    }\n"
                        + "\n"
                        + "    /* Tables */\n"
                        + "    .table-container {\n"
                        + "      background: var(--card-bg);\n"
                        + "      border: 1px solid var(--border-color);\n"
                        + "      border-radius: 8px;\n"
                        + "      overflow: hidden;\n"
                        + "      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);\n"
                        + "      margin-bottom: 30px;\n"
                        + "    }\n"
                        + "\n"
                        + "    table {\n"
                        + "      width: 100%;\n"
                        + "      border-collapse: collapse;\n"
                        + "      text-align: left;\n"
                        + "      font-size: 0.95rem;\n"
                        + "    }\n"
                        + "\n"
                        + "    th {\n"
                        + "      background-color: #1e293b;\n"
                        + "      color: #ffffff;\n"
                        + "      padding: 12px 18px;\n"
                        + "      font-weight: 600;\n"
                        + "      font-size: 0.85rem;\n"
                        + "      text-transform: uppercase;\n"
                        + "      letter-spacing: 0.05em;\n"
                        + "    }\n"
                        + "\n"
                        + "    td {\n"
                        + "      padding: 12px 18px;\n"
                        + "      border-bottom: 1px solid var(--border-color);\n"
                        + "      vertical-align: middle;\n"
                        + "    }\n"
                        + "\n"
                        + "    tr:last-child td {\n"
                        + "      border-bottom: none;\n"
                        + "    }\n"
                        + "\n"
                        + "    tr:hover {\n"
                        + "      background-color: #f8fafc;\n"
                        + "    }\n"
                        + "\n"
                        + "    /* Category Banners */\n"
                        + "    .category-banner {\n"
                        + "      background: var(--card-bg);\n"
                        + "      border: 1px solid var(--border-color);\n"
                        + "      border-radius: 8px;\n"
                        + "      margin-bottom: 20px;\n"
                        + "      overflow: hidden;\n"
                        + "      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);\n"
                        + "    }\n"
                        + "\n"
                        + "    .banner-header {\n"
                        + "      background-color: #1e293b;\n"
                        + "      color: #ffffff;\n"
                        + "      padding: 12px 20px;\n"
                        + "      display: flex;\n"
                        + "      justify-content: space-between;\n"
                        + "      align-items: center;\n"
                        + "      font-weight: 600;\n"
                        + "      font-size: 0.95rem;\n"
                        + "    }\n"
                        + "\n"
                        + "    .banner-header .count-pill {\n"
                        + "      background-color: rgba(255, 255, 255, 0.15);\n"
                        + "      color: #ffffff;\n"
                        + "      padding: 3px 10px;\n"
                        + "      border-radius: 12px;\n"
                        + "      font-size: 0.8rem;\n"
                        + "      font-weight: 700;\n"
                        + "    }\n"
                        + "\n"
                        + "    .banner-header.clean {\n"
                        + "      background-color: #334155;\n"
                        + "    }\n"
                        + "\n"
                        + "    .no-errors {\n"
                        + "      padding: 18px 20px;\n"
                        + "      color: var(--text-muted);\n"
                        + "      font-style: italic;\n"
                        + "    }\n"
                        + "\n"
                        + "    /* Badges & Text Utilities */\n"
                        + "    .badge {\n"
                        + "      display: inline-block;\n"
                        + "      padding: 4px 10px;\n"
                        + "      border-radius: 4px;\n"
                        + "      font-size: 0.75rem;\n"
                        + "      font-weight: 700;\n"
                        + "      text-transform: uppercase;\n"
                        + "      letter-spacing: 0.03em;\n"
                        + "    }\n"
                        + "\n"
                        + "    .badge-danger { background-color: var(--danger-bg); color: var(--danger-text); }\n"
                        + "    .badge-warning { background-color: var(--warning-bg); color: var(--warning-text); }\n"
                        + "    .badge-success { background-color: var(--success-bg); color: var(--success-text); }\n"
                        + "    .badge-info { background-color: var(--info-bg); color: var(--info-text); }\n"
                        + "\n"
                        + "    .code-snippet {\n"
                        + "      font-family: 'Courier New', Courier, monospace;\n"
                        + "      font-size: 0.85rem;\n"
                        + "      background: #f1f5f9;\n"
                        + "      padding: 2px 6px;\n"
                        + "      border-radius: 4px;\n"
                        + "    }\n"
                        + "\n"
                        + "    .text-bold { font-weight: 700; }\n"
                        + "    .subtitle { margin: 5px 0 0; color: var(--text-muted); }\n"
                        + "    .empty-row { padding: 20px; color: var(--text-muted); font-style: italic; }\n"
                        + "    .table-container, .banner-body { overflow-x: auto; }\n"
                        + "    td { overflow-wrap: anywhere; }\n"
                        + "    .badge { margin: 2px 4px 2px 0; }\n"
                        + "    @media (max-width: 700px) {\n"
                        + "      body { padding: 20px 12px; }\n"
                        + "      .header { flex-direction: column; align-items: flex-start; gap: 16px; }\n"
                        + "      .header .meta { text-align: left; }\n"
                        + "      .header h1 { font-size: 1.5rem; }\n"
                        + "      .metrics-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }\n"
                        + "      table { min-width: 620px; }\n"
                        + "    }\n"
                        + "    @media print {\n"
                        + "      body { padding: 0; background: #fff; }\n"
                        + "      .container { max-width: none; }\n"
                        + "      .metric-card, .table-container, .category-banner { box-shadow: none; }\n"
                        + "      .table-container, .banner-body { overflow: visible; }\n"
                        + "      .metric-card, tr { break-inside: avoid; }\n"
                        + "      thead { display: table-header-group; }\n"
                        + "      th, td { padding: 8px; }\n"
                        + "    }\n")
                .__() // style
            .__() // head
            .body()
                .div().attrClass("container")
                .div().attrClass("header")
                    .div()
                        .h1().text("AutoGrader Cohort Master Report").__()
                        .p().attrClass("subtitle")
                            .text("Grade Distribution, Score Analytics & Ingestion Audit Log").__()
                    .__()
                    .div().attrClass("meta")
                        .<ReportData>dynamic((meta, data) -> meta
                            .div()
                                .strong().text("Generated: ").__()
                                .text(java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Singapore"))
                                    .format(java.time.format.DateTimeFormatter.ofPattern(
                                        "MMM dd, uuuu '•' HH:mm 'SGT'", java.util.Locale.ENGLISH)))
                            .__()
                            .div()
                                .strong().text("Cohort Size: ").__()
                                .text(grades(data).size() + " Submissions")
                            .__()
                        )
                    .__()
                .__() // header

                // Section 1: Summary Analytics
                .h2().text("Cohort Score Summary").__()
                .div().attrClass("metrics-grid")
                    .<ReportData>dynamic((grid, data) -> grid
                        .div().attrClass("metric-card")
                            .div().attrClass("label").text("Total Submissions").__()
                            .div().attrClass("value").text(String.valueOf(grades(data).size())).__()
                        .__()
                        .div().attrClass("metric-card highlight")
                            .div().attrClass("label").text("Average Score").__()
                            .div().attrClass("value").text(String.format("%.1f / %.1f", data.getAverageScore(), config.getMaxScore())).__()
                        .__()
                        .div().attrClass("metric-card highlight")
                            .div().attrClass("label").text("Median Score").__()
                            .div().attrClass("value").text(String.format("%.1f / %.1f", data.getMedianScore(), config.getMaxScore())).__()
                        .__()
                        .div().attrClass("metric-card")
                            .div().attrClass("label").text("Lowest Score").__()
                            .div().attrClass("value").text(String.format("%.1f / %.1f", data.getMinScore(), config.getMaxScore())).__()
                        .__()
                        .div().attrClass("metric-card")
                            .div().attrClass("label").text("Highest Score").__()
                            .div().attrClass("value").text(String.format("%.1f / %.1f", data.getMaxScore(), config.getMaxScore())).__()
                        .__()
                    )
                .__() // metrics-grid

                // Section 2: Master Student Gradebook
                .h2().text("Student Gradebook").__()
                .div().attrClass("table-container")
                .table()
                    .thead()
                        .tr()
                            .th().text("Student ID").__()
                            .th().text("Student Name").__()
                            .th().text("Total Score").__()
                            .th().text("Status").__()
                        .__()
                    .__()
                    .tbody()
                        .<ReportData>dynamic((tbody, data) -> {
                            List<FinalGrade> grades = grades(data);

                            if (grades.isEmpty()) {
                                tbody.tr()
                                    .td().attrColspan(4)
                                        .attrClass("empty-row")
                                        .text("No student grades available.")
                                    .__()
                                .__();
                                return;
                            }

                            for (FinalGrade grade : grades) {
                                boolean hasPlagiarism = grade.getPlagiarismFlags() != null
                                        && !grade.getPlagiarismFlags().isEmpty();
                                boolean hasAnomalies = grade.getAnomalies() != null
                                        && !grade.getAnomalies().isEmpty();

                                tbody.tr()
                                        .td()
                                            .span().attrClass("code-snippet")
                                                .text(grade.getStudentId()).__()
                                            .__()
                                        .td()
                                            .text(grade.getName()).__()
                                        .td().attrClass("text-bold")
                                            .text(String.format("%.1f", grade.getTotalScore())).__()
                                        .td()
                                            .of(cell -> {
                                                if (hasAnomalies) {
                                                    cell.span().attrClass("badge badge-warning")
                                                        .text("ANOMALY").__();
                                                }
                                                if (hasPlagiarism) {
                                                    cell.span().attrClass("badge badge-danger")
                                                        .text("SIMILARITY FLAG").__();
                                                }
                                                if (!hasAnomalies && !hasPlagiarism) {
                                                    cell.span().attrClass("badge badge-success")
                                                        .text("CLEAN").__();
                                                }
                                            })
                                            .__()
                                     .__();
                            }
                        })
                    .__()
                .__() // table
                .__() // table-container

                // Section 3: Plagiarism Alerts 
                .h2().text("Plagiarism & Code Similarity Flags").__()
                .div().attrClass("table-container")
                .table()
                    .thead()
                        .tr()
                            .th().text("Student A ID").__()
                            .th().text("Student A Name").__()
                            .th().text("Student B ID").__()
                            .th().text("Student B Name").__()
                            .th().text("Similarity").__()
                            .th().text("Status").__()
                        .__()
                    .__()
                    .tbody()
                        .<ReportData>dynamic((tbody, data) -> {
                            List<PlagiarismResult> flags = data.getPlagiarismResults();

                            if (flags == null || flags.isEmpty()) {
                                tbody.tr()
                                    .td().attrColspan(6)
                                        .text("No code similarity alerts recorded.").__()
                                    .__();
                            } else {
                                for (PlagiarismResult flag : flags) {
                                    tbody.tr()
                                            .td().span().attrClass("code-snippet")
                                                    .text(flag.getStudentA()).__()
                                                .__()
                                            .td().text(data.lookupStudentName(flag.getStudentA())).__()
                                            .td().span().attrClass("code-snippet")
                                                    .text(flag.getStudentB()).__()
                                                .__()
                                            .td().text(data.lookupStudentName(flag.getStudentB())).__()
                                            .td().text(String.format("%.2f%%", flag.getSimilarityScore() * 100)).__()
                                            .td().span().attrClass("badge badge-danger").text("FLAGGED").__().__()
                                         .__();
                                }
                            }
                        })
                    .__()
                .__() // table
                .__() // table-container

                // Section 4: Categorized Anomaly Banners
                .h2().text("Ingestion & Structural Anomalies").__()
                .<ReportData>dynamic((container, data) -> {
                    Map<Anomaly.Type, List<Anomaly>> grouped = 
                        new EnumMap<>(Anomaly.Type.class);

                    for (Anomaly.Type type : Anomaly.Type.values()) {
                        grouped.put(type, new ArrayList<>());
                    }

                    List<Anomaly> anomalies = data.getAnomalies();

                    if (anomalies != null) {
                        for (Anomaly anomaly : anomalies) {
                            if (anomaly != null && anomaly.getType() != null) {
                                grouped.get(anomaly.getType()).add(anomaly);
                            }
                        }
                    }

                    for (Anomaly.Type type : Anomaly.Type.values()) {
                        List<Anomaly> typeAnomalies =
                            grouped.getOrDefault(type, Collections.emptyList());

                        container.div().attrClass("category-banner")
                            .div().attrClass(
                                typeAnomalies.isEmpty() 
                                    ? "banner-header clean" 
                                    : "banner-header")
                                .span().text(type.getLabel()).__()
                                .span().attrClass("count-pill")
                                    .text(
                                        typeAnomalies.size() 
                                        + (typeAnomalies.size() == 1
                                            ? " Issue"
                                            : " Issues"))
                                        .__()
                            .__() // banner-header
                            .div().attrClass("banner-body")
                                .of(body -> {
                                    if (typeAnomalies.isEmpty()) {
                                        body.div().attrClass("no-errors")
                                            .text("No issues recorded for " + type.getLabel() + ".")
                                        .__();
                                    } else {
                                        body.table()
                                            .thead()
                                                .tr()
                                                    .th().text("Student ID").__()
                                                    .th().text("Student Name").__()
                                                    .th().text("Severity").__()
                                                    .th().text("Details").__()
                                                    .th().text("Resolution").__()
                                                .__()
                                            .__()
                                            .tbody()
                                                .of(tbody -> {
                                                    for (Anomaly a : typeAnomalies) {
                                                        String sevBadge = a.getSeverity() == Anomaly.Severity.ERROR
                                                                ? "badge badge-danger"
                                                                : "badge badge-warning";

                                                        String resBadge = a.isResolved()
                                                                ? "badge badge-success"
                                                                : "badge badge-info";

                                                        tbody.tr()
                                                            .td().span().attrClass("code-snippet")
                                                                .text(a.studentId()).__()
                                                            .__()
                                                            .td().text(data.lookupStudentName(a.studentId())).__()
                                                            .td().span().attrClass(sevBadge)
                                                                .text(a.getSeverity().name()).__()
                                                            .__()
                                                            .td().text(a.getDetails()).__()
                                                            .td().span().attrClass(resBadge)
                                                                .text(a.getResolution()).__()
                                                                .__()
                                                        .__();
                                                    }
                                                })
                                            .__()  // Close tbody
                                        .__();   // Close table
                                    }
                                })
                            .__() // banner body
                        .__(); // category-banner
                    }
                })
                .__() // container
            .__() // body
        .__() // html
    );
    }

    @Override
    protected String buildContent(ReportData data){
        return VIEW.render(data);
    }

    private static List<FinalGrade> grades(ReportData data) {
        return data.getGrades() == null
            ? Collections.emptyList()
            : data.getGrades();
    }

}
