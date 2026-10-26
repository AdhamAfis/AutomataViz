package com.dfavisualizer.ui;

import java.awt.*;
import java.util.function.BooleanSupplier;
import javax.swing.*;
import com.dfavisualizer.algorithm.ConstructionTrace;
import com.dfavisualizer.algorithm.RegexParser;
import com.dfavisualizer.ui.dialogs.ConstructionStepsDialog;
import com.dfavisualizer.ui.dialogs.StepByStepDialog;
import com.dfavisualizer.ui.components.NfaLessonPanel;

public class ConstructionLessonCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        SwingUtilities.invokeAndWait(() -> {
            MainFrame app = new MainFrame(); app.display();
            try {
                StepByStepDialog lesson = new StepByStepDialog(app, "(a|b)*abb", "aabb", true);
                JDialog dialog = (JDialog) app.getFrame().getOwnedWindows()[0];
                dialog.setModal(false); lesson.display();
                await(() -> named(dialog, "walkthrough.phase") instanceof JLabel
                    && ((JLabel) named(dialog, "walkthrough.phase")).getText().startsWith("Ready"));
                JTabbedPane tabs = LearningReliabilityCheck.find(dialog.getContentPane(), JTabbedPane.class);
                for (int stage = 1; stage <= 3; stage++) {
                    Container panel = (Container) tabs.getComponentAt(stage);
                    assert LearningReliabilityCheck.button(panel, "Construction steps") != null;
                }
                Container activity = named(dialog, "exercise.feedback").getParent().getParent().getParent();
                JComboBox<?>[] choices = combos(activity);
                for (int question = 0; question < choices[0].getItemCount(); question++) {
                    choices[0].setSelectedIndex(question);
                    String word = question == 3 ? "" : choices[0].getSelectedItem().toString();
                    boolean accepts = java.util.regex.Pattern.matches("(a|b)*abb", word);
                    choices[1].setSelectedIndex(accepts ? 1 : 2);
                    LearningReliabilityCheck.button(activity, "Check prediction").doClick(0);
                    assert ((JTextArea) named(dialog, "exercise.feedback")).getText().startsWith("Correct.") : "Incorrect exercise answer";
                }
                choices[0].setSelectedIndex(1); choices[1].setSelectedIndex(1);
                LearningReliabilityCheck.button(activity, "Check prediction").doClick(0);
                assert ((JTextArea) named(dialog, "exercise.feedback")).getText().contains("Rejected: the string ends in bba");
                LearningReliabilityCheck.button(activity, "Trace in NFA").doClick(0);
                assert tabs.getSelectedIndex() == 1;
                NfaLessonPanel nfa = LearningReliabilityCheck.find((Container) tabs.getComponentAt(1), NfaLessonPanel.class);
                assert LearningReliabilityCheck.find(nfa, JTextField.class).getText().equals("abba");
                dialog.dispose();

                ConstructionTrace history = new ConstructionTrace(); new RegexParser(history).parse("a(b|c)*");
                ConstructionStepsDialog trace = new ConstructionStepsDialog(app.getFrame(), "Thompson construction", history);
                trace.setModal(false); trace.setVisible(true);
                try {
                    await(() -> graphReady(trace));
                    AutomataGraph graph = LearningReliabilityCheck.find(trace.getContentPane(), AutomataGraph.class);
                    assert graph.getGraph().isCellsLocked() : "Snapshots are editable";
                    String first = ((JTextArea) named(trace, "construction.explanation")).getText();
                    assert !LearningReliabilityCheck.button(trace.getContentPane(), "Back").isEnabled();
                    LearningReliabilityCheck.button(trace.getContentPane(), "Next").doClick(0);
                    await(() -> graphReady(trace));
                    assert !((JTextArea) named(trace, "construction.explanation")).getText().equals(first);
                    LearningReliabilityCheck.button(trace.getContentPane(), "Back").doClick(0);
                    await(() -> graphReady(trace));
                    assert ((JTextArea) named(trace, "construction.explanation")).getText().equals(first);
                    trace.validate();
                    LearningReliabilityCheck.find(trace.getContentPane(), com.dfavisualizer.ui.components.VisualizationPanel.class).fitView();
                    AutomataGraph visible = LearningReliabilityCheck.find(trace.getContentPane(), AutomataGraph.class);
                    com.mxgraph.util.mxRectangle bounds = visible.getGraph().getGraphBounds();
                    assert bounds.getWidth() > 0 && bounds.getHeight() > 0 : "Snapshot graph is blank";
                    assert visible.getViewport().getViewRect().intersects(bounds.getRectangle()) : "Snapshot is offscreen";
                    java.awt.image.BufferedImage shot = new java.awt.image.BufferedImage(trace.getWidth(), trace.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = shot.createGraphics(); trace.paint(g); g.dispose();
                    new java.io.File("target/visual-checks").mkdirs();
                    try { javax.imageio.ImageIO.write(shot, "png", new java.io.File("target/visual-checks/construction-steps.png")); }
                    catch (java.io.IOException ex) { throw new RuntimeException(ex); }
                } finally { trace.dispose(); }
            } finally { for (Window w : app.getFrame().getOwnedWindows()) w.dispose(); app.getFrame().dispose(); }
        });
        System.out.println("Construction lesson checks passed (exercise feedback, simulator input, stage actions, read-only history, Back/Next)");
    }
    private static boolean graphReady(JDialog dialog) {
        AutomataGraph graph = LearningReliabilityCheck.find(dialog.getContentPane(), AutomataGraph.class);
        return graph != null && graph.getViewport().getExtentSize().width > 40;
    }
    private static JComboBox<?>[] combos(Container root) {
        java.util.List<JComboBox<?>> list = new java.util.ArrayList<>(); collect(root, list);
        return list.toArray(new JComboBox<?>[0]);
    }
    private static void collect(Container root, java.util.List<JComboBox<?>> list) {
        for (Component c : root.getComponents()) { if (c instanceof JComboBox) list.add((JComboBox<?>) c); if (c instanceof Container) collect((Container)c, list); }
    }
    private static Component named(Container root, String name) {
        for (Component c : root.getComponents()) { if (name.equals(c.getName())) return c; if (c instanceof Container) { Component found = named((Container)c, name); if (found != null) return found; } }
        return null;
    }
    private static void await(BooleanSupplier ready) {
        if (ready.getAsBoolean()) return;
        SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
        long deadline = System.nanoTime() + 30_000_000_000L;
        Timer timer = new Timer(10, e -> { if (ready.getAsBoolean() || System.nanoTime() > deadline) loop.exit(); });
        timer.start(); loop.enter(); timer.stop(); assert ready.getAsBoolean() : "Lesson did not become ready";
    }
}
