package com.dfavisualizer.ui.components;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import com.dfavisualizer.ui.MainFrame;
import com.dfavisualizer.ui.UiTheme;
import com.mxgraph.swing.mxGraphComponent;

/** Graph choices stay with the graph; occasional canvas preferences live in a menu. */
public class GraphControlsPanel extends JPanel {
    private final JToggleButton compare = new JToggleButton("Compare NFA / DFA");
    private final JComboBox<String> variant = new JComboBox<>(new String[] {"Original", "Minimized"});
    private final JCheckBoxMenuItem highlightDead = new JCheckBoxMenuItem("Highlight dead states in DFA", true);
    private final JCheckBoxMenuItem gridSnap = new JCheckBoxMenuItem("Snap states to grid", false);
    private final JButton alternatives = new JButton("Regex alternatives");
    private boolean syncing;

    public GraphControlsPanel(MainFrame app) {
        super(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        setOpaque(false);
        UiTheme.quietButton(compare);
        compare.setToolTipText("Show the NFA and DFA side by side");
        compare.addActionListener(e -> app.updateVisualizationLayout(compare.isSelected()));
        variant.setSelectedIndex(1);
        variant.setEnabled(false);
        variant.getAccessibleContext().setAccessibleName("DFA representation");
        variant.setToolTipText("Compare the DFA before and after equivalent states are merged");
        variant.addActionListener(e -> {
            if (!syncing) app.getVisualizationController().setDfaMinimized(isDfaMinimized());
        });
        JLabel label = new JLabel("DFA");
        label.setLabelFor(variant);
        label.setForeground(UiTheme.MUTED);
        JPopupMenu menu = new JPopupMenu();
        menu.add(highlightDead); menu.add(gridSnap);
        highlightDead.addActionListener(e -> {
            app.getDfaVisualizer().setHighlightDeadStates(highlightDead.isSelected());
            app.getVisualizationController().refreshDfaView();
        });
        gridSnap.addActionListener(e -> {
            boolean snap = gridSnap.isSelected();
            app.getDfaVisualizer().setGridSnapEnabled(snap);
            app.getNfaVisualizer().setGridSnap(snap);
            for (VisualizationPanel panel : new VisualizationPanel[] {app.getDfaPanel(), app.getNfaPanel()}) {
                if (panel.getVisualizationComponent() instanceof mxGraphComponent) {
                    mxGraphComponent graph = (mxGraphComponent) panel.getVisualizationComponent();
                    graph.setGridVisible(snap);
                    graph.getGraph().setGridSize(20);
                    graph.getGraph().setGridEnabled(snap);
                    graph.refresh();
                }
            }
        });
        JButton options = new JButton("View options");
        options.setComponentPopupMenu(menu);
        options.setToolTipText("Dead-state highlighting and grid snapping");
        options.addActionListener(e -> menu.show(options, 0, options.getHeight()));
        UiTheme.quietButton(options);
        alternatives.setEnabled(false);
        alternatives.addActionListener(e -> app.showRegexAlternatives());
        UiTheme.quietButton(alternatives);
        add(compare); add(label); add(variant); add(alternatives); add(options);
    }

    public boolean isDfaMinimized() { return variant.getSelectedIndex() == 1; }
    public boolean isHighlightDead() { return highlightDead.isSelected(); }
    public JToggleButton getCompareToggle() { return compare; }
    public JComboBox<String> getDfaVariant() { return variant; }
    public JCheckBoxMenuItem getHighlightDeadItem() { return highlightDead; }
    public JCheckBoxMenuItem getGridSnapItem() { return gridSnap; }

    public void showDfaVariant(boolean minimized, boolean visible) {
        syncing = true;
        try { variant.setSelectedIndex(minimized ? 1 : 0); }
        finally { syncing = false; }
        variant.setEnabled(visible);
        alternatives.setEnabled(visible);
    }
}
