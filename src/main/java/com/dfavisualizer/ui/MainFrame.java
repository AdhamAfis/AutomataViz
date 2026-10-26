package com.dfavisualizer.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import com.dfavisualizer.ui.components.ExpressionGuidePanel;
import com.dfavisualizer.ui.components.GraphControlsPanel;
import com.dfavisualizer.ui.dialogs.ThemeChooserDialog;

import com.dfavisualizer.algorithm.DfaMinimizer;
import com.dfavisualizer.algorithm.NfaMinimizer;
import com.dfavisualizer.algorithm.RegexToDfaConverter;
import com.dfavisualizer.controller.VisualizationController;
import com.dfavisualizer.model.DFA;
import com.dfavisualizer.model.NFA;
import com.dfavisualizer.ui.components.InputPanel;
import com.dfavisualizer.ui.components.LegendPanel;
import com.dfavisualizer.ui.components.StatusPanel;
import com.dfavisualizer.ui.components.VisualizationPanel;

public class MainFrame {
    private JFrame frame;
    private InputPanel inputPanel;
    private VisualizationPanel nfaPanel;
    private VisualizationPanel dfaPanel;
    private StatusPanel statusPanel;
    private LegendPanel legendPanel;
    private JSplitPane visualizationSplitPane;
    private JSplitPane resultsSplitPane;
    private JPanel graphWorkspace;
    private JTabbedPane graphTabs;
    private ExpressionGuidePanel expressionGuide;
    private GraphControlsPanel graphControls;
    private JPanel graphHeader;
    private ThemeChooserDialog themeChooser;
    
    private RegexToDfaConverter converter;
    private DfaVisualizer dfaVisualizer;
    private NfaVisualizer nfaVisualizer;
    private DFA currentDfa;
    private DFA nonMinimizedDfa;
    private NFA currentNfa;
    private NFA minimizedNfa;
    private DfaMinimizer dfaMinimizer;
    private NfaMinimizer nfaMinimizer;
    private VisualizationController visualizationController;
    private javax.swing.SwingWorker<String, Void> expressionWorker;
    private long expressionRevision;
    private String graphSourceExpression = "";
    
    public MainFrame() {
        this.converter = new RegexToDfaConverter();
        this.dfaVisualizer = new DfaVisualizer();
        this.nfaVisualizer = new NfaVisualizer();
        this.dfaMinimizer = new DfaMinimizer();
        this.nfaMinimizer = new NfaMinimizer();
        
        initializeUI();
        
        this.visualizationController = new VisualizationController(this);
        for (VisualizationPanel panel : new VisualizationPanel[] {dfaPanel, nfaPanel}) {
            panel.addPropertyChangeListener("visualComponent", event -> {
                if (event.getNewValue() instanceof AutomataGraph) {
                    AutomataGraph graph = (AutomataGraph) event.getNewValue();
                    graph.addPropertyChangeListener("automaton.edited", edited -> synchronizeEditedGraph(graph));
                }
            });
        }
    }
    
