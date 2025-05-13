package com.dfavisualizer;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
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
        // Create a directed graph
        DefaultDirectedGraph<String, LabeledEdge> graph = 
            new DefaultDirectedGraph<>(LabeledEdge.class);
        
        // Add all states as vertices
        for (DFA.State state : dfa.getStates()) {
            graph.addVertex(state.getName());
        }
        
        // Add all transitions as edges
        for (Map.Entry<DFA.StateTransition, DFA.State> entry : dfa.getTransitions().entrySet()) {
            DFA.StateTransition transition = entry.getKey();
            DFA.State targetState = entry.getValue();
            
            String sourceLabel = transition.getState().getName();
            String targetLabel = targetState.getName();
            char symbol = transition.getSymbol();
            
            LabeledEdge edge = graph.addEdge(sourceLabel, targetLabel);
            if (edge != null) {
                edge.setLabel(String.valueOf(symbol));
            }
        }
        
        // Create the graph adapter
        JGraphXAdapter<String, LabeledEdge> graphAdapter = 
            new JGraphXAdapter<>(graph);
        
        // Set up the style for the graph
        setupGraphStyle(graphAdapter, dfa);
        
        // Create a layout for the graph
        mxIGraphLayout layout = new mxCircleLayout(graphAdapter);
        layout.execute(graphAdapter.getDefaultParent());
        
        // Create a component with the visualization
        mxGraphComponent graphComponent = new mxGraphComponent(graphAdapter);
        graphComponent.setConnectable(false);
        graphComponent.getGraph().setAllowDanglingEdges(false);
        graphComponent.getGraph().setEdgeLabelsMovable(false);
        graphComponent.setToolTips(true);
        
        return graphComponent;
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
        
        // Style for start state
        Map<String, Object> startStyle = new HashMap<>(vertexStyle);
        startStyle.put(mxConstants.STYLE_FILLCOLOR, "#C0FFC0"); // Light green
        
        // Style for accept states
        Map<String, Object> acceptStyle = new HashMap<>(vertexStyle);
        acceptStyle.put(mxConstants.STYLE_STROKEWIDTH, 3);
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
        
        // Register the styles
        stylesheet.putCellStyle("VERTEX", vertexStyle);
        stylesheet.putCellStyle("START", startStyle);
        stylesheet.putCellStyle("ACCEPT", acceptStyle);
        stylesheet.putCellStyle("START_ACCEPT", startAcceptStyle);
        stylesheet.putCellStyle("EDGE", edgeStyle);
        
        // Apply styles to vertices and edges
        Object[] vertices = graphAdapter.getVertexToCellMap().values().toArray();
        for (Object cell : vertices) {
            String stateName = graphAdapter.getVertexToCellMap().entrySet().stream()
                .filter(entry -> entry.getValue().equals(cell))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("");
            
            boolean isStart = dfa.getStartState().getName().equals(stateName);
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
            graphAdapter.setCellStyle("EDGE", new Object[] { cell });
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