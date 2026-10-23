package com.dfavisualizer.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import com.dfavisualizer.ui.components.ExpressionGuidePanel;
import com.dfavisualizer.ui.components.InputPanel;
import com.dfavisualizer.ui.components.NfaLessonPanel;
import com.dfavisualizer.algorithm.RegexParser;
import com.dfavisualizer.algorithm.SubsetConstruction;
import com.dfavisualizer.model.NFA;

/** Exercises the actual default learning workspace with a large expression and small window. */
public class LearningWorkspaceCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        MainFrame[] holder = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> { holder[0] = new MainFrame(); holder[0].display(); });
        MainFrame app = holder[0];
        try {
            SwingUtilities.invokeAndWait(() -> {
                InputPanel input = find(app.getFrame().getContentPane(), InputPanel.class);
                assert !app.getGraphControls().getCompareToggle().isSelected() : "Comparison should be opt-in";
                String regex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+[.][a-zA-Z][a-zA-Z]+";
                input.getRegexField().setText(regex);
                assert app.getVisualizationController().visualizeDfa(regex, true, false, true, false);
                app.getFrame().validate();
                assert app.getDfaPanel().isShowing() && !app.getNfaPanel().isShowing() : "Default view is not a single diagram";
                ExpressionGuidePanel guide = find(app.getFrame().getContentPane(), ExpressionGuidePanel.class);
                JComboBox<?> parts = find(guide, JComboBox.class);
                parts.setSelectedIndex(1);
                javax.swing.text.Highlighter.Highlight[] highlights = input.getRegexField().getHighlighter().getHighlights();
                assert highlights.length == 1;
                assert "@".equals(regex.substring(highlights[0].getStartOffset(), highlights[0].getEndOffset())) : "Selecting fragment highlights wrong source";
                AutomataGraph graph = (AutomataGraph) app.getDfaPanel().getVisualizationComponent();
                com.dfavisualizer.model.DFA model = app.getCurrentDfa();
                com.mxgraph.model.mxCell edge = graph.transition(model.getStartState().getName(),
                        model.getTransitionTarget(model.getStartState(), 'a').getName(), 'a');
                graph.getGraph().setSelectionCell(edge);
                Object binding = graph.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
                        .get(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0));
                assert binding != null : "Focused graph has no Enter binding";
                boolean[] exactSymbolsShown = {false};
                javax.swing.Timer dismiss = new javax.swing.Timer(100, event -> {
                    for (java.awt.Window window : app.getFrame().getOwnedWindows()) {
                        if (window instanceof javax.swing.JDialog && "Transition symbols".equals(((javax.swing.JDialog) window).getTitle())) {
                            javax.swing.JTextArea details = find(((javax.swing.JDialog) window).getContentPane(), javax.swing.JTextArea.class);
                            exactSymbolsShown[0] = details != null && details.getText().contains("'a'") && details.getText().contains("consumes exactly one");
                            window.dispose();
                        }
                    }
                });
                dismiss.setRepeats(false); dismiss.start();
                graph.getActionMap().get(binding).actionPerformed(new java.awt.event.ActionEvent(graph, 0, "Enter"));
                dismiss.stop();
                assert exactSymbolsShown[0] : "Keyboard activation does not reveal exact transition symbols";
                graph.getGraph().clearSelection();
                input.getTestStringField().setText("adham@gmail.com");
                PlaybackCheck.button(input, "Animate").doClick(0);
                PlaybackCheck.button(app.getStatusPanel(), "Pause").doClick(0);
            });
            Thread.sleep(300);
            for (int[] size : new int[][] {{900, 650}, {1200, 820}}) {
                SwingUtilities.invokeAndWait(() -> { app.getFrame().setSize(size[0], size[1]); app.getFrame().validate(); });
                Thread.sleep(300);
                SwingUtilities.invokeAndWait(() -> {
                    AutomataGraph graph = (AutomataGraph) app.getDfaPanel().getVisualizationComponent();
                    com.mxgraph.view.mxCellState state = graph.getGraph().getView().getState(graph.state(app.getCurrentDfa().getStartState().getName()));
                    assert graph.getViewport().getViewRect().contains(state.getRectangle()) : "Resize hides the active state";
                    checkControls(app.getFrame().getContentPane());
                    save(app.getFrame(), "learning-" + size[0]);
                });
            }
            SwingUtilities.invokeAndWait(() -> {
                app.getVisualizationController().getAnimationController().stopAnimation();
                JTabbedPane tabs = find(app.getFrame().getContentPane(), JTabbedPane.class);
                tabs.setSelectedIndex(1);
                assert app.getNfaPanel().isShowing() && !app.getDfaPanel().isShowing();
                InputPanel input = find(app.getFrame().getContentPane(), InputPanel.class);
                app.getGraphControls().getCompareToggle().doClick(0);
                assert app.getNfaPanel().isShowing() && app.getDfaPanel().isShowing();
                app.getGraphControls().getCompareToggle().doClick(0);
                assert app.getDfaPanel().isShowing() && !app.getNfaPanel().isShowing();
                input.getRegexField().setText("a(b|c)*");
                javax.swing.JMenuBar menuBar = app.getFrame().getJMenuBar();
                menuBar.getMenu(1).getItem(0).doClick(0);
                assert app.getStatusPanel().getStatusArea().getText().contains("Debug complete") : "Debug menu lost its action";
                boolean[] lessonOpened = {false};
                javax.swing.Timer closeLesson = new javax.swing.Timer(100, event -> {
                    for (java.awt.Window window : app.getFrame().getOwnedWindows()) {
                        if (window instanceof javax.swing.JDialog && "Step-by-Step Conversion Process".equals(((javax.swing.JDialog) window).getTitle())) {
                            JTabbedPane steps = find(((javax.swing.JDialog) window).getContentPane(), JTabbedPane.class);
                            lessonOpened[0] = steps != null && "Epsilon-NFA".equals(steps.getTitleAt(1));
                            window.dispose();
                        }
                    }
                });
                closeLesson.setRepeats(false); closeLesson.start();
                menuBar.getMenu(0).getItem(0).doClick(0);
                closeLesson.stop();
                assert lessonOpened[0] : "Learn menu does not open the conversion lesson";
                NFA nfa = new RegexParser().parse("a(b|c)*");
                SubsetConstruction subsets = new SubsetConstruction(); subsets.convertNfaToDfa(nfa);
                NfaLessonPanel lesson = new NfaLessonPanel(nfa, subsets.getStateSubsets());
                JFrame lessonFrame = new JFrame("NFA lesson check");
                try {
                    lessonFrame.setContentPane(lesson); lessonFrame.setSize(900, 650); lessonFrame.setVisible(true); lessonFrame.validate();
                    lesson.start("ab"); lesson.next(); lesson.next(); lesson.next();
                    lessonFrame.validate();
                    lesson.fitView();
                    checkControls(lesson);
                    save(lessonFrame, "nfa-lesson");
                } finally { lessonFrame.dispose(); }
            });
        } finally { SwingUtilities.invokeAndWait(() -> { app.getVisualizationController().getAnimationController().stopAnimation(); app.getFrame().dispose(); }); }
        System.out.println("Learning workspace checks passed (email, default tabs, comparison, source selection, controls at 900/1200)");
    }
    private static <T> T find(Container parent, Class<T> type) {
        if (type.isInstance(parent)) return type.cast(parent);
        for (Component child : parent.getComponents()) if (child instanceof Container) {
            T found = find((Container) child, type); if (found != null) return found;
        }
        return null;
    }
    private static void checkControls(Container parent) {
        for (Component child : parent.getComponents()) {
            if (!child.isShowing()) continue;
            if (child instanceof AbstractButton || child instanceof JComboBox) {
                assert child.getX() >= 0 && child.getY() >= 0 && child.getX() + child.getWidth() <= parent.getWidth()
                        && child.getY() + child.getHeight() <= parent.getHeight() : "Clipped learning control: " + child;
            }
            if (child instanceof Container) checkControls((Container) child);
        }
    }
    private static void save(JFrame frame, String name) {
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); frame.paint(graphics); graphics.dispose();
        try { Files.createDirectories(Path.of("target/visual-checks")); ImageIO.write(image, "png", Path.of("target/visual-checks/" + name + ".png").toFile()); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
