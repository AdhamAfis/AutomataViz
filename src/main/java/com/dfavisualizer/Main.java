package com.dfavisualizer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

public class Main {
    private JFrame frame;
    private JTextField regexField;
    private JTextField testStringField;
    private JSplitPane visualizationSplitPane;
    private JPanel nfaPanel;
    private JPanel dfaPanel;
    private RegexToDfaConverter converter;
    private DfaVisualizer dfaVisualizer;
    private NfaVisualizer nfaVisualizer;
    private JTextArea statusArea;
    private DFA currentDfa;
    private DFA nonMinimizedDfa; // Store the non-minimized DFA
    private JCheckBox splitViewCheckbox;
    private JCheckBox minimizeDfaCheckbox; // New checkbox for DFA minimization
    private JCheckBox showDeadStatesCheckbox; // New checkbox for showing dead states
    private double zoomFactor = 1.0;
    private DfaMinimizer minimizer; // Direct reference to minimizer
    private JButton animateButton;
    private boolean animationInProgress = false;
    private ScheduledExecutorService animationExecutor;
    private JPanel legendPanel; // New panel to display the legend

    public Main() {
        converter = new RegexToDfaConverter();
        dfaVisualizer = new DfaVisualizer();
        nfaVisualizer = new NfaVisualizer();
        minimizer = new DfaMinimizer(); // Initialize minimizer
        initializeUI();
    }

