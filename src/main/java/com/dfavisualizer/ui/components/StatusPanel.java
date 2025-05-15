package com.dfavisualizer.ui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Panel for displaying status messages and simulation results.
 */
public class StatusPanel extends JPanel {
    private JTextArea statusArea;
    
    /**
     * Constructor - initializes the status panel
     */
    public StatusPanel() {
        setLayout(new BorderLayout());
        
        // Create the status text area
        statusArea = new JTextArea("Ready");
        statusArea.setEditable(false);
        
        // Add to a scroll pane
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setPreferredSize(new Dimension(0, 100));
        
        // Add to the panel
        add(statusScroll, BorderLayout.CENTER);
    }
    
    /**
     * Set the status text
     * 
     * @param text The text to display
     */
    public void setStatus(String text) {
        statusArea.setText(text);
    }
    
    /**
     * Append text to the status area
     * 
     * @param text The text to append
     */
    public void appendStatus(String text) {
        statusArea.append(text + "\n");
        // Scroll to the bottom
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }
    
    /**
     * Clear the status area
     */
    public void clearStatus() {
        statusArea.setText("");
    }
    
    /**
     * Get the status text area
     * 
     * @return The status text area
     */
    public JTextArea getStatusArea() {
        return statusArea;
    }
} 