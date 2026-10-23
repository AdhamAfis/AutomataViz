package com.dfavisualizer.ui;

import javax.swing.SwingUtilities;
import com.dfavisualizer.model.DFA;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;

public class GraphDrawingCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        SwingUtilities.invokeAndWait(() -> {
            DFA dfa = new DFA();
            DFA.State start = new DFA.State("q0"), accept = new DFA.State("q1");
            dfa.setStartState(start);
            dfa.addAcceptState(accept);
            dfa.addTransition(start, 'a', accept);
            dfa.addTransition(start, 'b', accept);
            dfa.addTransition(accept, 'a', start);
            dfa.addTransition(accept, 'b', accept);
            AutomataGraph component = (AutomataGraph) new DfaVisualizer().visualizeDfa(dfa);
            mxGraph graph = component.getGraph();
            Object[] states = graph.getChildVertices(graph.getDefaultParent());
            assert states.length == 2 : "Self-loop badges create fake states";
            mxCell startCell = null, acceptCell = null;
            for (Object state : states) {
                mxCell cell = (mxCell) state;
                if ("q0".equals(cell.getValue())) startCell = cell;
                if ("q1".equals(cell.getValue())) acceptCell = cell;
            }
            assert mxConstants.SHAPE_DOUBLE_ELLIPSE.equals(graph.getCellStyle(acceptCell).get(mxConstants.STYLE_SHAPE))
                    : "Accepting states lack a double ring";
            int loops = 0, startArrows = 0;
            for (Object edge : graph.getChildEdges(graph.getDefaultParent())) {
                mxCell cell = (mxCell) edge;
                if (cell.getSource() == acceptCell && cell.getTarget() == acceptCell) loops++;
                if (cell.getSource() == null && cell.getTarget() == startCell) startArrows++;
            }
            assert loops == 1 : "Self-loop is not drawn as a real transition";
            assert startArrows == 1 : "Start state lacks an incoming start arrow";
            assert component.transition("q0", "q1", 'a') == component.transition("q0", "q1", 'b') : "Same destination symbols not grouped";
            assert component.transition("q0", "q1", 'c') == null : "Compact label changes symbol membership";
            java.util.Set<Character> symbols = new java.util.TreeSet<>();
            for (char symbol = 'a'; symbol <= 'z'; symbol++) symbols.add(symbol);
            for (char symbol = '0'; symbol <= '9'; symbol++) symbols.add(symbol);
            assert "[0–9, a–z]".equals(AutomataGraph.symbolsLabel(symbols));
            AutomataGraph.Transition label = (AutomataGraph.Transition) component.transition("q0", "q1", 'a').getValue();
            assert label.details().contains("'a'") && label.details().contains("'b'") && label.details().contains("2 symbols");
        });
        System.out.println("Graph drawing checks passed");
    }
}
