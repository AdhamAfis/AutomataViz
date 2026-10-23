package com.dfavisualizer.ui;

import javax.swing.SwingUtilities;

public class MainApp {
    
    public static void main(String[] args) {
        UiTheme.installPreferred();

        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.display();
        });
    }
} 