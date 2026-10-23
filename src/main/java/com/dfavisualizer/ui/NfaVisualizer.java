package com.dfavisualizer.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import com.dfavisualizer.model.NFA;

public class NfaVisualizer {
    private boolean gridSnap;
    public void setGridSnap(boolean enabled) { gridSnap = enabled; }
    public boolean isGridSnap() { return gridSnap; }

    public JComponent visualizeNfa(NFA nfa) {
        AutomataGraph graph = new AutomataGraph(prepareNfa(nfa), gridSnap);
        return graph;
    }

    public AutomataGraph.Builder prepareNfa(NFA nfa) {
        if (nfa == null || nfa.getStates().isEmpty()) throw new IllegalArgumentException("Cannot visualize empty NFA");
        AutomataGraph.Builder graph = new AutomataGraph.Builder(UiTheme.NFA_FILL);
        graph.nfa = nfa;
        graph.graph.getModel().beginUpdate();
        try {
            List<Integer> states = new ArrayList<>(nfa.getStates());
            Collections.sort(states);
            for (int state : states) graph.addState("q" + state, state == nfa.getStartState(), nfa.getAcceptStates().contains(state), false);
            List<Map.Entry<NFA.NFATransition, Set<Integer>>> transitions = new ArrayList<>(nfa.getTransitions().entrySet());
            transitions.sort(Comparator.comparingInt((Map.Entry<NFA.NFATransition, Set<Integer>> e) -> e.getKey().getState())
                    .thenComparingInt(e -> e.getKey().getSymbol()));
            for (Map.Entry<NFA.NFATransition, Set<Integer>> entry : transitions) {
                List<Integer> targets = new ArrayList<>(entry.getValue());
                Collections.sort(targets);
                for (int target : targets) graph.addTransition("q" + entry.getKey().getState(), "q" + target,
                        entry.getKey().getSymbol(), entry.getKey().getSymbol() == NFA.EPSILON);
            }
            graph.finish();
        } finally { graph.graph.getModel().endUpdate(); }
        return graph;
    }
}
