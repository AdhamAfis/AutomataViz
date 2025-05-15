package com.dfavisualizer.ui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main entry point for the DFA Visualizer application.
 * This class initializes and launches the application.
 */
public class MainApp {
    
    /**
     * Main method to start the application
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Set the look and feel to the system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Launch the application on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.display();
        });
    }
} 