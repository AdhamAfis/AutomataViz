package com.dfavisualizer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    private JFrame frame;
    private JTextField regexField;
    private JPanel visualizationPanel;
    private RegexToDfaConverter converter;
    private DfaVisualizer visualizer;
    private JTextArea statusArea;

    public Main() {
        converter = new RegexToDfaConverter();
        visualizer = new DfaVisualizer();
        initializeUI();
    }

    private void initializeUI() {
        // Set up the main frame
        frame = new JFrame("Regex to DFA Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Input panel with regex field and buttons
        JPanel inputPanel = new JPanel(new BorderLayout());
        
        JPanel regexPanel = new JPanel(new BorderLayout());
        regexPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel regexLabel = new JLabel("Regular Expression: ");
        regexField = new JTextField();
        regexPanel.add(regexLabel, BorderLayout.WEST);
        regexPanel.add(regexField, BorderLayout.CENTER);
        
        // Panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton visualizeButton = new JButton("Visualize DFA");
        visualizeButton.addActionListener(this::visualizeDfa);
        buttonPanel.add(visualizeButton);
        
        JButton debugButton = new JButton("Debug");
        debugButton.addActionListener(this::debugRegex);
        buttonPanel.add(debugButton);
        
        regexPanel.add(buttonPanel, BorderLayout.EAST);
        
        inputPanel.add(regexPanel, BorderLayout.NORTH);
        
        // Info panel with supported syntax
        JTextArea syntaxInfo = new JTextArea(
            "Supported Syntax:\n" +
            "a, b, c... - Basic symbols\n" +
            "| - Alternation (OR)\n" +
            "* - Kleene star (zero or more)\n" +
            "+ - One or more\n" +
            "? - Zero or one\n" +
            ". - Any character\n" +
            "() - Grouping\n" +
            "Examples: a(b|c)*, (a|b)*abb"
        );
        syntaxInfo.setEditable(false);
        syntaxInfo.setBackground(new Color(240, 240, 240));
        syntaxInfo.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JScrollPane syntaxScroll = new JScrollPane(syntaxInfo);
        syntaxScroll.setPreferredSize(new Dimension(0, 100));
        inputPanel.add(syntaxScroll, BorderLayout.CENTER);
        
        frame.add(inputPanel, BorderLayout.NORTH);

        // Visualization panel
        visualizationPanel = new JPanel(new BorderLayout());
        visualizationPanel.setBorder(BorderFactory.createTitledBorder("DFA Visualization"));
        frame.add(visualizationPanel, BorderLayout.CENTER);

        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusArea = new JTextArea("Ready");
        statusArea.setEditable(false);
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setPreferredSize(new Dimension(0, 100));
        statusPanel.add(statusScroll, BorderLayout.CENTER);
        frame.add(statusPanel, BorderLayout.SOUTH);
    }

    private void visualizeDfa(ActionEvent e) {
        String regex = regexField.getText().trim();
        if (regex.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a regular expression.", 
                    "Empty Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            statusArea.setText("Converting regex to DFA...");
            DFA dfa = converter.convertRegexToDfa(regex);
            
            // Clear the visualization panel
            visualizationPanel.removeAll();
            
            // Add the new visualization
            JComponent visualization = visualizer.visualizeDfa(dfa);
            visualizationPanel.add(new JScrollPane(visualization), BorderLayout.CENTER);
            
            // Refresh the UI
            visualizationPanel.revalidate();
            visualizationPanel.repaint();
            
            statusArea.setText("Conversion successful: " + countDfaStats(dfa));
        } catch (Exception ex) {
            statusArea.setText("Error: " + ex.getMessage());
            JOptionPane.showMessageDialog(frame, "Error parsing regex: " + ex.getMessage(), 
                    "Parsing Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void debugRegex(ActionEvent e) {
        String regex = regexField.getText().trim();
        if (regex.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a regular expression.", 
                    "Empty Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            statusArea.setText("Debugging regex: " + regex + "\n");
            
            // Step 1: Create a regex parser
            RegexParser parser = new RegexParser();
            
            // Step 2: Parse the regex to NFA
            statusArea.append("Step 1: Parsing regex to NFA...\n");
            NFA nfa = parser.parse(regex);
            statusArea.append("NFA created: " + nfa.getStates().size() + " states, " + 
                             nfa.getAlphabet().size() + " symbols, " + 
                             nfa.getAcceptStates().size() + " accept states\n");
            
            statusArea.append("NFA Transitions:\n");
            for (Map.Entry<NFA.NFATransition, Set<Integer>> entry : nfa.getTransitions().entrySet()) {
                NFA.NFATransition transition = entry.getKey();
                Set<Integer> targets = entry.getValue();
                for (int target : targets) {
                    statusArea.append("  " + transition.getState() + " --[" + 
                                     (transition.getSymbol() == NFA.EPSILON ? "ε" : transition.getSymbol()) + 
                                     "]--> " + target + "\n");
                }
            }
            
            // Step 3: Convert NFA to DFA
            statusArea.append("\nStep 2: Converting NFA to DFA using subset construction...\n");
            SubsetConstruction sc = new SubsetConstruction();
            DFA dfa = sc.convertNfaToDfa(nfa);
            
            // Step 4: Minimize DFA
            statusArea.append("\nStep 3: Minimizing DFA...\n");
            DfaMinimizer minimizer = new DfaMinimizer();
            DFA minimizedDfa = minimizer.minimize(dfa);
            
            statusArea.append("\nFinal minimized DFA: " + countDfaStats(minimizedDfa) + "\n");
            
            // Step 5: Show all transitions
            statusArea.append("\nTransitions in minimized DFA:\n");
            for (Map.Entry<DFA.StateTransition, DFA.State> entry : minimizedDfa.getTransitions().entrySet()) {
                DFA.StateTransition transition = entry.getKey();
                DFA.State targetState = entry.getValue();
                statusArea.append("  " + transition.getState() + " --[" + transition.getSymbol() + 
                                 "]--> " + targetState + "\n");
            }
            
            // Success message
            statusArea.append("\nDebug complete. No errors detected in the conversion process.");
            
        } catch (Exception ex) {
            statusArea.append("\nError during debug: " + ex.getMessage() + "\n");
            StackTraceElement[] stackTrace = ex.getStackTrace();
            for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                statusArea.append("  at " + stackTrace[i] + "\n");
            }
        }
    }
    
    private String countDfaStats(DFA dfa) {
        return dfa.getStates().size() + " states, " + 
               dfa.getAlphabet().size() + " symbols, " + 
               dfa.getAcceptStates().size() + " accept states, " +
               dfa.getTransitions().size() + " transitions";
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static void main(String[] args) {
        // Set the look and feel to the system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        new Main().show();
    }
} 