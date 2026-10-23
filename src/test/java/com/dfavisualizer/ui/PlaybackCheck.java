package com.dfavisualizer.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import com.dfavisualizer.controller.AnimationController;
import com.dfavisualizer.ui.components.InputPanel;
import com.dfavisualizer.model.DFA;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxConstants;

public class PlaybackCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        MainFrame[] holder = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> { holder[0] = new MainFrame(); holder[0].display(); });
        MainFrame app = holder[0];
        AnimationController player = app.getVisualizationController().getAnimationController();
        try {
            SwingUtilities.invokeAndWait(() -> {
                DFA dfa = fixture();
                AutomataGraph graph = load(app, dfa);
                mxCell start = graph.state("q0"), accept = graph.state("q1");
                String startStyle = start.getStyle(), acceptStyle = accept.getStyle();
                app.getFrame().validate();
                graph.getGraph().getView().setTranslate(new com.mxgraph.util.mxPoint(10000, 10000));
                startPaused(app, "ab");
                app.getFrame().validate();
                assert graph.getGraph().getGraphBounds().getRectangle().intersects(graph.getViewport().getViewRect())
                        : "Starting playback leaves the graph offscreen";
                button(app.getStatusPanel(), "Next Step").doClick(0);
                assert text(app).contains("Step 1") && text(app).contains("q1") : "Step does not consume exactly one symbol";
                assert ribbon(app).getText().contains("1 / 2") : "Input ribbon does not advance with the state";
                assert mxConstants.SHAPE_DOUBLE_ELLIPSE.equals(graph.getGraph().getCellStyle(accept).get(mxConstants.STYLE_SHAPE))
                        : "Highlighting destroys accepting-state notation";
                button(app.getStatusPanel(), "Next Step").doClick(0);
                assert text(app).contains("ACCEPTED") && ribbon(app).getText().contains("2 / 2") : "Self-loop did not consume the final symbol";
                button(app.getStatusPanel(), "Restart").doClick(0);
                button(app.getStatusPanel(), "Pause").doClick(0);
                assert ribbon(app).getText().contains("0 / 2") : "Restart does not reset input position";
                player.stopAnimation();
                assert startStyle.equals(start.getStyle()) && acceptStyle.equals(accept.getStyle()) : "Stopping loses original state styles";

                startPaused(app, "ac");
                button(app.getStatusPanel(), "Next Step").doClick(0);
                button(app.getStatusPanel(), "Next Step").doClick(0);
                assert text(app).contains("REJECTED") && text(app).contains("outside") : "Invalid suffix accepted in an accepting state";
                assert ribbon(app).getText().contains("1 / 2") : "Rejected symbol was counted as consumed";
                startPaused(app, "z");
                button(app.getStatusPanel(), "Next Step").doClick(0);
                assert text(app).contains("No transition") : "Missing transition not explained";
                startPaused(app, "x");
                button(app.getStatusPanel(), "Next Step").doClick(0);
                assert text(app).contains("not an accepting state") : "Non-accepting final state not explained";
                player.animateString("");
                assert text(app).contains("REJECTED") : "Empty input incorrectly accepted";
                dfa.addAcceptState(dfa.getStartState());
                load(app, dfa);
                player.animateString("");
                assert text(app).contains("ACCEPTED") && ribbon(app).getText().contains("0 / 0") : "Empty input fails at accepting start state";
                load(app, fixture());
                startPaused(app, " a&");
                for (int i = 0; i < 3; i++) button(app.getStatusPanel(), "Next Step").doClick(0);
                assert text(app).contains("ACCEPTED") && ribbon(app).getText().contains("3 / 3") : "Whitespace or literal symbols lost";

                DFA range = new DFA();
                DFA.State q0 = new DFA.State("q0"), q1 = new DFA.State("q1");
                range.setStartState(q0); range.addAcceptState(q1);
                for (char symbol = 'a'; symbol <= 'z'; symbol++) range.addTransition(q0, symbol, q1);
                load(app, range);
                startPaused(app, "m");
                button(app.getStatusPanel(), "Next Step").doClick(0);
                assert text(app).contains("ACCEPTED") : "Grouped range does not match the actual symbol";

                assert app.getVisualizationController().visualizeDfa("(a|b)*abb", true, true, true, true);
                assert !player.isAnimationRunning() : "Replacing a graph leaves the old timer running";
                inputPanel(app).getRegexField().setText("(a|b)*abb");
                speedSelector(app.getStatusPanel()).setSelectedIndex(2);
                inputPanel(app).getTestStringField().setText("aabb");
                button(inputPanel(app), "Animate").doClick(0);
                assert player.isAnimationRunning();
            });
            await(() -> text(app).contains("Step 1: read"));
            Thread.sleep(100);
            SwingUtilities.invokeAndWait(() -> {
                button(app.getStatusPanel(), "Pause").doClick(0);
                assert !player.isAnimationRunning() : "Pause leaves the clock running";
                saveImage(app);
            });
            Thread.sleep(100);
            SwingUtilities.invokeAndWait(() -> {
                assert ribbon(app).getText().contains("0 / 4") : "Paused playback consumed input";
                button(app.getStatusPanel(), "Resume").doClick(0);
            });
            await(() -> text(app).contains("ACCEPTED"));
            SwingUtilities.invokeAndWait(() -> {
                assert ribbon(app).getText().contains("4 / 4") && !player.isAnimationRunning();
                player.animateString("aabb");
                assert app.getVisualizationController().visualizeDfa("a*", true, true, true, true);
                assert !player.isAnimationRunning() && !ribbon(app).isVisible() : "Graph replacement did not clear playback";
            });
            Thread.sleep(550);
            SwingUtilities.invokeAndWait(() -> {
                assert !text(app).contains("Step ") : "An old timer changed the replacement graph's status";
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> { player.stopAnimation(); app.getFrame().dispose(); });
        }
        System.out.println("Playback checks passed (stepping, pause/resume, restart, loops, rejection, empty input, ranges, cancellation, autoplay)");
    }

    private static DFA fixture() {
        DFA dfa = new DFA();
        DFA.State q0 = new DFA.State("q0"), q1 = new DFA.State("q1"), q2 = new DFA.State("q2");
        dfa.setStartState(q0); dfa.addAcceptState(q1);
        dfa.addTransition(q0, 'a', q1); dfa.addTransition(q1, 'b', q1);
        dfa.addTransition(q0, ' ', q0); dfa.addTransition(q1, '&', q1);
        dfa.addTransition(q0, 'x', q2); dfa.addTransition(q2, 'y', q1); dfa.addSymbol('z');
        return dfa;
    }

    private static AutomataGraph load(MainFrame app, DFA dfa) {
        app.getVisualizationController().getAnimationController().stopAnimation();
        app.setCurrentDfa(dfa);
        AutomataGraph graph = (AutomataGraph) app.getDfaVisualizer().visualizeDfa(dfa);
        app.getDfaPanel().setVisualizationComponent(graph);
        return graph;
    }
    private static void startPaused(MainFrame app, String input) {
        inputPanel(app).getTestStringField().setText(input);
        button(inputPanel(app), "Animate").doClick(0);
        assert app.getVisualizationController().getAnimationController().isAnimationRunning();
        button(app.getStatusPanel(), "Pause").doClick(0);
    }
    private static InputPanel inputPanel(MainFrame app) {
        return (InputPanel) ((java.awt.BorderLayout) app.getFrame().getContentPane().getLayout()).getLayoutComponent(java.awt.BorderLayout.NORTH);
    }
    private static JComboBox<?> speedSelector(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JComboBox) return (JComboBox<?>) child;
            if (child instanceof Container) { JComboBox<?> found = speedSelector((Container) child); if (found != null) return found; }
        }
        return null;
    }
    private static String text(MainFrame app) { return app.getStatusPanel().getStatusArea().getText(); }
    private static JLabel ribbon(MainFrame app) { return (JLabel) named(app.getStatusPanel(), "playback.input"); }
    private static Component named(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container) { Component found = named((Container) child, name); if (found != null) return found; }
        }
        return null;
    }
    static AbstractButton button(Container parent, String text) {
        for (Component child : parent.getComponents()) {
            if (child instanceof AbstractButton && text.equals(((AbstractButton) child).getText())) return (AbstractButton) child;
            if (child instanceof Container) { AbstractButton found = button((Container) child, text); if (found != null) return found; }
        }
        return null;
    }
    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            boolean[] ready = new boolean[1];
            SwingUtilities.invokeAndWait(() -> ready[0] = condition.getAsBoolean());
            if (ready[0]) return;
            Thread.sleep(20);
        }
        throw new AssertionError("Playback did not reach the expected phase within five seconds");
    }
    private static void saveImage(MainFrame app) {
        BufferedImage image = new BufferedImage(app.getFrame().getWidth(), app.getFrame().getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); app.getFrame().paint(graphics); graphics.dispose();
        try { Files.createDirectories(Path.of("target/visual-checks")); ImageIO.write(image, "png", Path.of("target/visual-checks/playback.png").toFile()); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
