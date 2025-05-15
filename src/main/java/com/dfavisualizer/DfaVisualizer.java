package com.dfavisualizer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.mxgraph.view.mxEdgeStyle;
import com.mxgraph.view.mxStylesheet;

/**
 * Visualizes a DFA as a directed graph.
 */
public class DfaVisualizer {

    private static final int STATE_SIZE = 50;
    private static final String[] STATE_COLORS = {
        "#E6F2FF", // Light blue
        "#FFF2CC", // Light yellow
        "#E6FFCC", // Light green
        "#FFE6CC", // Light orange
        "#F2E6FF"  // Light purple
    };
    
    // For transition grouping
    private static final int MAX_DISPLAY_SYMBOLS = 12;
    private static final int MIN_RANGE_SIZE = 3;

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
        
        return graphComponent;
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
     * Adjusts edge label positions for better visibility.
     */
    private void adjustEdgeLabels(JGraphXAdapter<String, LabeledEdge> graphAdapter) {
        Object[] edges = graphAdapter.getEdgeToCellMap().values().toArray();
        for (Object edge : edges) {
            // Adjust geometry to move label away from line
            mxGeometry geometry = graphAdapter.getModel().getGeometry(edge);
            if (geometry != null) {
                geometry = (mxGeometry) geometry.clone();
                
                // For self-loops, position them based on their location in the graph
                if (graphAdapter.getModel().getTerminal(edge, true) == 
                    graphAdapter.getModel().getTerminal(edge, false)) {
                    
                    // Get the state cell's geometry to determine its position in the graph
                    Object stateCell = graphAdapter.getModel().getTerminal(edge, true);
                    mxGeometry stateGeometry = graphAdapter.getModel().getGeometry(stateCell);
                    
                    if (stateGeometry != null) {
                        // Position self-loops differently based on location
                        double centerX = graphAdapter.getView().getGraphBounds().getCenterX();
                        double centerY = graphAdapter.getView().getGraphBounds().getCenterY();
                        
                        // Determine which quadrant the state is in relative to center
                        boolean isLeft = stateGeometry.getCenterX() < centerX;
                        boolean isTop = stateGeometry.getCenterY() < centerY;
                        
                        // Choose loop direction based on quadrant
                        if (isLeft && isTop) {
                            // Top-left quadrant: loop to the northwest
                            geometry.setOffset(new mxPoint(-30, -30));
                        } else if (!isLeft && isTop) {
                            // Top-right quadrant: loop to the northeast
                            geometry.setOffset(new mxPoint(30, -30));
                        } else if (isLeft && !isTop) {
                            // Bottom-left quadrant: loop to the southwest
                            geometry.setOffset(new mxPoint(-30, 30));
                        } else {
                            // Bottom-right quadrant: loop to the southeast
                            geometry.setOffset(new mxPoint(30, 30));
                        }
                    } else {
                        // Fallback to a default offset
                        geometry.setOffset(new mxPoint(0, -30));
                    }
                } else {
                    // Better position for standard edges
                    geometry.setOffset(new mxPoint(0, -10));
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
        
        // Style for regular states
        Map<String, Object> vertexStyle = new HashMap<>();
        vertexStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
        vertexStyle.put(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_ELLIPSE);
        vertexStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        vertexStyle.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
        vertexStyle.put(mxConstants.STYLE_STROKECOLOR, "#2C3E50");
        vertexStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        vertexStyle.put(mxConstants.STYLE_FONTSIZE, 16);
        vertexStyle.put(mxConstants.STYLE_FONTFAMILY, "Arial");
        vertexStyle.put(mxConstants.STYLE_FONTSTYLE, mxConstants.FONT_BOLD);
        vertexStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
        vertexStyle.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
        vertexStyle.put(mxConstants.STYLE_SHADOW, true);
        vertexStyle.put(mxConstants.STYLE_ROUNDED, true);
        vertexStyle.put(mxConstants.STYLE_OPACITY, 90);
        vertexStyle.put(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_MIDDLE);
        vertexStyle.put(mxConstants.STYLE_SPACING, 10);
        
        // Set fixed size for states using custom properties
        vertexStyle.put("width", STATE_SIZE);
        vertexStyle.put("height", STATE_SIZE);
        
        // Style for start state
        Map<String, Object> startStyle = new HashMap<>(vertexStyle);
        startStyle.put(mxConstants.STYLE_FILLCOLOR, "#82E0AA"); // Light green
        startStyle.put(mxConstants.STYLE_STROKECOLOR, "#186A3B"); // Dark green
        startStyle.put(mxConstants.STYLE_FONTCOLOR, "#196F3D");
        
        // Style for accept states - thicker border and a distinctive pattern
        Map<String, Object> acceptStyle = new HashMap<>(vertexStyle);
        acceptStyle.put(mxConstants.STYLE_STROKEWIDTH, 4);
        acceptStyle.put(mxConstants.STYLE_STROKECOLOR, "#5D6D7E");
        acceptStyle.put(mxConstants.STYLE_FILLCOLOR, "#D5F5E3");
        acceptStyle.put(mxConstants.STYLE_DASHED, false);
        
        // Style for start-accept states
        Map<String, Object> startAcceptStyle = new HashMap<>(acceptStyle);
        startAcceptStyle.put(mxConstants.STYLE_FILLCOLOR, "#82E0AA"); // Light green
        startAcceptStyle.put(mxConstants.STYLE_STROKECOLOR, "#1E8449"); // Darker green
        startAcceptStyle.put(mxConstants.STYLE_FONTCOLOR, "#196F3D");
        
        // Style for transitions
        Map<String, Object> edgeStyle = new HashMap<>();
        edgeStyle.put(mxConstants.STYLE_ROUNDED, true);
        edgeStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
        edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        edgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#2C3E50");
        edgeStyle.put(mxConstants.STYLE_FONTCOLOR, "#2C3E50");
        edgeStyle.put(mxConstants.STYLE_FONTFAMILY, "Arial");
        edgeStyle.put(mxConstants.STYLE_FONTSIZE, 14);
        edgeStyle.put(mxConstants.STYLE_FONTSTYLE, mxConstants.FONT_BOLD);
        edgeStyle.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
        edgeStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_BOTTOM);
        edgeStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "#FFFFFF");
        edgeStyle.put(mxConstants.STYLE_LABEL_POSITION, mxConstants.ALIGN_CENTER);
        edgeStyle.put("labelBorderColor", "#E5E7E9"); // Custom property
        edgeStyle.put(mxConstants.STYLE_STROKEWIDTH, 1.5);
        
        // Enhanced style for self-loop edges - make them more distinctive
        Map<String, Object> loopStyle = new HashMap<>(edgeStyle);
        loopStyle.put(mxConstants.STYLE_EDGE, mxEdgeStyle.Loop);
        loopStyle.put(mxConstants.STYLE_LOOP, mxConstants.DIRECTION_WEST);
        loopStyle.put(mxConstants.STYLE_LABEL_POSITION, mxConstants.ALIGN_LEFT);
        loopStyle.put(mxConstants.STYLE_FONTCOLOR, "#1A5276");
        loopStyle.put(mxConstants.STYLE_STROKECOLOR, "#3498DB"); // Brighter blue
        loopStyle.put(mxConstants.STYLE_STROKEWIDTH, 2.5); // Thicker line
        loopStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "#EBF5FB"); // Light blue background
        loopStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_OPEN); // Different arrow style
        
