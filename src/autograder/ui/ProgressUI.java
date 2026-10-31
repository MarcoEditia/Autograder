package autograder.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class ProgressUI {
    private static JFrame mainFrame;
    private static JPanel barPanel, sidePanel, innerSidePanel, openFilePanel;
    private static JProgressBar bar;
    private static JLabel title, sideTextZip, sideTextCopy, sideTextComplie, sideTextEvaluate, 
    sideTextPlagiarism, sideTextReport, studentCounterText, progressText, questionText;
    private static JTextArea errorText;
    private static GridBagConstraints gbc;
    private static JButton openReport, openCSV;
    private static int totalStudents = 0;
    private static int studentCounter = 1;
    private static String reportsFolder, fullReportFilePath, fullCsvFilePath;

    public static void main(String[] args) {
        // main method for testing only
        setupUI();
        setFrameVisibility(true);
    }

    public static void setupUI(){
        mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.setTitle("AutoGrader");
        mainFrame.setSize(700, 500);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setResizable(false);
        mainFrame.setLayout(new BorderLayout(10, 0));

        title = new JLabel("AutoGrader", SwingConstants.CENTER);
        title.setFont(new Font("Arial", 0, 30));

        sidePanel = new JPanel(new BorderLayout());
        innerSidePanel = new JPanel(new GridLayout(6, 1, 0, 15));
        
        innerSidePanel.setBackground(Color.white);
        sideTextZip = new JLabel("Unzipping files");
        sideTextZip.setForeground(Color.LIGHT_GRAY);

        sideTextCopy = new JLabel("Copying Test files");
        sideTextCopy.setForeground(Color.LIGHT_GRAY);

        sideTextComplie = new JLabel("Compiling Files");
        sideTextComplie.setForeground(Color.LIGHT_GRAY);

        sideTextEvaluate = new JLabel("Marking submissions");
        sideTextEvaluate.setForeground(Color.LIGHT_GRAY);

        sideTextPlagiarism = new JLabel("Plagiarism check");
        sideTextPlagiarism.setForeground(Color.LIGHT_GRAY);

        sideTextReport = new JLabel("Generate report");
        sideTextReport.setForeground(Color.LIGHT_GRAY);

        innerSidePanel.add(sideTextZip);
        innerSidePanel.add(sideTextCopy);
        innerSidePanel.add(sideTextComplie);
        innerSidePanel.add(sideTextEvaluate);
        innerSidePanel.add(sideTextPlagiarism);
        innerSidePanel.add(sideTextReport);

        sidePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        sidePanel.add(innerSidePanel, BorderLayout.NORTH);

        barPanel = new JPanel();
        barPanel.setLayout(new GridBagLayout());
        barPanel.setBackground(Color.LIGHT_GRAY);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        errorText = new JTextArea();
        errorText.setAlignmentX(0.5f);
        errorText.setOpaque(false);
        errorText.setLineWrap(true);
        errorText.setEditable(false);
        errorText.setWrapStyleWord(true);
        errorText.setForeground(Color.RED);
        errorText.setFont(new Font("Arial", 0, 15));
        barPanel.add(errorText, gbc);


        gbc.gridy = 1;
        bar = new JProgressBar();
        bar.setForeground(Color.GREEN);
        bar.setStringPainted(true);
        bar.setPreferredSize(new Dimension(500, 30));

        barPanel.add(bar, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;

        gbc.gridy = 2;
        studentCounterText = new JLabel();
        studentCounterText.setFont(new Font("Arial", 0, 14));

        barPanel.add(studentCounterText, gbc);

        gbc.gridy = 3;
        progressText = new JLabel("Starting...");
        progressText.setFont(new Font("Arial", 0, 12));

        barPanel.add(progressText, gbc);

        gbc.gridy = 4;
        questionText = new JLabel("Do you want to open HTML report or CSV?");
        questionText.setVisible(false);

        barPanel.add(questionText, gbc);

        gbc.gridy = 5;

        openFilePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 0));
        openFilePanel.setOpaque(false);
        openReport = new JButton("Open report");
        openReport.addActionListener(e -> handleOpenFile(true));
        openCSV = new JButton("Open CSV");
        openCSV.addActionListener(e -> handleOpenFile(false));
        openFilePanel.add(openReport);
        openFilePanel.add(openCSV);
        openFilePanel.setVisible(false);

        barPanel.add(openFilePanel, gbc);
        
        mainFrame.add(title, BorderLayout.NORTH);
        mainFrame.add(barPanel, BorderLayout.CENTER);
        mainFrame.add(sidePanel, BorderLayout.WEST);
    }

    private static void handleOpenFile(boolean openReport){
        File folder = new File(reportsFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            setErrorText("Unable to locate reports folder");
            return;
        }

        try{
            File file;
            if(openReport){
                file = new File(fullReportFilePath);
            }else{
                file = new File(fullCsvFilePath);
            }
            if(!file.exists()){
                setErrorText("File not found at " + file.getPath());
                return;
            }

            if(Desktop.isDesktopSupported()){
                Desktop desktop = Desktop.getDesktop();
                desktop.open(file);
            }else{
                setErrorText("Desktop operations are not supported in this OS. Please open manually");
            }
        }catch(Exception exception){
            errorText.setText("Error opening file: " + exception.getMessage());
        }
    }

    public static void setFrameVisibility(boolean visibility){
        mainFrame.setVisible(visibility);
    }

    public static void addBarValue(){
        if(bar.getValue() < bar.getMaximum()){
            bar.setValue(bar.getValue() + 1);
        }
    }

    public static void resetBar(){
        bar.setValue(0);
    }

    public static void completeBar(){
        bar.setValue(bar.getMaximum());
    }

    public static void setErrorText(String errorMsg){
        errorText.setText(errorMsg);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void clearErrorText(){
        errorText.setText(null);
    }

    // outside of the zip file for loop
    public static void onProgramStart(int numOfStudents){
        totalStudents = numOfStudents;
        bar.setMaximum(numOfStudents);
    }
    
    // in the zip file for loop, right before the student zip file get processed
    public static void onStudentStart(String zipFileName){
        sideTextZip.setForeground(Color.LIGHT_GRAY);
        sideTextCopy.setForeground(Color.LIGHT_GRAY);
        sideTextComplie.setForeground(Color.LIGHT_GRAY);
        sideTextEvaluate.setForeground(Color.LIGHT_GRAY);
        studentCounterText.setText("Student " + studentCounter + "/" + totalStudents + " (" + zipFileName + ")");
    }

    // call after ingestion so text can be changed to student ID
    public static void onStudentIdKnown(String studentId){
        studentCounterText.setText("Student " + studentCounter + "/" + totalStudents + " (" + studentId + ")");
    }

    public static void onUnzip(){
        progressText.setText("Unzipping file...");
    }

    public static void onUnzipSuccess(){
        sideTextZip.setForeground(Color.GREEN);
    }

    public static void onCopyTestFile(String testFileName){
        sideTextCopy.setForeground(Color.LIGHT_GRAY);
        sideTextComplie.setForeground(Color.LIGHT_GRAY);
        sideTextEvaluate.setForeground(Color.LIGHT_GRAY);
        progressText.setText("Copying Tester file (" + testFileName + ")");
    }

    public static void onCopyTestFileSuccess(){
        sideTextCopy.setForeground(Color.GREEN);
    }

    public static void onComplieFile(String fileName){
        progressText.setText("Compiling and running file (" + fileName + ")");
    }

    public static void onComplieFileSuccess(){
        sideTextComplie.setForeground(Color.GREEN);
    }

    public static void onEvaluate(String questionName){
        progressText.setText("Marking " + questionName);
    }

    public static void onEvaluateSuccess(){
        sideTextEvaluate.setForeground(Color.GREEN);
    }

    // call at the end of zip loop before processing next student
    public static void onStudentDone(){
        addBarValue();
        studentCounter ++;
    }

    // call before plagiarism check, call add barValue after each pair compare
    public static void onPlagiarismCheck(int numOfComparisons){
        resetBar();
        progressText.setText("Plagiarism check...");
        studentCounterText.setText(null);
        bar.setMaximum(numOfComparisons);
    }

    public static void onPlagiarismSuccess(){
        progressText.setText("Plagiarism check completed");
        sideTextPlagiarism.setForeground(Color.GREEN);
    }

    public static void onGenerateReport(){
        resetBar();
        progressText.setText("Generating report...");
        bar.setMaximum(100);
    }

    public static void onGenerateReportSuccess(String reportsFolder, String fullReportPath, String fullCsvPath){
        completeBar();
        progressText.setText("Generating report completed");
        sideTextReport.setForeground(Color.GREEN);
        questionText.setVisible(true);
        openFilePanel.setVisible(true);
        ProgressUI.reportsFolder = reportsFolder;
        ProgressUI.fullReportFilePath = fullReportPath;
        ProgressUI.fullCsvFilePath = fullCsvPath;
    }

    public static void onProgramCompleted(){
        progressText.setText("Program completed. Please refer to Report and CSV for more details.");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