    private void initializeUI() {
        // Set up the main frame
        frame = new JFrame("Regex to DFA Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700); // Larger default size
        frame.setLayout(new BorderLayout());

        // Input panel with regex field and buttons
        JPanel inputPanel = new JPanel(new BorderLayout());
        
        JPanel regexPanel = new JPanel(new BorderLayout());
        regexPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel regexLabel = new JLabel("Regular Expression: ");
        regexField = new JTextField();
        regexPanel.add(regexLabel, BorderLayout.WEST);
        regexPanel.add(regexField, BorderLayout.CENTER);
        
        // Panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        // Add checkbox for split view
        splitViewCheckbox = new JCheckBox("Show NFA/DFA Split View");
        splitViewCheckbox.setSelected(true);
        buttonPanel.add(splitViewCheckbox);
        
        // Add checkbox for DFA minimization
        minimizeDfaCheckbox = new JCheckBox("Minimize DFA");
        minimizeDfaCheckbox.setSelected(true);
        minimizeDfaCheckbox.setToolTipText("Apply Hopcroft's algorithm to minimize the DFA");
        buttonPanel.add(minimizeDfaCheckbox);
        
        // Add checkbox for showing dead states
        showDeadStatesCheckbox = new JCheckBox("Highlight Dead States");
        showDeadStatesCheckbox.setSelected(true);
        showDeadStatesCheckbox.setToolTipText("Highlight states that cannot reach an accept state");
        buttonPanel.add(showDeadStatesCheckbox);
        
        JButton visualizeButton = new JButton("Visualize DFA");
        visualizeButton.addActionListener(this::visualizeDfa);
        buttonPanel.add(visualizeButton);
        
        JButton debugButton = new JButton("Debug");
        debugButton.addActionListener(this::debugRegex);
        buttonPanel.add(debugButton);
        
        regexPanel.add(buttonPanel, BorderLayout.EAST);
        
        inputPanel.add(regexPanel, BorderLayout.NORTH);
        
        // Test string panel
        JPanel testPanel = new JPanel(new BorderLayout());
        testPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        JLabel testLabel = new JLabel("Test String: ");
        testStringField = new JTextField();
        
        JPanel testButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton testButton = new JButton("Test");
        testButton.addActionListener(this::testString);
        
        animateButton = new JButton("Animate");
        animateButton.addActionListener(this::animateString);
        animateButton.setToolTipText("Animate string processing through the DFA");
        
        testButtonPanel.add(testButton);
        testButtonPanel.add(animateButton);
        
        testPanel.add(testLabel, BorderLayout.WEST);
        testPanel.add(testStringField, BorderLayout.CENTER);
        testPanel.add(testButtonPanel, BorderLayout.EAST);
        
        inputPanel.add(testPanel, BorderLayout.CENTER);
        
        // Info panel with supported syntax
        JTextArea syntaxInfo = new JTextArea(
            "Supported Syntax:\n" +
            "a, b, c... - Basic symbols\n" +
            "| - Alternation (OR)\n" +
            "* - Kleene star (zero or more)\n" +
            "+ - One or more\n" +
            "? - Zero or one\n" +
            ". - Any character\n" +
            "() - Grouping\n" +
            "Examples: a(b|c)*, (a|b)*abb"
        );
        syntaxInfo.setEditable(false);
        syntaxInfo.setBackground(new Color(240, 240, 240));
        syntaxInfo.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JScrollPane syntaxScroll = new JScrollPane(syntaxInfo);
        syntaxScroll.setPreferredSize(new Dimension(0, 100));
        inputPanel.add(syntaxScroll, BorderLayout.SOUTH);
        
        frame.add(inputPanel, BorderLayout.NORTH);

        // Create split pane for visualization
        nfaPanel = new JPanel(new BorderLayout());
        nfaPanel.setBorder(BorderFactory.createTitledBorder("NFA Visualization"));
        
        dfaPanel = new JPanel(new BorderLayout());
        dfaPanel.setBorder(BorderFactory.createTitledBorder("DFA Visualization"));
        
        visualizationSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, nfaPanel, dfaPanel);
        visualizationSplitPane.setResizeWeight(0.5); // Equal sizing
        visualizationSplitPane.setOneTouchExpandable(true);
        
        // Add visualization controls to both panels
        addVisualizationControls(nfaPanel);
        addVisualizationControls(dfaPanel);
        
        // Create the legend panel
        createLegendPanel();
        
        // Create a panel to hold both the visualization and the legend
        JPanel centerPanel = new JPanel(new BorderLayout(0, 5)); // 5px vertical gap
        
        // Show or hide the split view based on checkbox
        if (splitViewCheckbox.isSelected()) {
            visualizationSplitPane.setLeftComponent(nfaPanel);
            visualizationSplitPane.setRightComponent(dfaPanel);
            centerPanel.add(visualizationSplitPane, BorderLayout.CENTER);
            visualizationSplitPane.setDividerLocation(0.5);
        } else {
            centerPanel.add(dfaPanel, BorderLayout.CENTER);
        }
        
        // Add legend at the bottom with some padding
        JPanel legendWrapper = new JPanel(new BorderLayout());
        legendWrapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        legendWrapper.add(legendPanel, BorderLayout.CENTER);
        centerPanel.add(legendWrapper, BorderLayout.SOUTH);
        
        // Replace current center panel with new one
        // First, remove any existing components in the CENTER position
        Component centerComponent = ((BorderLayout)frame.getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (centerComponent != null) {
            frame.remove(centerComponent);
        }
        
        // Add the new center panel
        frame.add(centerPanel, BorderLayout.CENTER);
        
        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusArea = new JTextArea("Ready");
        statusArea.setEditable(false);
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setPreferredSize(new Dimension(0, 100));
        statusPanel.add(statusScroll, BorderLayout.CENTER);
        frame.add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void addVisualizationControls(JPanel panel) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton zoomInButton = new JButton("+");
        zoomInButton.setToolTipText("Zoom In (+ key)");
        zoomInButton.addActionListener(e -> {
            JComponent component = (JComponent) panel.getClientProperty("visualComponent");
            if (component instanceof mxGraphComponent) {
                mxGraphComponent graphComponent = (mxGraphComponent) component;
                double scale = graphComponent.getGraph().getView().getScale();
                graphComponent.zoomTo(scale * 1.2, true);
            }
        });
        
        JButton zoomOutButton = new JButton("-");
        zoomOutButton.setToolTipText("Zoom Out (- key)");
        zoomOutButton.addActionListener(e -> {
            JComponent component = (JComponent) panel.getClientProperty("visualComponent");
            if (component instanceof mxGraphComponent) {
                mxGraphComponent graphComponent = (mxGraphComponent) component;
                double scale = graphComponent.getGraph().getView().getScale();
                graphComponent.zoomTo(scale / 1.2, true);
            }
        });
        
        JButton resetViewButton = new JButton("Reset");
        resetViewButton.setToolTipText("Reset View (Home key)");
        resetViewButton.addActionListener(e -> {
            JComponent component = (JComponent) panel.getClientProperty("visualComponent");
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
            JComponent component = (JComponent) panel.getClientProperty("visualComponent");
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
        exportButton.addActionListener(e -> exportToPng(panel));
        toolBar.add(exportButton);
        
        // Add keyboard shortcuts for zooming
        KeyStroke zoomInKey = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0);
        KeyStroke zoomOutKey = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0);
        KeyStroke resetKey = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0);
        
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(zoomInKey, "zoomIn");
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(zoomOutKey, "zoomOut");
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(resetKey, "resetView");
        
        panel.getActionMap().put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomInButton.doClick();
            }
        });
        
        panel.getActionMap().put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomOutButton.doClick();
            }
        });
        
        panel.getActionMap().put("resetView", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetViewButton.doClick();
            }
        });
        
        panel.add(toolBar, BorderLayout.NORTH);
    }
    
    /**
     * Exports the current graph visualization to a PNG file
     * 
     * @param panel The panel containing the visualization component
     */
    private void exportToPng(JPanel panel) {
        JComponent component = (JComponent) panel.getClientProperty("visualComponent");
        if (component instanceof mxGraphComponent) {
            mxGraphComponent graphComponent = (mxGraphComponent) component;
            
            // Create a file chooser
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save PNG Image");
            
            // Set file filter to only show PNG files
            fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images (*.png)", "png"));
            
            // Show save dialog
            int userSelection = fileChooser.showSaveDialog(frame);
            
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
                        statusArea.setText("Graph exported successfully to: " + fileToSave.getAbsolutePath());
                    } else {
                        JOptionPane.showMessageDialog(frame, 
                            "Failed to create image from graph.", 
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, 
                        "Error exporting graph: " + ex.getMessage(), 
                        "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Ensures all animation resources are properly cleaned up
     */
    private void ensureAnimationCleanup() {
        // Make sure animation is properly stopped
        if (animationInProgress || animationExecutor != null) {
            if (animationExecutor != null) {
                try {
                    animationExecutor.shutdownNow();
                    // Wait briefly for threads to terminate
                    animationExecutor.awaitTermination(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    animationExecutor = null;
                }
            }
            
            // Reset animation state
            animationInProgress = false;
            animateButton.setText("Animate");
            animateButton.setBackground(null);
        }
        
        // Reset any graph component to a clean state
        JComponent component = (JComponent) dfaPanel.getClientProperty("visualComponent");
        if (component instanceof mxGraphComponent) {
            final mxGraphComponent graphComponent = (mxGraphComponent) component;
            resetAllCellStyles(graphComponent.getGraph());
            graphComponent.refresh();
        }
    }
    
    private void visualizeDfa(ActionEvent e) {
        // If animation is in progress, stop it first
        if (animationInProgress) {
            stopAnimation();
        }
        
        // Ensure all animation resources are properly cleaned up
        ensureAnimationCleanup();

        String regex = regexField.getText().trim();
        if (regex.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a regular expression.", 
                    "Empty Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            statusArea.setText("Converting regex to DFA...");
            
            // Configure the DFA visualizer to highlight dead states based on checkbox
            dfaVisualizer.setHighlightDeadStates(showDeadStatesCheckbox.isSelected());
            
            // First, convert regex to non-minimized DFA
            nonMinimizedDfa = converter.convertRegexToDfaWithoutMinimization(regex);
            
            // Apply minimization if selected
            if (minimizeDfaCheckbox.isSelected()) {
                currentDfa = minimizer.minimize(nonMinimizedDfa);
                statusArea.setText("Conversion and minimization successful: " + countDfaStats(currentDfa));
            } else {
                currentDfa = nonMinimizedDfa;
                statusArea.setText("Conversion successful (no minimization): " + countDfaStats(currentDfa));
            }
            
            boolean showSplitView = splitViewCheckbox.isSelected();
            
            // Fully clear all panels first to prevent any lingering components
            nfaPanel.removeAll();
            dfaPanel.removeAll();
            
            // Recreate the visualization split pane to ensure a clean state
            if (visualizationSplitPane.getParent() != null) {
                visualizationSplitPane.getParent().remove(visualizationSplitPane);
            }
            visualizationSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, nfaPanel, dfaPanel);
            visualizationSplitPane.setResizeWeight(0.5);
            visualizationSplitPane.setOneTouchExpandable(true);
            
            // Re-add the toolbar
            addVisualizationControls(nfaPanel);
            addVisualizationControls(dfaPanel);
            
            // Visualize NFA if split view is enabled
            if (showSplitView && converter.getLastNfa() != null) {
                JComponent nfaVisualization = nfaVisualizer.visualizeNfa(converter.getLastNfa());
                enablePanAndZoom(nfaVisualization);
                JScrollPane nfaScroll = new JScrollPane(nfaVisualization);
                nfaScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                nfaScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                nfaScroll.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
                nfaPanel.add(nfaScroll, BorderLayout.CENTER);
                nfaPanel.putClientProperty("visualComponent", nfaVisualization);
            }
            
            // Visualize DFA
            JComponent dfaVisualization = dfaVisualizer.visualizeDfa(currentDfa);
            enablePanAndZoom(dfaVisualization);
            JScrollPane dfaScroll = new JScrollPane(dfaVisualization);
            dfaScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            dfaScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            dfaScroll.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
            dfaPanel.add(dfaScroll, BorderLayout.CENTER);
            dfaPanel.putClientProperty("visualComponent", dfaVisualization);
            
            // Create main panel with visualization and legend
            JPanel centerPanel = new JPanel(new BorderLayout(0, 5)); // 5px vertical gap
            
            // Show or hide the split view based on checkbox
            if (showSplitView) {
                centerPanel.add(visualizationSplitPane, BorderLayout.CENTER);
                visualizationSplitPane.setDividerLocation(0.5);
            } else {
                centerPanel.add(dfaPanel, BorderLayout.CENTER);
            }
            
            // Add legend at the bottom with some padding
            JPanel legendWrapper = new JPanel(new BorderLayout());
            legendWrapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            legendWrapper.add(legendPanel, BorderLayout.CENTER);
            centerPanel.add(legendWrapper, BorderLayout.SOUTH);
            
            // Replace current center panel with new one
            // First, remove any existing components in the CENTER position
            Component centerComponent = ((BorderLayout)frame.getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER);
            if (centerComponent != null) {
                frame.remove(centerComponent);
            }
            
            // Add the new center panel
            frame.add(centerPanel, BorderLayout.CENTER);
            
            // Refresh the UI
            frame.revalidate();
            frame.repaint();
        } catch (Exception ex) {
            statusArea.setText("Error: " + ex.getMessage());
            JOptionPane.showMessageDialog(frame, "Error parsing regex: " + ex.getMessage(), 
                    "Parsing Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void enablePanAndZoom(JComponent component) {
        if (component instanceof mxGraphComponent) {
            mxGraphComponent graphComponent = (mxGraphComponent) component;
            
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
            
            // Enable basic panning with mouse
            graphComponent.setPanning(true);
            
            // Add enhanced pan and zoom controls with mouse
            graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
                private int lastX, lastY;
                
                @Override
                public void mousePressed(MouseEvent e) {
                    // Capture start position for panning
                    lastX = e.getX();
                    lastY = e.getY();
                    
                    // Request focus to enable keyboard shortcuts
                    graphComponent.requestFocusInWindow();
                }
            });
            
            graphComponent.getGraphControl().addMouseMotionListener(new MouseAdapter() {
                private int lastX, lastY;
                
                @Override
                public void mouseDragged(MouseEvent e) {
                    // If middle button or right button is pressed, pan the view
                    if (e.isAltDown() || e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                        int dx = e.getX() - lastX;
                        int dy = e.getY() - lastY;
                        
                        if (dx != 0 || dy != 0) {
                            mxPoint translate = graphComponent.getGraph().getView().getTranslate();
                            graphComponent.getGraph().getView().setTranslate(
                                new mxPoint(translate.getX() + dx / graphComponent.getZoomFactor(),
                                           translate.getY() + dy / graphComponent.getZoomFactor()));
                            
                            // Update for next drag event
                            lastX = e.getX();
                            lastY = e.getY();
                        }
                    }
                }
                
                @Override
                public void mousePressed(MouseEvent e) {
                    // Capture start position for panning
                    lastX = e.getX();
                    lastY = e.getY();
                }
            });
            
            // Add mouse wheel zoom support
            graphComponent.addMouseWheelListener(e -> {
                if (e.isControlDown()) {
                    if (e.getWheelRotation() < 0) {
                        // Zoom in
                        double scale = graphComponent.getGraph().getView().getScale();
                        graphComponent.zoomTo(scale * 1.1, true);
                    } else {
                        // Zoom out
                        double scale = graphComponent.getGraph().getView().getScale();
                        graphComponent.zoomTo(scale / 1.1, true);
                    }
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
            
            // Add keyboard shortcuts for navigation
            graphComponent.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    JScrollPane scrollPane = getParentScrollPane(graphComponent);
                    if (scrollPane != null) {
                        int scrollAmount = 20;
                        
                        switch (e.getKeyCode()) {
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
            });
        }
    }
    
    /**
     * Helper method to find the parent JScrollPane of a component
     */
    private JScrollPane getParentScrollPane(Component component) {
        Container parent = component.getParent();
        while (parent != null && !(parent instanceof JScrollPane)) {
            parent = parent.getParent();
        }
        return (JScrollPane) parent;
    }
    
    private void testString(ActionEvent e) {
        if (currentDfa == null) {
            JOptionPane.showMessageDialog(frame, 
                    "Please create a DFA first by entering a regex and clicking 'Visualize DFA'.", 
                    "No DFA Available", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String testStr = testStringField.getText();
        if (testStr.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a string to test.", 
                    "Empty Test String", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        boolean accepted = simulateDfa(currentDfa, testStr);
        String result = "String \"" + testStr + "\": " + (accepted ? "ACCEPTED" : "REJECTED");
        
        statusArea.setText(result + "\n" + generateSimulationTrace(currentDfa, testStr));
        
        if (accepted) {
            JOptionPane.showMessageDialog(frame, result, "Test Result", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(frame, result, "Test Result", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private boolean simulateDfa(DFA dfa, String input) {
        DFA.State currentState = dfa.getStartState();
        
        for (int i = 0; i < input.length(); i++) {
            char symbol = input.charAt(i);
            
            // Find transition for this symbol
            DFA.State nextState = dfa.getTransitionTarget(currentState, symbol);
            
            if (nextState == null) {
                // No valid transition found
                return false;
            }
            
            currentState = nextState;
        }
        
        // Check if we ended in an accept state
        return dfa.getAcceptStates().contains(currentState);
    }
    
    private String generateSimulationTrace(DFA dfa, String input) {
        StringBuilder trace = new StringBuilder("Simulation trace:\n");
        DFA.State currentState = dfa.getStartState();
        
        trace.append("Start state: ").append(currentState).append("\n");
        
        for (int i = 0; i < input.length(); i++) {
            char symbol = input.charAt(i);
            
            // Find transition for this symbol
            DFA.State nextState = dfa.getTransitionTarget(currentState, symbol);
            
            if (nextState == null) {
                trace.append("No transition found from state ")
                     .append(currentState)
                     .append(" on symbol '")
                     .append(symbol)
                     .append("'\n");
                trace.append("String rejected at position ").append(i);
                return trace.toString();
            }
            
            trace.append("Transition: ")
                 .append(currentState)
                 .append(" --[")
                 .append(symbol)
                 .append("]--> ")
                 .append(nextState)
                 .append("\n");
            
            currentState = nextState;
        }
        
        boolean isAcceptState = dfa.getAcceptStates().contains(currentState);
        trace.append("Final state: ")
             .append(currentState)
             .append(" (")
             .append(isAcceptState ? "accept" : "non-accept")
             .append(" state)");
        
        return trace.toString();
    }
    
    private void debugRegex(ActionEvent e) {
        String regex = regexField.getText().trim();
        if (regex.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a regular expression.", 
                    "Empty Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            statusArea.setText("Debugging regex: " + regex + "\n");
            
            // Step 1: Create a regex parser
            RegexParser parser = new RegexParser();
            
            // Step 2: Parse the regex to NFA
            statusArea.append("Step 1: Parsing regex to NFA...\n");
            NFA nfa = parser.parse(regex);
            statusArea.append("NFA created: " + nfa.getStates().size() + " states, " + 
                             nfa.getAlphabet().size() + " symbols, " + 
                             nfa.getAcceptStates().size() + " accept states\n");
            
            statusArea.append("NFA Transitions:\n");
            for (Map.Entry<NFA.NFATransition, Set<Integer>> entry : nfa.getTransitions().entrySet()) {
                NFA.NFATransition transition = entry.getKey();
                Set<Integer> targets = entry.getValue();
                for (int target : targets) {
                    statusArea.append("  " + transition.getState() + " --[" + 
                                     (transition.getSymbol() == NFA.EPSILON ? "ε" : transition.getSymbol()) + 
                                     "]--> " + target + "\n");
                }
            }
            
            // Step 3: Convert NFA to DFA
            statusArea.append("\nStep 2: Converting NFA to DFA using subset construction...\n");
            SubsetConstruction sc = new SubsetConstruction();
            DFA dfa = sc.convertNfaToDfa(nfa);
            
            // Step 4: Minimize DFA
            statusArea.append("\nStep 3: Minimizing DFA...\n");
            DfaMinimizer minimizer = new DfaMinimizer();
            DFA minimizedDfa = minimizer.minimize(dfa);
            
            statusArea.append("\nFinal minimized DFA: " + countDfaStats(minimizedDfa) + "\n");
            
            // Step 5: Show all transitions
            statusArea.append("\nTransitions in minimized DFA:\n");
            for (Map.Entry<DFA.StateTransition, DFA.State> entry : minimizedDfa.getTransitions().entrySet()) {
                DFA.StateTransition transition = entry.getKey();
                DFA.State targetState = entry.getValue();
                statusArea.append("  " + transition.getState() + " --[" + transition.getSymbol() + 
                                 "]--> " + targetState + "\n");
            }
            
            // Success message
            statusArea.append("\nDebug complete. No errors detected in the conversion process.");
            
        } catch (Exception ex) {
            statusArea.append("\nError during debug: " + ex.getMessage() + "\n");
            StackTraceElement[] stackTrace = ex.getStackTrace();
            for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                statusArea.append("  at " + stackTrace[i] + "\n");
            }
        }
    }
    
    private String countDfaStats(DFA dfa) {
        String stats = "States: " + dfa.getStates().size() + 
                       ", Accept States: " + dfa.getAcceptStates().size() + 
                       ", Transitions: " + dfa.getTransitions().size();
                       
        // Add information about dead states if any
        Set<DFA.State> deadStates = dfa.getDeadStates();
        if (!deadStates.isEmpty()) {
            stats += ", Dead States: " + deadStates.size();
        }
        
        return stats;
    }

    private void animateString(ActionEvent e) {
        if (currentDfa == null) {
            JOptionPane.showMessageDialog(frame, 
                    "Please create a DFA first by entering a regex and clicking 'Visualize DFA'.", 
                    "No DFA Available", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String testStr = testStringField.getText();
        if (testStr.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a string to animate.", 
                    "Empty Test String", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // If animation is already in progress, stop it
        if (animationInProgress) {
            stopAnimation();
            return;
        }
        
        // Set up animation
        prepareAnimation();
        
        // Start animation
        startAnimation(testStr);
    }
    
    private void prepareAnimation() {
        // Set button state to indicate animation is running
        animationInProgress = true;
        animateButton.setText("Stop Animation");
        animateButton.setBackground(new Color(255, 200, 200));
        
        // Ensure we're showing only the DFA panel during animation to focus on it
        Component centerComponent = ((BorderLayout)frame.getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (centerComponent != null) {
            frame.remove(centerComponent);
        }
        
        // Create a new panel for the animation
        JPanel animationPanel = new JPanel(new BorderLayout(0, 5));
        animationPanel.add(dfaPanel, BorderLayout.CENTER);
        
        // Add legend at the bottom with some padding
        JPanel legendWrapper = new JPanel(new BorderLayout());
        legendWrapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        legendWrapper.add(legendPanel, BorderLayout.CENTER);
        animationPanel.add(legendWrapper, BorderLayout.SOUTH);
        
        // Add the animation panel
        frame.add(animationPanel, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
        
        // Get the graph component
        JComponent component = (JComponent) dfaPanel.getClientProperty("visualComponent");
        if (component instanceof mxGraphComponent) {
            mxGraphComponent graphComponent = (mxGraphComponent) component;
            
            // Reset all cell styles before animation to ensure clean state
            resetAllCellStyles(graphComponent.getGraph());
            graphComponent.refresh();
        }
    }
    
    private void startAnimation(String input) {
        // Create a scheduled executor for the animation
        animationExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Set up animation state
        final DFA.State startState = currentDfa.getStartState();
        JComponent component = (JComponent) dfaPanel.getClientProperty("visualComponent");
        
        if (component instanceof mxGraphComponent) {
            final mxGraphComponent graphComponent = (mxGraphComponent) component;
            
            // Reset all cell styles before starting animation
            resetAllCellStyles(graphComponent.getGraph());
            
            // Highlight start state
            SwingUtilities.invokeLater(() -> {
                highlightState(graphComponent.getGraph(), startState.getName(), new Color(100, 255, 100));
                graphComponent.refresh();
            });
            
            // Track the current state as we process
            DFA.State currentState = startState;
            
            // Schedule animation steps for each character in the input
            for (int i = 0; i < input.length(); i++) {
                final int step = i;
                final DFA.State fromState = currentState;
                final char symbol = input.charAt(i);
                
                // Find transition target
                DFA.State nextState = currentDfa.getTransitionTarget(currentState, symbol);
                
                // If no valid transition, stop here
                if (nextState == null) {
                    // Schedule the final step that will show the error
                    final int finalStep = step;
                    animationExecutor.schedule(() -> {
                        SwingUtilities.invokeLater(() -> {
                            // Reset all styles before highlighting the error
                            resetAllCellStyles(graphComponent.getGraph());
                            highlightInvalidTransition(graphComponent.getGraph(), fromState.getName(), symbol);
                            statusArea.setText("Animation complete - String REJECTED at position " + finalStep + 
                                             ": No transition from state " + fromState.getName() + 
                                             " on symbol '" + symbol + "'");
                            graphComponent.refresh();
                            animationComplete(false);
                        });
                    }, (step + 1) * 1000, TimeUnit.MILLISECONDS);
                    return;
                }
                
                // Schedule this step
                final DFA.State toState = nextState;
                animationExecutor.schedule(() -> {
                    SwingUtilities.invokeLater(() -> {
                        // Reset all styles before highlighting the transition
                        resetAllCellStyles(graphComponent.getGraph());
                        highlightTransition(graphComponent.getGraph(), fromState.getName(), toState.getName(), symbol);
                        // Update status text
                        statusArea.setText("Step " + (step + 1) + ": " + fromState.getName() + 
                                         " --[" + symbol + "]--> " + toState.getName());
                        graphComponent.refresh();
                    });
                }, (step + 1) * 1000, TimeUnit.MILLISECONDS);
                
                currentState = nextState;
            }
            
            // Schedule the final state check
            final DFA.State finalState = currentState;
            animationExecutor.schedule(() -> {
                SwingUtilities.invokeLater(() -> {
                    boolean isAcceptState = currentDfa.getAcceptStates().contains(finalState);
                    
                    // Reset all styles before the final highlight
                    resetAllCellStyles(graphComponent.getGraph());
                    
                    if (isAcceptState) {
                        // Highlight the final state as accept
                        highlightState(graphComponent.getGraph(), finalState.getName(), new Color(255, 200, 100));
                        statusArea.setText("Animation complete - String ACCEPTED: Ended in accept state " + 
                                         finalState.getName());
                    } else {
                        // Highlight the final state as non-accept
                        highlightState(graphComponent.getGraph(), finalState.getName(), new Color(255, 150, 150));
                        statusArea.setText("Animation complete - String REJECTED: Ended in non-accept state " + 
                                         finalState.getName());
                    }
                    
                    graphComponent.refresh();
                    animationComplete(isAcceptState);
                });
            }, (input.length() + 1) * 1000, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Reset all cell styles in the graph to their default values
     */
    private void resetAllCellStyles(mxGraph graph) {
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
    
    private void stopAnimation() {
        if (animationExecutor != null) {
            animationExecutor.shutdownNow();
            animationExecutor = null;
        }
        
        animationInProgress = false;
        animateButton.setText("Animate");
        animateButton.setBackground(null);
        
        // Reset all cell colors on the UI thread
        JComponent component = (JComponent) dfaPanel.getClientProperty("visualComponent");
        if (component instanceof mxGraphComponent) {
            final mxGraphComponent graphComponent = (mxGraphComponent) component;
            SwingUtilities.invokeLater(() -> {
                resetAllCellStyles(graphComponent.getGraph());
                graphComponent.refresh();
            });
        }
        
        statusArea.setText("Animation stopped.");
    }
    
    private void animationComplete(boolean accepted) {
        animationInProgress = false;
        animateButton.setText("Animate");
        animateButton.setBackground(null);
        
        if (animationExecutor != null) {
            animationExecutor.shutdown();
            animationExecutor = null;
        }
        
        // Show dialog when complete
        SwingUtilities.invokeLater(() -> {
            String result = "String \"" + testStringField.getText() + "\": " + 
                          (accepted ? "ACCEPTED" : "REJECTED");
            
            if (accepted) {
                JOptionPane.showMessageDialog(frame, result, "Animation Result", 
                                            JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame, result, "Animation Result", 
                                            JOptionPane.WARNING_MESSAGE);
            }
        });
    }
    
    private void highlightState(mxGraph graph, String stateName, Color color) {
        Object[] vertices = graph.getChildVertices(graph.getDefaultParent());
        for (Object vertex : vertices) {
            if (graph.getLabel(vertex).equals(stateName)) {
                String hexColor = String.format("#%02x%02x%02x", 
                                              color.getRed(), color.getGreen(), color.getBlue());
                graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, hexColor, new Object[] { vertex });
                graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "3", new Object[] { vertex });
                
                // Ensure the highlighted state is visible (bring to front)
                graph.orderCells(false, new Object[] { vertex });
                
                break;
            }
        }
    }
    
    private void highlightTransition(mxGraph graph, String fromState, String toState, char symbol) {
        // Find and highlight the current state
        highlightState(graph, fromState, new Color(100, 255, 100));
        
        // Find and highlight the edge
        Object[] edges = graph.getChildEdges(graph.getDefaultParent());
        for (Object edge : edges) {
            Object source = graph.getModel().getTerminal(edge, true);
            Object target = graph.getModel().getTerminal(edge, false);
            
            if (graph.getLabel(source).equals(fromState) && graph.getLabel(target).equals(toState)) {
                String edgeLabel = graph.getLabel(edge);
                
                // Check if this edge represents the transition we want
                if (edgeLabel != null && (edgeLabel.indexOf(symbol) >= 0 || edgeLabel.equals(String.valueOf(symbol)))) {
                    graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#FF0000", new Object[] { edge });
                    graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "3", new Object[] { edge });
                    graph.setCellStyles(mxConstants.STYLE_FONTCOLOR, "#FF0000", new Object[] { edge });
                    graph.setCellStyles(mxConstants.STYLE_FONTSIZE, "14", new Object[] { edge });
                    
                    // Ensure the highlighted edge is visible (bring to front)
                    graph.orderCells(false, new Object[] { edge });
                    
                    // After a delay, highlight the target state
                    animationExecutor.schedule(() -> {
                        SwingUtilities.invokeLater(() -> {
                            // Highlight the target state
                            highlightState(graph, toState, new Color(100, 100, 255));
                            graph.refresh();
                        });
                    }, 500, TimeUnit.MILLISECONDS);
                    
                    break;
                }
            }
        }
    }
    
    private void highlightInvalidTransition(mxGraph graph, String stateName, char symbol) {
        // Reset previous highlighted states
        resetHighlightedStates(graph);
        
        // Highlight the current state in red to indicate error
        highlightState(graph, stateName, new Color(255, 100, 100));
        
        // Display the missing transition symbol nearby
        Object[] vertices = graph.getChildVertices(graph.getDefaultParent());
        for (Object vertex : vertices) {
            if (graph.getLabel(vertex).equals(stateName)) {
                // Add a temporary label to indicate the missing transition
                graph.getModel().beginUpdate();
                try {
                    mxGeometry geo = graph.getModel().getGeometry(vertex);
                    Object errorLabel = graph.insertVertex(
                        graph.getDefaultParent(), null, "No transition for '" + symbol + "'",
                        geo.getX() + geo.getWidth() + 10, geo.getY(), 120, 30, 
                        "fontSize=10;fontColor=#FF0000;fillColor=#FFEEEE;strokeColor=#FF0000;rounded=1;");
                } finally {
                    graph.getModel().endUpdate();
                }
                break;
            }
        }
    }
    
    private void resetHighlightedStates(mxGraph graph) {
        Object[] vertices = graph.getChildVertices(graph.getDefaultParent());
        for (Object vertex : vertices) {
            resetCellStyle(graph, vertex);
        }
    }
    
    private void resetCellStyle(mxGraph graph, Object cell) {
        // Check if this is a vertex or edge
        if (graph.getModel().isVertex(cell)) {
            // For vertices (states), reset to original style based on state type
            String stateName = graph.getLabel(cell);
            DFA.State state = findStateByName(currentDfa, stateName);
            
            if (state != null) {
                boolean isStart = (currentDfa.getStartState() == state);
                boolean isAccept = currentDfa.getAcceptStates().contains(state);
                
                if (isStart && isAccept) {
                    // Start+Accept state
                    graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#FFD6A5", new Object[] { cell });
                    graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#008800", new Object[] { cell });
                } else if (isStart) {
                    // Start state
                    graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#E6FFCC", new Object[] { cell });
                    graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#008800", new Object[] { cell });
                } else if (isAccept) {
                    // Accept state
                    graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#FFEBCC", new Object[] { cell });
                    graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#444444", new Object[] { cell });
                } else {
                    // Regular state
                    graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#E6F2FF", new Object[] { cell });
                    graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#444444", new Object[] { cell });
                }
                
                graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, isStart || isAccept ? "3" : "1.5", 
                                   new Object[] { cell });
            }
        } else if (graph.getModel().isEdge(cell)) {
            // For edges (transitions), reset to original style
            boolean isLoop = graph.getModel().getTerminal(cell, true) == graph.getModel().getTerminal(cell, false);
            
            if (isLoop) {
                graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#6666FF", new Object[] { cell });
                graph.setCellStyles(mxConstants.STYLE_FONTCOLOR, "#6666FF", new Object[] { cell });
            } else {
                graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#666666", new Object[] { cell });
                graph.setCellStyles(mxConstants.STYLE_FONTCOLOR, "#666666", new Object[] { cell });
            }
            
            graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "1.5", new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_FONTSIZE, "12", new Object[] { cell });
        }
    }
    
    private DFA.State findStateByName(DFA dfa, String name) {
        for (DFA.State state : dfa.getStates()) {
            if (state.getName().equals(name)) {
                return state;
            }
        }
        return null;
    }

    /**
     * Creates a legend panel explaining the color scheme used in the visualization
     */
    private void createLegendPanel() {
        legendPanel = new JPanel();
        legendPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 2)); // Reduce vertical spacing
        legendPanel.setBorder(BorderFactory.createTitledBorder("Legend"));
        
        // Add color squares with explanations in a more compact grid layout
        JPanel gridPanel = new JPanel(new GridLayout(0, 3, 10, 2)); // 3 columns, as many rows as needed
        
        // Add items to the grid panel instead of directly to legendPanel
        addLegendItem(gridPanel, new Color(230, 242, 255), "Regular State");
        addLegendItem(gridPanel, new Color(255, 235, 204), "Accept State");
        addLegendItem(gridPanel, new Color(230, 255, 204), "Start State");
        addLegendItem(gridPanel, new Color(255, 204, 204), "Dead State");
        addLegendItem(gridPanel, Color.decode("#CCCCFF"), "↻ Self-loop Badge");
        
        // Add the grid panel to the legend panel
        legendPanel.add(gridPanel);
        
        // Add a compact note about dragging
        JLabel dragNote = new JLabel("Tip: Drag states to reposition | Right-click+drag to pan | Arrows to scroll");
        dragNote.setFont(dragNote.getFont().deriveFont(10.0f));
        legendPanel.add(dragNote);
        
        // Keep legend panel very compact
        legendPanel.setPreferredSize(new Dimension(0, 60));
        legendPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
    }
    
    /**
     * Adds a single colored square with a label to the legend
     */
    private void addLegendItem(JPanel legendPanel, Color color, String description) {
        JPanel colorBox = new JPanel();
        colorBox.setBackground(color);
        colorBox.setPreferredSize(new Dimension(12, 12));
        colorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        
        JLabel label = new JLabel(description);
        label.setFont(label.getFont().deriveFont(9.0f));
        
        JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        itemPanel.add(colorBox);
        itemPanel.add(label);
        
        legendPanel.add(itemPanel);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static void main(String[] args) {
        // Set the look and feel to the system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        new Main().show();
    }
    

} 