        // Register the styles
        stylesheet.putCellStyle("VERTEX", vertexStyle);
        stylesheet.putCellStyle("START", startStyle);
        stylesheet.putCellStyle("ACCEPT", acceptStyle);
        stylesheet.putCellStyle("START_ACCEPT", startAcceptStyle);
        stylesheet.putCellStyle("EDGE", edgeStyle);
        stylesheet.putCellStyle("LOOP", loopStyle);
        
        // Apply different colors to states for better visual distinction
        int colorIndex = 0;
        Map<String, Integer> stateColorMap = new HashMap<>();
        
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
                // Assign a consistent color to this state
                if (!stateColorMap.containsKey(stateName)) {
                    stateColorMap.put(stateName, colorIndex % STATE_COLORS.length);
                    colorIndex++;
                }
                
                // Create a custom style with a unique color
                int stateColor = stateColorMap.get(stateName);
                Map<String, Object> customStyle = new HashMap<>(vertexStyle);
                customStyle.put(mxConstants.STYLE_FILLCOLOR, STATE_COLORS[stateColor]);
                
                // Add the custom style
                String customStyleName = "VERTEX_" + stateColor;
                stylesheet.putCellStyle(customStyleName, customStyle);
                styleName = customStyleName;
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
                
                // Try to add a special "star" decoration to indicate Kleene star
                String edgeLabel = graphAdapter.getModel().getValue(cell).toString();
                if (edgeLabel.length() > 0) {
                    Object stateCell = graphAdapter.getModel().getTerminal(cell, true);
                    String stateName = graphAdapter.getModel().getValue(stateCell).toString();
                    
                    // Create a "★" indicator for self-loops to show Kleene star operation
                    if (hasKleeneStar(dfa, stateName, edgeLabel.charAt(0))) {
                        // This is a loop coming from a Kleene star in the regex
                        graphAdapter.getModel().setValue(cell, edgeLabel + " ★");
                    }
                }
            } else {
                graphAdapter.setCellStyle("EDGE", new Object[] { cell });
            }
        }
    }
    
    /**
     * Detects if this self-loop is likely from a Kleene star operation
     * This is a heuristic and might not be perfect for all DFAs
     */
    private boolean hasKleeneStar(DFA dfa, String stateName, char symbol) {
        // In a minimized DFA, a state with a self-loop might be part of a Kleene star
        // This is just a heuristic - a more accurate approach would require tracking
        // the regex → NFA → DFA conversion process
        
        // For now, let's assume any state with a self-loop is likely from a Kleene star
        return true;
    }
    
    /**
     * Custom edge class that can hold a label.
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