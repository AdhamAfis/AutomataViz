package com.dfavisualizer.ui;

import java.awt.Color;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.formdev.flatlaf.FlatDarkLaf;
import com.mxgraph.util.mxCellRenderer;

public class GraphThemeCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        SwingUtilities.invokeAndWait(() -> {
            assert UIManager.getLookAndFeel() instanceof FlatDarkLaf : "Not using the demo's Flat Dark theme";
            assert contrast(UIManager.getColor("Label.foreground"), UIManager.getColor("Panel.background")) >= 4.5;
            AutomataGraph graph = new AutomataGraph(UiTheme.DFA_FILL, false);
            graph.addState("q0", true, false, false);
            graph.addState("q1", false, true, false);
            graph.addState("q2", false, false, true);
            graph.addTransition("q0", "q1", 'a', false);
            graph.addTransition("q0", "q2", 'b', false);
            graph.addTransition("q1", "q1", 'a', false);
            graph.addTransition("q0", "q1", '\0', true);
            graph.finish();
            for (Object cell : graph.getGraph().getChildVertices(graph.getGraph().getDefaultParent())) {
                java.util.Map<String, Object> style = graph.getGraph().getCellStyle(cell);
                assert contrast(color(style.get("fontColor")), color(style.get("fillColor"))) >= 4.5 : "State label lacks contrast";
                assert contrast(color(style.get("strokeColor")), UiTheme.CANVAS) >= 3 : "State outline blends into canvas";
            }
            for (Object cell : graph.getGraph().getChildEdges(graph.getGraph().getDefaultParent())) {
                java.util.Map<String, Object> style = graph.getGraph().getCellStyle(cell);
                assert contrast(color(style.get("strokeColor")), UiTheme.CANVAS) >= 3 : "Edge blends into canvas";
                if (!graph.getGraph().getModel().getValue(cell).toString().isEmpty())
                    assert contrast(color(style.get("fontColor")), color(style.get("labelBackgroundColor"))) >= 4.5 : "Transition label lacks contrast";
            }
            assert contrast(color(UiTheme.ACTIVE_STATE), color(UiTheme.DFA_FILL)) >= 3 : "Active state lacks contrast";
            assert contrast(color(UiTheme.ACTIVE_EDGE), UiTheme.CANVAS) >= 3 : "Active transition lacks contrast";
            java.awt.image.BufferedImage exported = mxCellRenderer.createBufferedImage(graph.getGraph(), null, 1, UiTheme.CANVAS, true, null);
            assert exported != null && exported.getWidth() > 100;
            assert exported.getRGB(exported.getWidth() - 1, exported.getHeight() - 1) == UiTheme.CANVAS.getRGB() : "Rendered PNG uses a light background";
            try {
                java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/visual-checks"));
                javax.imageio.ImageIO.write(exported, "png", java.nio.file.Path.of("target/visual-checks/dark-diagram.png").toFile());
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        System.out.println("Dark graph theme checks passed (text, state/edge contrast, active highlights, PNG rendering)");
    }
    private static Color color(Object value) { return Color.decode(value.toString()); }
    private static double contrast(Color a, Color b) { return (Math.max(light(a), light(b)) + .05) / (Math.min(light(a), light(b)) + .05); }
    private static double light(Color color) {
        double result = 0;
        int[] channels = {color.getRed(), color.getGreen(), color.getBlue()};
        double[] weights = {.2126, .7152, .0722};
        for (int i = 0; i < 3; i++) { double c = channels[i] / 255.0; result += weights[i] * (c <= .04045 ? c / 12.92 : Math.pow((c + .055) / 1.055, 2.4)); }
        return result;
    }
}
