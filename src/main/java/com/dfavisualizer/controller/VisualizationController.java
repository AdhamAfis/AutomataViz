package com.dfavisualizer.controller;

import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import com.dfavisualizer.algorithm.DfaMinimizer;
import com.dfavisualizer.algorithm.NfaMinimizer;
import com.dfavisualizer.algorithm.RegexParser;
import com.dfavisualizer.algorithm.SubsetConstruction;
import com.dfavisualizer.model.DFA;
import com.dfavisualizer.model.NFA;
import com.dfavisualizer.ui.MainFrame;

/**
 * Controller class that handles automata visualization operations.
 * Acts as an intermediary between the UI and the algorithm logic.
 */
public class VisualizationController {
    private MainFrame mainFrame;
    private AnimationController animationController;
    
    /**
     * Constructor - initializes the controller
     * 
     * @param mainFrame The main application frame
     */
    public VisualizationController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.animationController = new AnimationController(mainFrame);
    }
    
    /**
     * Visualize a DFA from a regex
     * 
     * @param regex The regular expression to convert
     * @param minimizeDfa Whether to minimize the DFA
     * @param minimizeNfa Whether to minimize the NFA
     * @param showDeadStates Whether to highlight dead states
     * @param showSplitView Whether to show both NFA and DFA
     * @return True if visualization was successful
     */
    public boolean visualizeDfa(String regex, boolean minimizeDfa, boolean minimizeNfa, boolean showDeadStates, boolean showSplitView) {
        if (regex.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), "Please enter a regular expression.", 
                    "Empty Input", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        try {
            mainFrame.getStatusPanel().setStatus("Converting regex to DFA...");
            
            // Configure the DFA visualizer to highlight dead states
            mainFrame.getDfaVisualizer().setHighlightDeadStates(showDeadStates);
            
            // First, convert regex to non-minimized DFA
            DFA nonMinimizedDfa = mainFrame.getConverter().convertRegexToDfaWithoutMinimization(regex);
            mainFrame.setNonMinimizedDfa(nonMinimizedDfa);
            
            // Get the original NFA from the converter
            NFA currentNfa = mainFrame.getConverter().getLastNfa();
            mainFrame.setCurrentNfa(currentNfa);
            
            // Apply NFA minimization if selected
            if (minimizeNfa && currentNfa != null) {
                NFA minimizedNfa = mainFrame.getNfaMinimizer().minimizeNfa(currentNfa);
                mainFrame.setMinimizedNfa(minimizedNfa);
                mainFrame.getStatusPanel().setStatus("NFA minimization successful. Original NFA: " + currentNfa.getStates().size() + 
                           " states, Minimized NFA: " + minimizedNfa.getStates().size() + " states.");
            } else {
                mainFrame.setMinimizedNfa(null);
            }
            
            // Apply DFA minimization if selected
            if (minimizeDfa) {
                DFA currentDfa = mainFrame.getDfaMinimizer().minimize(nonMinimizedDfa);
                mainFrame.setCurrentDfa(currentDfa);
                mainFrame.getStatusPanel().setStatus("Conversion and minimization successful: " + countDfaStats(currentDfa));
            } else {
                mainFrame.setCurrentDfa(nonMinimizedDfa);
                mainFrame.getStatusPanel().setStatus("Conversion successful (no minimization): " + countDfaStats(nonMinimizedDfa));
            }
            
            // Get the panels ready for visualization
            clearVisualizationPanels();
            
            // Visualize NFA if split view is enabled
            if (showSplitView) {
                // Determine which NFA to display - minimized or original
                NFA nfaToDisplay = (minimizeNfa && mainFrame.getMinimizedNfa() != null) ? mainFrame.getMinimizedNfa() : mainFrame.getCurrentNfa();
                
                if (nfaToDisplay != null) {
                    // Create visualization component
                    JComponent nfaVisualization = mainFrame.getNfaVisualizer().visualizeNfa(nfaToDisplay);
                    
                    // Set the visualization component on the panel
                    mainFrame.getNfaPanel().setVisualizationComponent(nfaVisualization);
                }
            }
            
            // Visualize DFA
            DFA dfaToDisplay = mainFrame.getCurrentDfa();
            if (dfaToDisplay != null) {
                // Create visualization component
                JComponent dfaVisualization = mainFrame.getDfaVisualizer().visualizeDfa(dfaToDisplay);
                
                // Set the visualization component on the panel
                mainFrame.getDfaPanel().setVisualizationComponent(dfaVisualization);
            }
            
            // Update the visualization layout
            mainFrame.updateVisualizationLayout(showSplitView);
            
            return true;
        } catch (Exception ex) {
            mainFrame.getStatusPanel().setStatus("Error: " + ex.getMessage());
            JOptionPane.showMessageDialog(mainFrame.getFrame(), "Error parsing regex: " + ex.getMessage(), 
                    "Parsing Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    /**
     * Debug a regex, showing the internal steps and states
     * 
     * @param regex The regular expression to debug
     */
    public void debugRegex(String regex) {
        if (regex.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), "Please enter a regular expression.", 
                    "Empty Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            mainFrame.getStatusPanel().setStatus("Debugging regex: " + regex + "\n");
            
            // Step 1: Create a regex parser
            RegexParser parser = new RegexParser();
            
            // Step 2: Parse the regex to NFA
            mainFrame.getStatusPanel().appendStatus("Step 1: Parsing regex to NFA...");
            NFA nfa = parser.parse(regex);
            mainFrame.getStatusPanel().appendStatus("NFA created: " + nfa.getStates().size() + " states, " + 
                             nfa.getAlphabet().size() + " symbols, " + 
                             nfa.getAcceptStates().size() + " accept states");
            
            mainFrame.getStatusPanel().appendStatus("NFA Transitions:");
            for (Map.Entry<NFA.NFATransition, Set<Integer>> entry : nfa.getTransitions().entrySet()) {
                NFA.NFATransition transition = entry.getKey();
                Set<Integer> targets = entry.getValue();
                for (int target : targets) {
                    mainFrame.getStatusPanel().appendStatus("  " + transition.getState() + " --[" + 
                                     (transition.getSymbol() == NFA.EPSILON ? "ε" : transition.getSymbol()) + 
                                     "]--> " + target);
                }
            }
            
            // Step 2.5: Minimize NFA
            mainFrame.getStatusPanel().appendStatus("\nStep 2: Minimizing NFA...");
            NfaMinimizer nfaMinimizer = new NfaMinimizer();
            NFA minimizedNfa = nfaMinimizer.minimizeNfa(nfa);
            mainFrame.getStatusPanel().appendStatus("Minimized NFA: " + minimizedNfa.getStates().size() + " states, " + 
                             minimizedNfa.getAlphabet().size() + " symbols, " + 
                             minimizedNfa.getAcceptStates().size() + " accept states");
            
            mainFrame.getStatusPanel().appendStatus("Minimized NFA Transitions:");
            for (Map.Entry<NFA.NFATransition, Set<Integer>> entry : minimizedNfa.getTransitions().entrySet()) {
                NFA.NFATransition transition = entry.getKey();
                Set<Integer> targets = entry.getValue();
                for (int target : targets) {
                    mainFrame.getStatusPanel().appendStatus("  " + transition.getState() + " --[" + 
                                     (transition.getSymbol() == NFA.EPSILON ? "ε" : transition.getSymbol()) + 
                                     "]--> " + target);
                }
            }
            
            // Step 3: Convert NFA to DFA
            mainFrame.getStatusPanel().appendStatus("\nStep 3: Converting NFA to DFA using subset construction...");
            SubsetConstruction sc = new SubsetConstruction();
            DFA dfa = sc.convertNfaToDfa(nfa);
            
            // Step 4: Minimize DFA
            mainFrame.getStatusPanel().appendStatus("\nStep 4: Minimizing DFA...");
            DfaMinimizer minimizer = new DfaMinimizer();
            DFA minimizedDfa = minimizer.minimize(dfa);
            
            mainFrame.getStatusPanel().appendStatus("\nFinal minimized DFA: " + countDfaStats(minimizedDfa));
            
            // Step 5: Show all transitions
            mainFrame.getStatusPanel().appendStatus("\nTransitions in minimized DFA:");
            for (Map.Entry<DFA.StateTransition, DFA.State> entry : minimizedDfa.getTransitions().entrySet()) {
                DFA.StateTransition transition = entry.getKey();
                DFA.State targetState = entry.getValue();
                mainFrame.getStatusPanel().appendStatus("  " + transition.getState() + " --[" + transition.getSymbol() + 
                                 "]--> " + targetState);
            }
            
            // Success message
            mainFrame.getStatusPanel().appendStatus("\nDebug complete. No errors detected in the conversion process.");
            
        } catch (Exception ex) {
            mainFrame.getStatusPanel().appendStatus("\nError during debug: " + ex.getMessage());
            StackTraceElement[] stackTrace = ex.getStackTrace();
            for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                mainFrame.getStatusPanel().appendStatus("  at " + stackTrace[i]);
            }
        }
    }
    
    /**
     * Generate statistics for a DFA
     * 
     * @param dfa The DFA to get stats for
     * @return A string containing stats about the DFA
     */
    private String countDfaStats(DFA dfa) {
        String stats = "States: " + dfa.getStates().size() + 
                       ", Accept States: " + dfa.getAcceptStates().size() + 
                       ", Transitions: " + dfa.getTransitions().size();
                       
        // Add information about dead states if any
        Set<DFA.State> deadStates = dfa.getDeadStates();
        if (!deadStates.isEmpty()) {
            stats += ", Dead States: " + deadStates.size();
        }
        
        return stats;
    }
    
    /**
     * Test if a string is accepted by the current DFA
     * 
     * @param testString The string to test
     * @return True if the string is accepted
     */
    public boolean testString(String testString) {
        DFA currentDfa = mainFrame.getCurrentDfa();
        if (currentDfa == null) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), 
                    "Please create a DFA first by entering a regex and clicking 'Visualize DFA'.", 
                    "No DFA Available", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        if (testString.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), "Please enter a string to test.", 
                    "Empty Test String", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        boolean accepted = simulateDfa(currentDfa, testString);
        String result = "String \"" + testString + "\": " + (accepted ? "ACCEPTED" : "REJECTED");
        
        mainFrame.getStatusPanel().setStatus(result + "\n" + generateSimulationTrace(currentDfa, testString));
        
        if (accepted) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), result, "Test Result", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), result, "Test Result", JOptionPane.WARNING_MESSAGE);
        }
        
        return accepted;
    }
    
    /**
     * Animate a string through the DFA
     * 
     * @param testString The string to animate
     */
    public void animateString(String testString) {
        // Use the animation controller to animate the string
        animationController.animateString(testString);
    }
    
    /**
     * Simulate a DFA on an input string
     * 
     * @param dfa The DFA to simulate
     * @param input The input string
     * @return True if the string is accepted
     */
    private boolean simulateDfa(DFA dfa, String input) {
        DFA.State currentState = dfa.getStartState();
        
        for (int i = 0; i < input.length(); i++) {
            char symbol = input.charAt(i);
            
            // Check if the symbol is in the alphabet
            if (!dfa.getAlphabet().contains(symbol)) {
                return false;
            }
            
            // Find transition for this symbol
            DFA.State nextState = dfa.getTransitionTarget(currentState, symbol);
            
            if (nextState == null) {
                // No valid transition found
                return false;
            }
            
            currentState = nextState;
        }
        
        // Check if we ended in an accept state
        return dfa.getAcceptStates().contains(currentState);
    }
    
    /**
     * Generate a trace of DFA execution for a string
     * 
     * @param dfa The DFA to trace
     * @param input The input string
     * @return A string containing the execution trace
     */
    private String generateSimulationTrace(DFA dfa, String input) {
        StringBuilder trace = new StringBuilder("Simulation trace:\n");
        DFA.State currentState = dfa.getStartState();
        
        trace.append("Start state: ").append(currentState).append("\n");
        
        for (int i = 0; i < input.length(); i++) {
            char symbol = input.charAt(i);
            
            // Check if the symbol is in the alphabet
            if (!dfa.getAlphabet().contains(symbol)) {
                trace.append("Symbol '")
                     .append(symbol)
                     .append("' is not in the alphabet of the regular expression.\n");
                trace.append("String rejected at position ").append(i);
                return trace.toString();
            }
            
            // Find transition for this symbol
            DFA.State nextState = dfa.getTransitionTarget(currentState, symbol);
            
            if (nextState == null) {
                trace.append("No transition found from state ")
                     .append(currentState)
                     .append(" on symbol '")
                     .append(symbol)
                     .append("'\n");
                trace.append("String rejected at position ").append(i);
                return trace.toString();
            }
            
            trace.append("Transition: ")
                 .append(currentState)
                 .append(" --[")
                 .append(symbol)
                 .append("]--> ")
                 .append(nextState)
                 .append("\n");
            
            currentState = nextState;
        }
        
        boolean isAcceptState = dfa.getAcceptStates().contains(currentState);
        trace.append("Final state: ")
             .append(currentState)
             .append(" (")
             .append(isAcceptState ? "accept" : "non-accept")
             .append(" state)");
        
        return trace.toString();
    }
    
    /**
     * Clear the visualization panels
     */
    private void clearVisualizationPanels() {
        mainFrame.getNfaPanel().removeAll();
        mainFrame.getDfaPanel().removeAll();
    }
    
    /**
     * Set whether loop animation is enabled for DFA visualization
     * 
     * @param enabled True to enable loop animation, false to disable
     */
    public void setLoopAnimationEnabled(boolean enabled) {
        if (animationController != null) {
            animationController.setLoopAnimationEnabled(enabled);
            mainFrame.getStatusPanel().setStatus("Loop animation " + (enabled ? "enabled" : "disabled"));
        }
    }
} 