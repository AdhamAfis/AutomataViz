package com.dfavisualizer.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import com.dfavisualizer.ui.dialogs.StepByStepDialog;

/** Reproduces the pre-popup email freeze and checks UI responsiveness, completion, and closing. */
public class StepResponsivenessCheck {
    private static final String EMAIL = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+[.][a-zA-Z][a-zA-Z]+";
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        MainFrame[] app = new MainFrame[1]; JDialog[] dialog = new JDialog[1]; int[] beats = {0}; Timer[] timer = new Timer[1];
        SwingUtilities.invokeAndWait(() -> { app[0] = new MainFrame(); app[0].display(); });
        try {
            SwingUtilities.invokeAndWait(() -> {
                long started = System.nanoTime();
                StepByStepDialog lesson = new StepByStepDialog(app[0], EMAIL);
                assert (System.nanoTime() - started) < 1_000_000_000L : "Walkthrough constructor blocks before showing the popup";
                dialog[0] = latest(app[0]);
                assert named(dialog[0].getContentPane(), "walkthrough.phase") != null : "No responsive loading state";
                assert !find(dialog[0].getContentPane(), JTabbedPane.class).isEnabledAt(1) : "Blank learning tabs are selectable during preparation";
                assert named(dialog[0].getContentPane(), "walkthrough.expression") != null : "Lesson does not show its expression";
                dialog[0].setModal(false); lesson.display();
                assert dialog[0].isShowing() : "Loading popup did not appear";
                timer[0] = new Timer(10, e -> beats[0]++); timer[0].start();
            });
            long deadline = System.nanoTime() + 40_000_000_000L;
            boolean ready = false;
            while (System.nanoTime() < deadline) {
                String[] phase = {null};
                SwingUtilities.invokeAndWait(() -> phase[0] = ((JLabel) named(dialog[0].getContentPane(), "walkthrough.phase")).getText());
                if (phase[0].startsWith("Ready")) { ready = true; break; }
                assert !phase[0].startsWith("Could not") : phase[0];
                Thread.sleep(20);
            }
            assert ready : "Email walkthrough did not finish preparation";
            SwingUtilities.invokeAndWait(() -> {
                assert beats[0] > 2 : "UI event loop did not respond during preparation";
                JTabbedPane tabs = find(dialog[0].getContentPane(), JTabbedPane.class);
                assert tabs.getTabCount() == 4;
                tabs.setSelectedIndex(1);
                assert find((Container) tabs.getSelectedComponent(), com.dfavisualizer.ui.components.NfaLessonPanel.class) != null;
                dialog[0].dispose();
                StepByStepDialog cancelled = new StepByStepDialog(app[0], EMAIL);
                dialog[0] = latest(app[0]); dialog[0].setModal(false); cancelled.display(); dialog[0].dispose();
            });
            Thread.sleep(300);
            SwingUtilities.invokeAndWait(() -> { assert !dialog[0].isShowing() : "Closing preparation reopened the popup"; });
        } finally {
            SwingUtilities.invokeAndWait(() -> { if (timer[0] != null) timer[0].stop(); app[0].getFrame().dispose(); });
        }
        System.out.println("Step-by-step responsiveness checks passed (email, immediate popup, UI heartbeat, all stages, close during load)");
    }
    private static JDialog latest(MainFrame app) {
        JDialog result = null;
        for (Window window : app.getFrame().getOwnedWindows()) if (window instanceof JDialog && "Step-by-Step Conversion Process".equals(((JDialog) window).getTitle())) result = (JDialog) window;
        return result;
    }
    private static Component named(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container) { Component found = named((Container) child, name); if (found != null) return found; }
        }
        return null;
    }
    private static <T> T find(Container parent, Class<T> type) {
        if (type.isInstance(parent)) return type.cast(parent);
        for (Component child : parent.getComponents()) if (child instanceof Container) { T found = find((Container) child, type); if (found != null) return found; }
        return null;
    }
}
