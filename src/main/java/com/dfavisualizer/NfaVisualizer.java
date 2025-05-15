package com.dfavisualizer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
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
 * Visualizes an NFA as a directed graph.
 */
public class NfaVisualizer {

    private static final int STATE_SIZE = 50;
    // Updated color scheme for better contrast and distinguishability
    private static final String[] STATE_COLORS = {
        "#F2E6FF", // Light purple - Regular states
        "#FFEBCC", // Light orange - Accept states
        "#E6FFCC", // Light green - Start states
        "#CCFFD5", // Mint green - Special states
        "#E6F2FF"  // Light blue - Intermediate states
    };
    
    // Colors for edges
    private static final String LOOP_COLOR = "#6666FF"; // Blue for loops
    private static final String NORMAL_EDGE_COLOR = "#666666"; // Dark gray for normal edges
    private static final String EPSILON_COLOR = "#FF6666"; // Red for epsilon transitions

    /**
     * Creates a graphical visualization of the NFA.
     * 
     * @param nfa The NFA to visualize
     * @return A Swing component containing the visualization
     */
    public JComponent visualizeNfa(NFA nfa) {
        if (nfa == null || nfa.getStates().isEmpty()) {
            throw new IllegalArgumentException("Cannot visualize empty NFA");
        }
        
        // Create a directed graph
        DefaultDirectedGraph<String, LabeledEdge> graph = 
            new DefaultDirectedGraph<>(LabeledEdge.class);
        
        // Add all states as vertices, sorting them for consistent visualization
        List<Integer> sortedStates = new ArrayList<>(nfa.getStates());
        Collections.sort(sortedStates);
        
        for (Integer state : sortedStates) {
            graph.addVertex("q" + state);
        }
        
        // Group transitions by source-target pair to combine multiple symbols
        Map<Integer, Map<Integer, List<Character>>> combinedTransitions = new HashMap<>();
        
        // Add all transitions as edges
        for (Map.Entry<NFA.NFATransition, Set<Integer>> entry : nfa.getTransitions().entrySet()) {
            NFA.NFATransition transition = entry.getKey();
            Set<Integer> targetStates = entry.getValue();
            
            int sourceState = transition.getState();
            char symbol = transition.getSymbol();
            
            for (Integer targetState : targetStates) {
                // Combine transitions between the same states
                combinedTransitions
                    .computeIfAbsent(sourceState, k -> new HashMap<>())
                    .computeIfAbsent(targetState, k -> new ArrayList<>())
                    .add(symbol);
            }
        }
        
        // Add combined edges to the graph
        for (Map.Entry<Integer, Map<Integer, List<Character>>> sourceEntry : combinedTransitions.entrySet()) {
            int sourceState = sourceEntry.getKey();
            String sourceLabel = "q" + sourceState;
            
            for (Map.Entry<Integer, List<Character>> targetEntry : sourceEntry.getValue().entrySet()) {
                int targetState = targetEntry.getKey();
                String targetLabel = "q" + targetState;
                List<Character> symbolsList = targetEntry.getValue();
                
                // Sort the symbols for consistent display
                Collections.sort(symbolsList);
                
                // Create a readable label
                String symbolsLabel = createSymbolsLabel(symbolsList);
                
                // Create the edge with the combined label
                LabeledEdge edge = graph.addEdge(sourceLabel, targetLabel);
                if (edge != null) {
                    edge.setLabel(symbolsLabel);
                    edge.setOriginalSymbols(symbolsList);
                    // Mark if this is a self-loop
                    edge.setIsLoop(sourceState == targetState);
                    // Check if this is an epsilon transition
                    edge.setIsEpsilon(symbolsList.size() == 1 && symbolsList.get(0) == NFA.EPSILON);
                }
            }
        }
        
        // Create the graph adapter
        JGraphXAdapter<String, LabeledEdge> graphAdapter = 
            new JGraphXAdapter<>(graph);
        
        // Set up the style for the graph
        setupGraphStyle(graphAdapter, nfa);
        
        // Create a layout for the graph based on size and structure
        mxIGraphLayout layout = createAppropriateLayout(graphAdapter, nfa);
        
        // Execute the layout
        layout.execute(graphAdapter.getDefaultParent());
        
        // Adjust edge labels
        adjustEdgeLabels(graphAdapter);
        
        // Create a component with the visualization
        mxGraphComponent graphComponent = createGraphComponent(graphAdapter, nfa);
        
        return graphComponent;
    }
    
