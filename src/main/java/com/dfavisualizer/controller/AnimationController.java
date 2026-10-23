package com.dfavisualizer.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import com.dfavisualizer.model.DFA;
import com.dfavisualizer.ui.AutomataGraph;
import com.dfavisualizer.ui.MainFrame;
import com.dfavisualizer.ui.UiTheme;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;

/** Plays one DFA transition at a time on the Swing event thread. */
public class AnimationController {
    private final MainFrame mainFrame;
    private final Timer timer;
    private AutomataGraph graph;
    private DFA dfa;
    private String input;
    private int position;
    private DFA.State currentState;
    private DFA.State nextState;
    private mxCell edge;
    private boolean paused;
    private boolean finished;
    private double speed = 1;
    private double elapsed;
    private long lastTick;
    private String explanation;
    private boolean focusMode = true;
    private final List<DFA.State> history = new ArrayList<>();

    public AnimationController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        timer = new Timer(16, e -> tick());
        mainFrame.getStatusPanel().bindPlayback(this);
    }

    public boolean animateString(String testString) {
        if (mainFrame.getCurrentDfa() == null) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), "Create a DFA before animating a string.",
                    "No DFA Available", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        JComponent component = mainFrame.getDfaPanel().getVisualizationComponent();
        if (!(component instanceof AutomataGraph)) {
            mainFrame.getStatusPanel().setStatus("Cannot animate: DFA graph is not available.");
            return false;
        }
        stopAnimation();
        graph = (AutomataGraph) component;
        graph.putClientProperty("playback.active", true);
        dfa = mainFrame.getCurrentDfa();
        input = testString;
        position = 0;
        currentState = dfa.getStartState();
        history.clear();
        history.add(currentState);
        nextState = null;
        edge = null;
        paused = false;
        finished = false;
        elapsed = 0;
        highlightState();
        explanation = "Start at " + currentState + ". " + preview();
        if (input.isEmpty()) finish();
        else {
            update();
            lastTick = System.nanoTime();
            timer.start();
        }
        mainFrame.showPlayback();
        if (focusMode) graph.revealState(graph.state(currentState.getName()), true);
        return true;
    }

    private void tick() {
        long now = System.nanoTime();
        elapsed += (now - lastTick) / 1_000_000.0 * speed;
        lastTick = now;
        if (nextState == null) {
            if (elapsed >= 900) beginTransition();
        } else {
            double progress = Math.min(1, elapsed / 700);
            graph.showToken(edge, progress);
            if (progress >= 1) arrive();
        }
    }

    private void beginTransition() {
        if (finished || input == null) return;
        if (position == input.length()) { finish(); return; }
        char symbol = input.charAt(position);
        String label = AutomataGraph.symbolLabel(symbol);
        if (!dfa.getAlphabet().contains(symbol)) {
            reject("Symbol '" + label + "' at position " + (position + 1) + " is outside the DFA alphabet. " + expected());
            return;
        }
        nextState = dfa.getTransitionTarget(currentState, symbol);
        if (nextState == null) {
            reject("No transition leaves " + currentState + " for '" + label + "' at position " + (position + 1) + ". " + expected());
            return;
        }
        edge = graph.transition(currentState.getName(), nextState.getName(), symbol);
        if (edge == null) {
            timer.stop();
            finished = true;
            explanation = "Playback stopped: the displayed graph is missing the model's transition.";
            update();
            return;
        }
        graph.restoreStyles();
        highlightState();
        graph.getGraph().setCellStyles(mxConstants.STYLE_STROKECOLOR, UiTheme.ACTIVE_EDGE, new Object[] {edge});
        graph.getGraph().setCellStyles(mxConstants.STYLE_FONTCOLOR, UiTheme.ACTIVE_EDGE, new Object[] {edge});
        graph.getGraph().setCellStyles(mxConstants.STYLE_STROKEWIDTH, "3", new Object[] {edge});
        graph.showToken(edge, 0);
        elapsed = 0;
        explanation = "Step " + (position + 1) + ": read '" + label + "' — " + currentState + " → " + nextState
                + (currentState.equals(nextState) ? ". This self-loop consumes the symbol and stays in the same state." : ". Follow the highlighted arrow.");
        update();
        if (focusMode) graph.revealState(graph.state(nextState.getName()), false);
    }

    private void arrive() {
        String source = currentState.toString();
        boolean deadDestination = dfa.getDeadStates().contains(nextState);
        String expectedBefore = deadDestination ? expected() : "";
        char symbol = input.charAt(position);
        currentState = nextState;
        nextState = null;
        position++;
        history.add(currentState);
        elapsed = 0;
        graph.clearToken();
        graph.restoreStyles();
        highlightState();
        explanation = "Step " + position + ": " + source + " — '" + AutomataGraph.symbolLabel(symbol) + "' → "
                + currentState + ". " + position + " of " + input.length() + " symbols consumed.";
        if (deadDestination) reject("Reading '" + AutomataGraph.symbolLabel(symbol) + "' from " + source
                + " enters " + currentState + ", a dead state: no continuation can reach acceptance. " + expectedBefore);
        else if (position == input.length()) finish();
        else { explanation += " " + preview(); update(); }
    }

    private void finish() {
        if (dfa.getAcceptStates().contains(currentState)) {
            explanation = "ACCEPTED — all " + input.length() + " symbols consumed; " + currentState + " is an accepting state (double ring).";
            complete(true);
        } else reject("All input was consumed, but " + currentState + " is not an accepting state. " + expected());
    }

    private void reject(String reason) {
        explanation = "REJECTED — " + reason;
        complete(false);
    }

    private void complete(boolean accepted) {
        timer.stop();
        finished = true;
        nextState = null;
        graph.clearToken();
        graph.restoreStyles();
        mxCell state = graph.state(currentState.getName());
        graph.getGraph().setCellStyles(mxConstants.STYLE_STROKECOLOR, accepted ? UiTheme.START_STROKE : UiTheme.DEAD_STROKE, new Object[] {state});
        graph.getGraph().setCellStyles(mxConstants.STYLE_STROKEWIDTH, "4", new Object[] {state});
        update();
    }

    private void highlightState() {
        mxCell state = graph.state(currentState.getName());
        graph.getGraph().setCellStyles(mxConstants.STYLE_STROKECOLOR, UiTheme.ACTIVE_STATE, new Object[] {state});
        graph.getGraph().setCellStyles(mxConstants.STYLE_STROKEWIDTH, "4", new Object[] {state});
    }

    private void update() {
        mainFrame.getStatusPanel().setStatus((paused && !finished ? "Paused. " : "") + explanation);
        graph.focusOn(Set.of(graph.state(currentState.getName())), focusMode);
        mainFrame.getStatusPanel().showPlayback(input, position, paused, finished, position > 0 || nextState != null);
    }

    public void refreshTheme() { if (input != null) update(); }

    public void togglePause() {
        if (input == null || finished) return;
        paused = !paused;
        if (paused) timer.stop();
        else { lastTick = System.nanoTime(); timer.start(); }
        update();
    }

    public void stepForward() {
        if (input == null || finished) return;
        timer.stop();
        paused = true;
        if (nextState == null) beginTransition();
        if (!finished && nextState != null) arrive();
    }

    private String preview() {
        if (position >= input.length()) return "All input consumed.";
        char symbol = input.charAt(position);
        DFA.State target = dfa.getTransitionTarget(currentState, symbol);
        return "Next character: '" + AutomataGraph.symbolLabel(symbol) + "'. From " + currentState
                + (target == null ? ", no matching transition. " + expected()
                : ", take the matching arrow to " + target + ".");
    }

    private String expected() {
        Set<Character> symbols = new TreeSet<>();
        Set<DFA.State> dead = dfa.getDeadStates();
        dfa.getTransitions().forEach((transition, target) -> {
            if (transition.getState().equals(currentState) && !dead.contains(target)) symbols.add(transition.getSymbol());
        });
        return "Read prefix '" + input.substring(0, position) + "'. Expected "
                + (symbols.isEmpty() ? "no continuation that can reach acceptance."
                : "one of " + AutomataGraph.symbolsLabel(symbols) + " to stay on a path to acceptance.");
    }

    public void stepBack() {
        if (input == null || (position == 0 && nextState == null)) return;
        timer.stop();
        paused = true;
        finished = false;
        nextState = null;
        edge = null;
        elapsed = 0;
        if (position > 0) position--;
        while (history.size() > position + 1) history.remove(history.size() - 1);
        currentState = history.get(position);
        graph.restoreStyles();
        highlightState();
        explanation = "Back to " + currentState + ". " + position + " of " + input.length() + " symbols consumed. " + preview();
        update();
        if (focusMode) graph.revealState(graph.state(currentState.getName()), false);
    }

    public void setFocusMode(boolean enabled) {
        focusMode = enabled;
        if (graph == null) return;
        graph.stopFollowing();
        update();
        if (enabled) graph.revealState(graph.state(currentState.getName()), true);
        else mainFrame.getDfaPanel().fitView();
    }

    public void restart() { if (input != null) animateString(input); }
    public void setSpeed(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier <= 0) throw new IllegalArgumentException("Playback speed must be positive");
        speed = multiplier;
    }

    public void stopAnimation() {
        timer.stop();
        if (graph != null) { graph.putClientProperty("playback.active", null); graph.stopFollowing(); graph.restoreStyles(); }
        graph = null;
        dfa = null;
        input = null;
        nextState = null;
        edge = null;
        mainFrame.getStatusPanel().hidePlayback();
    }

    public boolean isAnimationRunning() { return timer.isRunning(); }
    public mxGraphComponent getGraphComponent() { return graph; }
}
