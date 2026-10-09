package analytics;

import model.PlagiarismResult;
import java.util.HashSet;
import java.util.Set;

public class PlagiarismEngine {
    
    // The public API that Yan Tat's GradingPipeline will call later.
    // It returns the PlagiarismResult object that Gabriele's report generator needs.
    public static PlagiarismResult compareStudents(String studentA_Id, String codeA, String studentB_Id, String codeB) {
        Set<String> tokensA = tokenize(codeA);
        Set<String> tokensB = tokenize(codeB);
        
        double score = calculateSim(tokensA, tokensB);
        
        return new PlagiarismResult(studentA_Id, studentB_Id, score);
    }

    private static Set<String> tokenize(String rawCode) {
        String noComments = rawCode.replaceAll("(?s)/\\*.*?\\*/", "")
                                   .replaceAll("//.*", "");
        
        String[] lines = noComments.split("\n");
        Set<String> tokens = new HashSet<>();
        
        for (String line : lines) {
            String masked = line.replaceAll("[a-zA-Z_]+", "X");
            String stripped = masked.replaceAll("\\s+", "");
            
            if (!stripped.isEmpty()) {
                tokens.add(stripped);
            }
        }
        
        return tokens;
    }

    private static double calculateSim(Set<String> setA, Set<String> setB) {
        if (setA.isEmpty() && setB.isEmpty()) return 1.0;
        if (setA.isEmpty() || setB.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        return (double) intersection.size() / union.size();
    }
}