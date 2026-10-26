package com.dfavisualizer.ui.components;

import java.awt.BorderLayout;
import java.util.prefs.Preferences;
import javax.swing.*;

public final class GettingStartedPanel extends JPanel {
    private final Preferences preferences;
    public GettingStartedPanel(Preferences preferences) {
        super(new BorderLayout(10, 0));
        this.preferences = preferences;
        setName("gettingStarted");
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Getting started"),
            BorderFactory.createEmptyBorder(2, 6, 4, 6)));
        JTextArea hint = new JTextArea("1. Enter an expression (or choose Examples), then click Visualize DFA.\n"
            + "2. Enter a test string and choose Animate.\n"
            + "3. Open Learn → Step-by-Step conversion to explore the construction.");
        hint.setEditable(false); hint.setFocusable(false); hint.setOpaque(false);
        hint.setFont(UIManager.getFont("Label.font")); hint.setLineWrap(true); hint.setWrapStyleWord(true);
        hint.setRows(3); hint.setColumns(45);
        JButton dismiss = new JButton("Dismiss");
        dismiss.addActionListener(e -> remember(false));
        add(hint, BorderLayout.CENTER); add(dismiss, BorderLayout.EAST);
        setVisible(!preferences.getBoolean("gettingStarted.dismissed", false));
    }
    public void showAgain() { remember(true); }
    private void remember(boolean visible) {
        setVisible(visible);
        preferences.putBoolean("gettingStarted.dismissed", !visible);
        try { preferences.flush(); }
        catch (java.util.prefs.BackingStoreException ex) {
            System.err.println("Could not save getting-started preference: " + ex.getMessage());
        }
        if (getParent() != null) { getParent().revalidate(); getParent().repaint(); }
    }
}
