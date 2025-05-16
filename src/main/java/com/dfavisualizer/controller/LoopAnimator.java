package com.dfavisualizer.controller;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.SwingUtilities;

import com.dfavisualizer.model.DFA;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

/**
 * Handles animation of self-loops in DFA visualizations.
 */
public class LoopAnimator {
    private static final int ANIMATION_FRAMES = 30;
    private static final int ANIMATION_DELAY = 20; // milliseconds between frames (faster animation)
    private static final Color LOOP_COLOR = new Color(102, 102, 255); // Blue for loops
    private static final Color ANIMATION_COLOR = new Color(255, 0, 0); // Red for animation
    private static final Color GLOW_COLOR = new Color(255, 255, 100); // Yellow glow
    private static final Color[] PARTICLE_COLORS = {
        new Color(255, 100, 100),  // Red
        new Color(100, 100, 255),  // Blue
        new Color(255, 200, 50),   // Yellow-Orange
        new Color(100, 255, 100),  // Green
        new Color(200, 100, 255)   // Purple
    };

    private mxGraphComponent graphComponent;
    private mxGraph graph;
    private Timer animationTimer;
    private int currentFrame = 0;
    private Map<Object, List<ParticleAnimation>> activeAnimations = new HashMap<>();
    private JPanel overlayPanel;
    private Random random = new Random();
    
    /**
     * Class to store animation data for a particle
     */
    private static class ParticleAnimation {
        mxCell loopBadge;
        mxPoint start;
        mxPoint control1;
        mxPoint control2;
        mxPoint end;
        Color color;
        double animationProgress = 0.0;
        double animationDelta;
        double size;
        boolean clockwise;
        
        ParticleAnimation(mxCell badge, mxPoint start, mxPoint control1, mxPoint control2, 
                         mxPoint end, Color color, double speed, double size, boolean clockwise) {
            this.loopBadge = badge;
            this.start = start;
            this.control1 = control1;
            this.control2 = control2;
            this.end = end;
            this.color = color;
            this.animationDelta = speed;
            this.size = size;
            this.clockwise = clockwise;
        }
        
        public void update() {
            animationProgress += animationDelta;
            if (animationProgress >= 1.0) {
                animationProgress = 0.0;
            }
        }
        
        public mxPoint getCurrentPosition() {
            double t = animationProgress;
            // Cubic Bezier curve formula
            double x = Math.pow(1-t, 3) * start.getX() + 
                       3 * Math.pow(1-t, 2) * t * control1.getX() + 
                       3 * (1-t) * t * t * control2.getX() + 
                       t * t * t * end.getX();
            double y = Math.pow(1-t, 3) * start.getY() + 
                       3 * Math.pow(1-t, 2) * t * control1.getY() + 
                       3 * (1-t) * t * t * control2.getY() + 
                       t * t * t * end.getY();
            return new mxPoint(x, y);
        }
    }

    /**
     * Constructor
     * @param graphComponent The graph component containing the DFA visualization
     */
    public LoopAnimator(mxGraphComponent graphComponent) {
        this.graphComponent = graphComponent;
        this.graph = graphComponent.getGraph();
        createOverlayPanel();
        
        // Debug message to confirm loop animator initialization
        System.out.println("LoopAnimator initialized with graph component: " + graphComponent);
    }

