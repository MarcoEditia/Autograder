package autograder.execution;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessRunner {
    public static class ProcessResult {
        private final int exitCode;
        private final String output;
        private final boolean timedOut;
        
        public ProcessResult(int exitCode, String output, boolean timedOut) {
            this.exitCode = exitCode;
            this.output = output;
            this.timedOut = timedOut;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }

        public boolean isTimedOut() {return timedOut;}
        public boolean isSuccess() {return exitCode == 0 && !timedOut;}
        }

    public static ProcessResult run(List<String> command, long timeoutSeconds) {
        StringBuilder outputLog = new StringBuilder();
        //StringBuilder just help stores output logs better than an array, 
        // handles line separators like \n too when u output it to string

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            //ProcessBuilder will run the command
            //multiple files will be run. For example, javac -d /path/to/classes /path/to/Q1a.java /path/to/Q1aTester.java
            
            pb.redirectErrorStream(true); //basically merges stderr into stdout for complete capture on screen
            
            Process process = pb.start();

            Thread readerThread = new Thread(() -> {
                //readerThread does background work which collects text into outputLog
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                //process.getInputStream gets the raw bytes
                //InputStreamReader translates the bytes into readable characters
                //BufferedReader packages the readable characters nicely and allows us to readLine
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputLog.append(line).append(System.lineSeparator());
                        //lineSeparator just adds newline break \n
                        //used lineSeparator because depending on windows and Mac, it can either be \n or \r\n
                }
            } catch (IOException e) {}
            });
            readerThread.start();
            

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            //let compilation process run for timeoutSeconds amount of seconds,
            //if compilation process terminates within time limit, whether pass or fail, finished becomes true

            if (!finished) {
                process.destroyForcibly();
                return new ProcessResult(-1, "Process timed out after " + timeoutSeconds + " seconds.", true);
            }
            readerThread.join(1000);
            //tells main thread to pause for readerThread to finish job of collecting logs within 1 second

            return new ProcessResult(process.exitValue(), outputLog.toString().trim(), false);
        } catch (IOException e) {
            return new ProcessResult(-1, "I/O Error: " + e.getMessage(), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProcessResult(-1, "Execution interrupted: " + e.getMessage(), false);
        }
    }
}