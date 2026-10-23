package com.dfavisualizer.ui.components;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import com.dfavisualizer.model.DFA;
import com.dfavisualizer.model.NFA;
import com.dfavisualizer.ui.AutomataGraph;
import com.dfavisualizer.ui.NfaVisualizer;
import com.dfavisualizer.ui.UiTheme;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxConstants;

/** Manual NFA execution keeps symbol moves and epsilon closure as separate teaching steps. */
public class NfaLessonPanel extends VisualizationPanel {
    private final NFA nfa;
    private final Map<DFA.State, Set<Integer>> subsets;
    private final AutomataGraph graph;
    private final JTextField inputField = new JTextField(12);
    private final JTextArea description = new JTextArea(4, 25);
    private final JButton next = new JButton("Next: ε-closure");
    private final JButton back = new JButton("Back");
    private final JCheckBox focus = new JCheckBox("Focus active states", true);
    private final List<Snapshot> history = new ArrayList<>();
    private String input = "";
    private Snapshot current;
    private boolean inputEdited;
    private boolean graphEdited;

    public NfaLessonPanel(NFA nfa, Map<DFA.State, Set<Integer>> subsets) {
        this(nfa, subsets, new NfaVisualizer().prepareNfa(nfa), false);
    }

    public NfaLessonPanel(NFA nfa, Map<DFA.State, Set<Integer>> subsets,
            AutomataGraph.Builder drawing, boolean gridSnap) {
        super("ε-NFA");
        this.nfa = nfa;
        this.subsets = subsets;
        graph = new AutomataGraph(drawing, gridSnap);
        setVisualizationComponent(graph);
        JPanel footer = new JPanel(new BorderLayout(6, 4));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JButton start = new JButton("Start / Reset");
        controls.add(new JLabel("Input")); controls.add(inputField); controls.add(start);
        controls.add(back); controls.add(next); controls.add(focus);
        start.addActionListener(e -> start(inputField.getText()));
        inputField.addActionListener(e -> start(inputField.getText()));
        inputField.setName("nfa.input");
        inputField.getAccessibleContext().setAccessibleName("Input for NFA lesson");
        back.addActionListener(e -> back());
        next.addActionListener(e -> next());
        focus.addActionListener(e -> { render(); if (!focus.isSelected()) fitView(); });
        description.setEditable(false);
        description.setLineWrap(true); description.setWrapStyleWord(true);
        description.setFont(javax.swing.UIManager.getFont("Label.font").deriveFont(13f));
        description.setBackground(javax.swing.UIManager.getColor("TextArea.background"));
        footer.add(controls, BorderLayout.NORTH);
        footer.add(new javax.swing.JScrollPane(description), BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        graph.addPropertyChangeListener("automaton.edited", e -> {
            graphEdited = true;
            start(inputField.getText());
        });
        start("");
        inputField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void edited() { inputEdited = true; render(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { edited(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { edited(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { edited(); }
        });
    }

    public void start(String input) {
        this.input = input;
        inputField.setText(input);
        inputEdited = false;
        history.clear();
        current = new Snapshot(Set.of(nfa.getStartState()), 0, true, new ArrayList<>(),
                "Start with {q" + nfa.getStartState() + "}. Next follow ε-transitions without consuming input.");
        render();
    }

    public void next() {
        if (inputEdited || current == null || (!current.closurePending && (current.position == input.length() || current.active.isEmpty()))) return;
        history.add(current);
        Set<Integer> active;
        List<mxCell> edges = new ArrayList<>();
        int position = current.position;
        String message;
        if (current.closurePending) {
            active = nfa.epsilonClosure(current.active);
            for (Map.Entry<NFA.NFATransition, Set<Integer>> entry : nfa.getTransitions().entrySet()) {
                if (entry.getKey().getSymbol() == NFA.EPSILON && active.contains(entry.getKey().getState()))
                    for (int target : entry.getValue()) edges.add(graph.transition("q" + entry.getKey().getState(), "q" + target, NFA.EPSILON));
            }
            message = "ε-closure: " + names(current.active) + " → " + names(active) + ". No input consumed. " + mapping(active);
        } else {
            char symbol = input.charAt(position++);
            active = nfa.move(current.active, symbol);
            for (int from : current.active) {
                Set<Integer> targets = nfa.getTransitions().get(new NFA.NFATransition(from, symbol));
                if (targets != null) for (int target : targets) edges.add(graph.transition("q" + from, "q" + target, symbol));
            }
            message = "Read '" + AutomataGraph.symbolLabel(symbol) + "': " + names(current.active) + " → " + names(active)
                    + ". One character consumed. Next take ε-closure before reading again.";
        }
        boolean pending = !current.closurePending && !active.isEmpty();
        if (!pending && position == input.length()) {
            Set<Integer> accepting = new HashSet<>(active); accepting.retainAll(nfa.getAcceptStates());
            message += accepting.isEmpty() ? " REJECTED: all input consumed; no active state is accepting."
                    : " ACCEPTED: all input consumed and active set includes accepting " + names(accepting) + ".";
        } else if (!pending && active.isEmpty()) message += " REJECTED: no active paths remain; input cannot be completed.";
        current = new Snapshot(active, position, pending, edges, message);
        render();
    }

    public void back() {
        if (inputEdited || history.isEmpty()) return;
        current = history.remove(history.size() - 1);
        render();
    }

    private String mapping(Set<Integer> active) {
        if (graphEdited) return "Custom NFA: the original DFA subset mapping no longer applies.";
        for (Map.Entry<DFA.State, Set<Integer>> entry : subsets.entrySet())
            if (entry.getValue().equals(active)) return "This set is DFA state " + entry.getKey() + " before minimization.";
        return "An NFA can have several active states at once.";
    }

    private void render() {
        graph.getGraph().getModel().beginUpdate();
        try {
            graph.restoreStyles();
            Set<mxCell> active = new HashSet<>();
            for (int state : current.active) active.add(graph.state("q" + state));
            graph.focusOn(active, focus.isSelected());
            for (mxCell state : active) {
                graph.getGraph().setCellStyles(mxConstants.STYLE_STROKECOLOR, UiTheme.ACTIVE_STATE, new Object[] {state});
                graph.getGraph().setCellStyles(mxConstants.STYLE_STROKEWIDTH, "4", new Object[] {state});
            }
            for (mxCell edge : current.edges) if (edge != null) {
                graph.getGraph().setCellStyles(mxConstants.STYLE_OPACITY, "100", new Object[] {edge});
                graph.getGraph().setCellStyles(mxConstants.STYLE_TEXT_OPACITY, "100", new Object[] {edge});
                graph.getGraph().setCellStyles(mxConstants.STYLE_STROKECOLOR, UiTheme.ACTIVE_EDGE, new Object[] {edge});
                graph.getGraph().setCellStyles(mxConstants.STYLE_STROKEWIDTH, "3", new Object[] {edge});
            }
        } finally { graph.getGraph().getModel().endUpdate(); }
        description.setText("Input " + current.position + " / " + input.length() + " · Active states " + names(current.active) + "\n" + current.message);
        description.setCaretPosition(0);
        if (inputEdited) {
            description.setText("Input edited. Choose Start / Reset to begin a new trace.");
            next.setEnabled(false); back.setEnabled(false);
            return;
        }
        next.setText(current.closurePending ? "Next: ε-closure" : "Next: read symbol");
        next.setEnabled(current.closurePending || current.position < input.length() && !current.active.isEmpty());
        back.setEnabled(!history.isEmpty());
    }

    public Set<Integer> activeStates() { return java.util.Collections.unmodifiableSet(current.active); }
    public int consumed() { return current.position; }
    public String explanation() { return description.getText(); }
    public static String names(Set<Integer> states) {
        List<String> names = new ArrayList<>();
        for (int state : new TreeSet<>(states)) names.add("q" + state);
        return "{" + String.join(", ", names) + "}";
    }
    private static final class Snapshot {
        final Set<Integer> active;
        final int position;
        final boolean closurePending;
        final List<mxCell> edges;
        final String message;
        Snapshot(Set<Integer> active, int position, boolean pending, List<mxCell> edges, String message) {
            this.active = new TreeSet<>(active); this.position = position; this.closurePending = pending;
            this.edges = edges; this.message = message;
        }
    }
}
