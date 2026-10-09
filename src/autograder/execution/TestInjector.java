package autograder.execution;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class TestInjector {
    private final File testerDir;
    

    public TestInjector(File testerDir) {
        this.testerDir = testerDir;
    }

    public boolean injectTester(String testerFileName, File targetDir) {
        File sourceFile = new File(testerDir, testerFileName);

        if (!sourceFile.exists()) {
            System.out.println("[TestInjector] Source tester not found: " + sourceFile.getAbsolutePath());
            return false;
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        File destinationFile = new File(targetDir, testerFileName);

        try {
            Files.copy(sourceFile.toPath(), destinationFile.toPath(),StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            // TODO: handle exception
            System.out.println("[TestInjector] Error copying tester: " + e.getMessage());
            return false;
        }

        

    }
    
}