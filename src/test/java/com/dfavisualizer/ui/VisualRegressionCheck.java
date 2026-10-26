package com.dfavisualizer.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import com.dfavisualizer.ui.components.InputPanel;
import com.dfavisualizer.ui.components.VisualizationPanel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;

/** Run after mvn package with java -ea -cp target/test-classes:target/AutomataViz-1.2.0-jar-with-dependencies.jar com.dfavisualizer.ui.VisualRegressionCheck. */
public class VisualRegressionCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        MainFrame[] holder = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new MainFrame();
            holder[0].display();
        });
        MainFrame app = holder[0];
        JFrame frame = app.getFrame();
        try {
            SwingUtilities.invokeAndWait(() -> saveImage(frame, "empty"));
            for (String regex : new String[] {"a*", "a(b|c)*", "(a|b)*abb"}) {
                SwingUtilities.invokeAndWait(() -> {
                    InputPanel input = (InputPanel) ((BorderLayout) frame.getContentPane().getLayout())
                            .getLayoutComponent(BorderLayout.NORTH);
                    input.getRegexField().setText(regex);
                    assert app.getVisualizationController().visualizeDfa(regex, true, true, true, true);
                    frame.validate();
                });
                // Initial fit is queued after Swing lays out the new graph components.
                SwingUtilities.invokeAndWait(() -> {
                    for (VisualizationPanel panel : panels(app)) {
                        assertGraphVisible(panel);
                        Component center = ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                        assert center == panel.getVisualizationComponent() : "Nested graph scroll panes";
                        assert ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.NORTH) != null : "Missing graph controls";
                        assert countExports(panel) == 1 : "Export should appear once in each graph toolbar";
                    }
                    for (int i = 0; i < 3; i++) {
                        app.updateVisualizationLayout(false);
                        frame.validate();
                        assert app.getDfaPanel().isShowing();
                        app.updateVisualizationLayout(true);
                        frame.validate();
                        assert app.getVisualizationSplitPane().getRightComponent() == app.getDfaPanel() : "Lost DFA panel";
                        assert app.getNfaPanel().isShowing() && app.getDfaPanel().isShowing();
                    }
                });
            }
            for (boolean left : new boolean[] {true, false}) {
                SwingUtilities.invokeAndWait(() -> {
                    app.getVisualizationSplitPane().setDividerLocation(left
                            ? app.getVisualizationSplitPane().getMinimumDividerLocation()
                            : app.getVisualizationSplitPane().getMaximumDividerLocation());
                    frame.validate();
                });
                SwingUtilities.invokeAndWait(() -> {
                    for (VisualizationPanel panel : panels(app)) {
                        Container header = (Container) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
                        for (Component child : header.getComponents()) {
                            assert child.getX() >= 0 && child.getX() + child.getWidth() <= header.getWidth()
                                    : "Graph toolbar clipped at minimum split width";
                        }
                    }
                    checkControls(frame.getContentPane());
                });
            }
            SwingUtilities.invokeAndWait(() -> {
                app.getVisualizationSplitPane().setDividerLocation(0.5);
                frame.validate();
            });
            for (int[] size : new int[][] {{1000, 700}, {900, 650}, {1400, 900}}) {
                SwingUtilities.invokeAndWait(() -> {
                    frame.setSize(size[0], size[1]);
                    frame.validate();
                });
                // Let resize and validation events settle before inspecting descendants.
                SwingUtilities.invokeAndWait(() -> {
                    frame.validate();
                    for (VisualizationPanel panel : panels(app)) {
                        clickReset(panel);
                        assertGraphVisible(panel);
                        mxGraphComponent graph = (mxGraphComponent) panel.getVisualizationComponent();
                        for (Object state : graph.getGraph().getChildVertices(graph.getGraph().getDefaultParent())) {
                            if (graph.getGraph().getModel().getValue(state).toString().startsWith("q")) {
                                assert graph.getGraph().getCellGeometry(state).getWidth() >= 60 : "Unreadable state";
                                if (size[0] == 1000) {
                                    double fontSize = Double.parseDouble(graph.getGraph().getCellStyle(state)
                                            .get(mxConstants.STYLE_FONTSIZE).toString());
                                    assert fontSize * graph.getGraph().getView().getScale() >= 11
                                            : "Small example state labels shrink below 11 pixels: " + fontSize * graph.getGraph().getView().getScale() + " viewport=" + graph.getViewport().getExtentSize();
                                }
                            }
                        }
                    }
                    checkControls(frame.getContentPane());
                    saveImage(frame, Integer.toString(size[0]));
                });
            }
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
        System.out.println("Visual regression checks passed");
    }

    private static VisualizationPanel[] panels(MainFrame app) {
        return new VisualizationPanel[] {app.getNfaPanel(), app.getDfaPanel()};
    }

    private static void assertGraphVisible(VisualizationPanel panel) {
        assert panel.getVisualizationComponent() instanceof mxGraphComponent : "Missing graph";
        mxGraphComponent graph = (mxGraphComponent) panel.getVisualizationComponent();
        assert graph.getGraph().getGraphBounds().getRectangle().intersects(graph.getViewport().getViewRect()) : "Graph outside viewport";
    }

    private static void saveImage(JFrame frame, String name) {
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        frame.paint(graphics);
        graphics.dispose();
        try {
            Files.createDirectories(Path.of("target/visual-checks"));
            ImageIO.write(image, "png", Path.of("target/visual-checks/" + name + ".png").toFile());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static int countExports(Container parent) {
        int count = 0;
        for (Component child : parent.getComponents()) {
            if (child instanceof AbstractButton && ((AbstractButton) child).getText().startsWith("Export")) count++;
            else if (child instanceof Container) count += countExports((Container) child);
        }
        return count;
    }

    private static void clickReset(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof AbstractButton && "Reset".equals(((AbstractButton) child).getText())) {
                ((AbstractButton) child).doClick();
            } else if (child instanceof Container) clickReset((Container) child);
        }
    }

    private static void checkControls(Container parent) {
        for (Component child : parent.getComponents()) {
            if (!child.isShowing()) continue;
            boolean hasText = child instanceof AbstractButton
                    && ((AbstractButton) child).getText() != null && !((AbstractButton) child).getText().isEmpty()
                    || child instanceof JLabel && ((JLabel) child).getText() != null;
            if (hasText) {
                assert child.getX() >= 0 && child.getY() >= 0
                    && child.getX() + child.getWidth() <= parent.getWidth()
                    && child.getY() + child.getHeight() <= parent.getHeight() : "Clipped control: " + child;
            }
            if (child instanceof Container) checkControls((Container) child);
        }
    }
}