    /**
     * Creates an appropriate layout based on NFA characteristics
     */
    private mxIGraphLayout createAppropriateLayout(JGraphXAdapter<String, LabeledEdge> graphAdapter, NFA nfa) {
        int stateCount = nfa.getStates().size();
        
        if (stateCount > 8) {
            // Use hierarchical layout for larger NFAs
            mxHierarchicalLayout layout = new mxHierarchicalLayout(graphAdapter);
            layout.setIntraCellSpacing(50.0);
            layout.setInterRankCellSpacing(100.0);
            layout.setDisableEdgeStyle(false);
            layout.setOrientation(SwingConstants.WEST);
            return layout;
        } else {
            // Use circle layout for smaller NFAs
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
            return ""; // Should never happen
        }
        
        StringBuilder sb = new StringBuilder();
        for (Character c : symbols) {
            if (c == NFA.EPSILON) {
                sb.append("ε"); // Epsilon for empty transitions
            } else {
                appendSymbol(sb, c);
            }
            sb.append(",");
        }
        
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // Remove trailing comma
        }
        
        return sb.toString();
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
    private mxGraphComponent createGraphComponent(JGraphXAdapter<String, LabeledEdge> graphAdapter, NFA nfa) {
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
        graphComponent.getViewport().setBackground(new Color(250, 250, 250)); // Very light gray
        
        // Enable antialiasing for smoother graphics
        graphComponent.setAntiAlias(true);
        graphComponent.setTextAntiAlias(true);
        
        // Add tooltip with general information
        addNfaTooltip(graphComponent, nfa);
        
        return graphComponent;
    }
    
    /**
     * Add general tooltip information for the NFA
     */
    private void addNfaTooltip(mxGraphComponent graphComponent, NFA nfa) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html><b>NFA Visualization</b><br>");
        tooltip.append("Start State: q").append(nfa.getStartState()).append("<br>");
        
        tooltip.append("Accept States: ");
        for (Integer acceptState : nfa.getAcceptStates()) {
            tooltip.append("q").append(acceptState).append(" ");
        }
        
        tooltip.append("<br>");
        tooltip.append("Total States: ").append(nfa.getStates().size()).append("<br>");
        tooltip.append("Blue edges represent self-loops<br>");
        tooltip.append("Red edges represent epsilon transitions<br>");
        tooltip.append("</html>");
        
