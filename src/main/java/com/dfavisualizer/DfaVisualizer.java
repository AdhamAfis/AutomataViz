package com.dfavisualizer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.SwingConstants;

import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxStylesheet;

/**
 * Visualizes a DFA as a directed graph.
 */
public class DfaVisualizer {

    private static final int STATE_SIZE = 50;
    // Updated color scheme for better contrast and distinguishability
    private static final String[] STATE_COLORS = {
        "#E6F2FF", // Light blue - Regular states
        "#FFEBCC", // Light orange - Accept states
        "#E6FFCC", // Light green - Start states
        "#CCFFD5", // Mint green - Special states
        "#F2E6FF",  // Light purple - Intermediate states
        "#FFCCCC"  // Light red - Dead states
    };
    
    // Colors for edges
    private static final String LOOP_COLOR = "#6666FF"; // Blue for loops
    private static final String NORMAL_EDGE_COLOR = "#666666"; // Dark gray for normal edges
    private static final String SPECIAL_EDGE_COLOR = "#FF6666"; // Red for special transitions
    private static final String DEAD_STATE_BORDER_COLOR = "#AA0000"; // Dark red for dead state borders
    
    // For transition grouping
    private static final int MAX_DISPLAY_SYMBOLS = 12;
    private static final int MIN_RANGE_SIZE = 3;
    
    // Control flag for whether to highlight dead states
    private boolean highlightDeadStates = true;

    /**
     * Sets whether to highlight dead states in the visualization.
     * 
     * @param highlight true to highlight dead states, false otherwise
     */
    public void setHighlightDeadStates(boolean highlight) {
        this.highlightDeadStates = highlight;
    }

    /**
     * Creates a graphical visualization of the DFA.
     * 
     * @param dfa The DFA to visualize
     * @return A Swing component containing the visualization
     */
    public JComponent visualizeDfa(DFA dfa) {
        if (dfa == null || dfa.getStates().isEmpty()) {
            throw new IllegalArgumentException("Cannot visualize empty DFA");
        }
        
        // Create a directed graph
        DefaultDirectedGraph<String, LabeledEdge> graph = 
            new DefaultDirectedGraph<>(LabeledEdge.class);
        
        // Add all states as vertices, sorting them for consistent visualization
        List<DFA.State> sortedStates = new ArrayList<>(dfa.getStates());
        Collections.sort(sortedStates, Comparator.comparing(DFA.State::getName));
        
        for (DFA.State state : sortedStates) {
            graph.addVertex(state.getName());
        }
        
        // Group transitions by source-target pair to combine multiple symbols
        Map<String, Map<String, List<Character>>> combinedTransitions = new HashMap<>();
        
        // Add all transitions as edges
        for (Map.Entry<DFA.StateTransition, DFA.State> entry : dfa.getTransitions().entrySet()) {
            DFA.StateTransition transition = entry.getKey();
            DFA.State targetState = entry.getValue();
            
            String sourceLabel = transition.getState().getName();
            String targetLabel = targetState.getName();
            char symbol = transition.getSymbol();
            
            // Combine transitions between the same states
            combinedTransitions
                .computeIfAbsent(sourceLabel, k -> new HashMap<>())
                .computeIfAbsent(targetLabel, k -> new ArrayList<>())
                .add(symbol);
        }
        
        // Add combined edges to the graph
        for (Map.Entry<String, Map<String, List<Character>>> sourceEntry : combinedTransitions.entrySet()) {
            String sourceLabel = sourceEntry.getKey();
            
            for (Map.Entry<String, List<Character>> targetEntry : sourceEntry.getValue().entrySet()) {
                String targetLabel = targetEntry.getKey();
                List<Character> symbolsList = targetEntry.getValue();
                
                // Sort the symbols for consistent display
                Collections.sort(symbolsList);
                
                // Create a readable label
                String symbolsLabel = createSymbolsLabel(symbolsList);
                
                // Create the edge with the combined label
                LabeledEdge edge = graph.addEdge(sourceLabel, targetLabel);
                if (edge != null) {
                    edge.setLabel(symbolsLabel);
                    // Store original symbols for possible tooltips or future use
                    edge.setOriginalSymbols(symbolsList);
                    // Mark if this is a self-loop
                    edge.setIsLoop(sourceLabel.equals(targetLabel));
                }
            }
        }
        
        // Create the graph adapter
        JGraphXAdapter<String, LabeledEdge> graphAdapter = 
            new JGraphXAdapter<>(graph);
        
        // Set up the style for the graph
        setupGraphStyle(graphAdapter, dfa);
        
        // Create a layout for the graph based on size and structure
        mxIGraphLayout layout = createAppropriateLayout(graphAdapter, dfa);
        
        // Execute the layout
        layout.execute(graphAdapter.getDefaultParent());
        
        // Adjust the graph for better visualization
        adjustEdgeLabels(graphAdapter);
        
        // Create a component with the visualization
        mxGraphComponent graphComponent = createGraphComponent(graphAdapter, dfa);
        
        return graphComponent;
    }
    