    private void initializeUI() {
        frame = new JFrame("AutomataViz — Regular Expression Explorer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                if (visualizationController != null) {
                    visualizationController.cancelGeneration();
                    visualizationController.getAnimationController().stopAnimation();
                }
                cancelExpressionUpdate();
            }
        });
        frame.setSize(1200, 820);
        frame.setMinimumSize(new Dimension(900, 650));
        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(UiTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        frame.setContentPane(content);
        
        inputPanel = new InputPanel(this);
        frame.setJMenuBar(inputPanel.createMenuBar());
        nfaPanel = new VisualizationPanel("NFA");
        dfaPanel = new VisualizationPanel("DFA");
        statusPanel = new StatusPanel();
        legendPanel = new LegendPanel();
        legendPanel.setVisible(false);
        
        visualizationSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, nfaPanel, dfaPanel);
        visualizationSplitPane.setResizeWeight(0.5);
        visualizationSplitPane.setOneTouchExpandable(true);
        
        visualizationSplitPane.setBorder(BorderFactory.createEmptyBorder());
        visualizationSplitPane.setDividerSize(6);
        visualizationSplitPane.setContinuousLayout(true);
        visualizationSplitPane.setBackground(UiTheme.BACKGROUND);

        graphWorkspace = new JPanel(new BorderLayout(0, 0));
        graphWorkspace.setBackground(UiTheme.BACKGROUND);
        graphWorkspace.setMinimumSize(new Dimension(0, 200));
        graphTabs = new JTabbedPane();
        graphTabs.putClientProperty("JTabbedPane.showContentSeparator", false);
        graphTabs.putClientProperty("JTabbedPane.hasFullBorder", false);
        graphTabs.addTab("DFA", null, dfaPanel, "Deterministic automaton: one active state");
        graphTabs.addTab("NFA", null, nfaPanel, "Nondeterministic automaton: a set of active states");
        graphWorkspace.add(graphTabs, BorderLayout.CENTER);
        expressionGuide = new ExpressionGuidePanel(inputPanel.getRegexField(), message -> {
            if (visualizationController != null && visualizationController.getAnimationController().isAnimationRunning())
                visualizationController.getAnimationController().togglePause();
            statusPanel.setStatus(message);
        }, visible -> { legendPanel.setVisible(visible); graphWorkspace.revalidate(); });
        graphControls = new GraphControlsPanel(this);
        graphHeader = new JPanel(new BorderLayout(0, 0));
        graphHeader.setOpaque(false);
        graphHeader.add(expressionGuide, BorderLayout.NORTH);
        graphTabs.putClientProperty("JTabbedPane.trailingComponent", graphControls);
        graphWorkspace.add(graphHeader, BorderLayout.NORTH);
        graphTabs.addChangeListener(e -> updateGraphControls());
        graphWorkspace.add(legendPanel, BorderLayout.SOUTH);

        resultsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, graphWorkspace, statusPanel);
        resultsSplitPane.setResizeWeight(1);
        resultsSplitPane.setDividerSize(6);
        resultsSplitPane.setBorder(BorderFactory.createEmptyBorder());
        resultsSplitPane.setContinuousLayout(true);
        resultsSplitPane.setBackground(UiTheme.BACKGROUND);
        resultsSplitPane.getAccessibleContext().setAccessibleName("Resize graph workspace and results");
        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(resultsSplitPane, BorderLayout.CENTER);
    }

    public void display() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        resultsSplitPane.setDividerLocation(resultsSplitPane.getHeight()
                - statusPanel.getPreferredSize().height - resultsSplitPane.getDividerSize());
        visualizationSplitPane.setDividerLocation(0.5);
        inputPanel.getRegexField().requestFocusInWindow();
    }
    
    public StatusPanel getStatusPanel() {
        return statusPanel;
    }
    
    public DfaVisualizer getDfaVisualizer() {
        return dfaVisualizer;
    }
    
    public NfaVisualizer getNfaVisualizer() {
        return nfaVisualizer;
    }
    
    public RegexToDfaConverter getConverter() {
        return converter;
    }
    
    public DfaMinimizer getDfaMinimizer() {
        return dfaMinimizer;
    }
    
    public NfaMinimizer getNfaMinimizer() {
        return nfaMinimizer;
    }
    
    public DFA getCurrentDfa() {
        return currentDfa;
    }
    
    public void setCurrentDfa(DFA dfa) {
        this.currentDfa = dfa;
    }
    
    public DFA getNonMinimizedDfa() {
        return nonMinimizedDfa;
    }
    
    public void setNonMinimizedDfa(DFA dfa) {
        this.nonMinimizedDfa = dfa;
    }
    
    public NFA getCurrentNfa() {
        return currentNfa;
    }
    
    public void setCurrentNfa(NFA nfa) {
        this.currentNfa = nfa;
    }
    
    public NFA getMinimizedNfa() {
        return minimizedNfa;
    }
    
    public void setMinimizedNfa(NFA nfa) {
        this.minimizedNfa = nfa;
    }
    
    public JFrame getFrame() {
        return frame;
    }
    
    public VisualizationPanel getNfaPanel() {
        return nfaPanel;
    }
    
    public VisualizationPanel getDfaPanel() {
        return dfaPanel;
    }
    
    public JSplitPane getVisualizationSplitPane() {
        return visualizationSplitPane;
    }
    
    public void updateVisualizationLayout(boolean showSplitView) {
        graphControls.getCompareToggle().setSelected(showSplitView);
        graphWorkspace.remove(visualizationSplitPane);
        graphWorkspace.remove(graphTabs);
        graphTabs.putClientProperty("JTabbedPane.trailingComponent", null);
        graphHeader.remove(graphControls);
        graphTabs.removeAll();
        if (showSplitView) {
            graphHeader.add(graphControls, BorderLayout.CENTER);
            visualizationSplitPane.setLeftComponent(nfaPanel);
            visualizationSplitPane.setRightComponent(dfaPanel);
            graphWorkspace.add(visualizationSplitPane, BorderLayout.CENTER);
            visualizationSplitPane.setDividerLocation(0.5);
        } else {
            graphTabs.putClientProperty("JTabbedPane.trailingComponent", graphControls);
            graphTabs.addTab("DFA", null, dfaPanel, "Deterministic automaton: one active state");
            graphTabs.addTab("NFA", null, nfaPanel, "Nondeterministic automaton: a set of active states");
            graphWorkspace.add(graphTabs, BorderLayout.CENTER);
        }
        updateGraphControls();
        graphWorkspace.revalidate();
        graphWorkspace.repaint();
    }

    public VisualizationController getVisualizationController() {
        return visualizationController;
    }
    private void synchronizeEditedGraph(AutomataGraph edited) {
        synchronizeEditedGraph(edited, expression -> { }, error -> { });
    }

    public void cancelExpressionUpdate() {
        expressionRevision++;
        if (expressionWorker != null) expressionWorker.cancel(true);
    }

    public void setExpressionSource(String expression) {
        cancelExpressionUpdate();
        inputPanel.getRegexField().setText(expression);
        inputPanel.getRegexField().setCaretPosition(0);
        showExpression(expression);
    }

    public void synchronizeEditedGraph(AutomataGraph edited, java.util.function.Consumer<String> updated,
            java.util.function.Consumer<String> failed) {
        visualizationController.cancelGeneration();
        cancelExpressionUpdate();
        visualizationController.getAnimationController().stopAnimation();
        if (edited.getDfaModel() != null) {
            currentDfa = edited.getDfaModel();
            nonMinimizedDfa = currentDfa;
            NFA equivalent = new NFA();
            java.util.Map<DFA.State, Integer> ids = new java.util.HashMap<>();
            for (DFA.State state : currentDfa.getStates()) ids.put(state, equivalent.createState());
            equivalent.setStartState(ids.get(currentDfa.getStartState()));
            for (DFA.State state : currentDfa.getAcceptStates()) equivalent.addAcceptState(ids.get(state));
            for (java.util.Map.Entry<DFA.StateTransition, DFA.State> transition : currentDfa.getTransitions().entrySet())
                equivalent.addTransition(ids.get(transition.getKey().getState()), transition.getKey().getSymbol(), ids.get(transition.getValue()));
            currentNfa = equivalent;
            graphControls.showDfaVariant(false, true);
        } else {
            currentNfa = edited.getNfaModel();
            nonMinimizedDfa = new com.dfavisualizer.algorithm.SubsetConstruction().convertNfaToDfa(currentNfa);
            currentDfa = graphControls.isDfaMinimized() ? dfaMinimizer.minimize(nonMinimizedDfa) : nonMinimizedDfa;
        }
        minimizedNfa = null;
        expressionGuide.setExpression("");
        com.dfavisualizer.algorithm.AutomatonToRegex conversion = new com.dfavisualizer.algorithm.AutomatonToRegex(currentDfa);
        String sourceBefore = inputPanel.getRegexField().getText();
        String originalSource = graphSourceExpression;
        long revision = expressionRevision;
        // Rebuild after the drag handler finishes releasing its selected endpoint.
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (!frame.isDisplayable() || revision != expressionRevision) return;
            dfaPanel.setVisualizationComponent(dfaVisualizer.visualizeDfa(currentDfa));
            nfaPanel.setVisualizationComponent(nfaVisualizer.visualizeNfa(currentNfa));
            statusPanel.setStatus("Custom automaton: connection changed. Testing and animation use the edited transitions. "
                    + "Updating expression…");
            expressionWorker = new javax.swing.SwingWorker<String, Void>() {
                @Override protected String doInBackground() { return conversion.convert(originalSource, sourceBefore); }
                @Override protected void done() {
                    if (isCancelled() || this != expressionWorker || revision != expressionRevision || !frame.isDisplayable()) return;
                    try {
                        String expression = get();
                        if (!inputPanel.getRegexField().getText().equals(sourceBefore)) {
                            statusPanel.setStatus("Custom automaton: expression was edited manually during conversion; keeping your text.");
                            failed.accept("Expression changed manually; keeping your text.");
                            return;
                        }
                        inputPanel.getRegexField().setText(expression);
                        inputPanel.getRegexField().setCaretPosition(0);
                        expressionGuide.setExpression(expression);
                        statusPanel.setStatus("Custom automaton: expression updated to match the edited transitions. "
                                + "Testing and animation use the edited graph.");
                        updated.accept(expression);
                    } catch (java.util.concurrent.CancellationException ex) {
                        return;
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                        statusPanel.setStatus("Custom automaton: could not update expression: " + cause.getMessage());
                        failed.accept(cause.getMessage());
                    }
                }
            };
            expressionWorker.execute();
        });
    }

    public void showRegexAlternatives() {
        VisualizationPanel selected = graphTabs.getSelectedComponent() == nfaPanel ? nfaPanel : dfaPanel;
        if (!(selected.getVisualizationComponent() instanceof AutomataGraph)) return;
        new com.dfavisualizer.ui.dialogs.RegexAlternativesDialog(frame,
                (AutomataGraph) selected.getVisualizationComponent(),
                new String[] {graphSourceExpression, inputPanel.getRegexField().getText()}, expression -> {
                    setExpressionSource(expression);
                    statusPanel.setStatus("Verified equivalent expression applied. The graph and its layout are preserved.");
                }).setVisible(true);
    }

    public void showThemes() {
        if (themeChooser == null || !themeChooser.isDisplayable()) themeChooser = new ThemeChooserDialog(this);
        themeChooser.setVisible(true);
        themeChooser.toFront();
    }

    public void applyTheme(UiTheme.Theme theme) {
        visualizationController.cancelGeneration();
        if (visualizationController.getAnimationController().isAnimationRunning())
            visualizationController.getAnimationController().togglePause();
        UiTheme.apply(theme);
        visualizationController.getAnimationController().refreshTheme();
    }

    public InputPanel getInputPanel() { return inputPanel; }

    public GraphControlsPanel getGraphControls() { return graphControls; }

    private void updateGraphControls() {
        if (graphControls != null) graphControls.getDfaVariant().setEnabled(currentDfa != null
                && (graphControls.getCompareToggle().isSelected() || graphTabs.getSelectedIndex() == 0));
    }

    public void showExpression(String regex) { graphSourceExpression = regex; expressionGuide.setExpression(regex); }

    public void showPlayback() {
        if (graphTabs.getTabCount() > 0) graphTabs.setSelectedIndex(0);
        int desired = resultsSplitPane.getHeight() - statusPanel.getPreferredSize().height - resultsSplitPane.getDividerSize();
        if (statusPanel.getHeight() < statusPanel.getPreferredSize().height) {
            resultsSplitPane.setDividerLocation(Math.max(resultsSplitPane.getMinimumDividerLocation(), desired));
        }
        frame.validate();
        dfaPanel.fitView();
    }
} 