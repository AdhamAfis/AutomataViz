package com.dfavisualizer.controller;

import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.dfavisualizer.model.DFA;
import com.dfavisualizer.ui.MainFrame;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;

/**
 * Controller that handles animations of automata processing strings.
 */
public class AnimationController {
    private MainFrame mainFrame;
    private Timer animationTimer;
    private static final int ANIMATION_DELAY = 800; // milliseconds between animation steps
    private boolean animationRunning = false;
    
    // Animation state
    private String inputString;
    private int currentPosition;
    private DFA.State currentState;
    private mxGraphComponent graphComponent;
    private mxGraph graph;
    private mxCell currentStateCell;
    private mxCell currentEdgeCell;
    
    // Loop animation
    private LoopAnimator loopAnimator;
    private boolean loopAnimationEnabled = true;
    
    /**
     * Constructor - initializes the animation controller
     * 
     * @param mainFrame The main application frame
     */
    public AnimationController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    /**
     * Animate the processing of a string through a DFA
     * 
     * @param testString The string to animate
     * @return True if the animation was started successfully
     */
    public boolean animateString(String testString) {
        DFA currentDfa = mainFrame.getCurrentDfa();
        if (currentDfa == null) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), 
                    "Please create a DFA first by entering a regex and clicking 'Visualize DFA'.",
                    "No DFA Available", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        if (testString.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), "Please enter a string to animate.", 
                    "Empty Test String", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        // If another animation is running, stop it first
        stopAnimation();
        
        try {
            // Get the visualization component (we'll assume it's in the central panel)
            JComponent component = (JComponent) mainFrame.getDfaPanel().getClientProperty("visualComponent");
            if (!(component instanceof mxGraphComponent)) {
                JOptionPane.showMessageDialog(mainFrame.getFrame(), 
                        "Cannot animate: DFA visualization not available.",
                        "Animation Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            graphComponent = (mxGraphComponent) component;
            graph = graphComponent.getGraph();
            
            // Initialize loop animator if needed
            if (loopAnimator == null && loopAnimationEnabled) {
                loopAnimator = new LoopAnimator(graphComponent);
            }
            
            // Reset any styling from previous animations
            mainFrame.getDfaVisualizer().resetAllStyles();
            
            // Initialize animation state
            inputString = testString;
            currentPosition = 0;
            currentState = currentDfa.getStartState();
            currentStateCell = findCellForState(currentState);
            currentEdgeCell = null;
            
            if (currentStateCell == null) {
                JOptionPane.showMessageDialog(mainFrame.getFrame(), 
                        "Cannot animate: Unable to find start state in the visualization.",
                        "Animation Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            // Update status panel
            mainFrame.getStatusPanel().setStatus("Animation started for string: " + testString);
            
            // Start animation
            startAnimation();
            return true;
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), 
                    "Error starting animation: " + ex.getMessage(),
                    "Animation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    /**
     * Start the animation timer
     */
    private void startAnimation() {
        if (animationRunning) {
            return;
        }
        
        animationRunning = true;
        
        // Highlight the start state
        highlightCurrentState();
        
        // Create and start the animation timer
        animationTimer = new Timer(true);
        animationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> animationStep());
            }
        }, ANIMATION_DELAY, ANIMATION_DELAY);
    }
    
