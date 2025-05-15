package com.dfavisualizer.ui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.dfavisualizer.algorithm.DfaMinimizer;
import com.dfavisualizer.algorithm.NfaMinimizer;
import com.dfavisualizer.algorithm.RegexToDfaConverter;
import com.dfavisualizer.controller.VisualizationController;
import com.dfavisualizer.model.DFA;
import com.dfavisualizer.model.NFA;
import com.dfavisualizer.ui.components.InputPanel;
import com.dfavisualizer.ui.components.LegendPanel;
import com.dfavisualizer.ui.components.StatusPanel;
import com.dfavisualizer.ui.components.VisualizationPanel;

/**
 * Main application window that contains all UI components and coordinates their interactions.
 */
public class MainFrame {
    private JFrame frame;
    private InputPanel inputPanel;
    private VisualizationPanel nfaPanel;
    private VisualizationPanel dfaPanel;
    private StatusPanel statusPanel;
    private LegendPanel legendPanel;
    private JSplitPane visualizationSplitPane;
    
    // Application state
    private RegexToDfaConverter converter;
    private DfaVisualizer dfaVisualizer;
    private NfaVisualizer nfaVisualizer;
    private DFA currentDfa;
    private DFA nonMinimizedDfa;
    private NFA currentNfa;
    private NFA minimizedNfa;
    private DfaMinimizer dfaMinimizer;
    private NfaMinimizer nfaMinimizer;
    private VisualizationController visualizationController;
    
    /**
     * Constructor - initializes the application components
     */
    public MainFrame() {
        // Initialize algorithm components
        this.converter = new RegexToDfaConverter();
        this.dfaVisualizer = new DfaVisualizer();
        this.nfaVisualizer = new NfaVisualizer();
        this.dfaMinimizer = new DfaMinimizer();
        this.nfaMinimizer = new NfaMinimizer();
        
        // Initialize the UI
        initializeUI();
        
        // Initialize the controller
        this.visualizationController = new VisualizationController(this);
    }
    
    /**
     * Initialize the UI components
     */
    private void initializeUI() {
        // Set up the main frame
        frame = new JFrame("Regex to DFA Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout());
        
        // Create UI components
        inputPanel = new InputPanel(this);
        nfaPanel = new VisualizationPanel("NFA Visualization");
        dfaPanel = new VisualizationPanel("DFA Visualization");
        statusPanel = new StatusPanel();
        legendPanel = new LegendPanel();
        
        // Create split pane for visualizations
        visualizationSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, nfaPanel, dfaPanel);
        visualizationSplitPane.setResizeWeight(0.5);
        visualizationSplitPane.setOneTouchExpandable(true);
        
        // Create central panel to hold visualizations and legend
        JPanel centerPanel = new JPanel(new BorderLayout(0, 5));
        centerPanel.add(visualizationSplitPane, BorderLayout.CENTER);
        centerPanel.add(legendPanel, BorderLayout.SOUTH);
        
        // Add components to frame
        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(statusPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Display the main application window
     */
    public void display() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    /**
     * Get the status panel for displaying messages
     */
    public StatusPanel getStatusPanel() {
        return statusPanel;
    }
    
    /**
     * Get the DFA visualizer
     */
    public DfaVisualizer getDfaVisualizer() {
        return dfaVisualizer;
    }
    
    /**
     * Get the NFA visualizer
     */
    public NfaVisualizer getNfaVisualizer() {
        return nfaVisualizer;
    }
    
    /**
     * Get the RegexToDfaConverter
     */
    public RegexToDfaConverter getConverter() {
        return converter;
    }
    
    /**
     * Get the DfaMinimizer
     */
    public DfaMinimizer getDfaMinimizer() {
        return dfaMinimizer;
    }
    
    /**
     * Get the NfaMinimizer
     */
    public NfaMinimizer getNfaMinimizer() {
        return nfaMinimizer;
    }
    
    /**
     * Get the current DFA
     */
    public DFA getCurrentDfa() {
        return currentDfa;
    }
    
    /**
     * Set the current DFA
     */
    public void setCurrentDfa(DFA dfa) {
        this.currentDfa = dfa;
    }
    
    /**
     * Get the non-minimized DFA
     */
    public DFA getNonMinimizedDfa() {
        return nonMinimizedDfa;
    }
    
    /**
     * Set the non-minimized DFA
     */
    public void setNonMinimizedDfa(DFA dfa) {
        this.nonMinimizedDfa = dfa;
    }
    
    /**
     * Get the current NFA
     */
    public NFA getCurrentNfa() {
        return currentNfa;
    }
    
    /**
     * Set the current NFA
     */
    public void setCurrentNfa(NFA nfa) {
        this.currentNfa = nfa;
    }
    
    /**
     * Get the minimized NFA
     */
    public NFA getMinimizedNfa() {
        return minimizedNfa;
    }
    
    /**
     * Set the minimized NFA
     */
    public void setMinimizedNfa(NFA nfa) {
        this.minimizedNfa = nfa;
    }
    
    /**
     * Get the main JFrame
     */
    public JFrame getFrame() {
        return frame;
    }
    
    /**
     * Get the NFA panel
     */
    public VisualizationPanel getNfaPanel() {
        return nfaPanel;
    }
    
    /**
     * Get the DFA panel
     */
    public VisualizationPanel getDfaPanel() {
        return dfaPanel;
    }
    
    /**
     * Get the visualization split pane
     */
    public JSplitPane getVisualizationSplitPane() {
        return visualizationSplitPane;
    }
    
    /**
     * Update the visualization layout based on split view setting
     */
    public void updateVisualizationLayout(boolean showSplitView) {
        // Remove current center panel
        Component centerComponent = ((BorderLayout)frame.getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (centerComponent != null) {
            frame.remove(centerComponent);
        }
        
        // Create new center panel
        JPanel centerPanel = new JPanel(new BorderLayout(0, 5));
        
        if (showSplitView) {
            centerPanel.add(visualizationSplitPane, BorderLayout.CENTER);
            visualizationSplitPane.setDividerLocation(0.5);
        } else {
            centerPanel.add(dfaPanel, BorderLayout.CENTER);
        }
        
        // Add legend
        centerPanel.add(legendPanel, BorderLayout.SOUTH);
        
        // Add the new center panel and refresh
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
    }
    
    /**
     * Get the visualization controller
     */
    public VisualizationController getVisualizationController() {
        return visualizationController;
    }
} 