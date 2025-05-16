package com.dfavisualizer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.dfavisualizer.model.NFA;
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
    
    // Grid settings
    private static final int GRID_SIZE = 20; // Size of grid cells in pixels
    
    // Control flags
    private boolean gridSnap = false; // Grid snap enabled/disabled
    
    /**
     * Sets whether to enable grid snapping for states.
     * 
     * @param enabled true to enable grid snapping, false to disable it
     */
    public void setGridSnap(boolean enabled) {
        this.gridSnap = enabled;
    }
    
    /**
     * Gets whether grid snapping is enabled.
     * 
     * @return true if grid snapping is enabled, false otherwise
     */
    public boolean isGridSnap() {
        return gridSnap;
    }

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
        
        // Enable cells to be movable and selectable for better interaction
        graphComponent.getGraph().setCellsLocked(false);
        graphComponent.getGraph().setCellsMovable(true);
        graphComponent.getGraph().setCellsSelectable(true);
        graphComponent.getGraph().setCellsResizable(false);
        
        // Grid functionality is controlled by InputPanel's grid snap checkbox
        // Do not enable grid here - it will be managed by InputPanel
        
        // Improve drag behavior
        graphComponent.setPanning(true);
        graphComponent.setAutoScroll(true);
        graphComponent.setCenterPage(true);
        graphComponent.setCenterZoom(true);
        
        graphComponent.setToolTips(true);
        graphComponent.setBorder(null);
        graphComponent.getViewport().setOpaque(true);
        graphComponent.getViewport().setBackground(new Color(252, 252, 252)); // Very light gray
        
        // Enable antialiasing for smoother graphics
        graphComponent.setAntiAlias(true);
        graphComponent.setTextAntiAlias(true);
        
        // Make the component capture key events for shortcuts
        graphComponent.setFocusable(true);
        
        // Add tooltips for states
        addNfaTooltip(graphComponent, nfa);
        
        // Create and configure the minimap navigation
        configureMinimapNavigation(graphComponent);
        
        return graphComponent;
    }
    
    /**
     * Configures the minimap navigation for the graph component
     */
    private void configureMinimapNavigation(mxGraphComponent graphComponent) {
        // Create a custom minimap panel
        JPanel minimapPanel = new JPanel(new BorderLayout());
        minimapPanel.setBackground(new Color(240, 240, 240));
        minimapPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createLineBorder(Color.GRAY)
        ));
        
        // Create a custom minimap component that shows a simplified view of the graph
        JPanel minimap = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Fill the background
                g2d.setColor(new Color(252, 252, 252));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Get the graph bounds
                Rectangle graphBounds = graphComponent.getGraphControl().getBounds();
                
                // If graph has no size yet, just return
                if (graphBounds.width <= 0 || graphBounds.height <= 0) {
                    g2d.dispose();
                    return;
                }
                
                // Calculate scale to fit in minimap
                double scaleX = (double) getWidth() / graphBounds.width;
                double scaleY = (double) getHeight() / graphBounds.height;
                double scale = Math.min(scaleX, scaleY) * 0.9; // 90% to add some margin
                
                // Draw a simplified representation of the states
                g2d.setColor(new Color(230, 230, 255)); // Light purple for NFA states
                
                // Draw all child vertices (states) scaled to fit the minimap
                Object[] vertices = graphComponent.getGraph().getChildVertices(graphComponent.getGraph().getDefaultParent());
                for (Object vertex : vertices) {
                    mxGeometry geo = graphComponent.getGraph().getCellGeometry(vertex);
                    if (geo != null) {
                        double x = geo.getX() * scale;
                        double y = geo.getY() * scale;
                        double width = geo.getWidth() * scale;
                        double height = geo.getHeight() * scale;
                        
                        g2d.fillOval((int) x, (int) y, (int) width, (int) height);
                    }
                }
                
                // Draw the current viewport as a rectangle
                Rectangle viewRect = graphComponent.getViewport().getViewRect();
                g2d.setColor(new Color(255, 0, 0, 100)); // Semi-transparent red
                
                // Scale the view rectangle to match the minimap scale
                int viewX = (int) (viewRect.x * scale);
                int viewY = (int) (viewRect.y * scale);
                int viewWidth = (int) (viewRect.width * scale);
                int viewHeight = (int) (viewRect.height * scale);
                
                g2d.drawRect(viewX, viewY, viewWidth, viewHeight);
                g2d.setColor(new Color(255, 0, 0, 30)); // Very transparent red
                g2d.fillRect(viewX, viewY, viewWidth, viewHeight);
                
                g2d.dispose();
            }
        };
        
        minimap.setPreferredSize(new Dimension(150, 120));
        minimapPanel.add(minimap, BorderLayout.CENTER);
        
        // Add a title for the minimap
        JLabel minimapTitle = new JLabel("Overview", JLabel.CENTER);
        minimapTitle.setFont(minimapTitle.getFont().deriveFont(10.0f));
        minimapTitle.setForeground(Color.DARK_GRAY);
        minimapPanel.add(minimapTitle, BorderLayout.NORTH);
        
        // Add mouse listener to allow clicking on the minimap to navigate
        minimap.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Get the graph bounds
                Rectangle graphBounds = graphComponent.getGraphControl().getBounds();
                
                // Calculate scale
                double scaleX = (double) minimap.getWidth() / graphBounds.width;
                double scaleY = (double) minimap.getHeight() / graphBounds.height;
                double scale = Math.min(scaleX, scaleY) * 0.9;
                
                // Calculate the graph position to center on
                int graphX = (int) (e.getX() / scale);
                int graphY = (int) (e.getY() / scale);
                
                // Center the viewport on this position
                Rectangle viewRect = graphComponent.getViewport().getViewRect();
                graphComponent.getViewport().setViewPosition(new Point(
                    Math.max(0, graphX - viewRect.width / 2),
                    Math.max(0, graphY - viewRect.height / 2)
                ));
                
                // Request focus to the graph component after navigation
                graphComponent.requestFocusInWindow();
            }
        });
        
        // Register for viewport changes to update the minimap
        graphComponent.getViewport().addChangeListener(e -> minimap.repaint());
        
        // Create a floating panel in the bottom-right corner
        JPanel glassPane = new JPanel(null); // Use null layout for absolute positioning
        glassPane.setOpaque(false);
        
        glassPane.add(minimapPanel);
        
        // Position the minimap in the bottom right corner
        minimapPanel.setBounds(
            graphComponent.getWidth() - 170,
            graphComponent.getHeight() - 150,
            160,
            140
        );
        
        // Update minimap position when the graph component is resized
        graphComponent.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                minimapPanel.setBounds(
                    graphComponent.getWidth() - 170,
                    graphComponent.getHeight() - 150,
                    160,
                    140
                );
                minimap.repaint();
            }
        });
        
        // Add the glass pane to the graph component
        graphComponent.add(glassPane);
        glassPane.setVisible(true);
        
        // Force initial paint of the minimap
        minimap.repaint();
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