        graphComponent.setToolTipText(tooltip.toString());
    }
    
    /**
     * Adjusts edge labels for better visibility
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
                
                // Make self-loops more noticeable with a different color
                graphAdapter.setCellStyles(mxConstants.STYLE_STROKECOLOR, LOOP_COLOR, new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "2", new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_FONTCOLOR, LOOP_COLOR, new Object[] { edge });
            } else {
                // For regular edges, ensure they have the correct style
                graphAdapter.setCellStyles(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC, new Object[] { edge });
                
                // Check if this is an epsilon transition
                Object value = graphAdapter.getModel().getValue(edge);
                if (value instanceof LabeledEdge) {
                    LabeledEdge labeledEdge = (LabeledEdge) value;
                    if (labeledEdge.isEpsilon()) {
                        // Epsilon transitions get a special color
                        graphAdapter.setCellStyles(mxConstants.STYLE_STROKECOLOR, EPSILON_COLOR, new Object[] { edge });
                        graphAdapter.setCellStyles(mxConstants.STYLE_FONTCOLOR, EPSILON_COLOR, new Object[] { edge });
                        graphAdapter.setCellStyles(mxConstants.STYLE_DASHED, "1", new Object[] { edge });
                    } else {
                        // Regular transitions
                        graphAdapter.setCellStyles(mxConstants.STYLE_STROKECOLOR, NORMAL_EDGE_COLOR, new Object[] { edge });
                        graphAdapter.setCellStyles(mxConstants.STYLE_FONTCOLOR, NORMAL_EDGE_COLOR, new Object[] { edge });
                    }
                }
            }
            
            // Make edge labels more readable
            graphAdapter.setCellStyles(mxConstants.STYLE_FONTSIZE, "12", new Object[] { edge });
            graphAdapter.setCellStyles(mxConstants.STYLE_FONTSTYLE, String.valueOf(1), new Object[] { edge });
        }
    }
    
    /**
     * Sets up visual styles for the graph
     */
    private void setupGraphStyle(JGraphXAdapter<String, LabeledEdge> graphAdapter, NFA nfa) {
        // Get the stylesheet for customization
        mxStylesheet stylesheet = graphAdapter.getStylesheet();

        // Base style for all states (vertices)
        Map<String, Object> vertexStyle = new HashMap<>();
        vertexStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
        vertexStyle.put(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_ELLIPSE);
        vertexStyle.put(mxConstants.STYLE_FONTSIZE, "14");
        vertexStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        vertexStyle.put(mxConstants.STYLE_FILLCOLOR, STATE_COLORS[0]); // Default state color
        vertexStyle.put(mxConstants.STYLE_STROKECOLOR, "#444444");
        vertexStyle.put(mxConstants.STYLE_STROKEWIDTH, "1.5");
        vertexStyle.put(mxConstants.STYLE_SHADOW, true);
        vertexStyle.put(mxConstants.STYLE_ROUNDED, true);
        vertexStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
        vertexStyle.put(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_MIDDLE);
        vertexStyle.put(mxConstants.STYLE_SPACING, "10");
        
        stylesheet.putCellStyle("VERTEX", vertexStyle);
        
        // Start state style (additional green border)
        Map<String, Object> startStyle = new HashMap<>(vertexStyle);
        startStyle.put(mxConstants.STYLE_STROKECOLOR, "#008800"); // Darker green
        startStyle.put(mxConstants.STYLE_STROKEWIDTH, "3");
        startStyle.put(mxConstants.STYLE_FILLCOLOR, STATE_COLORS[2]); // Green for start state
        stylesheet.putCellStyle("START", startStyle);
        
        // Accept state style (double border)
        Map<String, Object> acceptStyle = new HashMap<>(vertexStyle);
        acceptStyle.put(mxConstants.STYLE_DASHED, false);
        acceptStyle.put(mxConstants.STYLE_STROKEWIDTH, "3");
        acceptStyle.put(mxConstants.STYLE_FILLCOLOR, STATE_COLORS[1]); // Orange for accept states
        stylesheet.putCellStyle("ACCEPT", acceptStyle);
        
        // Start+Accept state style (double green border)
        Map<String, Object> startAcceptStyle = new HashMap<>(vertexStyle);
        startAcceptStyle.put(mxConstants.STYLE_STROKECOLOR, "#008800"); // Dark green
        startAcceptStyle.put(mxConstants.STYLE_STROKEWIDTH, "3");
        startAcceptStyle.put(mxConstants.STYLE_FILLCOLOR, "#FFD6A5"); // Blend of green and orange
        startAcceptStyle.put(mxConstants.STYLE_DASHED, false);
        stylesheet.putCellStyle("START_ACCEPT", startAcceptStyle);
        
        // Standard edge style
        Map<String, Object> edgeStyle = new HashMap<>();
        edgeStyle.put(mxConstants.STYLE_ROUNDED, true);
        edgeStyle.put(mxConstants.STYLE_ORTHOGONAL, false);
        edgeStyle.put(mxConstants.STYLE_EDGE, "elbowConnector");
        edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        edgeStyle.put(mxConstants.STYLE_STROKECOLOR, NORMAL_EDGE_COLOR);
        edgeStyle.put(mxConstants.STYLE_FONTCOLOR, NORMAL_EDGE_COLOR);
        edgeStyle.put(mxConstants.STYLE_STROKEWIDTH, "1.5");
        edgeStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "#FFFFFF");
        edgeStyle.put(mxConstants.STYLE_FONTSIZE, "12");
        
        stylesheet.putCellStyle("EDGE", edgeStyle);
        
        // Special style for epsilon transitions
        Map<String, Object> epsilonStyle = new HashMap<>(edgeStyle);
        epsilonStyle.put(mxConstants.STYLE_STROKECOLOR, EPSILON_COLOR);
        epsilonStyle.put(mxConstants.STYLE_FONTCOLOR, EPSILON_COLOR);
        epsilonStyle.put(mxConstants.STYLE_DASHED, "1");
        
        stylesheet.putCellStyle("EPSILON", epsilonStyle);
        
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
        
        for (Map.Entry<String, Object> entry : vertexToCellMap.entrySet()) {
            String vertexName = entry.getKey();
            Object cell = entry.getValue();
            
            // Extract state number from vertex name (format: "q{number}")
            int stateNum = Integer.parseInt(vertexName.substring(1));
            
            boolean isStart = (stateNum == nfa.getStartState());
            boolean isAccept = nfa.getAcceptStates().contains(stateNum);
            
            if (isStart && isAccept) {
                graphAdapter.setCellStyle("START_ACCEPT", new Object[] { cell });
            } else if (isStart) {
                graphAdapter.setCellStyle("START", new Object[] { cell });
            } else if (isAccept) {
                graphAdapter.setCellStyle("ACCEPT", new Object[] { cell });
            } else {
                graphAdapter.setCellStyle("VERTEX", new Object[] { cell });
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
            
            if (edge.isLoop()) {
                graphAdapter.setCellStyle("LOOP", new Object[] { cell });
            } else if (edge.isEpsilon()) {
                graphAdapter.setCellStyle("EPSILON", new Object[] { cell });
            } else {
                graphAdapter.setCellStyle("EDGE", new Object[] { cell });
            }
        }
    }
    
    public static class LabeledEdge extends DefaultEdge {
        private String label = "";
        private List<Character> originalSymbols;
        private boolean isLoop = false;
        private boolean isEpsilon = false;
        
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
        
        public boolean isEpsilon() {
            return isEpsilon;
        }
        
        public void setIsEpsilon(boolean isEpsilon) {
            this.isEpsilon = isEpsilon;
        }
        
        @Override
        public String toString() {
            return label;
        }
    }
}
