package model;

public class ProcessResult {
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
    
    public boolean isTimedOut() {
        return timedOut;
    }

    public boolean isSuccess() {
        return exitCode == 0 && !timedOut;
    }
}