    /**
     * Creates an appropriate layout based on DFA characteristics
     */
    private mxIGraphLayout createAppropriateLayout(JGraphXAdapter<String, LabeledEdge> graphAdapter, DFA dfa) {
        int stateCount = dfa.getStates().size();
        
        if (stateCount > 10) {
            // Use hierarchical layout for larger DFAs
            mxHierarchicalLayout layout = new mxHierarchicalLayout(graphAdapter);
            layout.setIntraCellSpacing(50.0);
            layout.setInterRankCellSpacing(100.0);
            layout.setDisableEdgeStyle(false);
            
            // If there's an accept state, try to have it at the right/bottom
            if (!dfa.getAcceptStates().isEmpty()) {
                layout.setOrientation(SwingConstants.WEST);
            }
            
            return layout;
        } else if (stateCount > 5) {
            // Use hierarchical layout with different settings for medium DFAs
            mxHierarchicalLayout layout = new mxHierarchicalLayout(graphAdapter);
            layout.setIntraCellSpacing(60.0);
            layout.setInterRankCellSpacing(100.0);
            
            // If there's only one accept state, make it the target of the layout
            if (dfa.getAcceptStates().size() == 1) {
                // Put the accept state at the bottom
                String acceptStateName = dfa.getAcceptStates().iterator().next().getName();
                Object acceptCell = graphAdapter.getVertexToCellMap().get(acceptStateName);
                if (acceptCell != null) {
                    layout.setOrientation(SwingConstants.NORTH);
                }
            }
            
            return layout;
        } else {
            // Use circle layout for smaller DFAs
            mxCircleLayout layout = new mxCircleLayout(graphAdapter);
            layout.setRadius(180.0);
            return layout;
        }
    }
    
    /**
     * Creates a readable label for a list of transition symbols
     */
    private String createSymbolsLabel(List<Character> symbols) {
        if (symbols.isEmpty()) {
            return "ε"; // Epsilon for empty transitions
        }
        
        if (symbols.size() <= 5) {
            // Simple case: just join the characters
            StringBuilder sb = new StringBuilder();
            for (Character c : symbols) {
                appendSymbol(sb, c);
                sb.append(",");
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1); // Remove trailing comma
            }
            return sb.toString();
        }
        
