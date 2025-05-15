package com.dfavisualizer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
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
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
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
        
        frame.add(visualizationSplitPane, BorderLayout.CENTER);

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
        zoomInButton.setToolTipText("Zoom In");
        zoomInButton.addActionListener(e -> {
            JComponent component = (JComponent) panel.getClientProperty("visualComponent");
            if (component instanceof mxGraphComponent) {
                mxGraphComponent graphComponent = (mxGraphComponent) component;
                double scale = graphComponent.getGraph().getView().getScale();
                graphComponent.zoomTo(scale * 1.2, true);
            }
        });
        
        JButton zoomOutButton = new JButton("-");
        zoomOutButton.setToolTipText("Zoom Out");
        zoomOutButton.addActionListener(e -> {
            JComponent component = (JComponent) panel.getClientProperty("visualComponent");
            if (component instanceof mxGraphComponent) {
                mxGraphComponent graphComponent = (mxGraphComponent) component;
                double scale = graphComponent.getGraph().getView().getScale();
                graphComponent.zoomTo(scale / 1.2, true);
            }
        });
        
        JButton resetViewButton = new JButton("Reset");
        resetViewButton.setToolTipText("Reset View");
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
                    panModeButton.setBackground(new Color(220, 220, 255));
                    panModeButton.setText("🖐 (Active)");
                } else {
                    panModeButton.setBackground(null);
                    panModeButton.setText("🖐");
                }
                
                // Configure the graph component
                graphComponent.getGraphHandler().setEnabled(!panModeActive[0]);
                graphComponent.setPanning(panModeActive[0]);
                
                if (panModeActive[0]) {
                    graphComponent.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    graphComponent.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
        
        // Add Export to PNG button
        JButton exportButton = new JButton("📷");
        exportButton.setToolTipText("Export to PNG");
        exportButton.addActionListener(e -> exportToPng(panel));
        
        toolBar.add(zoomInButton);
        toolBar.add(zoomOutButton);
        toolBar.add(resetViewButton);
        toolBar.add(panModeButton);
        toolBar.add(exportButton);
        
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

    private void visualizeDfa(ActionEvent e) {
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
            
            // Clear the visualization panels
            nfaPanel.removeAll();
            dfaPanel.removeAll();
            
            // Re-add the toolbar
            addVisualizationControls(nfaPanel);
            addVisualizationControls(dfaPanel);
            
            // Visualize NFA if split view is enabled
            if (showSplitView && converter.getLastNfa() != null) {
                JComponent nfaVisualization = nfaVisualizer.visualizeNfa(converter.getLastNfa());
                enablePanAndZoom(nfaVisualization);
                JScrollPane nfaScroll = new JScrollPane(nfaVisualization);
                nfaPanel.add(nfaScroll, BorderLayout.CENTER);
                nfaPanel.putClientProperty("visualComponent", nfaVisualization);
            }
            
            // Visualize DFA
            JComponent dfaVisualization = dfaVisualizer.visualizeDfa(currentDfa);
            enablePanAndZoom(dfaVisualization);
            JScrollPane dfaScroll = new JScrollPane(dfaVisualization);
            dfaPanel.add(dfaScroll, BorderLayout.CENTER);
            dfaPanel.putClientProperty("visualComponent", dfaVisualization);
            
            // Show or hide the split view based on checkbox
            if (showSplitView) {
                frame.remove(visualizationSplitPane);
                frame.add(visualizationSplitPane, BorderLayout.CENTER);
                visualizationSplitPane.setDividerLocation(0.5);
            } else {
                frame.remove(visualizationSplitPane);
                frame.add(dfaPanel, BorderLayout.CENTER);
            }
            
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
                }
            });
        }
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
        
        // Ensure we're showing only the DFA panel
        frame.remove(visualizationSplitPane);
        frame.add(dfaPanel, BorderLayout.CENTER);
        
        // Get the graph component
        JComponent component = (JComponent) dfaPanel.getClientProperty("visualComponent");
        if (component instanceof mxGraphComponent) {
            mxGraphComponent graphComponent = (mxGraphComponent) component;
            
            // Reset all colors to default
            Object[] cells = graphComponent.getGraph().getChildCells(graphComponent.getGraph().getDefaultParent());
            for (Object cell : cells) {
                resetCellStyle(graphComponent.getGraph(), cell);
            }
        }
        
        // Update the UI
        frame.revalidate();
        frame.repaint();
    }
    
    private void startAnimation(String input) {
        // Create a scheduled executor for the animation
        animationExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Set up animation state
        DFA.State currentState = currentDfa.getStartState();
        JComponent component = (JComponent) dfaPanel.getClientProperty("visualComponent");
        
        if (component instanceof mxGraphComponent) {
            mxGraphComponent graphComponent = (mxGraphComponent) component;
            
            // Highlight start state
            highlightState(graphComponent.getGraph(), currentState.getName(), new Color(100, 255, 100));
            
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
                    animationExecutor.schedule(() -> {
                        highlightInvalidTransition(graphComponent.getGraph(), fromState.getName(), symbol);
                        statusArea.setText("Animation complete - String REJECTED at position " + step + 
                                         ": No transition from state " + fromState.getName() + 
                                         " on symbol '" + symbol + "'");
                        animationComplete(false);
                    }, (step + 1) * 1000, TimeUnit.MILLISECONDS);
                    return;
                }
                
                // Schedule this step
                final DFA.State toState = nextState;
                animationExecutor.schedule(() -> {
                    // Highlight the transition
                    highlightTransition(graphComponent.getGraph(), fromState.getName(), toState.getName(), symbol);
                    // Update status text
                    statusArea.setText("Step " + (step + 1) + ": " + fromState.getName() + 
                                     " --[" + symbol + "]--> " + toState.getName());
                }, (step + 1) * 1000, TimeUnit.MILLISECONDS);
                
                currentState = nextState;
            }
            
            // Schedule the final state check
            final DFA.State finalState = currentState;
            animationExecutor.schedule(() -> {
                boolean isAcceptState = currentDfa.getAcceptStates().contains(finalState);
                
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
                
                animationComplete(isAcceptState);
            }, (input.length() + 1) * 1000, TimeUnit.MILLISECONDS);
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
        
        // Reset all cell colors
        JComponent component = (JComponent) dfaPanel.getClientProperty("visualComponent");
        if (component instanceof mxGraphComponent) {
            mxGraphComponent graphComponent = (mxGraphComponent) component;
            
            Object[] cells = graphComponent.getGraph().getChildCells(graphComponent.getGraph().getDefaultParent());
            for (Object cell : cells) {
                resetCellStyle(graphComponent.getGraph(), cell);
            }
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
                break;
            }
        }
    }
    
    private void highlightTransition(mxGraph graph, String fromState, String toState, char symbol) {
        // Reset previous highlighted state
        resetHighlightedStates(graph);
        
        // Highlight the current state
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
                    
                    // After a delay, highlight the target state
                    animationExecutor.schedule(() -> {
                        // Highlight the target state
                        highlightState(graph, toState, new Color(100, 100, 255));
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