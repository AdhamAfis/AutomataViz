package com.dfavisualizer.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.dfavisualizer.algorithm.DfaMinimizer;
import com.dfavisualizer.algorithm.RegexParser;
import com.dfavisualizer.algorithm.SubsetConstruction;
import com.dfavisualizer.model.DFA;
import com.dfavisualizer.model.NFA;
import com.dfavisualizer.ui.MainFrame;
import com.dfavisualizer.ui.components.VisualizationPanel;
import com.dfavisualizer.ui.components.NfaLessonPanel;
import com.dfavisualizer.ui.AutomataGraph;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxEvent;
import javax.swing.JTextArea;

public class StepByStepDialog {
    private JDialog dialog;
    private MainFrame mainFrame;
    private String regex;
    private String initialInput;
    private boolean exercise;
    private javax.swing.SwingWorker<Void, String> worker;

    public StepByStepDialog(MainFrame mainFrame, String regex) {
        this(mainFrame, regex, "");
    }

    public StepByStepDialog(MainFrame mainFrame, String regex, String initialInput) {
        this(mainFrame, regex, initialInput, false);
    }

    public StepByStepDialog(MainFrame mainFrame, String regex, String initialInput, boolean exercise) {
        this.exercise = exercise;
        this.initialInput = initialInput;
        this.mainFrame = mainFrame;
        this.regex = regex;
        setupDialog();
    }