    /**
     * Creates a transparent overlay panel for drawing animations
     */
    private void createOverlayPanel() {
        overlayPanel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawLoopAnimations((Graphics2D) g);
            }
        };
        overlayPanel.setOpaque(false);
        overlayPanel.setLayout(null);
        
        // Add the overlay panel directly to the graph component
        // This avoids the parent container issue
        graphComponent.add(overlayPanel);
        overlayPanel.setBounds(0, 0, graphComponent.getWidth(), graphComponent.getHeight());
        
        // Ensure overlay is resized with graph component
        graphComponent.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                overlayPanel.setBounds(0, 0, graphComponent.getWidth(), graphComponent.getHeight());
                System.out.println("Overlay panel resized to: " + overlayPanel.getBounds());
            }
        });
        
        // Make sure the overlay panel is actually visible
        overlayPanel.setVisible(true);
        
        System.out.println("Overlay panel created with bounds: " + overlayPanel.getBounds());
    }

    /**
     * Draw loop animations on the overlay panel
     * @param g2d Graphics2D object for drawing
     */
    private void drawLoopAnimations(Graphics2D g2d) {
        Graphics2D g2 = (Graphics2D) g2d.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        for (List<ParticleAnimation> particles : activeAnimations.values()) {
            // Draw the loop path first with a subtle glow
            if (!particles.isEmpty()) {
                ParticleAnimation firstParticle = particles.get(0);
                
                // Create a path from the Bezier controls
                QuadCurve2D path = new QuadCurve2D.Double(
                    firstParticle.start.getX(), firstParticle.start.getY(),
                    firstParticle.control1.getX(), firstParticle.control1.getY(),
                    firstParticle.end.getX(), firstParticle.end.getY()
                );
                
                // Draw a subtle path
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(200, 200, 255, 30));
                g2.draw(path);
            }
            
            // Draw each particle with its effects
            for (ParticleAnimation particle : particles) {
                mxPoint pos = particle.getCurrentPosition();
                
                // Draw glow effect first
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                g2.setColor(new Color(particle.color.getRed(), particle.color.getGreen(), 
                                     particle.color.getBlue(), 80));
                double glowSize = particle.size * 2.5;
                g2.fill(new Ellipse2D.Double(pos.getX() - glowSize/2, pos.getY() - glowSize/2, 
                                           glowSize, glowSize));
                
                // Draw the particle
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                g2.setColor(particle.color);
                g2.fill(new Ellipse2D.Double(pos.getX() - particle.size/2, pos.getY() - particle.size/2, 
                                           particle.size, particle.size));
                
                // Draw a highlight to make it shiny
                g2.setColor(new Color(255, 255, 255, 180));
                g2.fill(new Ellipse2D.Double(pos.getX() - particle.size/4, pos.getY() - particle.size/4, 
                                           particle.size/3, particle.size/3));
            }
        }
        
        g2.dispose();
    }

    /**
     * Start animating a loop for a specific state
     * @param stateCell The state cell with the self-loop
     * @param loopBadge The loop badge cell
     */
    public void animateLoop(Object stateCell, Object loopBadge) {
        if (!(loopBadge instanceof mxCell) || !(stateCell instanceof mxCell)) {
            System.out.println("Invalid loop badge or state cell: not an mxCell");
            return;
        }
        
        mxCell badgeCell = (mxCell) loopBadge;
        mxCell state = (mxCell) stateCell;
        
        // Get geometries
        mxGeometry stateGeom = graph.getCellGeometry(state);
        mxGeometry badgeGeom = graph.getCellGeometry(badgeCell);
        
        if (stateGeom == null || badgeGeom == null) {
            System.out.println("Cannot animate loop: null geometry");
            return;
        }
        
        // Display debug info
        System.out.println("Animating loop for state: " + state.getValue() + 
                          " with badge: " + badgeCell.getValue());
        
        // Calculate animation points in component coordinate space
        double scale = graph.getView().getScale();
        double tx = graph.getView().getTranslate().getX();
        double ty = graph.getView().getTranslate().getY();
        
        // Center of the state in screen coordinates
        double centerX = (stateGeom.getCenterX() * scale) + tx;
        double centerY = (stateGeom.getCenterY() * scale) + ty;
        
        // Size of the loop
        double radius = stateGeom.getWidth() * scale * 0.8;
        
        // Create multiple particles with different paths around the state
        List<ParticleAnimation> particles = new ArrayList<>();
        
        // Add 3-6 particles with different properties
        int numParticles = 3 + random.nextInt(4);
        for (int i = 0; i < numParticles; i++) {
            // Calculate a random angle for this particle's starting position
            double angle = random.nextDouble() * Math.PI * 2;
            
            // Slight variation in radius for each particle
            double particleRadius = radius * (0.8 + random.nextDouble() * 0.4);
            
            // Calculate starting point on the circle
            mxPoint startPoint = new mxPoint(
                centerX + Math.cos(angle) * particleRadius,
                centerY + Math.sin(angle) * particleRadius
            );
            
            // Random determine if this particle goes clockwise or counterclockwise
            boolean clockwise = random.nextBoolean();
            
            // Control points to create a circular path
            double angleOffset = clockwise ? Math.PI / 2 : -Math.PI / 2;
            
            mxPoint control1 = new mxPoint(
                centerX + Math.cos(angle + angleOffset) * particleRadius,
                centerY + Math.sin(angle + angleOffset) * particleRadius
            );
            
            mxPoint control2 = new mxPoint(
                centerX + Math.cos(angle + angleOffset * 2) * particleRadius,
                centerY + Math.sin(angle + angleOffset * 2) * particleRadius
            );
            
            // End point is the same as start to make a complete loop
            mxPoint endPoint = startPoint;
            
            // Select a random color from our palette
            Color particleColor = PARTICLE_COLORS[random.nextInt(PARTICLE_COLORS.length)];
            
            // Random starting progress in the animation
            double startProgress = random.nextDouble();
            
            // Random speed variation
            double speed = 0.005 + random.nextDouble() * 0.015;
            
            // Random size variation
            double size = 8 + random.nextInt(8);
            
            // Create the particle animation
            ParticleAnimation particle = new ParticleAnimation(
                badgeCell, startPoint, control1, control2, endPoint, 
                particleColor, speed, size, clockwise
            );
            
            // Set random starting position
            particle.animationProgress = startProgress;
            
            // Add to our list
            particles.add(particle);
        }
        
        // Store animation data
        activeAnimations.put(stateCell, particles);
        
        // Start animation timer if not already running
        if (animationTimer == null || !animationTimer.isRunning()) {
            animationTimer = new Timer(ANIMATION_DELAY, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateAnimation();
                }
            });
            animationTimer.start();
            System.out.println("Loop animation timer started");
        }
    }

    /**
     * Update all active animations
     */
    private void updateAnimation() {
        boolean hasActiveAnimations = false;
        
        for (List<ParticleAnimation> particles : activeAnimations.values()) {
            if (!particles.isEmpty()) {
                hasActiveAnimations = true;
                
                // Update each particle
                for (ParticleAnimation particle : particles) {
                    particle.update();
                }
            }
        }
        
        // Stop timer if no active animations
        if (!hasActiveAnimations && animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
        
        // Make sure the overlay is visible
        if (overlayPanel != null && !overlayPanel.isVisible()) {
            overlayPanel.setVisible(true);
            System.out.println("Making overlay panel visible again");
        }
        
        // Repaint the overlay
        if (overlayPanel != null) {
            overlayPanel.repaint();
        }
    }

    /**
     * Stop all animations
     */
    public void stopAllAnimations() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
        activeAnimations.clear();
        if (overlayPanel != null) {
            overlayPanel.repaint();
        }
    }
    
    /**
     * Stop animation for a specific state
     * @param stateCell The state cell to stop animating
     */
    public void stopAnimation(Object stateCell) {
        activeAnimations.remove(stateCell);
        if (activeAnimations.isEmpty() && animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
        if (overlayPanel != null) {
            overlayPanel.repaint();
        }
    }
} 