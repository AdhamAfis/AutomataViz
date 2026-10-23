package com.dfavisualizer.ui;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import com.dfavisualizer.ui.components.InputPanel;
import com.dfavisualizer.ui.components.GraphControlsPanel;
import com.dfavisualizer.model.NFA;

public class GraphControlsCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        SwingUtilities.invokeAndWait(() -> {
            MainFrame app = new MainFrame();
            try {
                app.display();
                InputPanel input = findInput(app.getFrame().getContentPane());
                assert !hasCheckbox(input) : "Input area still has graph preferences";
                GraphControlsPanel controls = app.getGraphControls();
                input.getRegexField().setText("(a|b)*abb"); input.getRegexField().postActionEvent();
                NFA nfa = app.getCurrentNfa();
                assert nfa.getTransitions().keySet().stream().anyMatch(t -> t.getSymbol() == NFA.EPSILON) : "Teaching NFA lost epsilon structure";
                Object nfaGraph = app.getNfaPanel().getVisualizationComponent();
                int originalStates = app.getNonMinimizedDfa().getStates().size();
                input.getRegexField().setText("unfinished(");
                controls.getDfaVariant().setSelectedIndex(0);
                assert app.getCurrentDfa() == app.getNonMinimizedDfa() : "Original variant rebuilt edited expression";
                assert app.getCurrentDfa().getStates().size() == originalStates;
                controls.getDfaVariant().setSelectedIndex(1);
                assert app.getCurrentDfa().getStates().size() <= originalStates;
                assert nfaGraph == app.getNfaPanel().getVisualizationComponent() : "DFA variant replaced NFA";
                controls.getCompareToggle().doClick(0); app.getFrame().validate();
                assert app.getDfaPanel().isShowing() && app.getNfaPanel().isShowing();
                controls.getGridSnapItem().doClick(0);
                for (com.dfavisualizer.ui.components.VisualizationPanel panel : new com.dfavisualizer.ui.components.VisualizationPanel[] {app.getDfaPanel(), app.getNfaPanel()}) {
                    AutomataGraph graph = (AutomataGraph) panel.getVisualizationComponent();
                    assert graph.isGridVisible() && graph.getGraph().isGridEnabled();
                }
                app.getVisualizationController().getAnimationController().animateString("aabb");
                controls.getHighlightDeadItem().doClick(0);
                assert !app.getVisualizationController().getAnimationController().isAnimationRunning() : "View replacement kept stale animation";
                assert app.getDfaVisualizer().isGridSnap() : "Refresh lost snap preference";
                app.getConverter().setUseOptimizedImplementation(false);
                input.getRegexField().setText("ab"); input.getRegexField().postActionEvent();
                assert controls.getCompareToggle().isSelected() && controls.getGridSnapItem().isSelected();
                assert ((AutomataGraph) app.getDfaPanel().getVisualizationComponent()).isGridVisible();
                assert !app.getCurrentDfa().getDeadStates().isEmpty();
                for (com.dfavisualizer.model.DFA.State dead : app.getCurrentDfa().getDeadStates()) {
                    AutomataGraph graph = (AutomataGraph) app.getDfaPanel().getVisualizationComponent();
                    assert !UiTheme.DEAD_FILL.equals(graph.getGraph().getCellStyle(graph.state(dead.getName())).get("fillColor"));
                }
                controls.getHighlightDeadItem().doClick(0);
                for (com.dfavisualizer.model.DFA.State dead : app.getCurrentDfa().getDeadStates()) {
                    AutomataGraph graph = (AutomataGraph) app.getDfaPanel().getVisualizationComponent();
                    assert UiTheme.DEAD_FILL.equals(graph.getGraph().getCellStyle(graph.state(dead.getName())).get("fillColor"));
                }
            } finally { app.getVisualizationController().getAnimationController().stopAnimation(); app.getFrame().dispose(); }
        });
        System.out.println("Graph control checks passed (placement, variants, original NFA, compare, preferences, cancellation)");
    }
    private static InputPanel findInput(Container parent) {
        if (parent instanceof InputPanel) return (InputPanel) parent;
        for (Component child : parent.getComponents()) if (child instanceof Container) {
            InputPanel found = findInput((Container) child); if (found != null) return found;
        }
        return null;
    }
    private static boolean hasCheckbox(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JCheckBox) return true;
            if (child instanceof Container && hasCheckbox((Container) child)) return true;
        }
        return false;
    }
}
