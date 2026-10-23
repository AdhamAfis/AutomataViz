package com.dfavisualizer.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.io.File;
import com.dfavisualizer.ui.components.NfaLessonPanel;
import com.dfavisualizer.ui.dialogs.StepByStepDialog;

public class LearningDialogCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        MainFrame[] app = {null}; JDialog[] dialog = {null};
        SwingUtilities.invokeAndWait(() -> { app[0] = new MainFrame(); app[0].display(); });
        try {
            open(app[0], dialog, "a(b|c)*", "ab");
            await(dialog[0], "Ready");
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = LearningReliabilityCheck.find(dialog[0].getContentPane(), JTabbedPane.class);
                assert !LearningReliabilityCheck.button(dialog[0].getContentPane(), "< Previous stage").isEnabled();
                tabs.setSelectedIndex(1);
                NfaLessonPanel lesson = LearningReliabilityCheck.find((Container) tabs.getSelectedComponent(), NfaLessonPanel.class);
                assert "ab".equals(LearningReliabilityCheck.find(lesson, JTextField.class).getText()) : "Test input was discarded";
                AbstractButton next = LearningReliabilityCheck.button(lesson, "Next:");
                for (int step = 0; next.isEnabled() && step < 20; step++) next.doClick(0);
                assert lesson.explanation().contains("ACCEPTED") : lesson.explanation();
                for (int stage : new int[] {2, 3}) {
                    tabs.setSelectedIndex(stage);
                    Container panel = (Container) tabs.getSelectedComponent();
                    JComboBox<?> states = LearningReliabilityCheck.find(panel, JComboBox.class);
                    assert states != null && states.getItemCount() > 0 : "No accessible state inspector";
                    for (int i = 0; i < states.getItemCount(); i++) {
                        states.setSelectedIndex(i);
                        String explanation = LearningReliabilityCheck.find(panel, JTextArea.class).getText();
                        assert explanation.startsWith(states.getSelectedItem().toString() + " ") : "State inspector is stale";
                    }
                }
                assert !LearningReliabilityCheck.button(dialog[0].getContentPane(), "Next stage").isEnabled();
                dialog[0].setSize(900, 650); dialog[0].validate();
            });
            SwingUtilities.invokeAndWait(() -> {
                try {
                    BufferedImage shot = new BufferedImage(dialog[0].getWidth(), dialog[0].getHeight(), BufferedImage.TYPE_INT_RGB);
                    dialog[0].paint(shot.getGraphics());
                    new File("target/visual-checks").mkdirs();
                    ImageIO.write(shot, "png", new File("target/visual-checks/learning-fixed.png"));
                } catch (Exception ex) { throw new RuntimeException(ex); }
                dialog[0].dispose();
            });
            open(app[0], dialog, "a{2}", "aa");
            await(dialog[0], "Could not");
            SwingUtilities.invokeAndWait(() -> {
                JTabbedPane tabs = LearningReliabilityCheck.find(dialog[0].getContentPane(), JTabbedPane.class);
                assert tabs.getSelectedIndex() == 0 && !tabs.isEnabledAt(1);
                assert !LearningReliabilityCheck.button(dialog[0].getContentPane(), "Next stage").isEnabled();
                dialog[0].dispose();
            });
        } finally { SwingUtilities.invokeAndWait(app[0].getFrame()::dispose); }
        System.out.println("Learning dialog checks passed (seed input, full trace, subset/merge inspectors, navigation, invalid expression)");
    }
    private static void open(MainFrame app, JDialog[] dialog, String expression, String input) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            StepByStepDialog steps = new StepByStepDialog(app, expression, input);
            for (Window owned : app.getFrame().getOwnedWindows()) if (owned instanceof JDialog) dialog[0] = (JDialog) owned;
            dialog[0].setModal(false); steps.display();
        });
    }
    private static void await(JDialog dialog, String prefix) throws Exception {
        long end = System.nanoTime() + 15_000_000_000L;
        while (System.nanoTime() < end) {
            String[] phase = {""};
            SwingUtilities.invokeAndWait(() -> phase[0] = phase(dialog.getContentPane()));
            if (phase[0].startsWith(prefix)) return;
            Thread.sleep(20);
        }
        throw new AssertionError("Lesson did not reach " + prefix);
    }
    private static String phase(Container parent) {
        for (Component c : parent.getComponents()) {
            if ("walkthrough.phase".equals(c.getName())) return ((JLabel)c).getText();
            if (c instanceof Container) { String text = phase((Container)c); if (!text.isEmpty()) return text; }
        }
        return "";
    }
}
