package com.dfavisualizer.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.SwingConstants;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;

/** Shared notation, routing, and transition-token drawing for both automata. */
public class AutomataGraph extends mxGraphComponent {
    private final Builder builder;
    private final Map<String, mxCell> states;
    private final Map<mxCell, String> baseStyles;
    private mxCell tokenEdge;
    private double tokenProgress;
    private Timer camera;
    private mxCell followedCell;

    public AutomataGraph(String regularFill, boolean gridSnap) {
        this(new Builder(regularFill), gridSnap);
    }

    public AutomataGraph(Builder builder, boolean gridSnap) {
        super(builder.graph);
        this.builder = builder;
        this.states = builder.states;
        this.baseStyles = builder.baseStyles;
        getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent event) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (followedCell != null && Boolean.TRUE.equals(getClientProperty("playback.active"))) revealState(followedCell, false);
                });
            }
        });
        setConnectable(false);
        setToolTips(true);
        getGraphControl().addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getButton() != MouseEvent.BUTTON1 || event.getClickCount() != 2) return;
                Object cell = getCellAt(event.getX(), event.getY());
                if (cell instanceof mxCell && ((mxCell) cell).getValue() instanceof Transition) {
                    showTransitionDetails((mxCell) cell);
                }
            }
        });
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "transitionDetails");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "transitionDetails");
        getActionMap().put("transitionDetails", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                Object cell = getGraph().getSelectionCell();
                if (cell instanceof mxCell && ((mxCell) cell).getValue() instanceof Transition) showTransitionDetails((mxCell) cell);
            }
        });
        getAccessibleContext().setAccessibleDescription("Select a transition and drag an endpoint to reconnect it. Press Enter or double-click to inspect its exact symbols.");
        setBorder(null);
        setAntiAlias(true);
        setTextAntiAlias(true);
        setGridVisible(gridSnap);
        getViewport().setOpaque(true);
        setBackground(UiTheme.CANVAS);
        getGraphControl().setBackground(UiTheme.CANVAS);
        getViewport().setBackground(UiTheme.CANVAS);
        getGraph().setGridEnabled(gridSnap);
        getGraph().setGridSize(20);
        getGraph().setCellsEditable(false);
        getGraph().setCellsResizable(false);
        getGraph().setAllowDanglingEdges(false);
        getGraph().setAllowLoops(true);
        getGraph().setCellsDisconnectable(true);
        getGraph().setCellsCloneable(false);
        getGraph().setCellsDeletable(false);
        getGraph().addListener(mxEvent.CONNECT_CELL, (sender, event) -> {
            builder.synchronizeTransitions();
            putClientProperty("automaton.edited", new Object());
        });
        getGraph().setEdgeLabelsMovable(false);
        getGraph().setHtmlLabels(false);
        getGraph().addListener(mxEvent.CELLS_MOVED, (sender, event) -> builder.routeEdges());
        addListener(mxEvent.AFTER_PAINT, (sender, event) -> paintToken((Graphics2D) event.getProperty("g")));
    }

    /** Builds an unattached graph model, so conversion and layout can run off the UI thread. */
    public static final class Builder {
        boolean highlightDeadStates = true;
        com.dfavisualizer.model.DFA dfa;
        com.dfavisualizer.model.NFA nfa;
        final mxGraph graph = new mxGraph() {
            @Override public boolean isTerminalPointMovable(Object cell, boolean source) {
                return cell instanceof mxCell && ((mxCell) cell).getValue() instanceof Transition;
            }
            @Override public String getEdgeValidationError(Object edge, Object source, Object target) {
                if (!(edge instanceof mxCell) || !(((mxCell) edge).getValue() instanceof Transition)
                        || !states.containsValue(source) || !states.containsValue(target))
                    return "Connect both ends to existing states.";
                if (dfa != null) {
                    Transition label = (Transition) ((mxCell) edge).getValue();
                    for (Object object : getChildEdges(getDefaultParent())) {
                        mxCell other = (mxCell) object;
                        if (other != edge && other.getSource() == source && other.getValue() instanceof Transition && other.getTarget() != target
                                && !Collections.disjoint(label.symbols, ((Transition) other.getValue()).symbols))
                            return "A DFA state can have only one destination for each symbol.";
                    }
                }
                return null;
            }
            @Override public Object connectCell(Object edge, Object terminal, boolean source,
                    com.mxgraph.view.mxConnectionConstraint constraint) {
                Object from = source ? terminal : getModel().getTerminal(edge, true);
                Object to = source ? getModel().getTerminal(edge, false) : terminal;
                if (getEdgeValidationError(edge, from, to) != null) return edge;
                if (getModel().getTerminal(edge, source) == terminal) return edge;
                return super.connectCell(edge, terminal, source, constraint);
            }
        };
        private final Map<String, mxCell> states = new LinkedHashMap<>();
        private final Map<mxCell, String> baseStyles = new LinkedHashMap<>();
        private String regularFill;
        private mxCell startState, startArrow;

        public Builder(String regularFill) { this.regularFill = regularFill; }

        private static void checkCancelled() {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException("Graph preparation cancelled");
        }

        public void addState(String name, boolean start, boolean accept, boolean dead) {
            checkCancelled();
            String style = stateStyle(start, accept, dead);
            mxCell cell = (mxCell) graph.insertVertex(graph.getDefaultParent(), null, name, 0, 0, 64, 64, style);
            states.put(name, cell);
            if (start) startState = cell;
        }
    
        private String stateStyle(boolean start, boolean accept, boolean dead) {
            String fill = dead ? UiTheme.DEAD_FILL : accept ? UiTheme.ACCEPT_FILL : start ? UiTheme.START_FILL : regularFill;
            return "shape=" + (accept ? mxConstants.SHAPE_DOUBLE_ELLIPSE : mxConstants.SHAPE_ELLIPSE)
                    + ";perimeter=ellipsePerimeter;fontSize=18;fontColor=" + UiTheme.STATE_TEXT + ";fillColor=" + fill
                    + ";strokeColor=" + (start ? UiTheme.START_STROKE : dead ? UiTheme.DEAD_STROKE : UiTheme.STATE_STROKE)
                    + ";strokeWidth=2;shadow=0;dashed=" + (dead ? "1" : "0") + ";";
        }

        public void addTransition(String source, String target, char symbol, boolean epsilon) {
            checkCancelled();
            mxCell from = states.get(source), to = states.get(target);
            if (from == null || to == null) throw new IllegalArgumentException("Transition refers to an unknown state");
            for (Object edge : graph.getEdgesBetween(from, to, true)) {
                mxCell cell = (mxCell) edge;
                if (cell.getValue() instanceof Transition && ((Transition) cell.getValue()).epsilon == epsilon) {
                    Transition label = (Transition) cell.getValue();
                    label.symbols.add(symbol);
                    graph.getModel().setValue(cell, label);
                    return;
                }
            }
            Transition label = new Transition(symbol, epsilon);
            String color = epsilon ? UiTheme.EPSILON : source.equals(target) ? UiTheme.LOOP : UiTheme.EDGE;
            graph.insertEdge(graph.getDefaultParent(), null, label, from, to,
                    "endArrow=classic;endFill=1;endSize=9;strokeWidth=1.8;fontSize=14;fontColor=" + color
                    + ";strokeColor=" + color + ";labelBackgroundColor=" + UiTheme.CANVAS_COLOR + ";rounded=1;arcSize=16;"
                    + "edgeStyle=none;loopStyle=loopEdgeStyle;direction=" + (epsilon ? "south" : "north") + ";segment=34;dashed=" + (epsilon ? "1" : "0") + ";");
        }
    
        public void finish() {
            if (startState == null) throw new IllegalArgumentException("Automaton has no start state");
            checkCancelled();
            mxHierarchicalLayout layout = new mxHierarchicalLayout(graph, SwingConstants.WEST) {
                @Override public Object[] getEdges(Object cell) { checkCancelled(); return super.getEdges(cell); }
                @Override public com.mxgraph.util.mxRectangle getVertexBounds(Object cell) { checkCancelled(); return super.getVertexBounds(cell); }
            };
            layout.setInterRankCellSpacing(80);
            layout.setIntraCellSpacing(65);
            layout.setParallelEdgeSpacing(24);
            layout.setDisableEdgeStyle(false);
            // shortcut: dense graphs use breadth-first layers; revisit if a scalable crossing reducer is added.
            if (states.size() > 200 || graph.getChildEdges(graph.getDefaultParent()).length > 1000) {
                java.util.Map<mxCell, Integer> depths = new java.util.LinkedHashMap<>();
                java.util.ArrayDeque<mxCell> queue = new java.util.ArrayDeque<>();
                depths.put(startState, 0); queue.add(startState);
                while (!queue.isEmpty()) {
                    checkCancelled();
                    mxCell from = queue.remove();
                    for (Object object : graph.getOutgoingEdges(from)) {
                        mxCell target = (mxCell) ((mxCell) object).getTarget();
                        if (!depths.containsKey(target)) { depths.put(target, depths.get(from) + 1); queue.add(target); }
                    }
                }
                java.util.Map<Integer, Integer> rows = new java.util.HashMap<>();
                for (mxCell cell : states.values()) {
                    checkCancelled();
                    int depth = depths.getOrDefault(cell, 0);
                    int row = rows.getOrDefault(depth, 0);
                    rows.put(depth, row + 1);
                    mxGeometry geometry = (mxGeometry) cell.getGeometry().clone();
                    geometry.setX(depth * 180); geometry.setY(row * 130);
                    graph.getModel().setGeometry(cell, geometry);
                }
            } else {
                layout.execute(graph.getDefaultParent(), Collections.singletonList(startState));
            }
            checkCancelled();
            startArrow = (mxCell) graph.insertEdge(graph.getDefaultParent(), null, "", null, startState,
                    "endArrow=classic;endFill=1;endSize=10;strokeColor=" + UiTheme.START_STROKE + ";strokeWidth=2;edgeStyle=none;movable=0;selectable=0;");
            routeEdges();
            for (Object cell : graph.getChildCells(graph.getDefaultParent(), true, true)) {
                baseStyles.put((mxCell) cell, ((mxCell) cell).getStyle());
            }
        }
    
        private void synchronizeTransitions() {
            graph.getModel().beginUpdate();
            try {
                java.util.List<mxCell> kept = new ArrayList<>();
                for (Object object : graph.getChildEdges(graph.getDefaultParent())) {
                    mxCell edge = (mxCell) object;
                    if (!(edge.getValue() instanceof Transition)) continue;
                    Transition label = (Transition) edge.getValue();
                    mxCell duplicate = null;
                    for (mxCell other : kept) if (other.getSource() == edge.getSource() && other.getTarget() == edge.getTarget()
                            && ((Transition) other.getValue()).epsilon == label.epsilon) { duplicate = other; break; }
                    if (duplicate != null) {
                        Transition combined = (Transition) duplicate.getValue();
                        combined.symbols.addAll(label.symbols);
                        graph.getModel().setValue(duplicate, combined);
                        graph.removeCells(new Object[] {edge});
                        baseStyles.remove(edge);
                    } else kept.add(edge);
                }
                if (dfa != null) dfa.getTransitions().clear();
                if (nfa != null) nfa.getTransitions().clear();
                for (mxCell edge : kept) {
                    String from = edge.getSource().getValue().toString(), to = edge.getTarget().getValue().toString();
                    for (char symbol : ((Transition) edge.getValue()).symbols) {
                        if (dfa != null) dfa.addTransition(new com.dfavisualizer.model.DFA.State(from), symbol,
                                new com.dfavisualizer.model.DFA.State(to));
                        if (nfa != null) nfa.addTransition(Integer.parseInt(from.substring(1)), symbol, Integer.parseInt(to.substring(1)));
                    }
                    mxGeometry geometry = (mxGeometry) edge.getGeometry().clone();
                    geometry.setPoints(null);
                    graph.getModel().setGeometry(edge, geometry);
                }
                if (dfa != null) {
                    Set<com.dfavisualizer.model.DFA.State> dead = dfa.getDeadStates();
                    for (Map.Entry<String, mxCell> state : states.entrySet()) {
                        com.dfavisualizer.model.DFA.State model = new com.dfavisualizer.model.DFA.State(state.getKey());
                        String style = stateStyle(model.equals(dfa.getStartState()), dfa.getAcceptStates().contains(model),
                                highlightDeadStates && dead.contains(model));
                        baseStyles.put(state.getValue(), style);
                        graph.getModel().setStyle(state.getValue(), style);
                    }
                }
                for (mxCell edge : kept) {
                    Transition label = (Transition) edge.getValue();
                    String color = label.epsilon ? UiTheme.EPSILON : edge.getSource() == edge.getTarget() ? UiTheme.LOOP : UiTheme.EDGE;
                    graph.getModel().setStyle(edge, baseStyles.get(edge));
                    graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, color, new Object[] {edge});
                    graph.setCellStyles(mxConstants.STYLE_FONTCOLOR, color, new Object[] {edge});
                    baseStyles.put(edge, edge.getStyle());
                }
                routeEdges();
            } finally { graph.getModel().endUpdate(); }
        }

        private void routeEdges() {
            graph.getModel().beginUpdate();
            try {
                for (Object object : graph.getChildEdges(graph.getDefaultParent())) {
                    mxCell edge = (mxCell) object;
                    mxGeometry geometry = (mxGeometry) edge.getGeometry().clone();
                    if (edge == startArrow) {
                        mxGeometry start = startState.getGeometry();
                        geometry.setSourcePoint(new mxPoint(start.getX() - 44, start.getCenterY()));
                    } else if (edge.getSource() != edge.getTarget() && edge.getSource() != null && edge.getTarget() != null) {
                        mxGeometry from = edge.getSource().getGeometry(), to = edge.getTarget().getGeometry();
                        double dx = to.getCenterX() - from.getCenterX(), dy = to.getCenterY() - from.getCenterY();
                        boolean reverse = graph.getEdgesBetween(edge.getTarget(), edge.getSource(), true).length > 0;
                        Object[] parallel = graph.getEdgesBetween(edge.getSource(), edge.getTarget(), true);
                        if ((reverse || parallel.length > 1)
                                && (geometry.getPoints() == null || geometry.getPoints().size() <= 1)) {
                            double distance = Math.max(1, Math.hypot(dx, dy));
                            int lane = java.util.Arrays.asList(parallel).indexOf(edge);
                            double offset = (reverse ? 34 : 0) + (lane - (parallel.length - 1) / 2.0) * 28;
                            geometry.setPoints(Collections.singletonList(new mxPoint(
                                    (from.getCenterX() + to.getCenterX()) / 2 - dy / distance * offset,
                                    (from.getCenterY() + to.getCenterY()) / 2 + dx / distance * offset)));
                        }
                    }
                    graph.getModel().setGeometry(edge, geometry);
                }
            } finally { graph.getModel().endUpdate(); }
        }
    
    }

    public void addState(String name, boolean start, boolean accept, boolean dead) { builder.addState(name, start, accept, dead); }
    public void addTransition(String source, String target, char symbol, boolean epsilon) { builder.addTransition(source, target, symbol, epsilon); }
    public void finish() { builder.finish(); }

    public mxCell state(String name) { return states.get(name); }

    public com.dfavisualizer.model.DFA getDfaModel() { return builder.dfa; }
    public com.dfavisualizer.model.NFA getNfaModel() { return builder.nfa; }

    public mxCell transition(String source, String target, char symbol) {
        for (Object object : getGraph().getEdgesBetween(states.get(source), states.get(target), true)) {
            mxCell edge = (mxCell) object;
            if (edge.getValue() instanceof Transition && ((Transition) edge.getValue()).symbols.contains(symbol)) return edge;
        }
        return null;
    }

    public void focusOn(Set<mxCell> active, boolean enabled) {
        Set<Object> neighborhood = new HashSet<>(active);
        for (Object object : getGraph().getChildEdges(getGraph().getDefaultParent())) {
            mxCell edge = (mxCell) object;
            if (edge.getSource() != null && active.contains(edge.getSource())) {
                neighborhood.add(edge);
                neighborhood.add(edge.getTarget());
            }
            if (edge == builder.startArrow && active.contains(builder.startState)) neighborhood.add(edge);
        }
        getGraph().getModel().beginUpdate();
        try {
            for (Object object : getGraph().getChildCells(getGraph().getDefaultParent(), true, true)) {
                String opacity = !enabled || neighborhood.contains(object) ? "100" : "18";
                getGraph().setCellStyles(mxConstants.STYLE_OPACITY, opacity, new Object[] {object});
                getGraph().setCellStyles(mxConstants.STYLE_TEXT_OPACITY, opacity, new Object[] {object});
            }
        } finally { getGraph().getModel().endUpdate(); }
    }

    /** Move only when the current state is outside the viewport; preserve the user's zoom. */
    public void revealState(mxCell cell, boolean readableZoom) {
        if (cell == null || getViewport().getWidth() <= 0) return;
        stopCamera();
        followedCell = cell;
        if (readableZoom && getGraph().getView().getScale() < 0.85) getGraph().getView().setScale(0.85);
        mxCellState state = getGraph().getView().getState(cell);
        if (state == null) return;
        java.awt.Rectangle visible = getViewport().getViewRect();
        java.awt.Rectangle safe = new java.awt.Rectangle(visible);
        safe.grow(-32, -32);
        if (safe.contains(state.getRectangle())) return;
        double scale = getGraph().getView().getScale();
        mxPoint from = getGraph().getView().getTranslate();
        double startX = from.getX(), startY = from.getY();
        double dx = (visible.getCenterX() - state.getCenterX()) / scale;
        double dy = (visible.getCenterY() - state.getCenterY()) / scale;
        long started = System.nanoTime();
        camera = new Timer(16, e -> {
            double progress = Math.min(1, (System.nanoTime() - started) / 220_000_000.0);
            double eased = progress * progress * (3 - 2 * progress);
            getGraph().getView().setTranslate(new mxPoint(startX + dx * eased, startY + dy * eased));
            if (progress == 1) stopCamera();
        });
        camera.start();
    }

    public void stopFollowing() { followedCell = null; stopCamera(); }

    public void stopCamera() { if (camera != null) { camera.stop(); camera = null; } }

    public void applyPalette(Map<String, String> colors) {
        stopCamera();
        builder.regularFill = UiTheme.recolor(builder.regularFill, colors);
        baseStyles.replaceAll((cell, style) -> UiTheme.recolor(style, colors));
        getGraph().getModel().beginUpdate();
        try {
            for (Object cell : getGraph().getChildCells(getGraph().getDefaultParent(), true, true))
                getGraph().getModel().setStyle(cell, UiTheme.recolor(((mxCell) cell).getStyle(), colors));
        } finally { getGraph().getModel().endUpdate(); }
        setBackground(UiTheme.CANVAS);
        getViewport().setBackground(UiTheme.CANVAS);
        getGraphControl().setBackground(UiTheme.CANVAS);
        refresh();
    }

    public void restoreStyles() {
        getGraph().getModel().beginUpdate();
        try { baseStyles.forEach((cell, style) -> getGraph().getModel().setStyle(cell, style)); }
        finally { getGraph().getModel().endUpdate(); }
        clearToken();
    }

    public void showToken(mxCell edge, double progress) {
        tokenEdge = edge;
        tokenProgress = Math.max(0, Math.min(1, progress));
        getGraphControl().repaint();
    }

    public void clearToken() {
        tokenEdge = null;
        getGraphControl().repaint();
    }

    private void paintToken(Graphics2D graphics) {
        if (tokenEdge == null) return;
        mxCellState state = getGraph().getView().getState(tokenEdge);
        if (state == null || state.getAbsolutePoints() == null) return;
        List<mxPoint> points = state.getAbsolutePoints();
        double length = 0;
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i - 1) != null && points.get(i) != null) length += Math.hypot(
                    points.get(i).getX() - points.get(i - 1).getX(), points.get(i).getY() - points.get(i - 1).getY());
        }
        double remaining = length * tokenProgress;
        for (int i = 1; i < points.size(); i++) {
            mxPoint from = points.get(i - 1), to = points.get(i);
            if (from == null || to == null) continue;
            double segment = Math.hypot(to.getX() - from.getX(), to.getY() - from.getY());
            if (remaining <= segment || i == points.size() - 1) {
                double fraction = segment == 0 ? 0 : Math.min(1, remaining / segment);
                int x = (int) Math.round(from.getX() + (to.getX() - from.getX()) * fraction);
                int y = (int) Math.round(from.getY() + (to.getY() - from.getY()) * fraction);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(UiTheme.CANVAS);
                g.fillOval(x - 8, y - 8, 16, 16);
                g.setColor(Color.decode(UiTheme.ACTIVE_STATE));
                g.fillOval(x - 6, y - 6, 12, 12);
                g.dispose();
                return;
            }
            remaining -= segment;
        }
    }

    private void showTransitionDetails(mxCell edge) {
        javax.swing.JTextArea details = new javax.swing.JTextArea(((Transition) edge.getValue()).details(), 7, 50);
        details.setLineWrap(true); details.setWrapStyleWord(true); details.setEditable(false);
        details.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new javax.swing.JScrollPane(details), "Transition symbols", JOptionPane.INFORMATION_MESSAGE);
    }

    public static String symbolsLabel(Set<Character> symbols) {
        if (symbols.isEmpty()) return "∅";
        Transition label = new Transition(symbols.iterator().next(), false);
        label.symbols.addAll(symbols);
        return label.toString();
    }

    public static String symbolLabel(char symbol) {
        switch (symbol) {
            case ' ': return "␣";
            case '\n': return "\\n";
            case '\r': return "\\r";
            case '\t': return "\\t";
            default: return Character.toString(symbol);
        }
    }

    public static final class Transition {
        private final SortedSet<Character> symbols = new TreeSet<>();
        private final boolean epsilon;
        private Transition(char symbol, boolean epsilon) { symbols.add(symbol); this.epsilon = epsilon; }
        private boolean sameRange(char a, char b) {
            return (a >= 'a' && b <= 'z') || (a >= 'A' && b <= 'Z') || (a >= '0' && b <= '9');
        }
        public String details() {
            if (epsilon) return "ε: this transition consumes no input. Follow it without advancing the input position.";
            List<String> exact = new ArrayList<>();
            for (char symbol : symbols) exact.add("'" + symbolLabel(symbol) + "'");
            return "Matches one of " + symbols.size() + " symbols (consumes exactly one character):\n" + String.join(", ", exact);
        }
        @Override public String toString() {
            if (epsilon) return "ε";
            List<Character> ordered = new ArrayList<>(symbols);
            List<String> labels = new ArrayList<>();
            for (int i = 0; i < ordered.size(); i++) {
                int last = i;
                while (last + 1 < ordered.size() && ordered.get(last + 1) == ordered.get(last) + 1
                        && sameRange(ordered.get(last), ordered.get(last + 1))) last++;
                if (last - i >= 2 && Character.isLetterOrDigit(ordered.get(i))) {
                    labels.add(symbolLabel(ordered.get(i)) + "–" + symbolLabel(ordered.get(last)));
                    i = last;
                } else labels.add(symbolLabel(ordered.get(i)));
            }
            return ordered.size() > 1 ? "[" + String.join(", ", labels) + "]" : labels.get(0);
        }
    }
}
