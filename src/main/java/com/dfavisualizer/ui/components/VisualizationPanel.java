package com.dfavisualizer.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

/**
 * Panel for displaying automata visualizations (NFA or DFA).
 * Includes controls for zooming, panning, and exporting the visualization.
 */
public class VisualizationPanel extends JPanel {
    private String title;
    
    /**
     * Constructor - initializes the visualization panel
     * 
     * @param title The title for this visualization panel
     */
    public VisualizationPanel(String title) {
        this.title = title;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(title));
        addVisualizationControls();
    }
    
    /**
     * Add visualization controls to the panel
     */
    private void addVisualizationControls() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton zoomInButton = new JButton("+");
        zoomInButton.setToolTipText("Zoom In (+ key)");
        zoomInButton.addActionListener(e -> {
            JComponent component = (JComponent) getClientProperty("visualComponent");
            if (component instanceof mxGraphComponent) {
                mxGraphComponent graphComponent = (mxGraphComponent) component;
                double scale = graphComponent.getGraph().getView().getScale();
                graphComponent.zoomTo(scale * 1.2, true);
            }
        });
        
        JButton zoomOutButton = new JButton("-");
        zoomOutButton.setToolTipText("Zoom Out (- key)");
        zoomOutButton.addActionListener(e -> {
            JComponent component = (JComponent) getClientProperty("visualComponent");
            if (component instanceof mxGraphComponent) {
                mxGraphComponent graphComponent = (mxGraphComponent) component;
                double scale = graphComponent.getGraph().getView().getScale();
                graphComponent.zoomTo(scale / 1.2, true);
            }
        });
        
        JButton resetViewButton = new JButton("Reset");
        resetViewButton.setToolTipText("Reset View (Home key)");
        resetViewButton.addActionListener(e -> {
            JComponent component = (JComponent) getClientProperty("visualComponent");
            if (component instanceof mxGraphComponent) {
                mxGraphComponent graphComponent = (mxGraphComponent) component;
                graphComponent.zoomTo(1.0, true);
                graphComponent.getGraph().getView().setTranslate(new mxPoint(0, 0));
            }
        });
        
        JButton panModeButton = new JButton("🖐");
        panModeButton.setToolTipText("Pan Mode (drag to move)");
        
        // Track pan mode state
        final boolean[] panModeActive = {false};
        
        panModeButton.addActionListener(e -> {
            JComponent component = (JComponent) getClientProperty("visualComponent");
            if (component instanceof mxGraphComponent) {
                mxGraphComponent graphComponent = (mxGraphComponent) component;
                
                // Toggle pan mode
                panModeActive[0] = !panModeActive[0];
                
                // Update button appearance
                if (panModeActive[0]) {
                    panModeButton.setBackground(new Color(200, 230, 255));
                    panModeButton.setToolTipText("Exit Pan Mode");
                    
                    // Disable cell selection when in pan mode
                    graphComponent.getGraph().setCellsSelectable(false);
                    graphComponent.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    panModeButton.setBackground(null);
                    panModeButton.setToolTipText("Pan Mode (drag to move)");
                    
                    // Re-enable cell selection when exiting pan mode
                    graphComponent.getGraph().setCellsSelectable(true);
                    graphComponent.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
                
                // Configure the component for panning
                graphComponent.getGraphHandler().setEnabled(!panModeActive[0]);
                graphComponent.setPanning(panModeActive[0]);
            }
        });
        
        // Add buttons to toolbar
        toolBar.add(zoomInButton);
        toolBar.add(zoomOutButton);
        toolBar.add(resetViewButton);
        toolBar.add(panModeButton);
        
        // Add export button
        JButton exportButton = new JButton("Export");
        exportButton.setToolTipText("Export to PNG image");
        exportButton.addActionListener(e -> exportToPng());
        exportButton.setBackground(new Color(230, 255, 230)); // Light green background
        exportButton.setForeground(new Color(0, 100, 0)); // Dark green text
        toolBar.add(exportButton);
        
        // Add keyboard shortcuts for zooming
        KeyStroke zoomInKey = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0);
        KeyStroke zoomOutKey = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0);
        KeyStroke resetKey = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0);
        
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(zoomInKey, "zoomIn");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(zoomOutKey, "zoomOut");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(resetKey, "resetView");
        
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
        
        add(toolBar, BorderLayout.NORTH);
        
        // Add a separate export button at the bottom for more visibility
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportButtonBottom = new JButton("Export to PNG");
        exportButtonBottom.setToolTipText("Export visualization to PNG image");
        exportButtonBottom.setBackground(new Color(230, 255, 230)); // Light green background
        exportButtonBottom.setForeground(new Color(0, 100, 0)); // Dark green text
        exportButtonBottom.addActionListener(e -> exportToPng());
        bottomPanel.add(exportButtonBottom);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Set the visualization component (graph)
     * 
     * @param component The graph component to display
     */
    public void setVisualizationComponent(JComponent component) {
        // First remove any existing component from the center
        Component centerComp = ((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (centerComp != null) {
            remove(centerComp);
        }
        
        // Store the component for later access
        putClientProperty("visualComponent", component);
        
        // If this is a graph component, enhance its navigation capabilities
        if (component instanceof mxGraphComponent) {
            enhanceGraphNavigation((mxGraphComponent) component);
            
            // We don't process grid snap settings here anymore - it's handled directly by InputPanel
        }
        
        // Add the component to a scroll pane and add to the center
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
        add(scrollPane, BorderLayout.CENTER);
        
        // Refresh UI
        revalidate();
        repaint();
    }
    
    /**
     * Enhances the navigation capabilities of a graph component with
     * improved zoom, pan, and keyboard controls
     * 
     * @param graphComponent The graph component to enhance
     */
    private void enhanceGraphNavigation(mxGraphComponent graphComponent) {
        // Make component properly resize with parent
        graphComponent.setAutoExtend(true);
        graphComponent.setPreferredSize(new Dimension(800, 600));
        
        // Enable automatic resize when the window is resized
        graphComponent.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Get the current scale
                double scale = graphComponent.getGraph().getView().getScale();
                
                // Get the component dimensions
                int width = graphComponent.getWidth();
                int height = graphComponent.getHeight();
                
                // Only auto-resize if below a certain size threshold to prevent excessive shrinking
                if (width > 300 && height > 300) {
                    // Adjust scale based on component size if needed
                    if (scale < 0.5 && width > 600 && height > 400) {
                        graphComponent.zoomTo(Math.min(1.0, scale * 1.2), true);
                    } else if (scale > 1.5 && (width < 400 || height < 300)) {
                        graphComponent.zoomTo(Math.max(0.5, scale * 0.8), true);
                    }
                }
            }
        });
        
        // Enable basic panning with mouse by default (makes navigation more intuitive)
        graphComponent.setPanning(true);
        
        // Add enhanced pan and zoom controls with mouse wheel, inspired by standard mapping tools
        graphComponent.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                // More precise zoom control
                int wheelRotation = e.getWheelRotation();
                double zoomFactor = wheelRotation < 0 ? 1.1 : 0.9;
                
                // Get current scale
                double scale = graphComponent.getGraph().getView().getScale();
                
                // Calculate new scale with limits to prevent extreme zooming
                double newScale = scale * zoomFactor;
                newScale = Math.max(0.1, Math.min(5.0, newScale)); // Limit scale between 0.1 and 5.0
                
                // Get mouse position to zoom toward that point
                int mouseX = e.getX();
                int mouseY = e.getY();
                
                // Calculate graph position under mouse before zoom
                mxPoint graphPos = graphComponent.getPointForEvent(e);
                
                // Apply zoom
                graphComponent.getGraph().getView().setScale(newScale);
                
                                 // Calculate how much the view needs to be adjusted
                 // Convert screen coordinates back to graph coordinates after zoom
                 mxPoint newGraphPos = graphComponent.getPointForEvent(new MouseEvent(
                     graphComponent, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(),
                     0, mouseX, mouseY, 0, false));
                 double dx = (newGraphPos.getX() - graphPos.getX()) * newScale;
                 double dy = (newGraphPos.getY() - graphPos.getY()) * newScale;
                
                // Adjust view translate to keep point under mouse stable
                mxPoint translate = graphComponent.getGraph().getView().getTranslate();
                graphComponent.getGraph().getView().setTranslate(
                    new mxPoint(translate.getX() - dx / newScale, translate.getY() - dy / newScale));
                
                // Refresh
                graphComponent.refresh();
                
                e.consume();
            } else {
                // Allow normal scrolling when not pressing Ctrl
                JScrollPane scrollPane = getParentScrollPane(graphComponent);
                if (scrollPane != null) {
                    // Determine scroll direction and speed
                    int direction = e.getWheelRotation() > 0 ? 1 : -1;
                    int scrollAmount = 30 * direction;
                    
                    if (e.isShiftDown()) {
                        // Horizontal scroll when Shift is pressed
                        JScrollBar horizontal = scrollPane.getHorizontalScrollBar();
                        horizontal.setValue(horizontal.getValue() + scrollAmount);
                    } else {
                        // Vertical scroll
                        JScrollBar vertical = scrollPane.getVerticalScrollBar();
                        vertical.setValue(vertical.getValue() + scrollAmount);
                    }
                    e.consume();
                }
            }
        });
        
        // Add improved mouse drag for panning
        MouseAdapter mouseAdapter = new MouseAdapter() {
            private int lastX, lastY;
            private boolean isPanning = false;
            
            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
                
                // Middle-click or right-click activates direct panning mode
                if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                    isPanning = true;
                    graphComponent.setCursor(new Cursor(Cursor.MOVE_CURSOR));
                    e.consume();
                }
                
                // Request focus to enable keyboard shortcuts
                graphComponent.requestFocusInWindow();
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                // If middle button, right button, or space key is pressed, pan the view
                if (isPanning || e.isAltDown()) {
                    double scale = graphComponent.getGraph().getView().getScale();
                    int dx = e.getX() - lastX;
                    int dy = e.getY() - lastY;
                    
                    if (dx != 0 || dy != 0) {
                        mxPoint translate = graphComponent.getGraph().getView().getTranslate();
                        graphComponent.getGraph().getView().setTranslate(
                            new mxPoint(translate.getX() + dx / scale, translate.getY() + dy / scale));
                        graphComponent.refresh();
                        
                        // Update for next drag event
                        lastX = e.getX();
                        lastY = e.getY();
                        e.consume();
                    }
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (isPanning) {
                    isPanning = false;
                    graphComponent.setCursor(Cursor.getDefaultCursor());
                    e.consume();
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                // Update last positions for potential future dragging
                lastX = e.getX();
                lastY = e.getY();
            }
        };
        
        // Add the mouse listeners to the graph control for better precision
        graphComponent.getGraphControl().addMouseListener(mouseAdapter);
        graphComponent.getGraphControl().addMouseMotionListener(mouseAdapter);
        
        // Add keyboard shortcuts for navigation
        graphComponent.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                
                // Space key toggles pan mode temporarily
                if (keyCode == KeyEvent.VK_SPACE) {
                    graphComponent.setCursor(new Cursor(Cursor.MOVE_CURSOR));
                }
                
                // Keyboard navigation (arrow keys)
                JScrollPane scrollPane = getParentScrollPane(graphComponent);
                if (scrollPane != null) {
                    int scrollAmount = e.isShiftDown() ? 100 : 20; // Larger movements with shift
                    
                    switch (keyCode) {
                        case KeyEvent.VK_UP:
                            scrollPane.getVerticalScrollBar().setValue(
                                scrollPane.getVerticalScrollBar().getValue() - scrollAmount);
                            e.consume();
                            break;
                        case KeyEvent.VK_DOWN:
                            scrollPane.getVerticalScrollBar().setValue(
                                scrollPane.getVerticalScrollBar().getValue() + scrollAmount);
                            e.consume();
                            break;
                        case KeyEvent.VK_LEFT:
                            scrollPane.getHorizontalScrollBar().setValue(
                                scrollPane.getHorizontalScrollBar().getValue() - scrollAmount);
                            e.consume();
                            break;
                        case KeyEvent.VK_RIGHT:
                            scrollPane.getHorizontalScrollBar().setValue(
                                scrollPane.getHorizontalScrollBar().getValue() + scrollAmount);
                            e.consume();
                            break;
                    }
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                // Reset cursor when space is released
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    graphComponent.setCursor(Cursor.getDefaultCursor());
                }
            }
        });
        
        // Enable focus to receive key events
        graphComponent.setFocusable(true);
    }
    
    /**
     * Find the parent scroll pane of a component
     * 
     * @param component The component to find the parent for
     * @return The parent scroll pane or null if not found
     */
    private JScrollPane getParentScrollPane(Component component) {
        Container parent = component.getParent();
        while (parent != null && !(parent instanceof JScrollPane)) {
            parent = parent.getParent();
        }
        return (JScrollPane) parent;
    }
    
    /**
     * Export the visualization to a PNG file
     */
    private void exportToPng() {
        JComponent component = (JComponent) getClientProperty("visualComponent");
        if (component instanceof mxGraphComponent) {
            mxGraphComponent graphComponent = (mxGraphComponent) component;
            
            // Create a file chooser
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save PNG Image");
            
            // Set file filter to only show PNG files
            fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images (*.png)", "png"));
            
            // Show save dialog
            int userSelection = fileChooser.showSaveDialog(this);
            
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                
                // Ensure file has .png extension
                if (!fileToSave.getAbsolutePath().toLowerCase().endsWith(".png")) {
                    fileToSave = new File(fileToSave.getAbsolutePath() + ".png");
                }
                
                try {
                    // Create a BufferedImage of the graph
                    BufferedImage image = mxCellRenderer.createBufferedImage(
                        graphComponent.getGraph(), 
                        null, 
                        graphComponent.getGraph().getView().getScale(), 
                        Color.WHITE, 
                        graphComponent.isAntiAlias(),
                        null);
                    
                    if (image != null) {
                        // Save image to file
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
    
    /**
     * Reset all cell styles in the graph to their default values
     * 
     * @param graph The graph to reset styles for
     */
    public void resetAllCellStyles(mxGraph graph) {
        // Vertices (states)
        Object[] vertices = graph.getChildVertices(graph.getDefaultParent());
        for (Object vertex : vertices) {
            resetCellStyle(graph, vertex);
        }
        
        // Edges (transitions)
        Object[] edges = graph.getChildEdges(graph.getDefaultParent());
        for (Object edge : edges) {
            resetCellStyle(graph, edge);
        }
    }
    
    /**
     * Reset the style of a single cell
     * 
     * @param graph The graph containing the cell
     * @param cell The cell to reset
     */
    public void resetCellStyle(mxGraph graph, Object cell) {
        // Default implementation - should be overridden by specific visualizers
        if (graph.getModel().isVertex(cell)) {
            graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#E6F2FF", new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#444444", new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "1.5", new Object[] { cell });
        } else if (graph.getModel().isEdge(cell)) {
            graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#666666", new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_FONTCOLOR, "#666666", new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "1.5", new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_FONTSIZE, "12", new Object[] { cell });
        }
    }
    
    /**
     * Get the visualization component
     * 
     * @return The component used for visualization
     */
         public JComponent getVisualizationComponent() {
        return (JComponent) getClientProperty("visualComponent");
     }
} 