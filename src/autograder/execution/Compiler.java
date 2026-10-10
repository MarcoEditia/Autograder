package autograder.execution;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import model.Submission;

public class Compiler {
    public CompilationResult compile(Submission submission) {
        if (submission == null || submission.getFolderPath() == null) {
            return new CompilationResult(false, "Invalid submission or null folder path.");
        }

        File sourceDir = new File(submission.getFolderPath());
        File outputDir = new File(sourceDir, "classes");

        return compile(sourceDir, outputDir);
    }
    public CompilationResult compile(File sourceDir, File outputDir) {
        //sourceDir is student folder that has the java files including the tester files injected
        //outputDir is the classes folder inside the student's working space

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            return new CompilationResult(false, "Source directory does not exist: " + sourceDir.getPath());
        }

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File[] javaFiles = sourceDir.listFiles((dir, name) -> name.endsWith(".java"));
        //get a list of java files based on its suffix. not recursive

        if (javaFiles == null || javaFiles.length == 0) {
            return new CompilationResult(false, "No .java files found in directory " + sourceDir.getPath());
        }

        List<String> command = new ArrayList<>();
        command.add("javac");
        command.add("-d");
        command.add(outputDir.getAbsolutePath());

        for (File f : javaFiles) {
            command.add(f.getAbsolutePath());
        }

        ProcessRunner.ProcessResult r = ProcessRunner.run(command, 15);

        return new CompilationResult(r.isSuccess(), r.getOutput());
    }
}

// small example usage below. Will have to loop it in practice.

// Compile student code + tester
// CompilationResult result = compiler.compile(submission);
// if (!result.isSuccess()) {
//     System.out.println("Compilation failed: " + result.getOutput());
//     Give 0 marks and skip tests
// }