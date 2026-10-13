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
        
        // Grab last line
        String lastLine = lines[lines.length - 1].trim();

        double finalScore = 0.0;
        try {
            // Attempt to parse the last line into a decimal number
            finalScore = Double.parseDouble(lastLine);
        } catch (NumberFormatException e) {
            // If the code crashed, the last line is text, not a number
            // Catch error and give 0
            System.out.println("[Evaluator] Could not parse score for " + questionName + ". Defaulting to 0.0.");
        }

        return new QuestionScore(questionName, finalScore);
    }
}