package autograder.execution;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import model.Submission;
import model.ProcessResult;

public class Compiler {
    private void findJavaFiles(File directory, List<File> resultList) {
        //directory is 2023-2024-jing.lim.2021
        //resultList is empty at start
        //listFiles will find every file and subdirectory
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory() && !file.getName().equals("classes")) {
                findJavaFiles(file, resultList); //recursive if it is a directory that is not classes folder
            }
            else if (file.isFile() && file.getName().endsWith(".java")) {
                //check if its a java file
                resultList.add(file);
            }
        }
    }

    public CompilationResult compile(Submission submission) {
        if (submission == null || submission.getFolderPath() == null) {
            return new CompilationResult(false, "Invalid submission or null folder path.");
        }

        File sourceDir = new File(submission.getFolderPath());
        File outputDir = new File(sourceDir, "classes");
        //sourceDir example: temp/jing.lim.2021
        //outputDir example: temp/jing.lim.2021/classes

        return compile(sourceDir, outputDir);
    }
    
    public CompilationResult compile(Submission submission, String questionFolder, String questionName) {
        //isolates single question
        if (submission == null || submission.getFolderPath() == null) {
            return new CompilationResult(false, "Invalid submission or null folder path.");
        }

        File sourceDir = new File(submission.getFolderPath(), questionFolder);
        File outputDir = new File(sourceDir, "classes/" + questionName);
        //sourceDir example: temp/jing.lim.2021/Q1
        //outputDir example: temp/jing.lim.2021/Q1/classes/Q1a

        return compile(sourceDir, outputDir, questionName);
    }

    public CompilationResult compile(File sourceDir, File outputDir) {
        return compile(sourceDir, outputDir, null);
    }

    public CompilationResult compile(File sourceDir, File outputDir, String questionName) {
        //sourceDir is student folder that has the java files including the tester files injected
        //outputDir is the classes folder inside the student's working space

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            return new CompilationResult(false, "Source directory does not exist: " + sourceDir.getPath());
        }

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        List<File> javaFiles = new ArrayList<>();
        findJavaFiles(sourceDir, javaFiles);
        //now javaFiles contain:
            // temp/jing.lim.2021/Q1/Q1a.java
            // temp/jing.lim.2021/Q1/Q1aTester.java
            // temp/jing.lim.2021/Q1/Q1b.java
            // temp/jing.lim.2021/Q2/Q2a.java
            // temp/jing.lim.2021/Q2/Q2aTester.java
        
        //function below runs if questionName is specified and not null
        //get a list of java files based on its suffix. recursive
        // remove from the list of java files any other question or tester files that don't match the question
        if (questionName != null){
            javaFiles.removeIf(file -> {
                String name = file.getName();
                return !name.equals(questionName + ".java")
                        && !name.equals(questionName + "Tester.java")
                        && (name.endsWith("Tester.java") || name.matches("Q\\d+[a-zA-Z]?\\.java"));
            });
        }

        if (javaFiles.isEmpty()) {
            return new CompilationResult(false, "No .java files found in directory " + sourceDir.getPath());
        }

        List<String> command = new ArrayList<>();
        command.add("javac");
        command.add("-cp");
        command.add(outputDir.getAbsolutePath() + File.pathSeparator + sourceDir.getAbsolutePath());
        command.add("-d");
        command.add(outputDir.getAbsolutePath());

        for (File f : javaFiles) {
            command.add(f.getAbsolutePath());
        }

        //command example: 
        // javac -cp "C:\project\temp\jing.lim.2021\classes;C:\project\temp\jing.lim.2021" -d "C:\project\temp\jing.lim.2021\classes" "C:\project\temp\jing.lim.2021\Q1\Q1a.java"...
        // javac -cp "C:\project\temp\jing.lim.2021\Q1\classes\Q1a;C:\project\temp\jing.lim.2021\Q1" -d "C:\project\temp\jing.lim.2021\Q1\classes\Q1a" "C:\project\temp\jing.lim.2021\Q1\Q1a.java"...

        ProcessResult r = ProcessRunner.run(command, 15);

        return new CompilationResult(r.isSuccess(), r.getOutput());
    }
}

// small example usage below. Will have to loop it in practice.

// Compile student code + tester

// Compiler compiler = new Compiler();
// CompilationResult result = compiler.compile(submission);
// if (!result.isSuccess()) {
//     System.out.println("Compilation failed: " + result.getOutput());
//     Give 0 marks and skip tests
// }