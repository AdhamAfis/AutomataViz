package com.dfavisualizer.controller;

import java.util.Map;
import java.util.Set;

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
    
    private javax.swing.SwingWorker<Prepared, String> generation;

    public boolean isGenerating() { return generation != null; }

    public void cancelGeneration() {
        if (generation == null) return;
        generation.cancel(true); generation = null;
        mainFrame.getInputPanel().setGenerating(false);
        mainFrame.getStatusPanel().setStatus("Generation cancelled. The previous graph is unchanged.");
    }

    public void requestVisualization(String regex, boolean minimized, boolean minimizeNfa, boolean dead, boolean split) {
        cancelGeneration();
        if (regex == null || regex.trim().isEmpty()) {
            mainFrame.getStatusPanel().setStatus("Enter an expression, for example (a|b)*abb. Use () for the empty string.");
            return;
        }
        mainFrame.cancelExpressionUpdate();
        animationController.stopAnimation();
        mainFrame.getInputPanel().setGenerating(true);
        mainFrame.getStatusPanel().setStatus("Preparing graphs… You can cancel or enter a replacement expression.");
        generation = new javax.swing.SwingWorker<Prepared, String>() {
            @Override protected Prepared doInBackground() {
                return prepare(regex, minimized, minimizeNfa, dead, this::publish);
            }
            @Override protected void process(java.util.List<String> messages) {
                if (this == generation) mainFrame.getStatusPanel().setStatus(messages.get(messages.size() - 1));
            }
            @Override protected void done() {
                if (this != generation) return;
                generation = null;
                mainFrame.getInputPanel().setGenerating(false);
                if (isCancelled() || !mainFrame.getFrame().isDisplayable()) return;
                try { apply(get(), regex, minimized, dead, mainFrame.getGraphControls().getCompareToggle().isSelected(),
                    mainFrame.getDfaVisualizer().isGridSnap()); }
                catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    mainFrame.getStatusPanel().setStatus("Could not generate: " + cause.getMessage()
                        + "\nThe previous graph is unchanged. Use Syntax help for supported expressions.");
                }
            }
        };
        generation.execute();
    }

    private static final class Prepared {
        DFA original, dfa;
        NFA nfa, minimizedNfa;
        com.dfavisualizer.ui.AutomataGraph.Builder nfaDrawing, dfaDrawing;
    }

    private Prepared prepare(String regex, boolean minimized, boolean minimizeNfa, boolean dead,
            java.util.function.Consumer<String> progress) {
        Prepared result = new Prepared();
        progress.accept("Parsing expression…");
        result.nfa = new RegexParser().parse(regex);
        progress.accept("Discovering DFA subsets…");
        result.original = new SubsetConstruction().convertNfaToDfa(result.nfa);
        progress.accept("Minimizing states…");
        result.dfa = minimized ? new DfaMinimizer().minimize(result.original) : result.original;
        result.minimizedNfa = minimizeNfa ? new NfaMinimizer().minimizeNfa(result.nfa) : null;
        progress.accept("Laying out graphs…");
        com.dfavisualizer.ui.DfaVisualizer dfaView = new com.dfavisualizer.ui.DfaVisualizer();
        dfaView.setHighlightDeadStates(dead);
        result.dfaDrawing = dfaView.prepareDfa(result.dfa);
        result.nfaDrawing = new com.dfavisualizer.ui.NfaVisualizer().prepareNfa(
            result.minimizedNfa == null ? result.nfa : result.minimizedNfa);
        return result;
    }

    private void apply(Prepared result, String regex, boolean minimized, boolean dead, boolean split, boolean snap) {
        com.dfavisualizer.ui.AutomataGraph nfaView = new com.dfavisualizer.ui.AutomataGraph(result.nfaDrawing, snap);
        com.dfavisualizer.ui.AutomataGraph dfaView = new com.dfavisualizer.ui.AutomataGraph(result.dfaDrawing, snap);
        mainFrame.setCurrentNfa(result.nfa); mainFrame.setMinimizedNfa(result.minimizedNfa);
        mainFrame.setNonMinimizedDfa(result.original); mainFrame.setCurrentDfa(result.dfa);
        mainFrame.getDfaVisualizer().setHighlightDeadStates(dead);
        mainFrame.getNfaPanel().setVisualizationComponent(nfaView);
        mainFrame.getDfaPanel().setVisualizationComponent(dfaView);
        mainFrame.updateVisualizationLayout(split);
        mainFrame.getGraphControls().showDfaVariant(minimized, true);
        mainFrame.showExpression(regex);
        mainFrame.getStatusPanel().setStatus("Ready: " + countDfaStats(result.dfa));
    }

    /** Synchronous entry point for programmatic use; UI actions use requestVisualization. */
    public boolean visualizeDfa(String regex, boolean minimized, boolean minimizeNfa, boolean dead, boolean split) {
        cancelGeneration(); mainFrame.cancelExpressionUpdate(); animationController.stopAnimation();
        try {
            apply(prepare(regex, minimized, minimizeNfa, dead, message -> { }), regex, minimized, dead, split,
                mainFrame.getDfaVisualizer().isGridSnap());
            return true;
        } catch (Exception ex) {
            mainFrame.getStatusPanel().setStatus("Could not generate: " + ex.getMessage());
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
        if (isGenerating()) return false;
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
        cancelGeneration();
        if (mainFrame.getNonMinimizedDfa() == null) return;
        mainFrame.setCurrentDfa(minimized ? mainFrame.getDfaMinimizer().minimize(mainFrame.getNonMinimizedDfa())
                : mainFrame.getNonMinimizedDfa());
        refreshDfaView();
        mainFrame.getStatusPanel().setStatus((minimized ? "Minimized DFA: " : "Original DFA: ") + countDfaStats(mainFrame.getCurrentDfa()));
    }

    public void refreshDfaView() {
        cancelGeneration();
        if (mainFrame.getCurrentDfa() == null) return;
        animationController.stopAnimation();
        mainFrame.getDfaPanel().setVisualizationComponent(mainFrame.getDfaVisualizer().visualizeDfa(mainFrame.getCurrentDfa()));
        mainFrame.getStatusPanel().setStatus("DFA view updated: " + countDfaStats(mainFrame.getCurrentDfa()));
    }

    public AnimationController getAnimationController() { return animationController; }
}
