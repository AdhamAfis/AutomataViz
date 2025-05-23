package com.dfavisualizer.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import com.dfavisualizer.controller.VisualizationController;
import com.dfavisualizer.ui.MainFrame;
import com.dfavisualizer.ui.dialogs.StepByStepDialog;
import com.dfavisualizer.ui.dialogs.TheoryPopupDialog;

/**
 * Panel containing input controls for the regex input, test string, and various options.
 */
public class InputPanel extends JPanel {
    private MainFrame mainFrame;
    private JTextField regexField;
    private JTextField testStringField;
    private JCheckBox splitViewCheckbox;
    private JCheckBox minimizeDfaCheckbox;
    private JCheckBox minimizeNfaCheckbox;
    private JCheckBox showDeadStatesCheckbox;
    private JCheckBox gridSnapCheckbox;
    private JCheckBox animateLoopsCheckbox;
    private JButton visualizeButton;
    private JButton debugButton;
    private JButton stepByStepButton;
    private JButton theoryPopupButton;
    private JButton testButton;
    private JButton animateButton;
    
    /**
     * Constructor - initializes the input panel
     * 
     * @param mainFrame The main application frame
     */
    public InputPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        initializeComponents();
    }
    
    /**
     * Initialize panel components
     */
    private void initializeComponents() {
        // Since the VisualizationController is created after this panel in MainFrame,
        // we need to get the reference later when we actually need it
        
        // Regex input section
        JPanel regexPanel = new JPanel(new BorderLayout());
        regexPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JLabel regexLabel = new JLabel("Regular Expression: ");
        regexField = new JTextField();
        regexPanel.add(regexLabel, BorderLayout.WEST);
        regexPanel.add(regexField, BorderLayout.CENTER);
        
        // Add regex panel to the main input panel
        add(regexPanel, BorderLayout.NORTH);
        
        // Create toolbar for buttons and checkboxes
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // Add checkboxes
        splitViewCheckbox = new JCheckBox("Split View");
        splitViewCheckbox.setSelected(true);
        splitViewCheckbox.addActionListener(e -> mainFrame.updateVisualizationLayout(splitViewCheckbox.isSelected()));
        toolBar.add(splitViewCheckbox);
        toolBar.addSeparator(new Dimension(10, 10));
        
        minimizeDfaCheckbox = new JCheckBox("Minimize DFA");
        minimizeDfaCheckbox.setSelected(true);
        minimizeDfaCheckbox.setToolTipText("Apply Hopcroft's algorithm to minimize the DFA");
        toolBar.add(minimizeDfaCheckbox);
        toolBar.addSeparator(new Dimension(10, 10));
        
        minimizeNfaCheckbox = new JCheckBox("Minimize NFA");
        minimizeNfaCheckbox.setSelected(true);
        minimizeNfaCheckbox.setToolTipText("Apply Hopcroft's algorithm to minimize the NFA");
        toolBar.add(minimizeNfaCheckbox);
        toolBar.addSeparator(new Dimension(10, 10));
        
        showDeadStatesCheckbox = new JCheckBox("Highlight Dead");
        showDeadStatesCheckbox.setSelected(true);
        showDeadStatesCheckbox.setToolTipText("Highlight states that cannot reach an accept state");
        toolBar.add(showDeadStatesCheckbox);
        toolBar.addSeparator(new Dimension(10, 10));
        
        gridSnapCheckbox = new JCheckBox("Grid Snap");
        gridSnapCheckbox.setSelected(false);
        gridSnapCheckbox.setToolTipText("Snap states to grid when moving them");
        gridSnapCheckbox.addActionListener(this::handleGridSnapToggle);
        toolBar.add(gridSnapCheckbox);
        toolBar.addSeparator(new Dimension(10, 10));
        
        // Add animate loops checkbox
        animateLoopsCheckbox = new JCheckBox("Animate Loops");
        animateLoopsCheckbox.setSelected(true);
        animateLoopsCheckbox.setToolTipText("Animate self-loops during string testing");
        animateLoopsCheckbox.addActionListener(this::handleAnimateLoopsToggle);
        toolBar.add(animateLoopsCheckbox);
        
        // Add a larger separator
        toolBar.addSeparator(new Dimension(30, 10));
        
        // Add action buttons
        visualizeButton = new JButton("Visualize DFA");
        visualizeButton.addActionListener(this::visualizeDfa);
        toolBar.add(visualizeButton);
        toolBar.addSeparator(new Dimension(5, 10));
        
        debugButton = new JButton("Debug");
        debugButton.addActionListener(this::debugRegex);
        toolBar.add(debugButton);
        toolBar.addSeparator(new Dimension(5, 10));
        
        // Step-by-step conversion button
        stepByStepButton = new JButton("Step-by-Step");
        stepByStepButton.setToolTipText("Show step-by-step conversion from regex to DFA");
        stepByStepButton.setBackground(new Color(230, 230, 255)); // Light blue background
        stepByStepButton.addActionListener(e -> showStepByStepConversion());
        toolBar.add(stepByStepButton);
        toolBar.addSeparator(new Dimension(5, 10));
        
        // Theory popup button
        theoryPopupButton = new JButton("Theory");
        theoryPopupButton.setToolTipText("Show formal definitions and automata theory concepts");
        theoryPopupButton.setBackground(new Color(255, 230, 230)); // Light red background
        theoryPopupButton.addActionListener(e -> showTheoryPopup());
        toolBar.add(theoryPopupButton);
        
        // Add toolbar to a panel in the center position
        JPanel toolBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolBarPanel.add(toolBar);
        add(toolBarPanel, BorderLayout.CENTER);
        
        // Test string panel
        JPanel testPanel = new JPanel(new BorderLayout());
        testPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        JLabel testLabel = new JLabel("Test String: ");
        testStringField = new JTextField();
        
        JPanel testButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        testButton = new JButton("Test");
        testButton.addActionListener(this::testString);
        
        animateButton = new JButton("Animate");
        animateButton.addActionListener(this::animateString);
        animateButton.setToolTipText("Animate string processing through the DFA");
        
        testButtonPanel.add(testButton);
        testButtonPanel.add(animateButton);
        
        testPanel.add(testLabel, BorderLayout.WEST);
        testPanel.add(testStringField, BorderLayout.CENTER);
        testPanel.add(testButtonPanel, BorderLayout.EAST);
        
        // Add test panel to the main input panel
        add(testPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Handle the DFA visualization button click
     */
    private void visualizeDfa(ActionEvent e) {
        String regex = regexField.getText().trim();
        boolean minimizeDfa = minimizeDfaCheckbox.isSelected();
        boolean minimizeNfa = minimizeNfaCheckbox.isSelected();
        boolean showDeadStates = showDeadStatesCheckbox.isSelected();
        boolean showSplitView = splitViewCheckbox.isSelected();
        
        // Get the controller from MainFrame
        VisualizationController controller = mainFrame.getVisualizationController();
        controller.visualizeDfa(regex, minimizeDfa, minimizeNfa, showDeadStates, showSplitView);
    }
    
    /**
     * Handle the debug button click
     */
    private void debugRegex(ActionEvent e) {
        String regex = regexField.getText().trim();
        
        // Get the controller from MainFrame
        VisualizationController controller = mainFrame.getVisualizationController();
        controller.debugRegex(regex);
    }
    
    /**
     * Handle the test string button click
     */
    private void testString(ActionEvent e) {
        String testString = testStringField.getText().trim();
        
        // Get the controller from MainFrame
        VisualizationController controller = mainFrame.getVisualizationController();
        controller.testString(testString);
    }
    
    /**
     * Handle the animate string button click
     */
    private void animateString(ActionEvent e) {
        String testString = testStringField.getText().trim();
        
        // Get the controller from MainFrame
        VisualizationController controller = mainFrame.getVisualizationController();
        controller.animateString(testString);
    }
    
    /**
     * Handle toggling grid snap for states
     */
    private void handleGridSnapToggle(ActionEvent e) {
        boolean gridSnap = gridSnapCheckbox.isSelected();
        
        // Update grid snap setting in visualizers
        mainFrame.getDfaVisualizer().setGridSnapEnabled(gridSnap);
        
        // Update any current DFA visualization
        VisualizationPanel dfaPanel = mainFrame.getDfaPanel();
        if (dfaPanel != null) {
            JComponent dfaComponent = (JComponent) dfaPanel.getClientProperty("visualComponent");
            if (dfaComponent instanceof com.mxgraph.swing.mxGraphComponent) {
                com.mxgraph.swing.mxGraphComponent graphComponent = (com.mxgraph.swing.mxGraphComponent) dfaComponent;
                graphComponent.setGridVisible(gridSnap);
                
                // Configure grid size and enable snapping
                if (gridSnap) {
                    graphComponent.getGraph().setGridSize(20);
                    graphComponent.getGraph().setGridEnabled(true);
                } else {
                    graphComponent.getGraph().setGridEnabled(false);
                }
                
                // Refresh the component to update the grid display
                graphComponent.refresh();
            }
        }
        
        // Update NFA visualization if it exists
        VisualizationPanel nfaPanel = mainFrame.getNfaPanel();
        if (nfaPanel != null) {
            JComponent nfaComponent = (JComponent) nfaPanel.getClientProperty("visualComponent");
            if (nfaComponent instanceof com.mxgraph.swing.mxGraphComponent) {
                com.mxgraph.swing.mxGraphComponent graphComponent = (com.mxgraph.swing.mxGraphComponent) nfaComponent;
                graphComponent.setGridVisible(gridSnap);
                
                // Configure grid size and enable snapping
                if (gridSnap) {
                    graphComponent.getGraph().setGridSize(20);
                    graphComponent.getGraph().setGridEnabled(true);
                } else {
                    graphComponent.getGraph().setGridEnabled(false);
                }
                
                // Refresh the component to update the grid display
                graphComponent.refresh();
            }
        }
        
        if (gridSnap) {
            mainFrame.getStatusPanel().setStatus("Grid snap enabled - Grid size: 20px");
        } else {
            mainFrame.getStatusPanel().setStatus("Grid snap disabled");
        }
    }
    
    /**
     * Handle toggling animation for self-loops
     */
    private void handleAnimateLoopsToggle(ActionEvent e) {
        boolean animateLoops = animateLoopsCheckbox.isSelected();
        
        // Get the controller from MainFrame and set the loop animation state
        VisualizationController controller = mainFrame.getVisualizationController();
        if (controller != null) {
            controller.setLoopAnimationEnabled(animateLoops);
        }
    }
    
    /**
     * Show step-by-step conversion dialog
     */
    private void showStepByStepConversion() {
        String regex = regexField.getText().trim();
        if (regex.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), 
                "Please enter a regular expression first.", 
                "Empty Regex", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Display step-by-step dialog (implementation may vary)
        new StepByStepDialog(mainFrame, regex).display();
    }
    
    /**
     * Show theory popup
     */
    private void showTheoryPopup() {
        // Display theory popup (implementation may vary)
        new TheoryPopupDialog(mainFrame).display();
    }
    
    // Getters for components
    
    public JTextField getRegexField() {
        return regexField;
    }
    
    public JTextField getTestStringField() {
        return testStringField;
    }
    
    public JCheckBox getSplitViewCheckbox() {
        return splitViewCheckbox;
    }
    
    public JCheckBox getMinimizeDfaCheckbox() {
        return minimizeDfaCheckbox;
    }
    
    public JCheckBox getMinimizeNfaCheckbox() {
        return minimizeNfaCheckbox;
    }
    
    public JCheckBox getShowDeadStatesCheckbox() {
        return showDeadStatesCheckbox;
    }
    
    public JCheckBox getGridSnapCheckbox() {
        return gridSnapCheckbox;
    }
    
    public JCheckBox getAnimateLoopsCheckbox() {
        return animateLoopsCheckbox;
    }
    
    public JButton getAnimateButton() {
        return animateButton;
    }
} 