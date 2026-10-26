package reporting;

import htmlflow.HtmlFlow;
import htmlflow.HtmlView;

import model.Anomaly;
import model.FinalGrade;
import model.PlagiarismResult;
import model.ReportData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;

public class HtmlReportBuilder extends BaseReportBuilder{

    private static final HtmlView<ReportData> VIEW = HtmlFlow.view(page -> page
        .html()
            .head()
                .meta().attrCharset("UTF-8").__()
                .title().text("AutoGrader - Cohort Master Report").__()
                .style()
                    .raw("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 30px; background-color: #f8f9fa; color: #212529; }"
                        + "h1, h2 { color: #0f172a; margin-top: 25px; }"
                        + ".metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 15px; margin-bottom: 25px; }"
                        + ".metric-card { background: #ffffff; border: 1px solid #cbd5e1; border-radius: 8px; padding: 16px; }"
                        + ".metric-card .label { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; color: #64748b; margin-bottom: 6px; }"
                        + ".metric-card .value { font-size: 1.6rem; font-weight: 700; color: #2563eb; }"
                        + ".category-banner { background: #ffffff; border: 1px solid #cbd5e1; border-radius: 8px; margin-bottom: 20px; overflow: hidden; }"
                        + ".banner-header { background-color: #1e293b; color: #ffffff; padding: 12px 20px; display: flex; justify-content: space-between; font-weight: 600; }"
                        + ".banner-header.clean { background-color: #334155; }"
                        + ".count-pill { background-color: rgba(255, 255, 255, 0.15); padding: 3px 10px; border-radius: 12px; font-size: 0.8rem; }"
                        + ".no-errors { padding: 18px 20px; color: #64748b; font-style: italic; }"
                        + "table { width: 100%; border-collapse: collapse; background: #ffffff; margin-bottom: 0; }"
                        + "th, td { padding: 12px 18px; text-align: left; border-bottom: 1px solid #cbd5e1; }"
                        + "th { background-color: #f1f5f9; color: #334155; font-weight: 600; font-size: 0.85rem; text-transform: uppercase; }"
                        + "tr:hover { background-color: #f8fafc; }"
                        + ".badge { display: inline-block; padding: 4px 10px; border-radius: 4px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; }"
                        + ".badge-danger { background-color: #fee2e2; color: #991b1b; }"
                        + ".badge-warning { background-color: #fef3c7; color: #92400e; }"
                        + ".badge-success { background-color: #dcfce7; color: #166534; }"
                        + ".badge-info { background-color: #e0f2fe; color: #075985; }"
                        + ".code-snippet { font-family: 'Courier New', monospace; font-size: 0.85rem; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; }")
                .__() // style
            .__() // head
            .body()
                .h1().text("AutoGrader Cohort Master Report").__()

                // Section 1: Summary Analytics
                .h2().text("Cohort Score Summary").__()
                .div().attrClass("metrics-grid")
                    .<ReportData>dynamic((grid, data) -> grid
                        .div().attrClass("metric-card")
                            .div().attrClass("label").text("Total Submissions").__()
                            .div().attrClass("value").text(String.valueOf(grades(data).size())).__()
                        .__()
                        .div().attrClass("metric-card")
                            .div().attrClass("label").text("Average Score").__()
                            .div().attrClass("value").text(String.format("%.1f", data.getAverageScore())).__()
                        .__()
                        .div().attrClass("metric-card")
                            .div().attrClass("label").text("Median Score").__()
                            .div().attrClass("value").text(String.format("%.1f", data.getMedianScore())).__()
                        .__()
                        .div().attrClass("metric-card")
                            .div().attrClass("label").text("Lowest Score").__()
                            .div().attrClass("value").text(String.format("%.1f", data.getMinScore())).__()
                        .__()
                        .div().attrClass("metric-card")
                            .div().attrClass("label").text("Highest Score").__()
                            .div().attrClass("value").text(String.format("%.1f", data.getMaxScore())).__()
                        .__()
                    )
                .__() // metrics-grid

                // Section 2: Master Student Gradebook
                .h2().text("Student Gradebook").__()
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
                                boolean hasFlags = (grade.getPlagiarismFlags() != null && !grade.getPlagiarismFlags().isEmpty())
                                        || (grade.getAnomalies() != null && !grade.getAnomalies().isEmpty());

                                String statusBadge = hasFlags 
                                    ? "badge badge-warning" 
                                    : "badge badge-success";
                                
                                String statusText = hasFlags 
                                    ? "FLAGGED" 
                                    : "CLEAN";

                                tbody.tr()
                                        .td()
                                            .span().attrClass("code-snippet")
                                                .text(grade.getStudentId()).__()
                                            .__()
                                        .td()
                                            .text(grade.getName()).__()
                                        .td()
                                            .text(String.format("%.1f", grade.getTotalScore())).__()
                                        .td()
                                            .span().attrClass(statusBadge)
                                                .text(statusText).__()
                                            .__()
                                     .__();
                            }
                        })
                    .__()
                .__() // table

                // Section 3: Plagiarism Alerts 
                .h2().text("Plagiarism & Code Similarity Flags").__()
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
                            .div()
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
            .__() // body
        .__() // html
    );
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
