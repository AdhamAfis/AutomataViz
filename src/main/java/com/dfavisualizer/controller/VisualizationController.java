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

public class VisualizationController {
    private MainFrame mainFrame;
    private AnimationController animationController;
    
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
        mainFrame.cancelExpressionUpdate();
        if (regex.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), "Please enter a regular expression.", 
                    "Empty Input", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        try {
            animationController.stopAnimation();
            mainFrame.getStatusPanel().setStatus("Converting regex to DFA...");
            
            mainFrame.getDfaVisualizer().setHighlightDeadStates(showDeadStates);
            
            DFA nonMinimizedDfa = mainFrame.getConverter().convertRegexToDfaWithoutMinimization(regex);
            mainFrame.setNonMinimizedDfa(nonMinimizedDfa);
            
            NFA currentNfa = mainFrame.getConverter().getLastNfa();
            mainFrame.setCurrentNfa(currentNfa);
            
            if (minimizeNfa && currentNfa != null) {
                NFA minimizedNfa = mainFrame.getNfaMinimizer().minimizeNfa(currentNfa);
                mainFrame.setMinimizedNfa(minimizedNfa);
                mainFrame.getStatusPanel().setStatus("NFA minimization successful. Original NFA: " + currentNfa.getStates().size() + 
                           " states, Minimized NFA: " + minimizedNfa.getStates().size() + " states.");
            } else {
                mainFrame.setMinimizedNfa(null);
            }
            
            if (minimizeDfa) {
                DFA currentDfa = mainFrame.getDfaMinimizer().minimize(nonMinimizedDfa);
                mainFrame.setCurrentDfa(currentDfa);
                mainFrame.getStatusPanel().setStatus("Conversion and minimization successful: " + countDfaStats(currentDfa));
            } else {
                mainFrame.setCurrentDfa(nonMinimizedDfa);
                mainFrame.getStatusPanel().setStatus("Conversion successful (no minimization): " + countDfaStats(nonMinimizedDfa));
            }
            
            // Keep the hidden NFA current so reopening split view compares the same regex.
            NFA nfaToDisplay = (minimizeNfa && mainFrame.getMinimizedNfa() != null) ? mainFrame.getMinimizedNfa() : mainFrame.getCurrentNfa();
            
            if (nfaToDisplay != null) {
                JComponent nfaVisualization = mainFrame.getNfaVisualizer().visualizeNfa(nfaToDisplay);
                
                mainFrame.getNfaPanel().setVisualizationComponent(nfaVisualization);
            }

            DFA dfaToDisplay = mainFrame.getCurrentDfa();
            if (dfaToDisplay != null) {
                JComponent dfaVisualization = mainFrame.getDfaVisualizer().visualizeDfa(dfaToDisplay);
                
                mainFrame.getDfaPanel().setVisualizationComponent(dfaVisualization);
            }
            
            mainFrame.updateVisualizationLayout(showSplitView);
            mainFrame.getGraphControls().showDfaVariant(minimizeDfa, true);
            mainFrame.showExpression(regex);
            
            return true;
        } catch (Exception ex) {
            mainFrame.getStatusPanel().setStatus("Error: " + ex.getMessage());
            JOptionPane.showMessageDialog(mainFrame.getFrame(), "Error parsing regex: " + ex.getMessage(), 
                    "Parsing Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    public void debugRegex(String regex) {
        animationController.stopAnimation();
        if (regex.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), "Please enter a regular expression.", 
                    "Empty Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            mainFrame.getStatusPanel().setStatus("Debugging regex: " + regex + "\n");
            
            RegexParser parser = new RegexParser();
            
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
            
            mainFrame.getStatusPanel().appendStatus("\nStep 3: Converting NFA to DFA using subset construction...");
            SubsetConstruction sc = new SubsetConstruction();
            DFA dfa = sc.convertNfaToDfa(nfa);
            
            mainFrame.getStatusPanel().appendStatus("\nStep 4: Minimizing DFA...");
            DfaMinimizer minimizer = new DfaMinimizer();
            DFA minimizedDfa = minimizer.minimize(dfa);
            
            mainFrame.getStatusPanel().appendStatus("\nFinal minimized DFA: " + countDfaStats(minimizedDfa));
            
            mainFrame.getStatusPanel().appendStatus("\nTransitions in minimized DFA:");
            for (Map.Entry<DFA.StateTransition, DFA.State> entry : minimizedDfa.getTransitions().entrySet()) {
                DFA.StateTransition transition = entry.getKey();
                DFA.State targetState = entry.getValue();
                mainFrame.getStatusPanel().appendStatus("  " + transition.getState() + " --[" + transition.getSymbol() + 
                                 "]--> " + targetState);
            }
            
            mainFrame.getStatusPanel().appendStatus("\nDebug complete. No errors detected in the conversion process.");
            
        } catch (Exception ex) {
            mainFrame.getStatusPanel().appendStatus("\nError during debug: " + ex.getMessage());
            StackTraceElement[] stackTrace = ex.getStackTrace();
            for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                mainFrame.getStatusPanel().appendStatus("  at " + stackTrace[i]);
            }
        }
    }
    
    private String countDfaStats(DFA dfa) {
        String stats = "States: " + dfa.getStates().size() + 
                       ", Accept States: " + dfa.getAcceptStates().size() + 
                       ", Transitions: " + dfa.getTransitions().size();
                       
        Set<DFA.State> deadStates = dfa.getDeadStates();
        if (!deadStates.isEmpty()) {
            stats += ", Dead States: " + deadStates.size();
        }
        
        return stats;
    }
    
    public boolean testString(String testString) {
        animationController.stopAnimation();
        DFA currentDfa = mainFrame.getCurrentDfa();
        if (currentDfa == null) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), 
                    "Please create a DFA first by entering a regex and clicking 'Visualize DFA'.", 
                    "No DFA Available", JOptionPane.WARNING_MESSAGE);
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
    
    public void animateString(String testString) {
        animationController.animateString(testString);
    }
    
    private boolean simulateDfa(DFA dfa, String input) {
        DFA.State currentState = dfa.getStartState();
        
        for (int i = 0; i < input.length(); i++) {
            char symbol = input.charAt(i);
            
            if (!dfa.getAlphabet().contains(symbol)) {
                return false;
            }
            
            DFA.State nextState = dfa.getTransitionTarget(currentState, symbol);
            
            if (nextState == null) {
                return false;
            }
            
            currentState = nextState;
        }
        
        return dfa.getAcceptStates().contains(currentState);
    }
    
    private String generateSimulationTrace(DFA dfa, String input) {
        StringBuilder trace = new StringBuilder("Simulation trace:\n");
        DFA.State currentState = dfa.getStartState();
        
        trace.append("Start state: ").append(currentState).append("\n");
        
        for (int i = 0; i < input.length(); i++) {
            char symbol = input.charAt(i);
            
            if (!dfa.getAlphabet().contains(symbol)) {
                trace.append("Symbol '")
                     .append(symbol)
                     .append("' is not in the alphabet of the regular expression.\n");
                trace.append("String rejected at position ").append(i);
                return trace.toString();
            }
            
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
    
    public void setDfaMinimized(boolean minimized) {
        if (mainFrame.getNonMinimizedDfa() == null) return;
        mainFrame.setCurrentDfa(minimized ? mainFrame.getDfaMinimizer().minimize(mainFrame.getNonMinimizedDfa())
                : mainFrame.getNonMinimizedDfa());
        refreshDfaView();
        mainFrame.getStatusPanel().setStatus((minimized ? "Minimized DFA: " : "Original DFA: ") + countDfaStats(mainFrame.getCurrentDfa()));
    }

    public void refreshDfaView() {
        if (mainFrame.getCurrentDfa() == null) return;
        animationController.stopAnimation();
        mainFrame.getDfaPanel().setVisualizationComponent(mainFrame.getDfaVisualizer().visualizeDfa(mainFrame.getCurrentDfa()));
        mainFrame.getStatusPanel().setStatus("DFA view updated: " + countDfaStats(mainFrame.getCurrentDfa()));
    }

    public AnimationController getAnimationController() { return animationController; }
}
