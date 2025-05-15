package com.dfavisualizer.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.dfavisualizer.ui.MainFrame;

/**
 * Dialog showing formal definitions and theory concepts for automata theory.
 */
public class TheoryPopupDialog {
    private JDialog dialog;
    private MainFrame mainFrame;
    
    /**
     * Constructor - initializes the dialog
     * 
     * @param mainFrame The main application frame
     */
    public TheoryPopupDialog(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setupDialog();
    }
    
    /**
     * Set up the dialog content
     */
    private void setupDialog() {
        try {
            // Create a dialog for theory content
            dialog = new JDialog(mainFrame.getFrame(), "Automata Theory Concepts", true);
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(mainFrame.getFrame());
            dialog.setLayout(new BorderLayout());
            
            // Create a tabbed pane for different theory topics
            JTabbedPane tabbedPane = new JTabbedPane();
            
            // Formal Definition of Regular Expressions
            JPanel regexPanel = createTheoryPanel("Regular Expressions", 
                "A formal language for pattern matching in strings",
                new String[] {
                    "• ε: The empty string",
                    "• a: A single character from the alphabet",
                    "• (r₁r₂): Concatenation of patterns r₁ and r₂",
                    "• (r₁|r₂): Alternation (matches either r₁ or r₂)",
                    "• r*: Kleene star (matches zero or more occurrences of r)",
                    "• r+: Matches one or more occurrences of r (equivalent to rr*)",
                    "• r?: Matches zero or one occurrence of r (optional)"
                });
            
            // Formal Definition of NFAs
            JPanel nfaPanel = createTheoryPanel("Nondeterministic Finite Automata (NFA)", 
                "A state machine that can be in multiple states simultaneously",
                new String[] {
                    "• A finite set of states Q",
                    "• An input alphabet Σ",
                    "• A transition function δ: Q × (Σ ∪ {ε}) → P(Q)",
                    "• An initial state q₀ ∈ Q",
                    "• A set of final states F ⊆ Q",
                    "",
                    "An NFA accepts a string if there exists any path from the start state to an accept state.",
                    "NFAs can have ε-transitions (moves without consuming input) and multiple paths for the same input."
                });
            
            // Formal Definition of DFAs
            JPanel dfaPanel = createTheoryPanel("Deterministic Finite Automata (DFA)", 
                "A state machine with exactly one active state and one transition per input",
                new String[] {
                    "• A finite set of states Q",
                    "• An input alphabet Σ",
                    "• A transition function δ: Q × Σ → Q",
                    "• An initial state q₀ ∈ Q",
                    "• A set of final states F ⊆ Q",
                    "",
                    "A DFA accepts a string if following the transitions from the start state leads to an accept state.",
                    "DFAs have exactly one transition for each state and input symbol, with no ε-transitions."
                });
            
            // Equivalence and Closure Properties
            JPanel propertiesPanel = createTheoryPanel("Equivalence and Properties", 
                "Relationships between regular expressions, NFAs, and DFAs",
                new String[] {
                    "• Regular expressions, NFAs, and DFAs are all equivalent in expressive power",
                    "• Every NFA can be converted to a DFA (subset construction)",
                    "• Every DFA can be minimized to a unique minimal form",
                    "• Regular languages are closed under union, concatenation, and star operations",
                    "• Regular languages are closed under complement and intersection",
                    "• The pumping lemma can be used to prove a language is not regular"
                });
            
            // Add theory panels to the tabbed pane
            tabbedPane.addTab("Regular Expressions", regexPanel);
            tabbedPane.addTab("NFAs", nfaPanel);
            tabbedPane.addTab("DFAs", dfaPanel);
            tabbedPane.addTab("Properties", propertiesPanel);
            
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
                "Error showing theory popup: " + ex.getMessage(), 
                "Display Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Create a panel for theory content
     * 
     * @param title The theory topic title
     * @param description A brief description
     * @param points Array of theory points to display
     * @return A JPanel configured for the theory content
     */
    private JPanel createTheoryPanel(String title, String description, String[] points) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create header with title and description
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("<html><h2>" + title + "</h2></html>");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel descLabel = new JLabel("<html><p>" + description + "</p></html>");
        headerPanel.add(descLabel, BorderLayout.CENTER);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Create content panel with the formal definition points
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        StringBuilder htmlContent = new StringBuilder("<html><div style='margin: 10px;'>");
        for (String point : points) {
            if (point.isEmpty()) {
                htmlContent.append("<br>");
            } else {
                htmlContent.append("<p>").append(point).append("</p>");
            }
        }
        htmlContent.append("</div></html>");
        
        JLabel contentLabel = new JLabel(htmlContent.toString());
        contentPanel.add(contentLabel, BorderLayout.CENTER);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Show the dialog
     */
    public void display() {
        dialog.setVisible(true);
    }
} 