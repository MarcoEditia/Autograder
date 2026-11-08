package analytics;

import model.PlagiarismResult;
import autograder.ui.ProgressUI;
import autograder.util.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PlagiarismEngine {
    // Students with less lines than this are skipped to avoid false flags
    private static final int MIN_OWN_LINES = 5;

    // Loads starting code for questions
    public static Set<String> loadStarterLines(Path starterDir) {
        Set<String> starterLines = new HashSet<>();
        try {
            for (Path file : FileUtils.listFilePaths(starterDir)) {
                if (file.toString().endsWith(".java")) {
                    starterLines.addAll(tokenize(Files.readString(file)));
                }
            }
        } catch (IOException e) {
            // e.g. folder missing: carry on without it, but say so
            System.out.println("[PlagiarismEngine] Could not read starter files (" + e.getMessage()
                    + "); starter code will NOT be ignored.");
        }
        return starterLines;
    }

    // Input: studentId -> source code, and the starter lines to ignore. Output: A list of all flagged pairs.
    public static List<PlagiarismResult> checkAll(Map<String, String> codeById, double threshold, Set<String> starterLines) {
        List<String> ids = new ArrayList<>(codeById.keySet());
        
        // Tokenize each submission, then remove the lines that also appear in the starter code
        Map<String, Set<String>> tokens = new HashMap<>();
        for (String id : ids) {
            Set<String> ownLines = tokenize(codeById.get(id));
            ownLines.removeAll(starterLines);
            tokens.put(id, ownLines);
        }

        // Return a list of all plagiarised pairs
        List<PlagiarismResult> allFlaggedPairs = new ArrayList<>();

        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                String a = ids.get(i);
                String b = ids.get(j);
                
                // Only compare when both students wrote enough of their own code
                if (tokens.get(a).size() >= MIN_OWN_LINES && tokens.get(b).size() >= MIN_OWN_LINES) {
                    double score = calculateSim(tokens.get(a), tokens.get(b));

                    // Flag ANY pair that crosses the threshold
                    if (score > threshold) {
                        allFlaggedPairs.add(new PlagiarismResult(a, b, score));
                    }
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