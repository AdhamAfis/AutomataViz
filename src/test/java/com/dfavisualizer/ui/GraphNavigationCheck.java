package com.dfavisualizer.ui;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.Container;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import com.dfavisualizer.ui.components.VisualizationPanel;
import com.dfavisualizer.ui.dialogs.StepByStepDialog;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxPoint;

/** Desktop-only checks for graph navigation without scrollbars. Run with -ea. */
public class GraphNavigationCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        MainFrame[] holder = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new MainFrame();
            holder[0].display();
            assert holder[0].getVisualizationController().visualizeDfa("(a|b)*abb", true, true, true, true);
        });
        MainFrame app = holder[0];
        try {
            SwingUtilities.invokeAndWait(() -> {
                for (VisualizationPanel panel : panels(app)) {
                    for (int i = 0; i < 8; i++) button(panel, "+").doClick(0);
                }
                app.getFrame().validate();
            });
            SwingUtilities.invokeAndWait(() -> {
                for (VisualizationPanel panel : panels(app)) {
                    mxGraphComponent graph = (mxGraphComponent) panel.getVisualizationComponent();
                    assert !graph.getVerticalScrollBar().isVisible() && !graph.getHorizontalScrollBar().isVisible()
                            : "Scrollbars appear when zooming into a graph";
                    button(panel, "Pan").doClick(0);
                    drag(graph, 120, 120, 170, 150, MouseEvent.BUTTON1);
                    drag(graph, 170, 150, 90, 80, MouseEvent.BUTTON1);
                    button(panel, "Pan").doClick(0);
                    assert graph.getGraph().isCellsSelectable() && graph.getGraphHandler().isEnabled()
                            : "Leaving Pan mode prevents dragging states";
                    drag(graph, 100, 100, 140, 130, MouseEvent.BUTTON3);
                    double scale = graph.getGraph().getView().getScale();
                    mxPoint translate = graph.getGraph().getView().getTranslate();
                    double xBefore = (100 / scale) - translate.getX();
                    double yBefore = (100 / scale) - translate.getY();
                    graph.getGraphControl().dispatchEvent(new MouseWheelEvent(graph.getGraphControl(),
                            MouseEvent.MOUSE_WHEEL, 10, InputEvent.CTRL_DOWN_MASK, 100, 100, 0,
                            false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 1));
                    double newScale = graph.getGraph().getView().getScale();
                    mxPoint newTranslate = graph.getGraph().getView().getTranslate();
                    assert newScale < scale : "Ctrl+wheel does not zoom the canvas";
                    assert Math.abs(100 / newScale - newTranslate.getX() - xBefore) < 0.01
                            && Math.abs(100 / newScale - newTranslate.getY() - yBefore) < 0.01
                            : "Zoom moves the graph point under the cursor";
                    for (int modifiers : new int[] {0, InputEvent.SHIFT_DOWN_MASK}) {
                        mxPoint beforeWheel = graph.getGraph().getView().getTranslate();
                        graph.getGraphControl().dispatchEvent(new MouseWheelEvent(graph.getGraphControl(),
                                MouseEvent.MOUSE_WHEEL, 11, modifiers, 100, 100, 0,
                                false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 1));
                        mxPoint afterWheel = graph.getGraph().getView().getTranslate();
                        assert Math.abs((afterWheel.getX() - beforeWheel.getX()) * newScale
                                - (modifiers == 0 ? 0 : -30)) < 0.01
                                && Math.abs((afterWheel.getY() - beforeWheel.getY()) * newScale
                                - (modifiers == 0 ? -30 : 0)) < 0.01 : "Wheel does not pan the selected axis";
                    }
                    drag(graph, 100, 100, 3000, -3000, MouseEvent.BUTTON3);
                    button(panel, "Reset").doClick(0);
                }
            });
            SwingUtilities.invokeAndWait(() -> {
                for (VisualizationPanel panel : panels(app)) {
                    mxGraphComponent graph = (mxGraphComponent) panel.getVisualizationComponent();
                    assert !graph.getVerticalScrollBar().isVisible() && !graph.getHorizontalScrollBar().isVisible();
                    assert graph.getGraph().getGraphBounds().getRectangle().intersects(graph.getViewport().getViewRect())
                            : "Reset fails to recover a panned graph";
                }
            });
            JTabbedPane[] tabs = new JTabbedPane[1];
            SwingUtilities.invokeAndWait(() -> {
                StepByStepDialog steps = new StepByStepDialog(app, "a(b|c)*");
                for (Window owned : app.getFrame().getOwnedWindows()) {
                    if (owned instanceof JDialog && "Step-by-Step Conversion Process".equals(((JDialog) owned).getTitle())) {
                        JDialog dialog = (JDialog) owned;
                        dialog.setModal(false);
                        tabs[0] = (JTabbedPane) ((BorderLayout) dialog.getContentPane().getLayout())
                                .getLayoutComponent(BorderLayout.CENTER);
                    }
                }
                steps.display();
            });
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
            boolean[] ready = {false};
            while (!ready[0] && System.nanoTime() < deadline) {
                SwingUtilities.invokeAndWait(() -> ready[0] = ((Container) tabs[0].getComponentAt(1)).getComponentCount() > 0);
                Thread.sleep(20);
            }
            assert ready[0] : "Walkthrough did not finish loading";
            for (int tab = 1; tab <= 3; tab++) {
                final int selectedTab = tab;
                SwingUtilities.invokeAndWait(() -> tabs[0].setSelectedIndex(selectedTab));
                SwingUtilities.invokeAndWait(() -> {
                    Container step = (Container) tabs[0].getSelectedComponent();
                    Component center = ((BorderLayout) step.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                    assert center instanceof VisualizationPanel : "Step-by-step graph lacks pan and zoom controls";
                    VisualizationPanel panel = (VisualizationPanel) center;
                    mxGraphComponent graph = (mxGraphComponent) panel.getVisualizationComponent();
                    assert !graph.getVerticalScrollBar().isVisible() && !graph.getHorizontalScrollBar().isVisible();
                    assert graph.getGraph().getGraphBounds().getRectangle().intersects(graph.getViewport().getViewRect())
                            : "Newly shown graph tab is not fitted";
                    button(panel, "Pan").doClick(0);
                    drag(graph, 100, 100, 140, 130, MouseEvent.BUTTON1);
                    button(panel, "Reset").doClick(0);
                });
            }
        } finally {
            SwingUtilities.invokeAndWait(app.getFrame()::dispose);
        }
        System.out.println("Graph navigation checks passed");
    }

    private static void drag(mxGraphComponent graph, int x, int y, int endX, int endY, int button) {
        mxPoint before = graph.getGraph().getView().getTranslate();
        double scale = graph.getGraph().getView().getScale();
        Component control = graph.getGraphControl();
        int mask = button == MouseEvent.BUTTON1 ? InputEvent.BUTTON1_DOWN_MASK : InputEvent.BUTTON3_DOWN_MASK;
        control.dispatchEvent(new MouseEvent(control, MouseEvent.MOUSE_PRESSED, 1, mask, x, y, 1, false, button));
        control.dispatchEvent(new MouseEvent(control, MouseEvent.MOUSE_DRAGGED, 2, mask, endX, endY, 0, false));
        control.dispatchEvent(new MouseEvent(control, MouseEvent.MOUSE_RELEASED, 3, 0, endX, endY, 1, false, button));
        mxPoint after = graph.getGraph().getView().getTranslate();
        assert Math.abs((after.getX() - before.getX()) * scale - (endX - x)) < 0.01
                && Math.abs((after.getY() - before.getY()) * scale - (endY - y)) < 0.01
                : "Dragging does not pan by the pointer movement";
    }

    private static VisualizationPanel[] panels(MainFrame app) {
        return new VisualizationPanel[] {app.getNfaPanel(), app.getDfaPanel()};
    }

    private static AbstractButton button(Container parent, String text) {
        for (Component child : parent.getComponents()) {
            if (child instanceof AbstractButton && text.equals(((AbstractButton) child).getText())) return (AbstractButton) child;
            if (child instanceof Container) {
                AbstractButton found = button((Container) child, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
