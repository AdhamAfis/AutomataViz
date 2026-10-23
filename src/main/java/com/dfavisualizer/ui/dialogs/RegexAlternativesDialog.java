package com.dfavisualizer.ui.dialogs;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.*;
import com.dfavisualizer.algorithm.AutomatonToRegex;
import com.dfavisualizer.algorithm.SubsetConstruction;
import com.dfavisualizer.model.NFA;
import com.dfavisualizer.ui.AutomataGraph;

/** Compares two verified expressions for an immutable snapshot of the selected graph. */
public final class RegexAlternativesDialog extends JDialog {
    private final JLabel status = new JLabel("Finding and checking equivalent expressions…");
    private final JTextArea shortest = expressionArea("alternatives.shortest");
    private final JTextArea readable = expressionArea("alternatives.readable");
    private final JLabel shortestCheck = new JLabel("Checking…"), readableCheck = new JLabel("Checking…");
    private final java.util.List<JButton> actions = new ArrayList<>();
    private final SwingWorker<AutomatonToRegex.Alternatives, Void> worker;

    public RegexAlternativesDialog(Window owner, AutomataGraph graph, String[] preferred, Consumer<String> apply) {
        super(owner, "Regex alternatives", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        JPanel body = new JPanel(new GridLayout(2, 1, 0, 12));
        body.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        body.add(choice("Shortest found", "Fewest characters among the candidates searched.", shortest, shortestCheck, apply));
        body.add(choice("Easiest to read", "Favors shallow nesting and fewer alternative branches; this is a heuristic.", readable, readableCheck, apply));
        add(body, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(8, 8));
        footer.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        status.setName("alternatives.status");
        footer.add(status, BorderLayout.CENTER);
        JButton close = new JButton("Close"); close.addActionListener(e -> dispose());
        footer.add(close, BorderLayout.EAST); add(footer, BorderLayout.SOUTH);
        setSize(760, 440); setMinimumSize(new Dimension(580, 360)); setLocationRelativeTo(owner);
        AutomatonToRegex dfaSnapshot = graph.getDfaModel() == null ? null : new AutomatonToRegex(graph.getDfaModel());
        NFA nfaSnapshot = dfaSnapshot == null ? copyNfa(graph.getNfaModel()) : null;
        String[] candidates = preferred.clone();
        worker = new SwingWorker<AutomatonToRegex.Alternatives, Void>() {
            @Override protected AutomatonToRegex.Alternatives doInBackground() {
                AutomatonToRegex converter = dfaSnapshot == null
                        ? new AutomatonToRegex(new SubsetConstruction().convertNfaToDfa(nfaSnapshot)) : dfaSnapshot;
                return converter.alternatives(candidates);
            }
            @Override protected void done() {
                if (isCancelled() || !isDisplayable()) return;
                try {
                    AutomatonToRegex.Alternatives result = get();
                    shortest.setText(result.shortest); readable.setText(result.readable);
                    shortest.setCaretPosition(0); readable.setCaretPosition(0);
                    shortestCheck.setText("Equivalent ✓ · " + result.shortest.length() + " characters");
                    readableCheck.setText("Equivalent ✓ · " + result.readable.length() + " characters");
                    for (JButton action : actions) action.setEnabled(true);
                    status.setText(result.shortest.equals(result.readable)
                            ? "Verified: both goals found the same expression."
                            : "Verified against this graph for every possible input string.");
                } catch (java.util.concurrent.CancellationException ignored) {
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    status.setText("Could not prepare verified alternatives. Hover for details."); status.setToolTipText(cause.getMessage());
                    shortestCheck.setText("Unavailable"); readableCheck.setText("Unavailable");
                }
            }
        };
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { worker.execute(); }
            @Override public void windowClosed(WindowEvent e) { worker.cancel(true); }
        });
    }

    private JTextArea expressionArea(String name) {
        JTextArea area = new JTextArea(3, 40); area.setName(name); area.setEditable(false);
        area.setLineWrap(true); area.setWrapStyleWord(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        return area;
    }

    private JPanel choice(String title, String explanation, JTextArea expression, JLabel check, Consumer<String> apply) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JPanel heading = new JPanel(new BorderLayout());
        JLabel name = new JLabel(title); name.setFont(name.getFont().deriveFont(Font.BOLD));
        heading.add(name, BorderLayout.WEST); heading.add(check, BorderLayout.EAST);
        heading.add(new JLabel(explanation), BorderLayout.SOUTH); panel.add(heading, BorderLayout.NORTH);
        expression.getAccessibleContext().setAccessibleName(title + " expression");
        panel.add(new JScrollPane(expression), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton copy = new JButton("Copy"), use = new JButton("Use expression");
        copy.setName(expression.getName() + ".copy"); use.setName(expression.getName() + ".use");
        copy.addActionListener(e -> {
            try { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(expression.getText()), null); }
            catch (IllegalStateException ex) { status.setText("Clipboard busy; select the expression and copy it manually."); }
        });
        use.addActionListener(e -> { apply.accept(expression.getText()); dispose(); });
        for (JButton button : new JButton[] {copy, use}) { button.setEnabled(false); actions.add(button); buttons.add(button); }
        panel.add(buttons, BorderLayout.SOUTH); return panel;
    }

    private NFA copyNfa(NFA source) {
        if (source == null) throw new IllegalArgumentException("No automaton available");
        NFA copy = new NFA(); Map<Integer, Integer> states = new HashMap<>();
        for (int state : new TreeSet<>(source.getStates())) states.put(state, copy.createState());
        copy.setStartState(states.get(source.getStartState()));
        for (int state : source.getAcceptStates()) copy.addAcceptState(states.get(state));
        for (Map.Entry<NFA.NFATransition, Set<Integer>> entry : source.getTransitions().entrySet())
            for (int target : entry.getValue()) copy.addTransition(states.get(entry.getKey().getState()), entry.getKey().getSymbol(), states.get(target));
        return copy;
    }
}
