package reporting;

import model.Anomaly;
import model.FinalGrade;
import model.PlagiarismResult;
import model.ReportData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseReportBuiler {
    public final void generateReport(List<FinalGrade> grades, String outputPath) throws IOException{
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()){
            parentDir.mkdirs();
        }

        List<PlagiarismResult> allFlags = new ArrayList<>();
        List<Anomaly> allAnomalies = new ArrayList<>();

        for (FinalGrade grade : grades){
            if (grade.getPlagiarismFlags() != null){
                allFlags.addAll(grade.getPlagiarismFlags());
            }
            if (grade.getAnomalies() != null){
                allAnomalies.addAll(grade.getAnomalies());
            }
        }

        ReportData reportData = new ReportData(grades, allAnomalies, allFlags);

        String content = buildContent(reportData);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))){
            writer.write(content);
        }
    }

    protected abstract String buildContent(ReportData data);
}
