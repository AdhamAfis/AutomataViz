package com.dfavisualizer.ui;

import javax.swing.SwingUtilities;
import com.dfavisualizer.controller.AnimationController;
import com.dfavisualizer.model.DFA;
import com.mxgraph.model.mxCell;

public class GuidedPlaybackCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        SwingUtilities.invokeAndWait(() -> {
            MainFrame app = new MainFrame();
            try {
                app.display();
                DFA dfa = new DFA();
                DFA.State a = new DFA.State("q0"), b = new DFA.State("q1"), c = new DFA.State("q2"), d = new DFA.State("q3");
                dfa.setStartState(a); dfa.addAcceptState(c);
                dfa.addTransition(a, 'a', b); dfa.addTransition(b, 'b', c); dfa.addTransition(c, 'c', d);
                app.setCurrentDfa(dfa);
                AutomataGraph graph = (AutomataGraph) app.getDfaVisualizer().visualizeDfa(dfa);
                app.getDfaPanel().setVisualizationComponent(graph);
                AnimationController player = app.getVisualizationController().getAnimationController();
                player.animateString("ab"); player.togglePause();
                assert app.getStatusPanel().getStatusArea().getText().contains("Next character") : "No decision preview";
                assert "18".equals(graph.getGraph().getCellStyle(graph.state("q3")).get("opacity").toString()) : "Unrelated state not faded";
                player.stepForward(); player.stepForward();
                assert app.getStatusPanel().getStatusArea().getText().contains("ACCEPTED");
                player.stepBack();
                assert app.getStatusPanel().getStatusArea().getText().contains("q1") : "Back did not restore previous state";
                player.stepForward();
                assert app.getStatusPanel().getStatusArea().getText().contains("ACCEPTED") : "Forward after Back changed result";
                player.restart(); player.togglePause(); player.stepForward();
                player.setFocusMode(false);
                assert "100".equals(graph.getGraph().getCellStyle(graph.state("q3")).get("opacity").toString());
                player.stopAnimation();
                for (Object state : graph.getGraph().getChildVertices(graph.getGraph().getDefaultParent()))
                    assert !((mxCell) state).getStyle().contains("opacity=") : "Focus leaked after stopping";
                player.animateString("");
                player.stepBack(); player.stepForward(); player.togglePause();
                assert app.getStatusPanel().getStatusArea().getText().contains("REJECTED") && !player.isAnimationRunning()
                        : "Empty input trace can restart an invalid read";
                player.animateString("ax"); player.togglePause(); player.stepForward(); player.stepForward();
                String failure = app.getStatusPanel().getStatusArea().getText();
                assert failure.contains("Read prefix 'a'") && failure.contains("Expected") && failure.contains("b") : failure;
                player.stopAnimation();
                dfa.addTransition(b, 'x', d);
                app.getDfaPanel().setVisualizationComponent(app.getDfaVisualizer().visualizeDfa(dfa));
                player.animateString("axb"); player.togglePause(); player.stepForward(); player.stepForward();
                String dead = app.getStatusPanel().getStatusArea().getText();
                assert dead.contains("dead state") && dead.contains("Read prefix 'a'") && dead.contains("b") : dead;
                assert !player.isAnimationRunning() : "Dead-state failure kept consuming input";
                player.stepBack();
                assert app.getStatusPanel().getStatusArea().getText().contains("q1") : "Cannot undo entry into dead state";
            } finally { app.getVisualizationController().getAnimationController().stopAnimation(); app.getFrame().dispose(); }
        });
        System.out.println("Guided playback checks passed");
    }
}
