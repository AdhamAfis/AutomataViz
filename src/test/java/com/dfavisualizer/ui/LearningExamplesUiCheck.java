package com.dfavisualizer.ui;

import java.util.prefs.Preferences;
import javax.swing.*;
import com.dfavisualizer.ui.components.GettingStartedPanel;
import com.dfavisualizer.ui.dialogs.ExamplesDialog;

public class LearningExamplesUiCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        Preferences isolated = Preferences.userRoot().node("/automataviz-tests/" + java.util.UUID.randomUUID());
        try {
            SwingUtilities.invokeAndWait(() -> {
                GettingStartedPanel first = new GettingStartedPanel(isolated);
                assert first.isVisible();
                LearningReliabilityCheck.button(first, "Dismiss").doClick(0);
                assert !first.isVisible() && !new GettingStartedPanel(isolated).isVisible() : "Dismissal was not remembered";
                first.showAgain();
                assert first.isVisible() && new GettingStartedPanel(isolated).isVisible() : "Help cannot restore the hint";
                MainFrame app = new MainFrame(); app.display();
                try {
                    ExamplesDialog dialog = new ExamplesDialog(app);
                    dialog.setModal(false); dialog.setVisible(true);
                    try {
                        JList<?> choices = LearningReliabilityCheck.find(dialog.getContentPane(), JList.class);
                        for (int i = 0; i < choices.getModel().getSize(); i++) {
                            choices.setSelectedIndex(i);
                            assert LearningReliabilityCheck.find(dialog.getContentPane(), JTextArea.class).getText()
                                .contains(ExamplesDialog.EXAMPLES.get(i).expression) : "Missing advanced example details";
                        }
                        choices.setSelectedIndex(1);
                        JTextArea details = LearningReliabilityCheck.find(dialog.getContentPane(), JTextArea.class);
                        assert details.getText().contains("Zero a's is even") && details.getText().contains("(empty string)");
                        LearningReliabilityCheck.button(dialog.getContentPane(), "Load example").doClick(0);
                        GenerationCheck.await(app);
                        assert app.getInputPanel().getRegexField().getText().equals("b*(ab*ab*)*");
                        assert app.getInputPanel().getTestStringField().getText().isEmpty();
                        assert app.getCurrentDfa() != null && app.getCurrentDfa().getStates().size() == 2;
                        assert !dialog.isShowing();
                    } finally { dialog.dispose(); }
                } finally { app.getFrame().dispose(); }
            });
        } finally { isolated.removeNode(); }
        System.out.println("Learning example UI checks passed (dismissal persistence, restore, goals, empty input and loading graphs)");
    }
}
