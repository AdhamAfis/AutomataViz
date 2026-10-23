package com.dfavisualizer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;
import com.dfavisualizer.ui.components.InputPanel;
import com.dfavisualizer.ui.components.NfaLessonPanel;
import com.dfavisualizer.model.DFA;
import com.mxgraph.model.mxCell;

/** Cycles every bundled theme through a live graph and lesson without writing user preferences. */
public class ThemeSwitchCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        SwingUtilities.invokeAndWait(() -> {
            MainFrame app = new MainFrame();
            JFrame lessonFrame = new JFrame("Theme lesson check");
            try {
                app.display();
                InputPanel input = (InputPanel) ((BorderLayout) app.getFrame().getContentPane().getLayout()).getLayoutComponent(BorderLayout.NORTH);
                input.getRegexField().setText("(a|b)*abb"); input.getRegexField().postActionEvent();
                Object model = app.getCurrentDfa();
                AutomataGraph graph = (AutomataGraph) app.getDfaPanel().getVisualizationComponent();
                double x = graph.state(app.getCurrentDfa().getStartState().getName()).getGeometry().getX();
                NfaLessonPanel lesson = new NfaLessonPanel(app.getCurrentNfa(), java.util.Collections.emptyMap());
                lessonFrame.setContentPane(lesson); lessonFrame.setSize(900, 650); lessonFrame.setVisible(true);
                lesson.start("ab"); lesson.next(); lesson.next();
                Set<Integer> active = new java.util.TreeSet<>(lesson.activeStates());
                input.getTestStringField().setText("aabb");
                app.getVisualizationController().getAnimationController().animateString("aabb");
                double zoom = graph.getGraph().getView().getScale();
                assert UiTheme.THEMES.size() == 6 + com.formdev.flatlaf.intellijthemes.FlatAllIJThemes.INFOS.length;
                for (UiTheme.Theme theme : UiTheme.THEMES) {
                    app.applyTheme(theme);
                    assert theme.className.equals(UIManager.getLookAndFeel().getClass().getName()) : theme.name;
                    assert app.getCurrentDfa() == model && app.getDfaPanel().getVisualizationComponent() == graph : "Theme rebuilt the model/graph";
                    assert input.getRegexField().getText().equals("(a|b)*abb") && input.getTestStringField().getText().equals("aabb");
                    assert input.getRegexField().getHighlighter().getHighlights().length == 1 : "Expression highlight disappeared: " + theme.name;
                    assert graph.getGraph().getView().getScale() == zoom : "Theme changed zoom: " + theme.name;
                    assert graph.state(app.getCurrentDfa().getStartState().getName()).getGeometry().getX() == x : "Theme moved a state";
                    assert !app.getVisualizationController().getAnimationController().isAnimationRunning() : "Theme failed to pause playback";
                    JLabel ribbon = named(app.getStatusPanel(), "playback.input", JLabel.class);
                    assert ribbon.getText().contains("0 / 4") && ribbon.getText().contains(UiTheme.INPUT_BACKGROUND) : "Playback position/palette lost";
                    assert lesson.consumed() == 1 && lesson.activeStates().equals(active) : "Open lesson trace changed";
                    assert graph.getViewport().getBackground().equals(UiTheme.CANVAS) : "Stale canvas: " + theme.name;
                    for (Object vertex : graph.getGraph().getChildVertices(graph.getGraph().getDefaultParent())) {
                        java.util.Map<String, Object> style = graph.getGraph().getCellStyle(vertex);
                        assert contrast(Color.decode(style.get("fontColor").toString()), Color.decode(style.get("fillColor").toString())) >= 4.5 : "Unreadable graph: " + theme.name;
                    }
                    int mask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
                    assert DefaultEditorKit.pasteAction.equals(input.getRegexField().getInputMap().get(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, mask))) : "Paste lost: " + theme.name;
                }
                app.applyTheme(UiTheme.THEMES.get(0));
                app.getVisualizationController().getAnimationController().stepForward();
                assert named(app.getStatusPanel(), "playback.input", JLabel.class).getText().contains("1 / 4");
                app.applyTheme(UiTheme.THEMES.get(1));
                app.getVisualizationController().getAnimationController().stepBack();
                assert named(app.getStatusPanel(), "playback.input", JLabel.class).getText().contains("0 / 4");
                app.getVisualizationController().getAnimationController().stopAnimation();
                for (DFA.State state : app.getCurrentDfa().getStates()) {
                    mxCell cell = graph.state(state.getName());
                    assert UiTheme.STATE_TEXT.equals(graph.getGraph().getCellStyle(cell).get("fontColor")) : "Stop restored old-theme styles";
                }
                app.showThemes();
                JDialog chooser = null;
                for (java.awt.Window owned : app.getFrame().getOwnedWindows())
                    if (owned instanceof JDialog && "Themes".equals(((JDialog) owned).getTitle())) chooser = (JDialog) owned;
                assert chooser != null && !chooser.isModal();
                JList<?> list = named(chooser.getContentPane(), "themes.list", JList.class);
                JTextField search = named(chooser.getContentPane(), "themes.search", JTextField.class);
                JComboBox<?> filter = find(chooser.getContentPane(), JComboBox.class);
                filter.setSelectedIndex(1); assert list.getModel().getSize() == 6 : "Core theme filter failed";
                filter.setSelectedIndex(2);
                for (int i = 0; i < list.getModel().getSize(); i++) assert !((UiTheme.Theme) list.getModel().getElementAt(i)).dark;
                filter.setSelectedIndex(0); search.setText("nord");
                assert list.getModel().getSize() > 0 : "Search lost installed Nord theme";
                for (int i = 0; i < list.getModel().getSize(); i++) assert list.getModel().getElementAt(i).toString().toLowerCase(java.util.Locale.ROOT).contains("nord");
                search.setText("no-such-theme-000"); assert list.getModel().getSize() == 0;
                search.setText("");
                app.getFrame().validate();
                save(app.getFrame(), "themes-dark"); save(chooser, "theme-chooser");
                chooser.dispose();
                app.applyTheme(UiTheme.THEMES.get(0));
                app.getFrame().validate(); save(app.getFrame(), "themes-light");
            } finally {
                app.getVisualizationController().getAnimationController().stopAnimation();
                lessonFrame.dispose(); app.getFrame().dispose();
                UiTheme.install();
            }
        });
        System.out.println("Theme switching checks passed: " + UiTheme.THEMES.size() + " themes, live graphs/lessons, search/filter, zoom, input, playback, paste");
    }
    private static <T> T named(Container parent, String name, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName()) && type.isInstance(child)) return type.cast(child);
            if (child instanceof Container) { T found = named((Container) child, name, type); if (found != null) return found; }
        }
        return null;
    }
    private static <T> T find(Container parent, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container) { T found = find((Container) child, type); if (found != null) return found; }
        }
        return null;
    }
    private static void save(java.awt.Window window, String name) {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(window.getWidth(), window.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics(); window.paint(g); g.dispose();
        try { java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/visual-checks")); javax.imageio.ImageIO.write(image, "png", java.nio.file.Path.of("target/visual-checks/" + name + ".png").toFile()); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    private static double contrast(Color a, Color b) { return (Math.max(light(a), light(b)) + .05) / (Math.min(light(a), light(b)) + .05); }
    private static double light(Color color) {
        double result = 0; int[] channels = {color.getRed(), color.getGreen(), color.getBlue()}; double[] weights = {.2126, .7152, .0722};
        for (int i = 0; i < 3; i++) { double c = channels[i] / 255.0; result += weights[i] * (c <= .04045 ? c / 12.92 : Math.pow((c + .055) / 1.055, 2.4)); }
        return result;
    }
}