    private void setupDialog() {
        dialog = new JDialog(mainFrame.getFrame(), "Step-by-Step Conversion Process", true);
        dialog.setSize(1000, 740);
        dialog.setMinimumSize(new java.awt.Dimension(900, 650));
        dialog.setLocationRelativeTo(mainFrame.getFrame());
        dialog.setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel introduction = createStepPanel("Regex → NFA → DFA → minimized DFA",
                "Follow the same expression through each conversion stage.",
                "Stage navigation is below. Inside the NFA stage, Next advances the input trace instead.");
        JTextArea expression = new JTextArea(regex);
        expression.setName("walkthrough.expression");
        expression.setEditable(false); expression.setLineWrap(true); expression.setWrapStyleWord(true);
        expression.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 14));
        StringBuilder guide = new StringBuilder("Expression breakdown\n\n");
        for (com.dfavisualizer.ui.components.ExpressionGuidePanel.Part part :
                com.dfavisualizer.ui.components.ExpressionGuidePanel.parts(regex))
            guide.append(part.text).append(" — ").append(part.description).append("\n\n");
        guide.append("1. Epsilon-NFA: enter input, Start / Reset, then alternate ε-closure and symbol moves.\n")
             .append("2. NFA to DFA: select a state to inspect its exact set of NFA states.\n")
             .append("3. DFA Minimization: select a state to see which original DFA states were merged.\n\n")
             .append("A double ring marks an accepting state. The incoming arrow marks the start.\n")
             .append("Wildcards and negated classes use printable ASCII (space through ~).\n")
             .append("Use explicit classes such as [0-9] instead of shorthand escapes.\n")
             .append("Input is matched as a whole string; anchors and counted repetitions are unsupported.");
        JTextArea instructions = new JTextArea(guide.toString());
        instructions.setEditable(false); instructions.setLineWrap(true); instructions.setWrapStyleWord(true);
        instructions.setCaretPosition(0);
        JPanel content = new JPanel(new BorderLayout(0, 12));
        javax.swing.JScrollPane expressionScroll = new javax.swing.JScrollPane(expression);
        expressionScroll.setPreferredSize(new java.awt.Dimension(100, 70));
        content.add(expressionScroll, BorderLayout.NORTH);
        content.add(new javax.swing.JScrollPane(instructions), BorderLayout.CENTER);
        if (exercise) {
            com.dfavisualizer.ui.components.GuidedExercisePanel activity = new com.dfavisualizer.ui.components.GuidedExercisePanel(input -> {
                JPanel stage = (JPanel) tabbedPane.getComponentAt(1);
                NfaLessonPanel lesson = (NfaLessonPanel) ((BorderLayout) stage.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                lesson.start(input); tabbedPane.setSelectedIndex(1);
            });
            content.add(activity, BorderLayout.SOUTH);
        }
        introduction.add(content, BorderLayout.CENTER);
        tabbedPane.addTab("Introduction", introduction);
        for (String title : new String[] {"Epsilon-NFA", "NFA to DFA", "DFA Minimization"}) {
            tabbedPane.addTab(title, new JPanel());
            tabbedPane.setEnabledAt(tabbedPane.getTabCount() - 1, false);
        }
        JLabel phase = new JLabel("Preparing walkthrough…");
        phase.setName("walkthrough.phase");
        javax.swing.JProgressBar progress = new javax.swing.JProgressBar();
        progress.setIndeterminate(true);
        JPanel loading = new JPanel(new BorderLayout(10, 0));
        loading.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        loading.add(phase, BorderLayout.CENTER);
        loading.add(progress, BorderLayout.EAST);
        javax.swing.JTextField currentExpression = new javax.swing.JTextField(regex);
        currentExpression.setEditable(false);
        currentExpression.setName("walkthrough.currentExpression");
        JPanel sourceRow = new JPanel(new BorderLayout(6, 0));
        sourceRow.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        sourceRow.add(new JLabel("Expression"), BorderLayout.WEST);
        sourceRow.add(currentExpression, BorderLayout.CENTER);
        loading.add(sourceRow, BorderLayout.SOUTH);
        dialog.add(loading, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton previous = new JButton("< Previous stage");
        JButton next = new JButton("Next stage >");
        JButton close = new JButton("Close");
        JButton alternatives = new JButton("Regex alternatives");
        JButton restore = new JButton("Restore expression");
        restore.setVisible(false);
        restore.addActionListener(e -> {
            mainFrame.setExpressionSource(regex);
            mainFrame.getVisualizationController().requestVisualization(regex, mainFrame.getGraphControls().isDfaMinimized(),
                    false, mainFrame.getGraphControls().isHighlightDead(), mainFrame.getGraphControls().getCompareToggle().isSelected());
            dialog.dispose();
            javax.swing.SwingUtilities.invokeLater(() -> new StepByStepDialog(mainFrame, regex, initialInput).display());
        });
        Runnable updateNavigation = () -> {
            alternatives.setEnabled(tabbedPane.getSelectedIndex() > 0 && tabbedPane.isEnabledAt(tabbedPane.getSelectedIndex()));
            previous.setEnabled(tabbedPane.getSelectedIndex() > 0);
            next.setEnabled(tabbedPane.getSelectedIndex() < 3
                    && tabbedPane.isEnabledAt(tabbedPane.getSelectedIndex() + 1));
        };
        tabbedPane.addChangeListener(e -> updateNavigation.run());
        updateNavigation.run();
        previous.addActionListener(e -> tabbedPane.setSelectedIndex(Math.max(0, tabbedPane.getSelectedIndex() - 1)));
        next.addActionListener(e -> tabbedPane.setSelectedIndex(Math.min(3, tabbedPane.getSelectedIndex() + 1)));
        close.addActionListener(e -> dialog.dispose());
        alternatives.addActionListener(e -> {
            JPanel stage = (JPanel) tabbedPane.getSelectedComponent();
            VisualizationPanel visualization = (VisualizationPanel) ((BorderLayout) stage.getLayout()).getLayoutComponent(BorderLayout.CENTER);
            AutomataGraph selected = (AutomataGraph) visualization.getVisualizationComponent();
            new RegexAlternativesDialog(dialog, selected, new String[] {regex, currentExpression.getText()}, choice -> {
                currentExpression.setText(choice); currentExpression.setCaretPosition(0);
                expression.setText(choice); expression.setCaretPosition(0);
                StringBuilder breakdown = new StringBuilder("Selected equivalent expression. The diagrams retain the existing construction.\n")
                        .append("Original expression: ").append(regex).append("\n\n");
                for (com.dfavisualizer.ui.components.ExpressionGuidePanel.Part part :
                        com.dfavisualizer.ui.components.ExpressionGuidePanel.parts(choice))
                    breakdown.append(part.text).append(" — ").append(part.description).append("\n\n");
                instructions.setText(breakdown.toString()); instructions.setCaretPosition(0);
                phase.setText("Verified equivalent expression selected; graph retained");
                mainFrame.setExpressionSource(choice);
                mainFrame.getStatusPanel().setStatus("Verified lesson expression selected. Visualize DFA to rebuild the main workspace.");
            }).setVisible(true);
        });
        buttons.add(previous); buttons.add(next); buttons.add(alternatives); buttons.add(restore); buttons.add(close);
        dialog.add(tabbedPane, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        boolean gridSnap = mainFrame.getDfaVisualizer().isGridSnap();
        com.dfavisualizer.ui.DfaVisualizer visualizer = new com.dfavisualizer.ui.DfaVisualizer();
        visualizer.setHighlightDeadStates(mainFrame.getDfaVisualizer().isHighlightDeadStates());
        worker = new javax.swing.SwingWorker<Void, String>() {
            private final com.dfavisualizer.algorithm.ConstructionTrace thompson = new com.dfavisualizer.algorithm.ConstructionTrace();
            private final com.dfavisualizer.algorithm.ConstructionTrace subsets = new com.dfavisualizer.algorithm.ConstructionTrace();
            private final com.dfavisualizer.algorithm.ConstructionTrace partitions = new com.dfavisualizer.algorithm.ConstructionTrace();
            private NFA epsilonNfa;
            private SubsetConstruction sc;
            private DFA nonMinimizedDfa, minimizedDfa;
            private final DfaMinimizer minimizer = new DfaMinimizer(partitions);
            private AutomataGraph.Builder originalDrawing, nfaDrawing, minimizedDrawing;

            @Override protected Void doInBackground() {
                publish("Parsing expression…");
                epsilonNfa = new RegexParser(thompson).parse(regex);
                publish("Building DFA state subsets…");
                sc = new SubsetConstruction(subsets);
                nonMinimizedDfa = sc.convertNfaToDfa(epsilonNfa);
                publish("Minimizing DFA…");
                minimizedDfa = minimizer.minimize(nonMinimizedDfa);
                publish("Laying out NFA…");
                nfaDrawing = new com.dfavisualizer.ui.NfaVisualizer().prepareNfa(epsilonNfa);
                publish("Laying out DFA…");
                originalDrawing = visualizer.prepareDfa(nonMinimizedDfa);
                publish("Laying out minimized DFA…");
                minimizedDrawing = visualizer.prepareDfa(minimizedDfa);
                return null;
            }
            @Override protected void process(java.util.List<String> stages) {
                if (!isCancelled()) phase.setText(stages.get(stages.size() - 1));
            }
            @Override protected void done() {
                if (isCancelled() || !dialog.isDisplayable()) return;
                try {
                    get();
                    JPanel step2Panel = createStepPanel("Step 2: Epsilon-NFA",
                        "The initial NFA includes epsilon transitions (shown in red).",
                        "This NFA has " + epsilonNfa.getStates().size() + " states and uses epsilon transitions " +
                        "to connect the different parts of the regex.");


                    JComponent nonMinDfaViz = new AutomataGraph(originalDrawing, gridSnap);
                    NfaLessonPanel lesson = new NfaLessonPanel(epsilonNfa, sc.getStateSubsets(), nfaDrawing, gridSnap);
                    lesson.start(initialInput);
                    step2Panel.add(lesson, BorderLayout.CENTER);

                    JPanel step3Panel = createStepPanel("Step 3: Subset Construction (NFA to DFA)",
                        "The NFA is converted to a DFA using the subset construction algorithm.",
                        "Each state in the DFA corresponds to a set of NFA states. " +
                        "The resulting DFA has " + nonMinimizedDfa.getStates().size() + " states and no epsilon transitions.");
                    step3Panel.add(createVisualization("DFA", nonMinDfaViz), BorderLayout.CENTER);
                    addStateInspector(step3Panel, (AutomataGraph) nonMinDfaViz, name -> {
                        for (java.util.Map.Entry<DFA.State, java.util.Set<Integer>> entry : sc.getStateSubsets().entrySet()) {
                            if (entry.getKey().getName().equals(name)) return name + " = " + NfaLessonPanel.names(entry.getValue())
                                + (entry.getValue().isEmpty() ? ". Empty subset: no NFA paths remain (dead state)."
                                : ". One DFA state represents all these NFA states after ε-closure.")
                                + "\nAccepting if any NFA member is accepting. These sets apply before minimization.";
                        }
                        return "Select a DFA state.";
                    });

                    JComponent minDfaViz = new AutomataGraph(minimizedDrawing, gridSnap);

                    JPanel step4Panel = createStepPanel("Step 4: DFA Minimization (Hopcroft's Algorithm)",
                        "The DFA is minimized by combining equivalent states.",
                        "The minimized DFA has " + minimizedDfa.getStates().size() + " states, " +
                        "reduced from " + nonMinimizedDfa.getStates().size() + " states in the non-minimized DFA.");
                    step4Panel.add(createVisualization("Minimized DFA", minDfaViz), BorderLayout.CENTER);
                    addStateInspector(step4Panel, (AutomataGraph) minDfaViz, name -> {
                        java.util.Set<DFA.State> members = minimizer.getStateGroups().get(new DFA.State(name));
                        if (members == null) return "Select a minimized state.";
                        java.util.List<String> names = new java.util.ArrayList<>();
                        for (DFA.State member : members) names.add(member.getName());
                        java.util.Collections.sort(names);
                        return name + " represents original DFA states {" + String.join(", ", names) + "}.\n"
                                + (names.size() > 1 ? "These states are equivalent: every remaining suffix gives the same acceptance result."
                                : "This state remains separate; no other original state is equivalent.");
                    });

                    addConstructionSteps(step2Panel, "Thompson construction", thompson);
                    addConstructionSteps(step3Panel, "Subset construction", subsets);
                    addConstructionSteps(step4Panel, "Partition refinement", partitions);
                    enableExercise(introduction);
                    tabbedPane.setComponentAt(1, step2Panel);
                    tabbedPane.setComponentAt(2, step3Panel);
                    tabbedPane.setComponentAt(3, step4Panel);
                    for (int i = 1; i < 4; i++) tabbedPane.setEnabledAt(i, true);
                    updateNavigation.run();
                    for (int stage = 1; stage < 4; stage++) {
                        final int editedStage = stage;
                        JPanel step = (JPanel) tabbedPane.getComponentAt(stage);
                        VisualizationPanel visualization = (VisualizationPanel) ((BorderLayout) step.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                        AutomataGraph drawing = (AutomataGraph) visualization.getVisualizationComponent();
                        drawing.addPropertyChangeListener("automaton.edited", event -> {
                            phase.setText("Custom graph — updating expression…");
                            mainFrame.synchronizeEditedGraph(drawing, generated -> {
                                if (!dialog.isDisplayable()) return;
                                expression.setText(generated); expression.setCaretPosition(0);
                                currentExpression.setText(generated); currentExpression.setCaretPosition(0);
                                phase.setText("Custom graph — expression updated to match edited transitions");
                            }, error -> {
                                if (!dialog.isDisplayable()) return;
                                phase.setText("Custom graph — could not update expression");
                                phase.setToolTipText(error);
                            });
                            restore.setVisible(true);
                            for (int other = 0; other < 4; other++) tabbedPane.setEnabledAt(other, other == editedStage);
                            previous.setEnabled(false); next.setEnabled(false);
                            java.awt.Component header = ((BorderLayout) step.getLayout()).getLayoutComponent(BorderLayout.NORTH);
                            if (header != null) step.remove(header);
                            if (editedStage != 1) {
                                step.remove(((BorderLayout) step.getLayout()).getLayoutComponent(BorderLayout.SOUTH));
                                JTextArea notice = new JTextArea("Custom DFA: the original subset and merge explanations no longer apply. "
                                        + "Reconnect endpoints to experiment, or Restore expression to return to the conversion lesson.");
                                notice.setEditable(false); notice.setLineWrap(true); notice.setWrapStyleWord(true);
                                step.add(notice, BorderLayout.SOUTH);
                            }
                            step.revalidate(); step.repaint();
                        });
                    }
                    phase.setText("Ready — " + epsilonNfa.getStates().size() + " NFA states, "
                            + nonMinimizedDfa.getStates().size() + " DFA states");
                } catch (java.util.concurrent.CancellationException ex) {
                    return;
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    phase.setText("Could not prepare walkthrough");
                    instructions.setText("Could not prepare this expression:\n\n" + cause.getMessage()
                            + "\n\nClose this window, correct the expression and open Learn again.\n"
                            + "Supported: literals, |, *, +, ?, ordinary groups, character classes, and escaped punctuation.");
                    tabbedPane.setSelectedIndex(0);
                    next.setEnabled(false);
                } finally {
                    progress.setVisible(false);
                }
            }
        };
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { worker.cancel(true); }
        });
    }

    private void enableExercise(java.awt.Container container) {
        for (java.awt.Component component : container.getComponents()) {
            if ("exercise.trace".equals(component.getName())) component.setEnabled(true);
            if (component instanceof java.awt.Container) enableExercise((java.awt.Container) component);
        }
    }

    private void addConstructionSteps(JPanel stage, String title, com.dfavisualizer.algorithm.ConstructionTrace trace) {
        JPanel header = (JPanel) ((BorderLayout) stage.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        JPanel row = new JPanel(new BorderLayout(6, 4));
        java.awt.Component detail = ((BorderLayout) header.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        header.remove(detail); row.add(detail, BorderLayout.CENTER);
        JButton steps = new JButton("Construction steps…");
        steps.setToolTipText("Replay actual " + title.toLowerCase() + " decisions");
        steps.addActionListener(e -> new ConstructionStepsDialog(dialog, title, trace).setVisible(true));
        row.add(steps, BorderLayout.EAST); header.add(row, BorderLayout.SOUTH);
    }

    private JPanel createStepPanel(String title, String description, String details) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("<html><h2>" + title + "</h2></html>");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        headerPanel.add(titleLabel, BorderLayout.NORTH);

        JLabel descLabel = new JLabel("<html><p>" + description + "</p></html>");
        descLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        headerPanel.add(descLabel, BorderLayout.CENTER);

        JLabel detailsLabel = new JLabel("<html><p><i>" + details + "</i></p></html>");
        headerPanel.add(detailsLabel, BorderLayout.SOUTH);

        panel.add(headerPanel, BorderLayout.NORTH);

        return panel;
    }

    private void addStateInspector(JPanel step, AutomataGraph graph, java.util.function.Function<String, String> explain) {
        JPanel inspector = new JPanel(new BorderLayout(8, 4));
        javax.swing.JComboBox<String> selector = new javax.swing.JComboBox<>();
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Object object : graph.getGraph().getChildVertices(graph.getGraph().getDefaultParent()))
            names.add(((mxCell) object).getValue().toString());
        java.util.Collections.sort(names);
        for (String name : names) selector.addItem(name);
        JPanel choice = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        choice.add(new JLabel("Inspect state")); choice.add(selector);
        JTextArea details = new JTextArea(3, 40);
        details.setEditable(false); details.setLineWrap(true); details.setWrapStyleWord(true);
        selector.addActionListener(e -> {
            String name = (String) selector.getSelectedItem();
            details.setText(explain.apply(name)); details.setCaretPosition(0);
            mxCell cell = graph.state(name);
            if (graph.getGraph().getSelectionCell() != cell) graph.getGraph().setSelectionCell(cell);
        });
        graph.getGraph().getSelectionModel().addListener(mxEvent.CHANGE, (sender, event) -> {
            Object selected = graph.getGraph().getSelectionCell();
            if (selected instanceof mxCell && ((mxCell) selected).isVertex())
                selector.setSelectedItem(((mxCell) selected).getValue().toString());
        });
        selector.setSelectedIndex(-1);
        if (!names.isEmpty()) selector.setSelectedIndex(0);
        inspector.add(choice, BorderLayout.NORTH);
        inspector.add(new javax.swing.JScrollPane(details), BorderLayout.CENTER);
        step.add(inspector, BorderLayout.SOUTH);
    }

    private VisualizationPanel createVisualization(String title, JComponent component) {
        VisualizationPanel panel = new VisualizationPanel(title);
        panel.setVisualizationComponent(component);
        return panel;
    }

    public void display() {
        if (worker.getState() == javax.swing.SwingWorker.StateValue.PENDING) worker.execute();
        dialog.setVisible(true);
    }
}
