package com.dfavisualizer.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import com.dfavisualizer.algorithm.DfaMinimizer;
import com.dfavisualizer.algorithm.RegexParser;
import com.dfavisualizer.algorithm.SubsetConstruction;
import com.dfavisualizer.model.DFA;
import com.dfavisualizer.model.NFA;
import com.dfavisualizer.ui.MainFrame;

/**
 * Dialog showing a step-by-step visualization of the regex to DFA conversion process.
 */
public class StepByStepDialog {
    private JDialog dialog;
    private MainFrame mainFrame;
    private String regex;
    
    /**
     * Constructor - initializes the dialog
     * 
     * @param mainFrame The main application frame
     * @param regex The regular expression to convert
     */
    public StepByStepDialog(MainFrame mainFrame, String regex) {
        this.mainFrame = mainFrame;
        this.regex = regex;
        setupDialog();
    }
    
    /**
     * Set up the dialog content
     */
    private void setupDialog() {
        // Create a dialog to show the step-by-step process
        dialog = new JDialog(mainFrame.getFrame(), "Step-by-Step Conversion Process", true);
        dialog.setSize(900, 650);
        dialog.setLocationRelativeTo(mainFrame.getFrame());
        dialog.setLayout(new BorderLayout());
        
        try {
            // Create a tabbed pane to show different steps
            JTabbedPane tabbedPane = new JTabbedPane();
            
            // Step 1: Regex parsing and Thompson's construction
            JPanel step1Panel = createStepPanel("Step 1: Thompson's Construction", 
                "The regex is parsed and converted to an NFA using Thompson's construction algorithm.",
                "Each regex operator (concatenation, alternation, Kleene star) is converted to a small NFA fragment, " +
                "and these fragments are combined according to the structure of the regex.");
            
            // Step 2: Epsilon-NFA
            RegexParser parser = new RegexParser();
            NFA epsilonNfa = parser.parse(regex);
            JComponent epsilonNfaViz = mainFrame.getNfaVisualizer().visualizeNfa(epsilonNfa);
            enablePanAndZoom(epsilonNfaViz);
            
            JPanel step2Panel = createStepPanel("Step 2: Epsilon-NFA", 
                "The initial NFA includes epsilon transitions (shown in red).",
                "This NFA has " + epsilonNfa.getStates().size() + " states and uses epsilon transitions " +
                "to connect the different parts of the regex.");
            step2Panel.add(new JScrollPane(epsilonNfaViz), BorderLayout.CENTER);
            
            // Step 3: NFA to DFA (Subset Construction)
            SubsetConstruction sc = new SubsetConstruction();
            DFA nonMinimizedDfa = sc.convertNfaToDfa(epsilonNfa);
            JComponent nonMinDfaViz = mainFrame.getDfaVisualizer().visualizeDfa(nonMinimizedDfa);
            enablePanAndZoom(nonMinDfaViz);
            
            JPanel step3Panel = createStepPanel("Step 3: Subset Construction (NFA to DFA)", 
                "The NFA is converted to a DFA using the subset construction algorithm.",
                "Each state in the DFA corresponds to a set of NFA states. " +
                "The resulting DFA has " + nonMinimizedDfa.getStates().size() + " states and no epsilon transitions.");
            step3Panel.add(new JScrollPane(nonMinDfaViz), BorderLayout.CENTER);
            
            // Step 4: DFA Minimization (Hopcroft's Algorithm)
            DfaMinimizer minimizer = new DfaMinimizer();
            DFA minimizedDfa = minimizer.minimize(nonMinimizedDfa);
            JComponent minDfaViz = mainFrame.getDfaVisualizer().visualizeDfa(minimizedDfa);
            enablePanAndZoom(minDfaViz);
            
            JPanel step4Panel = createStepPanel("Step 4: DFA Minimization (Hopcroft's Algorithm)", 
                "The DFA is minimized by combining equivalent states.",
                "The minimized DFA has " + minimizedDfa.getStates().size() + " states, " +
                "reduced from " + nonMinimizedDfa.getStates().size() + " states in the non-minimized DFA.");
            step4Panel.add(new JScrollPane(minDfaViz), BorderLayout.CENTER);
            
            // Add all steps to the tabbed pane
            tabbedPane.addTab("Introduction", step1Panel);
            tabbedPane.addTab("Epsilon-NFA", step2Panel);
            tabbedPane.addTab("NFA to DFA", step3Panel);
            tabbedPane.addTab("DFA Minimization", step4Panel);
            
            // Add navigation buttons at the bottom
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            
            JButton prevButton = new JButton("< Previous Step");
            prevButton.addActionListener(e -> {
                int currentTab = tabbedPane.getSelectedIndex();
                if (currentTab > 0) {
                    tabbedPane.setSelectedIndex(currentTab - 1);
                }
            });
            
            JButton nextButton = new JButton("Next Step >");
            nextButton.addActionListener(e -> {
                int currentTab = tabbedPane.getSelectedIndex();
                if (currentTab < tabbedPane.getTabCount() - 1) {
                    tabbedPane.setSelectedIndex(currentTab + 1);
                }
            });
            
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> dialog.dispose());
            
            buttonPanel.add(prevButton);
            buttonPanel.add(nextButton);
            buttonPanel.add(closeButton);
            
            // Add components to dialog
            dialog.add(tabbedPane, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), 
                "Error creating step-by-step visualization: " + ex.getMessage(), 
                "Visualization Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Create a panel for a step in the conversion process
     * 
     * @param title The title of the step
     * @param description A short description
     * @param details Detailed explanation
     * @return A JPanel configured for the step
     */
    private JPanel createStepPanel(String title, String description, String details) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create header with title and description
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("<html><h2>" + title + "</h2></html>");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel descLabel = new JLabel("<html><p>" + description + "</p></html>");
        descLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        headerPanel.add(descLabel, BorderLayout.CENTER);
        
        JLabel detailsLabel = new JLabel("<html><p><i>" + details + "</i></p></html>");
        headerPanel.add(detailsLabel, BorderLayout.SOUTH);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        return panel;
    }
    
    /**
     * Enable pan and zoom for a component
     * 
     * @param component The component to enhance
     */
    private void enablePanAndZoom(JComponent component) {
        // This would be delegated to a utility class in a full implementation
        // For now, it's a placeholder
    }
    
    /**
     * Show the dialog
     */
    public void display() {
        dialog.setVisible(true);
    }
} 