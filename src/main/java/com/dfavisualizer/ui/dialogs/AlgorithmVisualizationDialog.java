package com.dfavisualizer.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.dfavisualizer.ui.MainFrame;

/**
 * Dialog showing visualizations of the automata theory algorithms.
 */
public class AlgorithmVisualizationDialog {
    private JDialog dialog;
    private MainFrame mainFrame;
    
    /**
     * Constructor - initializes the dialog
     * 
     * @param mainFrame The main application frame
     */
    public AlgorithmVisualizationDialog(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setupDialog();
    }
    
    /**
     * Set up the dialog content
     */
    private void setupDialog() {
        try {
            // Create a dialog for algorithm visualization
            dialog = new JDialog(mainFrame.getFrame(), "Algorithm Visualization", true);
            dialog.setSize(900, 650);
            dialog.setLocationRelativeTo(mainFrame.getFrame());
            dialog.setLayout(new BorderLayout());
            
            // Create a tabbed pane for different algorithms
            JTabbedPane tabbedPane = new JTabbedPane();
            
            // Thompson's Construction Visualization
            JPanel thompsonPanel = createAlgorithmPanel("Thompson's Construction", 
                "Visualizes how regular expressions are converted to NFAs",
                "Each regex component is converted to an NFA fragment with epsilon transitions.",
                new String[] {
                    "1. For a single character 'a', create start and end states connected by 'a'",
                    "2. For concatenation (ab), connect the accept state of first NFA to start state of second",
                    "3. For alternation (a|b), create new start/end states with epsilon transitions",
                    "4. For Kleene star (a*), create a loop with epsilon transitions",
                    "5. Recursively build the NFA by combining these patterns"
                });
            
            // Subset Construction Visualization
            JPanel subsetPanel = createAlgorithmPanel("Subset Construction", 
                "Visualizes how NFAs are converted to DFAs",
                "Creates DFA states from sets of NFA states, eliminating epsilon transitions.",
                new String[] {
                    "1. Start with the epsilon-closure of the NFA's start state",
                    "2. For each input symbol, compute transitions to new sets of states",
                    "3. Each set of NFA states becomes one DFA state",
                    "4. Continue until all reachable state sets are processed",
                    "5. A state is accepting if it contains any accepting NFA state"
                });
            
            // Hopcroft's Algorithm Visualization
            JPanel hopcroftPanel = createAlgorithmPanel("Hopcroft's Algorithm", 
                "Visualizes how DFAs are minimized",
                "Partitions states into equivalence classes to find minimum state DFA.",
                new String[] {
                    "1. Initially partition states into accepting and non-accepting sets",
                    "2. Refine partitions based on transitions to other partitions",
                    "3. For each input symbol and partition, check which states lead where",
                    "4. Split partitions when states have different transition behaviors",
                    "5. Continue until no more refinements are possible",
                    "6. Each final partition becomes one state in the minimized DFA"
                });
            
            // Add visualization panels to the tabbed pane
            tabbedPane.addTab("Thompson's Construction", thompsonPanel);
            tabbedPane.addTab("Subset Construction", subsetPanel);
            tabbedPane.addTab("Hopcroft's Algorithm", hopcroftPanel);
            
            // Add a close button
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> dialog.dispose());
            buttonPanel.add(closeButton);
            
            // Add components to dialog
            dialog.add(tabbedPane, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), 
                "Error creating algorithm visualization: " + ex.getMessage(), 
                "Visualization Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Create a panel for algorithm visualization
     * 
     * @param title The algorithm title
     * @param description A brief description
     * @param intro Introduction text
     * @param steps Array of steps in the algorithm
     * @return A JPanel configured for the algorithm visualization
     */
    private JPanel createAlgorithmPanel(String title, String description, String intro, String[] steps) {
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
        
        JLabel introLabel = new JLabel("<html><p><i>" + intro + "</i></p></html>");
        headerPanel.add(introLabel, BorderLayout.SOUTH);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Create steps panel
        JPanel stepsPanel = new JPanel(new GridLayout(steps.length, 1, 0, 5));
        stepsPanel.setBorder(BorderFactory.createTitledBorder("Algorithm Steps"));
        
        for (String step : steps) {
            JLabel stepLabel = new JLabel("<html><p>" + step + "</p></html>");
            stepLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            stepsPanel.add(stepLabel);
        }
        
        // Animation area (placeholder)
        JPanel animationPanel = new JPanel(new BorderLayout());
        animationPanel.setBorder(BorderFactory.createTitledBorder("Algorithm Animation"));
        JLabel animPlaceholder = new JLabel("<html><div style='text-align:center;'>Animation would be shown here.<br>Click Start to begin.</div></html>", JLabel.CENTER);
        animationPanel.add(animPlaceholder, BorderLayout.CENTER);
        
        JPanel animControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton startButton = new JButton("Start Animation");
        JButton pauseButton = new JButton("Pause");
        JButton resetButton = new JButton("Reset");
        
        // Disable pause button initially
        pauseButton.setEnabled(false);
        
        // Configure animation control buttons
        startButton.addActionListener(e -> {
            startButton.setEnabled(false);
            pauseButton.setEnabled(true);
            resetButton.setEnabled(true);
            animPlaceholder.setText("<html><div style='text-align:center;'>Animation in progress...</div></html>");
        });
        
        pauseButton.addActionListener(e -> {
            startButton.setEnabled(true);
            pauseButton.setEnabled(false);
            animPlaceholder.setText("<html><div style='text-align:center;'>Animation paused</div></html>");
        });
        
        resetButton.addActionListener(e -> {
            startButton.setEnabled(true);
            pauseButton.setEnabled(false);
            animPlaceholder.setText("<html><div style='text-align:center;'>Animation reset.<br>Click Start to begin.</div></html>");
        });
        
        animControlPanel.add(startButton);
        animControlPanel.add(pauseButton);
        animControlPanel.add(resetButton);
        
        animationPanel.add(animControlPanel, BorderLayout.SOUTH);
        
        // Create split pane with steps on left and animation on right
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, stepsPanel, animationPanel);
        splitPane.setResizeWeight(0.4);
        splitPane.setDividerLocation(300);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Show the dialog
     */
    public void display() {
        dialog.setVisible(true);
    }
} 