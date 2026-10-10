package autograder.execution;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import model.Submission;

public class TestInjector {
    private final File testerDir; //this is Tester-Files
    

    public TestInjector(File testerDir) {
        this.testerDir = testerDir;
    }

    public boolean injectTesters(String testerFileName, Submission submission) {
        if (submission == null || submission.getFolderPath() == null) {
            System.out.println("[TestInjector] Invalid Submission or null folder path.");
            return false;
        }
        File targetDir = new File(submission.getFolderPath());
        return injectTesters(testerFileName, targetDir);
    }

    public boolean injectTesters(String testerFileName, File targetDir) {
        //testerFileName is (Q1aTester.java, Q1bTester.java)
        //tagetDir is student's question folder (e.g. temp/jiagu.wen.2025/Q1, temp/jiagu.wen.2025/Q2)
        

        File sFile = new File(testerDir, testerFileName);
        //sourceFile then becomes like Tester-Files/Q1aTester.java

        if (!sFile.exists()) {
            System.out.println("[TestInjector] Source tester not found: " + sFile.getAbsolutePath());
            return false;
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        File dFile = new File(targetDir, testerFileName);
        //for example temp/jiagu.wen.2025/Q1/Q1aTester.java

        try {
            Files.copy(sFile.toPath(), dFile.toPath(),StandardCopyOption.REPLACE_EXISTING);
            //Files.copy has 3 arguments, first is where to read from, second is where to write to, 
            // and third is what if file is already there

            return true;
        } catch (IOException e) {
            System.out.println("[TestInjector] IOException Error copying tester: " + e.getMessage());
            return false;
        }
        catch (Exception e) {
            System.out.println("[TestInjector] General Error copying tester: " + e.getMessage());
            return false;
        }

    }
    
}

// simple usage Inject official tester (overwrites student copy)

// TestInjector injector = new TestInjector(new File("Tester-Files"));
// boolean injected = injector.injectTesters("Q1aTester.java", submission);

