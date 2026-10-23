package com.dfavisualizer.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import com.dfavisualizer.ui.UiTheme;

public class LegendPanel extends JPanel {
    public LegendPanel() {
        setLayout(new BorderLayout(12, 0));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JLabel title = new JLabel("Legend");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
        add(title, BorderLayout.WEST);
        JPanel items = new JPanel(new GridLayout(2, 4, 8, 4));
        addLegendItem(items, UiTheme.DFA_FILL, "DFA state");
        addLegendItem(items, UiTheme.NFA_FILL, "NFA state");
        addLegendItem(items, UiTheme.START_FILL, "Start state");
        addLegendItem(items, UiTheme.ACCEPT_FILL, "Accept (double ring)");
        addLegendItem(items, UiTheme.DEAD_FILL, "Dead state");
        addLegendItem(items, UiTheme.LOOP, "Self-loop");
        addLegendItem(items, UiTheme.EPSILON, "Epsilon transition");
        addLegendItem(items, UiTheme.ACTIVE_EDGE, "Active transition");
        add(items, BorderLayout.CENTER);
        setToolTipText("Drag states to rearrange. Ctrl+wheel zooms; right-click+drag pans. An incoming green arrow marks the start; double rings mark accepting states; amber marks the active transition.");
    }

    private void addLegendItem(JPanel parent, String color, String description) {
        JPanel swatch = new JPanel();
        swatch.setBackground(Color.decode(color));
        swatch.setPreferredSize(new Dimension(10, 10));
        swatch.setBorder(BorderFactory.createLineBorder(UiTheme.MUTED));
        JLabel label = new JLabel(description);
        label.setFont(label.getFont().deriveFont(11f));
        label.setForeground(UiTheme.MUTED);
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.add(swatch);
        item.add(label);
        parent.add(item);
    }
}
