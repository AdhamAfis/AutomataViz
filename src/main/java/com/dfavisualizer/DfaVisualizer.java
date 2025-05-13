package com.dfavisualizer;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

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
import com.mxgraph.view.mxEdgeStyle;
import com.mxgraph.view.mxStylesheet;

/**
 * Visualizes a DFA as a directed graph.
 */
public class DfaVisualizer {

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
        
        // Add all states as vertices
        for (DFA.State state : dfa.getStates()) {
            graph.addVertex(state.getName());
        }
        
        // Group transitions by source-target pair to combine multiple symbols
        Map<String, Map<String, StringBuilder>> combinedTransitions = new HashMap<>();
        
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
                .computeIfAbsent(targetLabel, k -> new StringBuilder())
                .append(symbol).append(",");
        }
        
        // Add combined edges to the graph
        for (Map.Entry<String, Map<String, StringBuilder>> sourceEntry : combinedTransitions.entrySet()) {
            String sourceLabel = sourceEntry.getKey();
            
            for (Map.Entry<String, StringBuilder> targetEntry : sourceEntry.getValue().entrySet()) {
                String targetLabel = targetEntry.getKey();
                String symbols = targetEntry.getValue().toString();
                
                // Clean up the symbols string
                if (symbols.endsWith(",")) {
                    symbols = symbols.substring(0, symbols.length() - 1);
                }
                
                // Simplify if there are many transitions
                if (symbols.length() > 10) {
                    // Try to find ranges of characters
                    symbols = simplifySymbols(symbols.split(","));
                }
                
                // Create the edge with the combined label
                LabeledEdge edge = graph.addEdge(sourceLabel, targetLabel);
                if (edge != null) {
                    edge.setLabel(symbols);
                }
            }
        }
        
        // Create the graph adapter
        JGraphXAdapter<String, LabeledEdge> graphAdapter = 
            new JGraphXAdapter<>(graph);
        
        // Set up the style for the graph
        setupGraphStyle(graphAdapter, dfa);
        
        // Create a layout for the graph
        mxIGraphLayout layout;
        if (dfa.getStates().size() > 5) {
            // Use hierarchical layout for larger DFAs
            layout = new mxHierarchicalLayout(graphAdapter);
            ((mxHierarchicalLayout)layout).setIntraCellSpacing(50.0);
            ((mxHierarchicalLayout)layout).setInterRankCellSpacing(80.0);
        } else {
            // Use circle layout for smaller DFAs
            layout = new mxCircleLayout(graphAdapter);
            ((mxCircleLayout)layout).setRadius(150.0);
        }
        
        // Execute the layout
        layout.execute(graphAdapter.getDefaultParent());
        
        // Adjust the graph for better visualization
        adjustEdgeLabels(graphAdapter);
        
        // Create a component with the visualization
        mxGraphComponent graphComponent = new mxGraphComponent(graphAdapter);
        graphComponent.setConnectable(false);
        graphComponent.getGraph().setAllowDanglingEdges(false);
        graphComponent.getGraph().setEdgeLabelsMovable(false);
        graphComponent.setToolTips(true);
        graphComponent.setBorder(null);
        graphComponent.getViewport().setOpaque(true);
        graphComponent.getViewport().setBackground(graphComponent.getBackground());
        
        return graphComponent;
    }
    
    /**
     * Attempts to simplify a large set of symbols by identifying ranges.
     */
    private String simplifySymbols(String[] symbols) {
        if (symbols.length <= 5) {
            return String.join(",", symbols);
        }
        
        // Sort the symbols to find consecutive ranges
        StringBuilder result = new StringBuilder();
        
        // For simplicity, just display count if too many
        if (symbols.length > 10) {
            return "[" + symbols.length + " symbols]";
        }
        
        return String.join(",", symbols);
    }
    
    /**
     * Adjusts edge label positions for better visibility.
     */
    private void adjustEdgeLabels(JGraphXAdapter<String, LabeledEdge> graphAdapter) {
        Object[] edges = graphAdapter.getEdgeToCellMap().values().toArray();
        for (Object edge : edges) {
            // Adjust geometry to move label away from line
            mxGeometry geometry = graphAdapter.getModel().getGeometry(edge);
            if (geometry != null) {
                geometry = (mxGeometry) geometry.clone();
                
                // For self-loops, move the label inside the loop
                if (graphAdapter.getModel().getTerminal(edge, true) == 
                    graphAdapter.getModel().getTerminal(edge, false)) {
                    geometry.setOffset(new mxPoint(0, -20));
                }
                
                graphAdapter.getModel().setGeometry(edge, geometry);
            }
        }
    }
    
    /**
     * Sets up the visual style of the graph, such as colors and shapes for states.
     */
    private void setupGraphStyle(JGraphXAdapter<String, LabeledEdge> graphAdapter, DFA dfa) {
        mxStylesheet stylesheet = graphAdapter.getStylesheet();
        
        // Style for states
        Map<String, Object> vertexStyle = new HashMap<>();
        vertexStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
        vertexStyle.put(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_ELLIPSE);
        vertexStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        vertexStyle.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
        vertexStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        vertexStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        vertexStyle.put(mxConstants.STYLE_FONTSIZE, 16);
        vertexStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
        vertexStyle.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
        
        // Style for start state
        Map<String, Object> startStyle = new HashMap<>(vertexStyle);
        startStyle.put(mxConstants.STYLE_FILLCOLOR, "#C0FFC0"); // Light green
        
        // Style for accept states - thicker border instead of double circle
        // since STYLE_DOUBLE is not available in this version of mxGraph
        Map<String, Object> acceptStyle = new HashMap<>(vertexStyle);
        acceptStyle.put(mxConstants.STYLE_STROKEWIDTH, 4);
        acceptStyle.put(mxConstants.STYLE_DASHED, false);
        
        // Style for start-accept states
        Map<String, Object> startAcceptStyle = new HashMap<>(acceptStyle);
        startAcceptStyle.put(mxConstants.STYLE_FILLCOLOR, "#C0FFC0"); // Light green
        
        // Style for transitions
        Map<String, Object> edgeStyle = new HashMap<>();
        edgeStyle.put(mxConstants.STYLE_ROUNDED, true);
        edgeStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
        edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        edgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        edgeStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        edgeStyle.put(mxConstants.STYLE_FONTSIZE, 14);
        edgeStyle.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
        edgeStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_BOTTOM);
        edgeStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "#FFFFFF");
        edgeStyle.put(mxConstants.STYLE_LABEL_POSITION, mxConstants.ALIGN_CENTER);
        
        // Style for self-loop edges
        Map<String, Object> loopStyle = new HashMap<>(edgeStyle);
        loopStyle.put(mxConstants.STYLE_EDGE, mxEdgeStyle.Loop);
        loopStyle.put(mxConstants.STYLE_LOOP, mxConstants.DIRECTION_WEST);
        loopStyle.put(mxConstants.STYLE_LABEL_POSITION, mxConstants.ALIGN_LEFT);
        
        // Register the styles
        stylesheet.putCellStyle("VERTEX", vertexStyle);
        stylesheet.putCellStyle("START", startStyle);
        stylesheet.putCellStyle("ACCEPT", acceptStyle);
        stylesheet.putCellStyle("START_ACCEPT", startAcceptStyle);
        stylesheet.putCellStyle("EDGE", edgeStyle);
        stylesheet.putCellStyle("LOOP", loopStyle);
        
        // Apply styles to vertices and edges
        Object[] vertices = graphAdapter.getVertexToCellMap().values().toArray();
        for (Object cell : vertices) {
            String stateName = graphAdapter.getVertexToCellMap().entrySet().stream()
                .filter(entry -> entry.getValue().equals(cell))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("");
            
            boolean isStart = dfa.getStartState() != null && 
                              dfa.getStartState().getName().equals(stateName);
            boolean isAccept = dfa.getAcceptStates().stream()
                .anyMatch(state -> state.getName().equals(stateName));
            
            String styleName;
            if (isStart && isAccept) {
                styleName = "START_ACCEPT";
            } else if (isStart) {
                styleName = "START";
            } else if (isAccept) {
                styleName = "ACCEPT";
            } else {
                styleName = "VERTEX";
            }
            
            graphAdapter.setCellStyle(styleName, new Object[] { cell });
        }
        
        // Apply style to edges
        Object[] edges = graphAdapter.getEdgeToCellMap().values().toArray();
        for (Object cell : edges) {
            // Check if it's a self-loop
            if (graphAdapter.getModel().getTerminal(cell, true) == 
                graphAdapter.getModel().getTerminal(cell, false)) {
                graphAdapter.setCellStyle("LOOP", new Object[] { cell });
            } else {
                graphAdapter.setCellStyle("EDGE", new Object[] { cell });
            }
        }
    }
    
    /**
     * Custom edge class that can hold a label.
     */
    public static class LabeledEdge extends DefaultEdge {
        private String label = "";
        
        public void setLabel(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
        
        @Override
        public String toString() {
            return label;
        }
    }
} 