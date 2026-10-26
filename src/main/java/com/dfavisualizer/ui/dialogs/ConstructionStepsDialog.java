package com.dfavisualizer.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.*;
import com.dfavisualizer.algorithm.ConstructionTrace;
import com.dfavisualizer.ui.AutomataGraph;
import com.dfavisualizer.ui.DfaVisualizer;
import com.dfavisualizer.ui.NfaVisualizer;
import com.dfavisualizer.ui.components.VisualizationPanel;

/** Replays algorithm snapshots without allowing edits to lesson history. */
public final class ConstructionStepsDialog extends JDialog {
    private final ConstructionTrace trace;
    private final VisualizationPanel canvas = new VisualizationPanel("Construction snapshot");
    private final JTextArea explanation = new JTextArea(6, 50);
    private final JLabel position = new JLabel();
    private final JButton back = new JButton("Back"), next = new JButton("Next");
    private SwingWorker<AutomataGraph.Builder, Void> worker;
    private int index;

    public ConstructionStepsDialog(Window owner, String title, ConstructionTrace trace) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.trace = trace;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 700); setMinimumSize(new java.awt.Dimension(700, 550));
        setLocationRelativeTo(owner);
        explanation.setEditable(false); explanation.setLineWrap(true); explanation.setWrapStyleWord(true);
        explanation.setName("construction.explanation");
        explanation.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        add(new JScrollPane(explanation), BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton close = new JButton("Close");
        controls.add(back); controls.add(position); controls.add(next); controls.add(close);
        back.addActionListener(e -> showStep(index - 1));
        next.addActionListener(e -> showStep(index + 1));
        close.addActionListener(e -> dispose());
        add(controls, BorderLayout.SOUTH);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { if (worker != null) worker.cancel(true); }
        });
        showStep(0);
    }

    private void showStep(int selected) {
        if (selected < 0 || selected >= trace.steps().size()) return;
        if (worker != null) worker.cancel(true);
        index = selected;
        ConstructionTrace.Step step = trace.steps().get(index);
        explanation.setText(step.explanation + (trace.isTruncated()
            ? "\nHistory capped for this expression. Use a smaller example for a full trace; the completed graph remains available in the lesson." : "")); explanation.setCaretPosition(0);
        position.setText("Step " + (index + 1) + " / " + trace.steps().size()
            + (trace.isTruncated() ? " (partial history)" : ""));
        back.setEnabled(index > 0); next.setEnabled(index + 1 < trace.steps().size());
        canvas.setVisualizationComponent(new JLabel("Preparing this snapshot…", SwingConstants.CENTER));
        worker = new SwingWorker<AutomataGraph.Builder, Void>() {
            @Override protected AutomataGraph.Builder doInBackground() {
                return step.nfa != null ? new NfaVisualizer().prepareNfa(step.nfa) : new DfaVisualizer().prepareDfa(step.dfa);
            }
            @Override protected void done() {
                if (this != worker || isCancelled()) return;
                try {
                    AutomataGraph graph = new AutomataGraph(get(), false);
                    graph.getGraph().setCellsLocked(true);
                    graph.getGraph().setCellsDisconnectable(false);
                    canvas.setVisualizationComponent(graph);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    canvas.setVisualizationComponent(new JLabel("Could not draw snapshot: " + cause.getMessage()));
                }
            }
        };
        worker.execute();
    }
}
