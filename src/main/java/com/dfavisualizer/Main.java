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

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxPoint;

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
        JButton testButton = new JButton("Test");
        testButton.addActionListener(this::testString);
        
        testPanel.add(testLabel, BorderLayout.WEST);
        testPanel.add(testStringField, BorderLayout.CENTER);
        testPanel.add(testButton, BorderLayout.EAST);
        
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