    /**
     * Stop the current animation
     */
    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.cancel();
            animationTimer = null;
        }
        
        animationRunning = false;
        
        // Stop loop animations if they're active
        if (loopAnimator != null) {
            loopAnimator.stopAllAnimations();
        }
        
        // Reset styling if a graph is available
        if (graph != null) {
            resetStyles();
        }
    }
    
    /**
     * Perform a single step of the animation
     */
    private void animationStep() {
        if (currentPosition >= inputString.length()) {
            // Animation complete
            finishAnimation();
            return;
        }
        
        // Get the current input symbol
        char symbol = inputString.charAt(currentPosition);
        
        // Find the next state
        DFA currentDfa = mainFrame.getCurrentDfa();
        
        // Check if the symbol is in the DFA's alphabet
        if (!currentDfa.getAlphabet().contains(symbol)) {
            // Symbol not in alphabet
            mainFrame.getStatusPanel().appendStatus("Symbol '" + symbol + "' is not in the alphabet of the regular expression. String rejected.");
            finishAnimation();
            return;
        }
        
        DFA.State nextState = currentDfa.getTransitionTarget(currentState, symbol);
        
        if (nextState == null) {
            // No valid transition - string rejected
            mainFrame.getStatusPanel().appendStatus("No transition found for symbol: " + symbol + 
                    " from state: " + currentState + ". String rejected.");
            finishAnimation();
            return;
        }
        
        // Find the transition edge and target state cell
        currentEdgeCell = findEdgeCell(currentState, nextState, symbol);
        mxCell nextStateCell = findCellForState(nextState);
        
        if (currentEdgeCell != null) {
            // Highlight the transition
            highlightTransition(currentEdgeCell);
        }
        
        // Check for self-loop animation
        boolean isSelfLoop = currentState.equals(nextState);
        if (isSelfLoop && loopAnimator != null && loopAnimationEnabled) {
            // Find loop badge (assuming it exists)
            Object loopBadge = findLoopBadge(currentStateCell);
            if (loopBadge != null) {
                loopAnimator.animateLoop(currentStateCell, loopBadge);
            }
        }
        
        // Update status
        mainFrame.getStatusPanel().appendStatus("Step " + (currentPosition + 1) + ": " + 
                currentState + " --[" + symbol + "]--> " + nextState);
        
        // Update current state
        currentState = nextState;
        currentStateCell = nextStateCell;
        currentPosition++;
        
        // Highlight the new current state
        highlightCurrentState();
        
        // If we've reached the end of the string, check if we're in an accept state
        if (currentPosition >= inputString.length()) {
            boolean accepted = currentDfa.getAcceptStates().contains(currentState);
            String result = "String \"" + inputString + "\": " + (accepted ? "ACCEPTED" : "REJECTED");
            mainFrame.getStatusPanel().appendStatus(result);
            
            // If we've processed the entire string, finish the animation
            // This ensures animation continues to show each transition first
            finishAnimation();
        }
    }
    
    /**
     * Finish the animation and display the result
     */
    private void finishAnimation() {
        // Stop the timer
        if (animationTimer != null) {
            animationTimer.cancel();
            animationTimer = null;
        }
        
        animationRunning = false;
        
        // Stop any active loop animations
        if (loopAnimator != null) {
            loopAnimator.stopAllAnimations();
        }
        
        DFA currentDfa = mainFrame.getCurrentDfa();
        
        // String is only accepted if we've processed the entire input AND ended in an accept state
        boolean accepted = currentPosition >= inputString.length() && 
                          currentDfa.getAcceptStates().contains(currentState);
        
        // Apply special styling to show final result
        if (accepted) {
            // Use green for accept state
            graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#90EE90", new Object[]{currentStateCell});
            graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#006400", new Object[]{currentStateCell});
            graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "3", new Object[]{currentStateCell});
            
            // Show completion message
            JOptionPane.showMessageDialog(mainFrame.getFrame(), 
                    "Animation complete. String \"" + inputString + "\" ACCEPTED", 
                    "Animation Result", JOptionPane.INFORMATION_MESSAGE);
        } else {
            // Use red for reject state
            graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#FFB6C1", new Object[]{currentStateCell});
            graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#8B0000", new Object[]{currentStateCell});
            graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "3", new Object[]{currentStateCell});
            
            // Determine the specific reason for rejection
            String rejectionReason = "";
            if (currentPosition < inputString.length()) {
                char symbol = inputString.charAt(currentPosition);
                if (!currentDfa.getAlphabet().contains(symbol)) {
                    rejectionReason = "\nReason: Symbol '" + symbol + "' is not in the alphabet of the regular expression.";
                } else {
                    rejectionReason = "\nReason: No valid transition from state " + currentState + " on symbol '" + symbol + "'.";
                }
            }
            
            // Show completion message with the specific reason
            JOptionPane.showMessageDialog(mainFrame.getFrame(), 
                    "Animation complete. String \"" + inputString + "\" REJECTED" + rejectionReason, 
                    "Animation Result", JOptionPane.WARNING_MESSAGE);
        }
        
        // Refresh the graph
        graph.refresh();
    }
    
    /**
     * Highlight the current state in the visualization
     */
    private void highlightCurrentState() {
        if (currentStateCell != null && graph != null) {
            // Reset any previous styling
            mainFrame.getDfaVisualizer().resetAllStyles();
            
            // Apply highlight styling
            graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#87CEFA", new Object[]{currentStateCell});
            graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#0000CD", new Object[]{currentStateCell});
            graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "3", new Object[]{currentStateCell});
            
            // If there's a current edge, highlight it too
            if (currentEdgeCell != null) {
                highlightTransition(currentEdgeCell);
            }
            
            graph.refresh();
            
            // Center the view on the current state
            graphComponent.scrollCellToVisible(currentStateCell, true);
        }
    }
    
    /**
     * Highlight a transition in the visualization
     * 
     * @param edgeCell The edge cell to highlight
     */
    private void highlightTransition(mxCell edgeCell) {
        if (edgeCell != null && graph != null) {
            graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#FF0000", new Object[]{edgeCell});
            graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "3", new Object[]{edgeCell});
            graph.setCellStyles(mxConstants.STYLE_FONTCOLOR, "#FF0000", new Object[]{edgeCell});
            graph.setCellStyles(mxConstants.STYLE_FONTSIZE, "14", new Object[]{edgeCell});
            graph.refresh();
        }
    }
    
    /**
     * Find the graph cell representing a DFA state
     * 
     * @param state The DFA state to find
     * @return The corresponding mxCell or null if not found
     */
    private mxCell findCellForState(DFA.State state) {
        Object[] vertices = graph.getChildVertices(graph.getDefaultParent());
        for (Object vertex : vertices) {
            if (vertex instanceof mxCell) {
                mxCell cell = (mxCell) vertex;
                if (cell.getValue() != null && cell.getValue().toString().equals(state.toString())) {
                    return cell;
                }
            }
        }
        return null;
    }
    
    /**
     * Find the graph edge representing a transition between states
     * 
     * @param source The source state
     * @param target The target state
     * @param symbol The transition symbol
     * @return The corresponding mxCell edge or null if not found
     */
    private mxCell findEdgeCell(DFA.State source, DFA.State target, char symbol) {
        mxCell sourceCell = findCellForState(source);
        mxCell targetCell = findCellForState(target);
        
        if (sourceCell == null || targetCell == null) {
            return null;
        }
        
        Object[] edges = graph.getChildEdges(graph.getDefaultParent());
        for (Object edge : edges) {
            if (edge instanceof mxCell) {
                mxCell cell = (mxCell) edge;
                if (cell.isEdge() && 
                    cell.getSource() == sourceCell && 
                    cell.getTarget() == targetCell && 
                    cell.getValue() != null && 
                    cell.getValue().toString().contains(String.valueOf(symbol))) {
                    return cell;
                }
            }
        }
        return null;
    }
    
    /**
     * Find a loop badge associated with a state cell
     * 
     * @param stateCell The state cell to find a loop badge for
     * @return The loop badge cell or null if not found
     */
    private Object findLoopBadge(mxCell stateCell) {
        if (stateCell == null || graph == null) {
            System.out.println("Cannot find loop badge: null state cell or graph");
            return null;
        }
        
        System.out.println("Finding loop badge for state: " + stateCell.getValue());
        
        // First, check direct connections from this state to badges
        Object[] edges = graph.getEdges(stateCell);
        if (edges != null) {
            for (Object edge : edges) {
                mxCell edgeCell = (mxCell) edge;
                Object target = edgeCell.getTarget();
                
                // Check if this is pointing to a loop badge
                if (target instanceof mxCell && 
                    ((mxCell)target).getValue() != null &&
                    ((mxCell)target).getValue().toString().startsWith("↻")) {
                    System.out.println("Found loop badge: " + ((mxCell)target).getValue());
                    return target;
                }
            }
        }
        
        // If no direct connection, search all cells
        Object[] allCells = graph.getChildCells(graph.getDefaultParent(), true, true);
        for (Object cell : allCells) {
            if (cell instanceof mxCell) {
                mxCell mxcell = (mxCell) cell;
                if (mxcell.getValue() != null && 
                    mxcell.getValue().toString().startsWith("↻") && 
                    mxcell.getStyle() != null && 
                    mxcell.getStyle().contains("LOOP_BADGE")) {
                    
                    // For badges without connections, use geometric proximity
                    mxGeometry stateGeom = graph.getCellGeometry(stateCell);
                    mxGeometry badgeGeom = graph.getCellGeometry(mxcell);
                    
                    if (stateGeom != null && badgeGeom != null) {
                        // Check if badge is near this state
                        double distance = Math.sqrt(
                            Math.pow(stateGeom.getCenterX() - badgeGeom.getCenterX(), 2) +
                            Math.pow(stateGeom.getCenterY() - badgeGeom.getCenterY(), 2)
                        );
                        
                        if (distance < stateGeom.getWidth() * 1.5) {
                            System.out.println("Found nearby loop badge: " + mxcell.getValue());
                            return mxcell;
                        }
                    }
                    
                    // Also check for connecting edges
                    Object[] badgeEdges = graph.getEdges(mxcell);
                    for (Object edge : badgeEdges) {
                        mxCell edgeCell = (mxCell) edge;
                        if ((edgeCell.getSource() == stateCell && edgeCell.getTarget() == mxcell) ||
                            (edgeCell.getSource() == mxcell && edgeCell.getTarget() == stateCell)) {
                            System.out.println("Found connected loop badge: " + mxcell.getValue());
                            return mxcell;
                        }
                    }
                }
            }
        }
        
        // If we still couldn't find a badge, look for self-loops in the graph
        for (Object edge : graph.getChildEdges(graph.getDefaultParent())) {
            mxCell edgeCell = (mxCell) edge;
            if (edgeCell.getSource() == stateCell && edgeCell.getTarget() == stateCell) {
                // This is a self-loop edge, we can animate directly with the state
                System.out.println("Found self-loop edge for state: " + stateCell.getValue());
                return stateCell; // Use the state itself as the badge
            }
        }
        
        System.out.println("No loop badge found for state: " + stateCell.getValue());
        return null;
    }
    
    /**
     * Check if an animation is currently running
     * 
     * @return True if an animation is running
     */
    public boolean isAnimationRunning() {
        return animationRunning;
    }
    
    /**
     * Get the component used for visualization
     * 
     * @return The mxGraphComponent for visualization
     */
    public mxGraphComponent getGraphComponent() {
        return graphComponent;
    }
    
    /**
     * Set whether loop animation is enabled
     * 
     * @param enabled True to enable loop animation, false to disable
     */
    public void setLoopAnimationEnabled(boolean enabled) {
        this.loopAnimationEnabled = enabled;
    }
    
    /**
     * Check if loop animation is enabled
     * 
     * @return True if loop animation is enabled
     */
    public boolean isLoopAnimationEnabled() {
        return loopAnimationEnabled;
    }
    
    /**
     * Reset any styling from animations
     */
    private void resetStyles() {
        if (graph != null) {
            // Reset all cells to their default styles
            // For vertices (states)
            Object[] vertices = graph.getChildVertices(graph.getDefaultParent());
            for (Object vertex : vertices) {
                resetCellStyle(graph, vertex);
            }
            
            // For edges (transitions)
            Object[] edges = graph.getChildEdges(graph.getDefaultParent());
            for (Object edge : edges) {
                resetCellStyle(graph, edge);
            }
            
            graph.refresh();
        }
    }
    
    /**
     * Reset the style of a cell
     * 
     * @param graph The graph
     * @param cell The cell to reset
     */
    private void resetCellStyle(mxGraph graph, Object cell) {
        if (graph.getModel().isVertex(cell)) {
            graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#E6F2FF", new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#444444", new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "1.5", new Object[] { cell });
        } else if (graph.getModel().isEdge(cell)) {
            graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "#666666", new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_FONTCOLOR, "#666666", new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_STROKEWIDTH, "1.5", new Object[] { cell });
            graph.setCellStyles(mxConstants.STYLE_FONTSIZE, "12", new Object[] { cell });
        }
    }
} 