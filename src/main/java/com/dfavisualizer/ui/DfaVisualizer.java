package com.dfavisualizer.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import com.dfavisualizer.model.DFA;

public class DfaVisualizer {
    private boolean highlightDeadStates = true;
    private boolean gridSnap;
    private AutomataGraph lastGraph;

    public void setHighlightDeadStates(boolean highlight) { highlightDeadStates = highlight; }
    public void setGridSnapEnabled(boolean enabled) { gridSnap = enabled; }
    public boolean isHighlightDeadStates() { return highlightDeadStates; }
    public boolean isGridSnap() { return gridSnap; }

    public JComponent visualizeDfa(DFA dfa) {
        AutomataGraph graph = new AutomataGraph(prepareDfa(dfa), gridSnap);
        lastGraph = graph;
        return graph;
    }

    public AutomataGraph.Builder prepareDfa(DFA dfa) {
        if (dfa == null || dfa.getStates().isEmpty()) throw new IllegalArgumentException("Cannot visualize empty DFA");
        AutomataGraph.Builder graph = new AutomataGraph.Builder(UiTheme.DFA_FILL);
        graph.dfa = dfa;
        graph.highlightDeadStates = highlightDeadStates;
        graph.graph.getModel().beginUpdate();
        try {
            List<DFA.State> states = new ArrayList<>(dfa.getStates());
            states.sort(Comparator.comparing(DFA.State::getName));
            Set<DFA.State> dead = dfa.getDeadStates();
            for (DFA.State state : states) graph.addState(state.getName(), state.equals(dfa.getStartState()),
                    dfa.getAcceptStates().contains(state), highlightDeadStates && dead.contains(state));
            List<Map.Entry<DFA.StateTransition, DFA.State>> transitions = new ArrayList<>(dfa.getTransitions().entrySet());
            transitions.sort(Comparator.comparing((Map.Entry<DFA.StateTransition, DFA.State> e) -> e.getKey().getState().getName())
                    .thenComparingInt(e -> e.getKey().getSymbol()).thenComparing(e -> e.getValue().getName()));
            for (Map.Entry<DFA.StateTransition, DFA.State> entry : transitions) graph.addTransition(
                    entry.getKey().getState().getName(), entry.getValue().getName(), entry.getKey().getSymbol(), false);
            graph.finish();
        } finally { graph.graph.getModel().endUpdate(); }
        return graph;
    }

    public void resetAllStyles() { if (lastGraph != null) lastGraph.restoreStyles(); }
}