        // Try to identify ranges to make the label more compact
        return simplifySymbols(symbols);
    }
    
    /**
     * Helper to append a symbol to a string builder, handling special cases
     */
    private void appendSymbol(StringBuilder sb, char c) {
        switch (c) {
            case '\n': sb.append("\\n"); break;
            case '\r': sb.append("\\r"); break;
            case '\t': sb.append("\\t"); break;
            case ' ': sb.append("␣"); break; // Space character
            default: sb.append(c);
        }
    }
    
    /**
     * Creates a configured graph component for visualization
     */
    private mxGraphComponent createGraphComponent(JGraphXAdapter<String, LabeledEdge> graphAdapter, DFA dfa) {
        mxGraphComponent graphComponent = new mxGraphComponent(graphAdapter);
        
        // Configure component settings for better readability
        graphComponent.setConnectable(false);
        graphComponent.getGraph().setAllowDanglingEdges(false);
        graphComponent.getGraph().setEdgeLabelsMovable(false);
        graphComponent.getGraph().setCellsEditable(false);
        graphComponent.getGraph().setCellsLocked(true);
        graphComponent.getGraph().setCellsResizable(false);
        graphComponent.getGraph().setCellsSelectable(false);
        graphComponent.setToolTips(true);
        graphComponent.setBorder(null);
        graphComponent.getViewport().setOpaque(true);
        graphComponent.getViewport().setBackground(new Color(252, 252, 252)); // Very light gray
        
        // Enable antialiasing for smoother graphics
        graphComponent.setAntiAlias(true);
        graphComponent.setTextAntiAlias(true);
        
        // Add tooltips for states
        addStateTooltips(graphComponent, dfa);
        
        return graphComponent;
    }
    
    /**
     * Adds tooltips to all states for better understanding
     */
    private void addStateTooltips(mxGraphComponent graphComponent, DFA dfa) {
        // We can't use custom tooltip handlers as easily with JGraphX
        // Instead, set a basic tooltip for the whole component that shows general info
        
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html><b>DFA Visualization</b><br>");
        tooltip.append("Start State: ").append(dfa.getStartState().getName()).append("<br>");
        
        tooltip.append("Accept States: ");
        for (DFA.State acceptState : dfa.getAcceptStates()) {
            tooltip.append(acceptState.getName()).append(" ");
        }
        
        tooltip.append("<br>");
        
        // Add information about dead states if highlighting is enabled
        Set<DFA.State> deadStates = dfa.getDeadStates();
        if (!deadStates.isEmpty() && highlightDeadStates) {
            tooltip.append("Dead States: ");
            for (DFA.State deadState : deadStates) {
                tooltip.append(deadState.getName()).append(" ");
            }
            tooltip.append("<br>");
        }
        
        tooltip.append("Total States: ").append(dfa.getStates().size()).append("<br>");
        tooltip.append("Blue edges represent self-loops<br>");
        tooltip.append("Grey edges represent normal transitions<br>");
        
        // Only mention dead states in the tooltip if highlighting is enabled
        if (highlightDeadStates && !deadStates.isEmpty()) {
            tooltip.append("Red-bordered states are dead states (can't reach accept state)<br>");
        }
        
        tooltip.append("</html>");
        
        graphComponent.setToolTipText(tooltip.toString());
    }
    
    /**
     * Checks if a state has any self-loops
     */
    private boolean hasLoop(DFA dfa, DFA.State state) {
        for (Map.Entry<DFA.StateTransition, DFA.State> entry : dfa.getTransitions().entrySet()) {
            DFA.StateTransition transition = entry.getKey();
            DFA.State targetState = entry.getValue();
            
            if (transition.getState() == state && targetState == state) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Counts outgoing transitions from a state
     */
    private int countOutgoingTransitions(DFA dfa, DFA.State state) {
        int count = 0;
        for (DFA.StateTransition transition : dfa.getTransitions().keySet()) {
            if (transition.getState() == state) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Counts incoming transitions to a state
     */
    private int countIncomingTransitions(DFA dfa, DFA.State state) {
        int count = 0;
        for (DFA.State targetState : dfa.getTransitions().values()) {
            if (targetState == state) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Adjusts edge label positions for better visibility.
     */
    private void adjustEdgeLabels(JGraphXAdapter<String, LabeledEdge> graphAdapter) {
        // Get all edge cells
        Object[] edges = graphAdapter.getChildCells(graphAdapter.getDefaultParent(), false, true);
        
        for (Object edge : edges) {
            // Get source and target vertices for this edge
            Object sourceVertex = graphAdapter.getModel().getTerminal(edge, true);
            Object targetVertex = graphAdapter.getModel().getTerminal(edge, false);
            
            // Check if this is a self-loop
            if (sourceVertex == targetVertex) {
                // Adjust the self-loop to be more clearly visible
                graphAdapter.setCellStyles(mxConstants.STYLE_LOOP, "1", new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_EDGE, "entityRelation", new Object[] { edge });
                
                // Add a larger offset to make the loop more visible
                mxGeometry geometry = graphAdapter.getModel().getGeometry(edge);
                if (geometry != null) {
                    geometry = (mxGeometry) geometry.clone();
                    geometry.setOffset(new mxPoint(0, -60));
                    graphAdapter.getModel().setGeometry(edge, geometry);
                }
                
                // Make self-loops more noticeable with a different color and line style
                graphAdapter.setCellStyles(mxConstants.STYLE_STROKECOLOR, LOOP_COLOR, new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "2", new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_FONTCOLOR, LOOP_COLOR, new Object[] { edge });
            } else {
                // For non-loop edges
                graphAdapter.setCellStyles(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC, new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_STROKECOLOR, NORMAL_EDGE_COLOR, new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_FONTCOLOR, NORMAL_EDGE_COLOR, new Object[] { edge });
            }
            
            // Make the edge labels more readable
            graphAdapter.setCellStyles(mxConstants.STYLE_FONTSIZE, "12", new Object[] { edge });
            graphAdapter.setCellStyles(mxConstants.STYLE_FONTSTYLE, String.valueOf(1), new Object[] { edge });
        }
    }
    
    /**
     * Sets up the visual style of the graph, such as colors and shapes for states.
     */
    private void setupGraphStyle(JGraphXAdapter<String, LabeledEdge> graphAdapter, DFA dfa) {
        // Get the stylesheet for customization
        mxStylesheet stylesheet = graphAdapter.getStylesheet();
        
        // Base style for all states (vertices)
        Map<String, Object> vertexStyle = new HashMap<>();
        vertexStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
        vertexStyle.put(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_ELLIPSE);
        vertexStyle.put(mxConstants.STYLE_FONTSIZE, "14"); // Slightly larger font
        vertexStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        vertexStyle.put(mxConstants.STYLE_FILLCOLOR, STATE_COLORS[0]); // Default state color
        vertexStyle.put(mxConstants.STYLE_STROKECOLOR, "#444444"); // Darker border
        vertexStyle.put(mxConstants.STYLE_STROKEWIDTH, "1.5");
        vertexStyle.put(mxConstants.STYLE_SHADOW, true);
        vertexStyle.put(mxConstants.STYLE_ROUNDED, true);
        vertexStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
        vertexStyle.put(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_MIDDLE);
        vertexStyle.put(mxConstants.STYLE_SPACING, "10");
        
        stylesheet.putCellStyle("VERTEX", vertexStyle);
        
        // Start state style
        Map<String, Object> startStyle = new HashMap<>(vertexStyle);
        startStyle.put(mxConstants.STYLE_STROKECOLOR, "#008800"); // Darker green
        startStyle.put(mxConstants.STYLE_STROKEWIDTH, "3");
        startStyle.put(mxConstants.STYLE_FILLCOLOR, STATE_COLORS[2]); // Green for start states
        stylesheet.putCellStyle("START", startStyle);
        
        // Accept state style
        Map<String, Object> acceptStyle = new HashMap<>(vertexStyle);
        acceptStyle.put(mxConstants.STYLE_DASHED, false);
        acceptStyle.put(mxConstants.STYLE_STROKEWIDTH, "3");
        acceptStyle.put(mxConstants.STYLE_FILLCOLOR, STATE_COLORS[1]); // Orange for accept states
        stylesheet.putCellStyle("ACCEPT", acceptStyle);
        
        // Start+Accept state style
        Map<String, Object> startAcceptStyle = new HashMap<>(vertexStyle);
        startAcceptStyle.put(mxConstants.STYLE_STROKECOLOR, "#008800"); // Darker green
        startAcceptStyle.put(mxConstants.STYLE_STROKEWIDTH, "3");
        startAcceptStyle.put(mxConstants.STYLE_FILLCOLOR, "#FFD6A5"); // Blend of green and orange
        startAcceptStyle.put(mxConstants.STYLE_DASHED, false);
        stylesheet.putCellStyle("START_ACCEPT", startAcceptStyle);
        
        // Dead state style
        Map<String, Object> deadStyle = new HashMap<>(vertexStyle);
        deadStyle.put(mxConstants.STYLE_STROKECOLOR, DEAD_STATE_BORDER_COLOR); // Dark red border
        deadStyle.put(mxConstants.STYLE_STROKEWIDTH, "3");
        deadStyle.put(mxConstants.STYLE_FILLCOLOR, STATE_COLORS[5]); // Light red fill
        deadStyle.put(mxConstants.STYLE_DASHED, true); // Dashed border for visual distinction
        stylesheet.putCellStyle("DEAD", deadStyle);
        
        // Dead+Start state (shouldn't happen in valid DFAs, but just in case)
        Map<String, Object> deadStartStyle = new HashMap<>(deadStyle);
        deadStartStyle.put(mxConstants.STYLE_FILLCOLOR, STATE_COLORS[2]); // Keep green fill
        stylesheet.putCellStyle("DEAD_START", deadStartStyle);
        
        // Edge style
        Map<String, Object> edgeStyle = new HashMap<>();
        edgeStyle.put(mxConstants.STYLE_ROUNDED, true);
        edgeStyle.put(mxConstants.STYLE_ORTHOGONAL, false);
        edgeStyle.put(mxConstants.STYLE_EDGE, "elbowConnector");
        edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        edgeStyle.put(mxConstants.STYLE_STROKECOLOR, NORMAL_EDGE_COLOR);
        edgeStyle.put(mxConstants.STYLE_STROKEWIDTH, "1.5");
        edgeStyle.put(mxConstants.STYLE_FONTCOLOR, NORMAL_EDGE_COLOR);
        edgeStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "#FFFFFF");
        edgeStyle.put(mxConstants.STYLE_FONTSIZE, "12");
        
        stylesheet.putCellStyle("EDGE", edgeStyle);
        
        // Special style for self-loops
        Map<String, Object> loopStyle = new HashMap<>(edgeStyle);
        loopStyle.put(mxConstants.STYLE_EDGE, "entityRelation");
        loopStyle.put(mxConstants.STYLE_STROKECOLOR, LOOP_COLOR);
        loopStyle.put(mxConstants.STYLE_FONTCOLOR, LOOP_COLOR);
        loopStyle.put(mxConstants.STYLE_STROKEWIDTH, "2");
        loopStyle.put(mxConstants.STYLE_LOOP, "1");
        
        stylesheet.putCellStyle("LOOP", loopStyle);
        
        // Apply vertex styles according to state type
        // Get the mapping from vertex names to cells
        Map<String, Object> vertexToCellMap = new HashMap<>();
        for (String key : graphAdapter.getVertexToCellMap().keySet()) {
            vertexToCellMap.put(key, graphAdapter.getVertexToCellMap().get(key));
        }
        
        // Get dead states for highlighting
        Set<DFA.State> deadStates = dfa.getDeadStates();
        
        for (Map.Entry<String, Object> entry : vertexToCellMap.entrySet()) {
            String vertexName = entry.getKey();
            Object cell = entry.getValue();
            
            DFA.State state = findStateByName(dfa, vertexName);
            
            if (state != null) {
                boolean isStart = (dfa.getStartState() == state);
                boolean isAccept = dfa.getAcceptStates().contains(state);
                boolean isDead = deadStates.contains(state);
                
                // Apply styles based on state type, respecting highlight flag for dead states
                if (isDead && highlightDeadStates) {
                    // Dead states get priority in visualization if highlighting is enabled
                    if (isStart) {
                        graphAdapter.setCellStyle("DEAD_START", new Object[] { cell });
                    } else {
                        graphAdapter.setCellStyle("DEAD", new Object[] { cell });
                    }
                } else if (isStart && isAccept) {
                    graphAdapter.setCellStyle("START_ACCEPT", new Object[] { cell });
                } else if (isStart) {
                    graphAdapter.setCellStyle("START", new Object[] { cell });
                } else if (isAccept) {
                    graphAdapter.setCellStyle("ACCEPT", new Object[] { cell });
                } else {
                    graphAdapter.setCellStyle("VERTEX", new Object[] { cell });
                }
            }
        }
        
        // Apply edge styles
        Map<LabeledEdge, Object> edgeToCellMap = new HashMap<>();
        for (LabeledEdge key : graphAdapter.getEdgeToCellMap().keySet()) {
            edgeToCellMap.put(key, graphAdapter.getEdgeToCellMap().get(key));
        }
        
        for (Map.Entry<LabeledEdge, Object> entry : edgeToCellMap.entrySet()) {
            LabeledEdge edge = entry.getKey();
            Object cell = entry.getValue();
            
            // Check if this is a loop edge
            if (edge.isLoop()) {
                graphAdapter.setCellStyle("LOOP", new Object[] { cell });
            } else {
                graphAdapter.setCellStyle("EDGE", new Object[] { cell });
            }
        }
    }
    
    /**
     * Helper method to find a state by name
     */
    private DFA.State findStateByName(DFA dfa, String name) {
        for (DFA.State state : dfa.getStates()) {
            if (state.getName().equals(name)) {
                return state;
            }
        }
        return null;
    }
    
    /**
     * Attempts to simplify a large set of symbols by identifying ranges.
     */
    private String simplifySymbols(List<Character> symbols) {
        if (symbols.size() <= 5) {
            return symbolsToString(symbols);
        }
        
        // Identify ranges in the sorted symbols list
        StringBuilder result = new StringBuilder();
        int start = 0;
        
        for (int i = 1; i <= symbols.size(); i++) {
            // Check if we've reached the end of a range
            if (i == symbols.size() || (int)symbols.get(i) != (int)symbols.get(i-1) + 1) {
                if (i - start >= MIN_RANGE_SIZE) {
                    // This is a range of multiple consecutive characters
                    result.append(symbols.get(start))
                          .append("-")
                          .append(symbols.get(i-1))
                          .append(",");
                } else {
                    // Individual characters
                    for (int j = start; j < i; j++) {
                        appendSymbol(result, symbols.get(j));
                        result.append(",");
                    }
                }
                
                if (i < symbols.size()) {
                    start = i;
                }
            }
        }
        
        // Remove trailing comma
        if (result.length() > 0 && result.charAt(result.length() - 1) == ',') {
            result.setLength(result.length() - 1);
        }
        
        // If still too many, show a summary
        if (result.length() > MAX_DISPLAY_SYMBOLS) {
            return "[" + symbols.size() + " symbols]";
        }
        
        return result.toString();
    }
    
    /**
     * Convert a list of symbols to a string
     */
    private String symbolsToString(List<Character> symbols) {
        StringBuilder sb = new StringBuilder();
        for (Character c : symbols) {
            appendSymbol(sb, c);
            sb.append(",");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // Remove trailing comma
        }
        return sb.toString();
    }
    
    /**
     * Checks if a state has a Kleene star (self-loop)
     */
    private boolean hasKleeneStar(DFA dfa, String stateName, char symbol) {
        DFA.State state = findStateByName(dfa, stateName);
        if (state == null) {
            return false;
        }
        
        DFA.StateTransition transition = new DFA.StateTransition(state, symbol);
        DFA.State targetState = dfa.getTransitions().get(transition);
        
        return targetState == state;
    }
    
    public static class LabeledEdge extends DefaultEdge {
        private String label = "";
        private List<Character> originalSymbols;
        private boolean isLoop = false;
        
        public void setLabel(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
        
        public void setOriginalSymbols(List<Character> symbols) {
            this.originalSymbols = new ArrayList<>(symbols);
        }
        
        public List<Character> getOriginalSymbols() {
            return originalSymbols;
        }
        
        public boolean isLoop() {
            return isLoop;
        }
        
        public void setIsLoop(boolean isLoop) {
            this.isLoop = isLoop;
        }
        
        @Override
        public String toString() {
            return label;
        }
    }
} 