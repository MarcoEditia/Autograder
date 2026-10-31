package analytics;

import model.PlagiarismResult;
import autograder.ui.ProgressUI;
import java.util.*;

public class PlagiarismEngine {

    // Input: studentId -> source code. Output: A list of all flagged pairs.
    public static List<PlagiarismResult> checkAll(Map<String, String> codeById) {
        List<String> ids = new ArrayList<>(codeById.keySet());
        
        // Tokenize each submission
        Map<String, Set<String>> tokens = new HashMap<>();
        for (String id : ids) {
            tokens.put(id, tokenize(codeById.get(id)));
        }

        // Return a list of all plagiarised pairs
        List<PlagiarismResult> allFlaggedPairs = new ArrayList<>();

        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                String a = ids.get(i);
                String b = ids.get(j);
                
                double score = calculateSim(tokens.get(a), tokens.get(b));

                // Flag ANY pair that crosses the threshold
                if (score > 0.75) {
                    allFlaggedPairs.add(new PlagiarismResult(a, b, score));
                }
                ProgressUI.addBarValue();
            }
        }
        return allFlaggedPairs;
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