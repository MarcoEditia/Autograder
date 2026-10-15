package reporting;

import model.FinalGrade;
import model.PlagiarismResult;
import model.ReportData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import htmlflow.HtmlFlow;
import htmlflow.HtmlView;

public class HtmlAnomalyBuilder {

    private static final HtmlView<ReportData> VIEW = HtmlFlow.view(page -> page
            .html()
                .head()
                    .meta().attrCharset("UTF-8").__()
                    .title().text("AutoGrader - Summary Audit Report").__()
                    .style()
                        .raw("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 30px; background-color: #f8f9fa; color: #212529; }"
                            + "h1, h2 { color: #0f172a; }"
                            + ".summary-card { background: #ffffff; padding: 15px 20px; border-radius: 6px; border: 1px solid #e2e8f0; margin-bottom: 25px; }"
                            + "table { width: 100%; border-collapse: collapse; background: #ffffff; margin-bottom: 30px; border-radius: 6px; overflow: hidden; }"
                            + "th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #e2e8f0; }"
                            + "th { background-color: #1e293b; color: #ffffff; font-weight: 600; }"
                            + "tr:hover { background-color: #f1f5f9; }"
                            + ".badge-danger { background-color: #fee2e2; color: #991b1b; padding: 4px 8px; border-radius: 4px; font-weight: bold; font-size: 0.85em; }")
                    .__()
                .__()
                .body()
                    .h1().text("AutoGrader Cohort Audit Report").__()
                    .div().attrClass("summary-card")
                        .<ReportData>dynamic((div, data) -> div
                        .p().strong().text("Total Submissions Processed: ").__()
                           .text(String.valueOf(data.getTotalSubmissions()))
                        .__()
                        )
                    .__()
                    .h2().text("Plagiarism & Code Similarity Flags").__()
                    .table()
                        .thead()
                            .tr()
                                .th().text("Student A").__()
                                .th().text("Student B").__()
                                .th().text("Similarity Score").__()
                                .th().text("Status").__()
                            .__()
                        .__()
                        .tbody()
                            .<ReportData>dynamic((tbody, data) -> {
                            List<PlagiarismResult> flags = data.getPlagiarismResults();
                            if (flags == null || flags.isEmpty()) {
                                tbody.tr()
                                        .td().attrColspan(4).text("No code similarity alerts recorded.").__()
                                     .__();
                            } else {
                                for (PlagiarismResult flag : flags) {
                                    tbody.tr()
                                            .td().text(flag.getStudentA()).__()
                                            .td().text(flag.getStudentB()).__()
                                            .td().text(String.format("%.2f%%", flag.getSimilarityScore() * 100)).__()
                                            .td().span().attrClass("badge-danger").text("FLAGGED").__().__()
                                         .__();
                                }
                            }
                        })
                    .__() // tbody
                .__() // table
            .__() // body
        .__() // html
    );
    
    public void generateHtmlReport(List<FinalGrade> grades, String fileName) throws IOException { 
        File outputFile = new File("reports/" + fileName); // hardcoded reports/ as parent dir
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        List<PlagiarismResult> allFlags = new ArrayList<>();
        for(FinalGrade grade : grades){
            List<PlagiarismResult> flags = grade.getPlagiarismFlags();
            if (flags != null){
                allFlags.addAll(flags);
            }
        } 

        ReportData reportData = new ReportData(grades.size(), allFlags);

        String htmlContent = VIEW.render(reportData);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(htmlContent);
        }

    }

}
