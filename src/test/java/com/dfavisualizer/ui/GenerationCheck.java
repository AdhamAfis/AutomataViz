package com.dfavisualizer.ui;

import javax.swing.*;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import com.dfavisualizer.controller.VisualizationController;
import com.dfavisualizer.model.DFA;

/** Checks UI-thread availability, atomic replacement, cancellation and latest-request ordering. */
public class GenerationCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        MainFrame[] app = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            app[0] = new MainFrame(); app[0].display();
            MainFrame frame = app[0]; VisualizationController controller = frame.getVisualizationController();
            try {
                assert controller.visualizeDfa("ab", true, false, true, false);
                DFA previous = frame.getCurrentDfa();
                int[] beats = {0}; Timer pulse = new Timer(5, e -> beats[0]++); pulse.start();
                try {
                    long started = System.nanoTime();
                    frame.getInputPanel().getRegexField().setText("(a|b)*abb");
                    frame.getInputPanel().getRegexField().postActionEvent();
                    assert System.nanoTime() - started < 1_000_000_000L : "Generation action blocks the UI";
                    assert controller.isGenerating() && frame.getCurrentDfa() == previous : "Partially installed result";
                    await(frame);
                    assert beats[0] > 0 : "No UI heartbeat during generation";
                    assert frame.getCurrentDfa().getStates().size() == 4;
                    previous = frame.getCurrentDfa();
                    controller.requestVisualization("a{2}", true, false, true, false); await(frame);
                    assert frame.getCurrentDfa() == previous : "Failed generation destroyed the graph";
                    assert frame.getStatusPanel().getStatusArea().getText().contains("Could not generate");
                    controller.requestVisualization("(a|b)*abb", true, false, true, false);
                    controller.cancelGeneration();
                    assert !controller.isGenerating() && frame.getCurrentDfa() == previous;
                    controller.requestVisualization("a*", true, false, true, false);
                    controller.requestVisualization("b*", true, false, true, false); await(frame);
                    assert frame.getCurrentDfa().getAlphabet().equals(java.util.Set.of('b')) : "Stale job replaced the latest request";
                    assert !Boolean.TRUE.equals(frame.getInputPanel().getClientProperty("generation.busy"));
                } finally { pulse.stop(); }
            } finally { frame.getFrame().dispose(); }
        });
        System.out.println("Generation checks passed (heartbeat, atomic replacement, failures, cancellation, latest request)");
    }

    /** Runs a nested event loop so assertions after a UI action can await background installation. */
    public static void await(MainFrame app) {
        assert SwingUtilities.isEventDispatchThread();
        if (!app.getVisualizationController().isGenerating()) return;
        SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
        long deadline = System.nanoTime() + 30_000_000_000L;
        Timer timer = new Timer(10, e -> {
            if (!app.getVisualizationController().isGenerating() || System.nanoTime() > deadline) loop.exit();
        });
        timer.start(); loop.enter(); timer.stop();
        assert !app.getVisualizationController().isGenerating() : "Generation did not finish";
    }
}
