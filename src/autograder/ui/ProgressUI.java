package autograder.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class ProgressUI {
    private static JFrame mainFrame;
    private static JPanel barPanel;
    private static JPanel sidePanel;
    private static JPanel innerSidePanel;
    private static JProgressBar bar;
    private static JLabel title;
    private static JLabel sideTextZip;
    private static JLabel sideTextCopy;
    private static JLabel sideTextEvaluate;
    private static JLabel sideTextPlagiarism;
    private static JLabel sideTextReport;
    private static JLabel progressText;
    private static JLabel errorText;
    private static GridBagConstraints gbc;

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
        innerSidePanel = new JPanel(new GridLayout(5, 1, 0, 15));
        
        innerSidePanel.setBackground(Color.white);
        sideTextZip = new JLabel("Unzipping files");
        sideTextZip.setForeground(Color.LIGHT_GRAY);

        sideTextCopy = new JLabel("Copying Test files");
        sideTextCopy.setForeground(Color.LIGHT_GRAY);

        sideTextEvaluate = new JLabel("Marking submissions");
        sideTextEvaluate.setForeground(Color.LIGHT_GRAY);

        sideTextPlagiarism = new JLabel("Plagiarism check");
        sideTextPlagiarism.setForeground(Color.LIGHT_GRAY);

        sideTextReport = new JLabel("Generate report");
        sideTextReport.setForeground(Color.LIGHT_GRAY);

        innerSidePanel.add(sideTextZip);
        innerSidePanel.add(sideTextCopy);
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
        errorText = new JLabel();
        errorText.setForeground(Color.RED);
        errorText.setFont(new Font("Arial", 0, 15));
        barPanel.add(errorText, gbc);


        gbc.gridy = 1;
        bar = new JProgressBar();
        bar.setForeground(Color.GREEN);
        bar.setStringPainted(true);
        bar.setPreferredSize(new Dimension(500, 30));

        barPanel.add(bar, gbc);

        gbc.gridy = 2;
        progressText = new JLabel("Starting...");
        progressText.setFont(new Font("Arial", 0, 12));

        barPanel.add(progressText, gbc);
        
        mainFrame.add(title, BorderLayout.NORTH);
        mainFrame.add(barPanel, BorderLayout.CENTER);
        mainFrame.add(sidePanel, BorderLayout.WEST);
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
    }

    public static void clearErrorText(){
        errorText.setText(null);
    }

    public static void onUnzip(int numOfZipFiles){
        progressText.setText("Unzipping files...");
        bar.setMaximum(numOfZipFiles);
    }

    public static void onUnzipSuccess(){
        sideTextZip.setForeground(Color.GREEN);
    }

    public static void onCopyTestFiles(int numOfFiles){
        progressText.setText("Copying Tester files...");
        bar.setMaximum(numOfFiles);
    }

    public static void onCopyTestFilesSuccess(){
        progressText.setText("Copying Tester files completed");
        sideTextCopy.setForeground(Color.GREEN);
    }

    public static void onEvaluate(int numOfFiles){
        progressText.setText("Marking student submissions...");
        bar.setMaximum(numOfFiles);
    }

    public static void onEvaluateSuccess(){
        progressText.setText("Marking submissions completed");
        sideTextEvaluate.setForeground(Color.GREEN);
    }

    public static void onPlagiarismCheck(int numOfFiles){
        progressText.setText("Plagiarism check...");
        bar.setMaximum(numOfFiles);
    }

    public static void onPlagiarismSuccess(){
        progressText.setText("Plagiarism check completed");
        sideTextPlagiarism.setForeground(Color.GREEN);
    }

    public static void onGenerateReport(){
        progressText.setText("Generating report...");
        bar.setMaximum(100);
    }

    public static void onGenrateReportSuccess(){
        completeBar();
        progressText.setText("Generating report completed");
        sideTextReport.setForeground(Color.GREEN);
    }

    public static void onProgramCompleted(){
        progressText.setText("Program completed. Please refer to Report and CSV for more details.");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void onProgramError(){
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
