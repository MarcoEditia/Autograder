package io;

import model.ReportData;
import java.io.IOException; 

public interface ReportExporter {
    void export(ReportData data, String outputPath) throws IOException;
} 
