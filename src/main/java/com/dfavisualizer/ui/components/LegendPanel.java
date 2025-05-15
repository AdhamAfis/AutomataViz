package com.dfavisualizer.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Panel displaying a legend explaining the color scheme used in the visualization.
 */
public class LegendPanel extends JPanel {
    
    /**
     * Constructor - initializes the legend panel
     */
    public LegendPanel() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 2)); // Reduce vertical spacing
        setBorder(BorderFactory.createTitledBorder("Legend"));
        createLegend();
    }
    
    /**
     * Create the legend content
     */
    private void createLegend() {
        // Add color squares with explanations in a grid layout
        JPanel gridPanel = new JPanel(new GridLayout(0, 3, 10, 2)); // 3 columns, as many rows as needed
        
        // Add items to the grid panel
        addLegendItem(gridPanel, new Color(230, 242, 255), "Regular State");
        addLegendItem(gridPanel, new Color(255, 235, 204), "Accept State");
        addLegendItem(gridPanel, new Color(230, 255, 204), "Start State");
        addLegendItem(gridPanel, new Color(255, 204, 204), "Dead State");
        addLegendItem(gridPanel, Color.decode("#CCCCFF"), "↻ Self-loop State");
        addLegendItem(gridPanel, new Color(255, 0, 0), "Current Transition");
        
        // Add epsilon transition legend for NFA
        addLegendItem(gridPanel, Color.decode("#FF6666"), "ε Epsilon Transition");
        
        // Add the grid panel to the legend panel
        add(gridPanel);
        
        // Add a compact note about animation
        JLabel animationNote = new JLabel("Animation: Red edges = current transition | Blue highlight = self-loops");
        animationNote.setFont(animationNote.getFont().deriveFont(10.0f));
        add(animationNote);
        
        // Add info about minimization
        JLabel minimizationNote = new JLabel("Minimization: DFA and NFA minimization reduces states while preserving behavior");
        minimizationNote.setFont(minimizationNote.getFont().deriveFont(10.0f));
        add(minimizationNote);
        
        // Add a compact note about dragging
        JLabel dragNote = new JLabel("Navigation: Drag states | Ctrl+wheel to zoom | Right-click+drag to pan");
        dragNote.setFont(dragNote.getFont().deriveFont(10.0f));
        add(dragNote);
        
        // Keep legend panel compact
        setPreferredSize(new Dimension(0, 80));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
    }
    
    /**
     * Adds a single colored square with a label to the legend
     * 
     * @param legendPanel The panel to add the item to
     * @param color The color for the square
     * @param description The description text
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
} 