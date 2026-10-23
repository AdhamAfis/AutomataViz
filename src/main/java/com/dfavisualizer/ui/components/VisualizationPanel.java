package com.dfavisualizer.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.dfavisualizer.ui.UiTheme;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;

public class VisualizationPanel extends JPanel {
    private String title;
    private JLabel heading;
    private JToggleButton panModeButton;
    
    public VisualizationPanel(String title) {
        this.title = title;
        setLayout(new BorderLayout());
        setBorder(UiTheme.surfaceBorder(0, 0, 0, 0));
        JLabel empty = new JLabel("<html><div style='text-align:center'><b>No " + title
                + " generated</b><br><br>Enter an expression and choose Visualize DFA.<br>Example: <code>(a|b)*abb</code></div></html>", JLabel.CENTER);
        empty.setBackground(UiTheme.CANVAS);
        empty.setOpaque(true);
        empty.setForeground(UiTheme.MUTED);
        add(empty, BorderLayout.CENTER);
        addVisualizationControls();
    }
    
    private void addVisualizationControls() {
        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.PLAIN, 12f));
        header.add(heading, BorderLayout.WEST);
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        
        JButton zoomInButton = new JButton("+");
        zoomInButton.setToolTipText("Zoom In (+ key)");
        zoomInButton.addActionListener(e -> {
            JComponent component = (JComponent) getClientProperty("visualComponent");
            if (component instanceof mxGraphComponent) {
                mxGraphComponent graphComponent = (mxGraphComponent) component;
                zoomGraph(graphComponent, 1.2, graphComponent.getViewport().getWidth() / 2.0,
                        graphComponent.getViewport().getHeight() / 2.0);
            }
        });
        
        JButton zoomOutButton = new JButton("-");
        zoomOutButton.setToolTipText("Zoom Out (- key)");
        zoomOutButton.addActionListener(e -> {
            JComponent component = (JComponent) getClientProperty("visualComponent");
            if (component instanceof mxGraphComponent) {
                mxGraphComponent graphComponent = (mxGraphComponent) component;
                zoomGraph(graphComponent, 1 / 1.2, graphComponent.getViewport().getWidth() / 2.0,
                        graphComponent.getViewport().getHeight() / 2.0);
            }
        });
        
        JButton resetViewButton = new JButton("Reset");
        resetViewButton.setToolTipText("Reset View (Home key)");
        resetViewButton.addActionListener(e -> {
            JComponent component = (JComponent) getClientProperty("visualComponent");
            if (component instanceof mxGraphComponent) {
                mxGraphComponent graphComponent = (mxGraphComponent) component;
                fitView();
            }
        });
        
        panModeButton = new JToggleButton("Pan");
        panModeButton.setToolTipText("Pan Mode (drag to move)");
        
        panModeButton.addActionListener(e -> {
            JComponent component = (JComponent) getClientProperty("visualComponent");
            if (component instanceof mxGraphComponent) {
                mxGraphComponent graphComponent = (mxGraphComponent) component;
                
                boolean panModeActive = panModeButton.isSelected();
                
                if (panModeActive) {
                    panModeButton.setToolTipText("Exit Pan Mode");
                    
                    graphComponent.getGraph().setCellsSelectable(false);
                    graphComponent.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    panModeButton.setToolTipText("Pan Mode (drag to move)");
                    
                    graphComponent.getGraph().setCellsSelectable(true);
                    graphComponent.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
                
                graphComponent.getGraphHandler().setEnabled(!panModeActive);
                graphComponent.getSelectionCellsHandler().setEnabled(!panModeActive);
            }
        });
        
        toolBar.add(zoomInButton);
        toolBar.add(zoomOutButton);
        toolBar.add(resetViewButton);
        toolBar.add(panModeButton);
        
        JButton exportButton = new JButton("Export");
        exportButton.setToolTipText("Export to PNG image");
        exportButton.addActionListener(e -> exportToPng());
        toolBar.add(exportButton);
        for (Component control : toolBar.getComponents())
            if (control instanceof javax.swing.AbstractButton) UiTheme.quietButton((javax.swing.AbstractButton) control);
        toolBar.setOpaque(false);
        
        KeyStroke zoomInKey = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0);
        KeyStroke zoomOutKey = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0);
        KeyStroke resetKey = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0);
        
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(zoomInKey, "zoomIn");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(zoomOutKey, "zoomOut");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(resetKey, "resetView");
        
        getActionMap().put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomInButton.doClick();
            }
        });
        
        getActionMap().put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomOutButton.doClick();
            }
        });
        
        getActionMap().put("resetView", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetViewButton.doClick();
            }
        });
        
        header.add(toolBar, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        setMinimumSize(new Dimension(header.getPreferredSize().width + 2, 150));
    }

    public void setVisualizationComponent(JComponent component) {
        Component centerComp = ((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (centerComp != null) {
            remove(centerComp);
        }
        
        putClientProperty("visualComponent", component);
        if (component instanceof mxGraphComponent) {
            mxGraph graph = ((mxGraphComponent) component).getGraph();
            int states = graph.getChildVertices(graph.getDefaultParent()).length;
            heading.setText(title + " · " + states + (states == 1 ? " state" : " states"));
            Component header = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.NORTH);
            setMinimumSize(new Dimension(header.getPreferredSize().width + 2, 150));
        }
        panModeButton.setSelected(false);
        panModeButton.setToolTipText("Pan Mode (drag to move)");
        
        if (component instanceof mxGraphComponent) {
            enhanceGraphNavigation((mxGraphComponent) component);
            
        }
        
        // mxGraphComponent already owns a viewport and scrollbars.
        add(component instanceof JScrollPane ? component : new JScrollPane(component), BorderLayout.CENTER);

        revalidate();
        repaint();
        if (component instanceof mxGraphComponent) {
            SwingUtilities.invokeLater(() -> {
                if (!Boolean.TRUE.equals(component.getClientProperty("playback.active"))) fitGraph((mxGraphComponent) component);
            });
            if (!component.isShowing()) {
                component.addHierarchyListener(new HierarchyListener() {
                    @Override
                    public void hierarchyChanged(HierarchyEvent e) {
                        if (component.isShowing()) {
                            component.removeHierarchyListener(this);
                            SwingUtilities.invokeLater(() -> {
                if (!Boolean.TRUE.equals(component.getClientProperty("playback.active"))) fitGraph((mxGraphComponent) component);
            });
                        }
                    }
                });
            }
        }
    }

    public void fitView() {
        JComponent component = getVisualizationComponent();
        if (component instanceof mxGraphComponent) fitGraph((mxGraphComponent) component);
    }

    private void fitGraph(mxGraphComponent component) {
        Dimension extent = component.getViewport().getExtentSize();
        if (extent.width <= 40 || extent.height <= 40) return;
        component.getGraph().getView().scaleAndTranslate(1, 0, 0);
        mxRectangle bounds = component.getGraph().getGraphBounds();
        double scale = Math.min(1, Math.min((extent.width - 40) / Math.max(1, bounds.getWidth()),
                (extent.height - 40) / Math.max(1, bounds.getHeight())));
        component.getGraph().getView().scaleAndTranslate(scale,
                (extent.width / scale - bounds.getWidth()) / 2 - bounds.getX(),
                (extent.height / scale - bounds.getHeight()) / 2 - bounds.getY());
        component.getViewport().setViewPosition(new java.awt.Point(0, 0));
    }

    private void enhanceGraphNavigation(mxGraphComponent graphComponent) {
        graphComponent.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        graphComponent.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        graphComponent.setWheelScrollingEnabled(false);
        // One pan handler owns the canvas translation; viewport scrolling must not compete.
        graphComponent.setPanning(false);
        graphComponent.setAutoScroll(false);
        graphComponent.setAutoExtend(false);

        graphComponent.getGraphControl().addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                zoomGraph(graphComponent, Math.pow(1.1, -e.getPreciseWheelRotation()), e.getX(), e.getY());
            } else {
                double distance = -30 * e.getPreciseWheelRotation();
                panGraph(graphComponent, e.isShiftDown() ? distance : 0, e.isShiftDown() ? 0 : distance);
            }
            e.consume();
        });

        MouseAdapter mouseAdapter = new MouseAdapter() {
            private int lastX, lastY;
            private boolean panning;

            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
                panning = e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3
                        || e.isAltDown() || panModeButton.isSelected();
                if (panning) {
                    graphComponent.getGraphHandler().setEnabled(false);
                    graphComponent.getGraphHandler().reset();
                    graphComponent.getSelectionCellsHandler().setEnabled(false);
                    graphComponent.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    e.consume();
                }
                graphComponent.requestFocusInWindow();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (panning) {
                    panGraph(graphComponent, e.getX() - lastX, e.getY() - lastY);
                    lastX = e.getX();
                    lastY = e.getY();
                    e.consume();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (panning) {
                    panning = false;
                    graphComponent.getGraphHandler().setEnabled(!panModeButton.isSelected());
                    graphComponent.getSelectionCellsHandler().setEnabled(!panModeButton.isSelected());
                    graphComponent.setCursor(Cursor.getPredefinedCursor(
                            panModeButton.isSelected() ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    e.consume();
                }
            }
        };
        graphComponent.getGraphControl().addMouseListener(mouseAdapter);
        graphComponent.getGraphControl().addMouseMotionListener(mouseAdapter);

        graphComponent.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int distance = e.isShiftDown() ? 100 : 20;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP: panGraph(graphComponent, 0, distance); break;
                    case KeyEvent.VK_DOWN: panGraph(graphComponent, 0, -distance); break;
                    case KeyEvent.VK_LEFT: panGraph(graphComponent, distance, 0); break;
                    case KeyEvent.VK_RIGHT: panGraph(graphComponent, -distance, 0); break;
                    default: return;
                }
                e.consume();
            }
        });
        graphComponent.setFocusable(true);
    }

    private void panGraph(mxGraphComponent component, double dx, double dy) {
        double scale = component.getGraph().getView().getScale();
        mxPoint translate = component.getGraph().getView().getTranslate();
        component.getGraph().getView().setTranslate(
                new mxPoint(translate.getX() + dx / scale, translate.getY() + dy / scale));
    }

    private void zoomGraph(mxGraphComponent component, double factor, double x, double y) {
        double scale = component.getGraph().getView().getScale();
        double newScale = Math.max(0.1, Math.min(5, scale * factor));
        mxPoint translate = component.getGraph().getView().getTranslate();
        component.getGraph().getView().scaleAndTranslate(newScale,
                translate.getX() + x * (1 / newScale - 1 / scale),
                translate.getY() + y * (1 / newScale - 1 / scale));
    }

    private void exportToPng() {
        JComponent component = (JComponent) getClientProperty("visualComponent");
        if (component instanceof mxGraphComponent) {
            mxGraphComponent graphComponent = (mxGraphComponent) component;
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save PNG Image");
            
            fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images (*.png)", "png"));
            
            int userSelection = fileChooser.showSaveDialog(this);
            
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                
                if (!fileToSave.getAbsolutePath().toLowerCase().endsWith(".png")) {
                    fileToSave = new File(fileToSave.getAbsolutePath() + ".png");
                }
                
                try {
                    BufferedImage image = mxCellRenderer.createBufferedImage(
                        graphComponent.getGraph(), 
                        null, 
                        graphComponent.getGraph().getView().getScale(), 
                        UiTheme.CANVAS,
                        graphComponent.isAntiAlias(),
                        null);
                    
                    if (image != null) {
                        ImageIO.write(image, "PNG", fileToSave);
                        JOptionPane.showMessageDialog(this, 
                            "Graph exported successfully to: " + fileToSave.getAbsolutePath(), 
                            "Export Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "Failed to create image from graph.", 
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, 
                        "Error exporting graph: " + ex.getMessage(), 
                        "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    public void resetAllCellStyles(mxGraph graph) {
        Object[] vertices = graph.getChildVertices(graph.getDefaultParent());
        for (Object vertex : vertices) {
            resetCellStyle(graph, vertex);
        }
        
        Object[] edges = graph.getChildEdges(graph.getDefaultParent());
        for (Object edge : edges) {
            resetCellStyle(graph, edge);
        }
    }
    
    public void resetCellStyle(mxGraph graph, Object cell) {
        // Default implementation - should be overridden by specific visualizers
        if (graph.getModel().isVertex(cell)) {
            graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, UiTheme.DFA_FILL, new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, UiTheme.STATE_STROKE, new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "1.5", new Object[] { cell });
        } else if (graph.getModel().isEdge(cell)) {
            graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, UiTheme.EDGE, new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_FONTCOLOR, UiTheme.EDGE, new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "1.5", new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_FONTSIZE, "12", new Object[] { cell });
        }
    }
    
         public JComponent getVisualizationComponent() {
        return (JComponent) getClientProperty("visualComponent");
     }
} 