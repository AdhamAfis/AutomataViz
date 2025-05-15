package com.dfavisualizer;

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
import java.util.Comparator;
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
    private static final String LOOP_BADGE_FILL = "#CCCCFF"; // Light blue for loop badges
    private static final String LOOP_BADGE_BORDER = "#6666FF"; // Blue for loop badge border
    
    // For transition grouping
    private static final int MAX_DISPLAY_SYMBOLS = 12;
    private static final int MIN_RANGE_SIZE = 3;
    
    // Grid settings
    private static final int GRID_SIZE = 20; // Size of grid cells in pixels
    
    // Control flags
    private boolean highlightDeadStates = true;
    private boolean gridSnap = false; // Grid snap enabled/disabled

    /**
     * Sets whether to highlight dead states in the visualization.
     * 
     * @param highlight true to highlight dead states, false otherwise
     */
    public void setHighlightDeadStates(boolean highlight) {
        this.highlightDeadStates = highlight;
    }
    
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
                    // Mark if this is a self-loop (important for visualization)
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
        
        // Convert self-loops to badges
        addLoopBadgesToStates(graphAdapter, dfa);
        
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
        
        // Enable cells to be movable and selectable for better interaction
        graphComponent.getGraph().setCellsLocked(false);
        graphComponent.getGraph().setCellsMovable(true);
        graphComponent.getGraph().setCellsSelectable(true);
        graphComponent.getGraph().setCellsResizable(false);
        
        // Enable grid functionality but keep it hidden by default
        graphComponent.setGridVisible(gridSnap);
        graphComponent.getGraph().setGridSize(GRID_SIZE);
        graphComponent.getGraph().setGridEnabled(gridSnap);
        
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
        addStateTooltips(graphComponent, dfa);
        
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
                g2d.setColor(new Color(200, 200, 255));
                
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
        tooltip.append("↻ badges above states represent self-loops<br>");
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
                // For self-loops, create a minimal curved arrow that sticks closely to the state
                graphAdapter.setCellStyles(mxConstants.STYLE_NOLABEL, "0", new Object[] { edge }); // Enable label
                
                // Position the small loop at the top-right corner of the state
                mxGeometry stateGeometry = graphAdapter.getCellGeometry(sourceVertex);
                mxGeometry geometry = graphAdapter.getModel().getGeometry(edge);
                
                if (geometry != null) {
                    geometry = (mxGeometry) geometry.clone();
                    
                    // Calculate edge attachment points
                    double stateRadius = STATE_SIZE / 2.0;
                    double angleStart = Math.PI / 4; // 45 degrees - top-right
                    double angleEnd = 0; // 0 degrees - right side
                    
                    // Offset from the edge of the state
                    geometry.setOffset(new mxPoint(5, -5));
                    
                    // Create a minimal curved arrow
                    List<mxPoint> points = new ArrayList<>();
                    // Create a very tight arc - almost like a decoration on the state
                    points.add(new mxPoint(stateRadius * Math.cos(angleStart), stateRadius * Math.sin(angleStart) - 5));
                    points.add(new mxPoint(stateRadius * Math.cos(angleStart) + 5, stateRadius * Math.sin(angleStart) - 10));
                    points.add(new mxPoint(stateRadius * Math.cos(angleEnd) + 5, stateRadius * Math.sin(angleEnd)));
                    
                    geometry.setPoints(points);
                    graphAdapter.getModel().setGeometry(edge, geometry);
                }
                
                // Apply special styling for the loop
                graphAdapter.setCellStyles(mxConstants.STYLE_EDGE, "orthogonalEdgeStyle", new Object[] { edge });
                // Using built-in rounded instead of unsupported curved property
                graphAdapter.setCellStyles(mxConstants.STYLE_ROUNDED, "1", new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_STROKECOLOR, LOOP_COLOR, new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "2", new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_FONTCOLOR, LOOP_COLOR, new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_FONTSIZE, "10", new Object[] { edge }); // Smaller font
                
                // Make arrow small but visible
                graphAdapter.setCellStyles(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC, new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_ENDSIZE, "5", new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_ROUNDED, "1", new Object[] { edge });
                
                // Position the label above the arrow
                graphAdapter.setCellStyles(mxConstants.STYLE_LABEL_POSITION, "right", new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_VERTICAL_LABEL_POSITION, "top", new Object[] { edge });
                
                // Add a special style for self-loops
                graphAdapter.setCellStyles(mxConstants.STYLE_DASHED, "0", new Object[] { edge });
            } else {
                // For non-loop edges
                graphAdapter.setCellStyles(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC, new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_STROKECOLOR, NORMAL_EDGE_COLOR, new Object[] { edge });
                graphAdapter.setCellStyles(mxConstants.STYLE_FONTCOLOR, NORMAL_EDGE_COLOR, new Object[] { edge });
                
                // Check if there's a reverse edge between the same states
                boolean hasReverseEdge = false;
                for (Object otherEdge : edges) {
                    if (edge != otherEdge) {
                        Object otherSource = graphAdapter.getModel().getTerminal(otherEdge, true);
                        Object otherTarget = graphAdapter.getModel().getTerminal(otherEdge, false);
                        
                        if (sourceVertex == otherTarget && targetVertex == otherSource) {
                            hasReverseEdge = true;
                            break;
                        }
                    }
                }
                
                // If there's a reverse edge, use a different style to separate the edges
                if (hasReverseEdge) {
                    graphAdapter.setCellStyles(mxConstants.STYLE_EDGE, "orthogonalEdgeStyle", new Object[] { edge });
                    // Add a slight bend to the edge
                    mxGeometry geometry = graphAdapter.getModel().getGeometry(edge);
                    if (geometry != null) {
                        geometry = (mxGeometry) geometry.clone();
                        // Create control points for the edge
                        List<mxPoint> points = new ArrayList<>();
                        points.add(new mxPoint(20, 20)); // Offset from direct line
                        geometry.setPoints(points);
                        graphAdapter.getModel().setGeometry(edge, geometry);
                    }
                }
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
        loopStyle.put(mxConstants.STYLE_STROKEWIDTH, "2.5"); // Make loops more visible
        loopStyle.put(mxConstants.STYLE_LOOP, "1");
        loopStyle.put(mxConstants.STYLE_FONTSIZE, "14"); // Larger font for loop labels
        // Explicitly set arrow properties for loops
        loopStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        loopStyle.put(mxConstants.STYLE_ENDSIZE, "7"); // Slightly larger arrow for visibility
        loopStyle.put(mxConstants.STYLE_ROUNDED, true);
        
        stylesheet.putCellStyle("LOOP", loopStyle);
        
        // Custom style for very tight loops
        Map<String, Object> tightLoopStyle = new HashMap<>(edgeStyle);
        tightLoopStyle.put(mxConstants.STYLE_EDGE, "orthogonalEdgeStyle"); // Use orthogonal style for tight bends
        tightLoopStyle.put(mxConstants.STYLE_STROKECOLOR, LOOP_COLOR);
        tightLoopStyle.put(mxConstants.STYLE_FONTCOLOR, LOOP_COLOR);
        tightLoopStyle.put(mxConstants.STYLE_STROKEWIDTH, "2");
        tightLoopStyle.put(mxConstants.STYLE_LOOP, "1");
        tightLoopStyle.put(mxConstants.STYLE_FONTSIZE, "12");
        tightLoopStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        tightLoopStyle.put(mxConstants.STYLE_ENDSIZE, "5");
        tightLoopStyle.put(mxConstants.STYLE_ROUNDED, "1");
        tightLoopStyle.put(mxConstants.STYLE_EXIT_X, "0.5"); // Control exit point
        tightLoopStyle.put(mxConstants.STYLE_EXIT_Y, "0");   // Exit from top
        tightLoopStyle.put(mxConstants.STYLE_ENTRY_X, "0.5"); // Control entry point
        tightLoopStyle.put(mxConstants.STYLE_ENTRY_Y, "0");   // Enter from top
        
        stylesheet.putCellStyle("loopEdge", tightLoopStyle);
        
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
    
    /**
     * Adds a small badge to states that have self-loops, instead of drawing loop edges.
     */
    private void addLoopBadgesToStates(JGraphXAdapter<String, LabeledEdge> graphAdapter, DFA dfa) {
        // Get all self-loops and organize them by state
        Map<String, List<Character>> stateLoops = new HashMap<>();
        
        // First pass: identify self-loops and remove them from the graph
        List<Object> loopEdgesToRemove = new ArrayList<>();
        Object[] allEdges = graphAdapter.getChildCells(graphAdapter.getDefaultParent(), false, true);
        
        for (Object edge : allEdges) {
            Object source = graphAdapter.getModel().getTerminal(edge, true);
            Object target = graphAdapter.getModel().getTerminal(edge, false);
            
            if (source == target) {
                // This is a self-loop
                String stateName = graphAdapter.getModel().getValue(source).toString();
                LabeledEdge labeledEdge = graphAdapter.getCellToEdgeMap().get(edge);
                
                if (labeledEdge != null && labeledEdge.getOriginalSymbols() != null) {
                    // Add the loop symbols to the state's loops map
                    stateLoops.computeIfAbsent(stateName, k -> new ArrayList<>())
                              .addAll(labeledEdge.getOriginalSymbols());
                }
                
                // Mark the edge for removal
                loopEdgesToRemove.add(edge);
            }
        }
        
        // Remove all loop edges from the graph
        graphAdapter.removeCells(loopEdgesToRemove.toArray());
        
        // Second pass: add loop badges to states
        for (Map.Entry<String, List<Character>> entry : stateLoops.entrySet()) {
            String stateName = entry.getKey();
            List<Character> loopSymbols = entry.getValue();
            
            // Sort the symbols for consistent display
            Collections.sort(loopSymbols);
            
            // Create a compact label for the loop symbols
            String loopLabel = createCompactSymbolsLabel(loopSymbols);
            
            // Get the vertex cell
            Object stateCell = null;
            for (String vertex : graphAdapter.getVertexToCellMap().keySet()) {
                if (vertex.equals(stateName)) {
                    stateCell = graphAdapter.getVertexToCellMap().get(vertex);
                    break;
                }
            }
            
            if (stateCell != null) {
                // Add a loop badge as a child cell of the state
                addLoopBadge(graphAdapter, stateCell, loopLabel);
            }
        }
    }
    
    /**
     * Creates a compact representation of symbols for loop badges.
     */
    private String createCompactSymbolsLabel(List<Character> symbols) {
        if (symbols.isEmpty()) {
            return "ε";
        }
        
        if (symbols.size() <= 3) {
            // For a few symbols, just concatenate them
            StringBuilder sb = new StringBuilder();
            for (Character c : symbols) {
                sb.append(c);
            }
            return sb.toString();
        } else {
            // For many symbols, use a more compact representation
            return symbols.size() + "×";
        }
    }
    
    /**
     * Adds a loop badge to a state cell.
     */
    private void addLoopBadge(JGraphXAdapter<String, LabeledEdge> graphAdapter, Object stateCell, String loopLabel) {
        // Get the state geometry
        mxGeometry stateGeometry = graphAdapter.getCellGeometry(stateCell);
        
        if (stateGeometry != null) {
            // Create a small badge cell
            Object badge = graphAdapter.insertVertex(
                graphAdapter.getDefaultParent(),
                null,
                "↻" + loopLabel, // Loop symbol + label
                stateGeometry.getX() + stateGeometry.getWidth() * 0.7, // Position at top-right
                stateGeometry.getY() - 15, // Above the state
                25, // Width
                15, // Height
                "LOOP_BADGE" // Style name
            );
            
            // Apply badge styling
            graphAdapter.setCellStyles(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE, new Object[] { badge });
            graphAdapter.setCellStyles(mxConstants.STYLE_ROUNDED, "1", new Object[] { badge });
            graphAdapter.setCellStyles(mxConstants.STYLE_FILLCOLOR, LOOP_BADGE_FILL, new Object[] { badge });
            graphAdapter.setCellStyles(mxConstants.STYLE_STROKECOLOR, LOOP_BADGE_BORDER, new Object[] { badge });
            graphAdapter.setCellStyles(mxConstants.STYLE_FONTCOLOR, LOOP_BADGE_BORDER, new Object[] { badge });
            graphAdapter.setCellStyles(mxConstants.STYLE_FONTSIZE, "10", new Object[] { badge });
            graphAdapter.setCellStyles(mxConstants.STYLE_FONTSTYLE, String.valueOf(1), new Object[] { badge }); // Bold
            graphAdapter.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "1", new Object[] { badge });
            
            // Connect the badge to the state with an invisible edge
            // This will make the badge move along with the state when dragged
            Object link = graphAdapter.insertEdge(
                graphAdapter.getDefaultParent(), 
                null, 
                "", 
                stateCell, 
                badge
            );
            
            // Style the edge to be invisible
            graphAdapter.setCellStyles(mxConstants.STYLE_STROKECOLOR, "none", new Object[] { link });
            graphAdapter.setCellStyles(mxConstants.STYLE_DASHED, "0", new Object[] { link });
            graphAdapter.setCellStyles(mxConstants.STYLE_STARTARROW, "none", new Object[] { link });
            graphAdapter.setCellStyles(mxConstants.STYLE_ENDARROW, "none", new Object[] { link });
            
            // Set the edge geometry to maintain a fixed offset from the state
            mxGeometry edgeGeometry = new mxGeometry();
            edgeGeometry.setRelative(true);
            graphAdapter.getModel().setGeometry(link, edgeGeometry);
            
            // Make the badge not independently movable
            graphAdapter.setCellStyles(mxConstants.STYLE_MOVABLE, "0", new Object[] { badge });
            graphAdapter.setCellStyles(mxConstants.STYLE_RESIZABLE, "0", new Object[] { badge });
            graphAdapter.setCellStyles(mxConstants.STYLE_EDITABLE, "0", new Object[] { badge });
        }
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