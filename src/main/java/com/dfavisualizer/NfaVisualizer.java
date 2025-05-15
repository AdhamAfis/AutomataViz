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
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxStylesheet;

/**
 * Visualizes an NFA as a directed graph.
 */
public class NfaVisualizer {

    private static final int STATE_SIZE = 50;
    private static final String[] STATE_COLORS = {
        "#F2E6FF", // Light purple
        "#E6F2FF", // Light blue
        "#FFF2CC", // Light yellow
        "#E6FFCC", // Light green
        "#FFE6CC", // Light orange
    };

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
        
        // Create a component with the visualization
        mxGraphComponent graphComponent = createGraphComponent(graphAdapter);
        
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
    private mxGraphComponent createGraphComponent(JGraphXAdapter<String, LabeledEdge> graphAdapter) {
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
        
        return graphComponent;
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
        vertexStyle.put(mxConstants.STYLE_FONTSIZE, "12");
        vertexStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        vertexStyle.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
        vertexStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        vertexStyle.put(mxConstants.STYLE_STROKEWIDTH, "1.5");
        vertexStyle.put(mxConstants.STYLE_SHADOW, true);
        vertexStyle.put(mxConstants.STYLE_ROUNDED, true);
        vertexStyle.put(mxConstants.STYLE_SPACING, "10");
        
        stylesheet.putCellStyle("VERTEX", vertexStyle);
        
        // Start state style (additional green border)
        Map<String, Object> startStyle = new HashMap<>(vertexStyle);
        startStyle.put(mxConstants.STYLE_STROKECOLOR, "#00AA00");
        startStyle.put(mxConstants.STYLE_STROKEWIDTH, "3");
        startStyle.put(mxConstants.STYLE_FILLCOLOR, STATE_COLORS[0]);
        stylesheet.putCellStyle("START", startStyle);
        
        // Accept state style (double border)
        Map<String, Object> acceptStyle = new HashMap<>(vertexStyle);
        acceptStyle.put(mxConstants.STYLE_DASHED, false);
        acceptStyle.put(mxConstants.STYLE_STROKEWIDTH, "3");
        acceptStyle.put(mxConstants.STYLE_FILLCOLOR, STATE_COLORS[1]);
        stylesheet.putCellStyle("ACCEPT", acceptStyle);
        
        // Start+Accept state style (double green border)
        Map<String, Object> startAcceptStyle = new HashMap<>(vertexStyle);
        startAcceptStyle.put(mxConstants.STYLE_STROKECOLOR, "#00AA00");
        startAcceptStyle.put(mxConstants.STYLE_STROKEWIDTH, "3");
        startAcceptStyle.put(mxConstants.STYLE_FILLCOLOR, STATE_COLORS[2]);
        stylesheet.putCellStyle("START_ACCEPT", startAcceptStyle);
        
        // Regular state styles with different colors
        for (int i = 0; i < STATE_COLORS.length; i++) {
            Map<String, Object> colorStyle = new HashMap<>(vertexStyle);
            colorStyle.put(mxConstants.STYLE_FILLCOLOR, STATE_COLORS[i]);
            stylesheet.putCellStyle("COLOR" + i, colorStyle);
        }
        
        // Edge style (transitions)
        Map<String, Object> edgeStyle = new HashMap<>();
        edgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#000066");
        edgeStyle.put(mxConstants.STYLE_STROKEWIDTH, "1.5");
        edgeStyle.put(mxConstants.STYLE_ROUNDED, true);
        edgeStyle.put(mxConstants.STYLE_FONTSIZE, "12");
        edgeStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        edgeStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "#FFFFFF");
        edgeStyle.put(mxConstants.STYLE_LABEL_BORDERCOLOR, "#AAAAAA");
        edgeStyle.put(mxConstants.STYLE_SPACING, "2");
        
        stylesheet.putCellStyle("EDGE", edgeStyle);
        
        // Apply styles to all cells
        for (Object cell : graphAdapter.getVertexToCellMap().values()) {
            String stateName = graphAdapter.convertValueToString(cell);
            int stateId = Integer.parseInt(stateName.substring(1)); // Remove 'q' prefix
            
            boolean isStart = (stateId == nfa.getStartState());
            boolean isAccept = nfa.getAcceptStates().contains(stateId);
            
            if (isStart && isAccept) {
                graphAdapter.setCellStyle("START_ACCEPT", new Object[]{cell});
            } else if (isStart) {
                graphAdapter.setCellStyle("START", new Object[]{cell});
            } else if (isAccept) {
                graphAdapter.setCellStyle("ACCEPT", new Object[]{cell});
            } else {
                // Use different colors for different states
                int colorIndex = stateId % STATE_COLORS.length;
                graphAdapter.setCellStyle("COLOR" + colorIndex, new Object[]{cell});
            }
        }
        
        for (Object cell : graphAdapter.getEdgeToCellMap().values()) {
            graphAdapter.setCellStyle("EDGE", new Object[]{cell});
        }
    }
    
    /**
     * Labeled edge class for the graph
     */
    public static class LabeledEdge extends DefaultEdge {
        private String label = "";
        private List<Character> originalSymbols;
        
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
        
        @Override
        public String toString() {
            return label;
        }
    }
} 