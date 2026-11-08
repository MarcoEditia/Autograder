package scoring;

import model.QuestionScore;

public class Evaluator {

    public static QuestionScore evaluate(String questionName, String consoleOutput) {
        // If time out or no output, give 0
        if (consoleOutput == null || consoleOutput.trim().isEmpty()) {
            return new QuestionScore(questionName, 0.0);
        }

        // Split string into lines
        String[] lines = consoleOutput.trim().split("\\r?\\n");
        
        // Get last line
        String lastLine = lines[lines.length - 1].trim();

        double finalScore = 0.0;
        try {
            // Try parse the last line into decimal number
            finalScore = Double.parseDouble(lastLine);
        } catch (NumberFormatException e) {
            // If error, last line is text, not a number
            // Catch error and give 0
            System.out.println("[Evaluator] Could not parse score for " + questionName + ". Defaulting to 0.0.");
        }

        // Count how many tests the tester itself reported as "Passed"
        int passed = 0;
        for (String line : lines) {
            if (line.trim().equals("Passed")) {
                passed++;
            }
        }

        // The tester's last line always equals its number of "Passed" lines.
        // If not, the number was not printed by the tester (e.g. student code printed it)
        if (finalScore != passed) {
            System.out.println("[Evaluator] Score for " + questionName + " (" + finalScore + ") does not match "
                    + passed + " passed tests. Defaulting to 0.0.");
            return new QuestionScore(questionName, 0.0);
        }

        return new QuestionScore(questionName, finalScore);
    }
}