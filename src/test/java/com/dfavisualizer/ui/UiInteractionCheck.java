package com.dfavisualizer.ui;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import com.dfavisualizer.ui.components.InputPanel;
import com.mxgraph.swing.mxGraphComponent;

/** Desktop-only checks for the input and resizable results workspace. Run with -ea. */
public class UiInteractionCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        SwingUtilities.invokeAndWait(() -> {
            MainFrame app = new MainFrame();
            JFrame frame = app.getFrame();
            try {
                app.display();
                JSplitPane results = findResultsSplit(frame.getContentPane());
                assert results != null : "Results cannot be resized";
                frame.setSize(1000, 700);
                frame.validate();
                int original = results.getDividerLocation();
                results.setDividerLocation(original - 40);
                frame.validate();
                assert results.getDividerLocation() == original - 40 : "Results divider cannot move";
                InputPanel input = findInput(frame.getContentPane());
                input.getRegexField().setText("a(b|c)*");
                input.getRegexField().postActionEvent();
                frame.validate();
                assert app.getCurrentDfa() != null : "Enter in regex input does not visualize";
                assert app.getDfaPanel().getVisualizationComponent() instanceof mxGraphComponent;
                app.getGraphControls().getCompareToggle().doClick();
                frame.validate();
                assert app.getDfaPanel().isShowing() : "DFA hidden in single view";
                assert results.getDividerLocation() == original - 40 : "Switching views resets results height";
                assert app.getStatusPanel().isShowing() : "Results hidden after view change";
                Object previousNfaGraph = app.getNfaPanel().getVisualizationComponent();
                input.getRegexField().setText("ab");
                input.getRegexField().postActionEvent();
                app.getGraphControls().getCompareToggle().doClick();
                frame.validate();
                mxGraphComponent nfa = (mxGraphComponent) app.getNfaPanel().getVisualizationComponent();
                assert nfa != previousNfaGraph : "Switching to split view shows the previous regex's NFA";
                for (Object object : nfa.getGraph().getChildEdges(nfa.getGraph().getDefaultParent())) {
                    com.mxgraph.model.mxCell edge = (com.mxgraph.model.mxCell) object;
                    assert !edge.getValue().toString().contains("c") : "NFA retains an old regex symbol";
                }
                app.getStatusPanel().setStatus("Result " + "long input ".repeat(100));
                assert app.getStatusPanel().getStatusArea().getLineWrap() : "Long results require horizontal scrolling";
            } finally {
                frame.dispose();
            }
        });
        System.out.println("UI interaction checks passed");
    }

    private static JSplitPane findResultsSplit(Container parent) {
        if (parent instanceof JSplitPane && ((JSplitPane) parent).getOrientation() == JSplitPane.VERTICAL_SPLIT) {
            return (JSplitPane) parent;
        }
        for (Component child : parent.getComponents()) {
            if (child instanceof Container) {
                JSplitPane found = findResultsSplit((Container) child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static InputPanel findInput(Container parent) {
        if (parent instanceof InputPanel) return (InputPanel) parent;
        for (Component child : parent.getComponents()) {
            if (child instanceof Container) {
                InputPanel found = findInput((Container) child);
                if (found != null) return found;
            }
        }
        return null;
    }
}
