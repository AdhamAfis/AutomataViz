package com.dfavisualizer.ui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.dfavisualizer.controller.VisualizationController;
import com.dfavisualizer.ui.MainFrame;
import com.dfavisualizer.ui.UiTheme;
import com.dfavisualizer.ui.dialogs.StepByStepDialog;
import com.dfavisualizer.ui.dialogs.TheoryPopupDialog;

/**
 * Expression and test inputs, with learning actions exposed through the application menu.
 */
public class InputPanel extends JPanel {
    private MainFrame mainFrame;
    private JTextField regexField;
    private JTextField testStringField;
    private JButton visualizeButton;
    private JButton testButton;
    private JButton animateButton;
    private GettingStartedPanel gettingStarted;
    
    public InputPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        initializeComponents();
    }
    
    private void initializeComponents() {
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JPanel inputs = new JPanel(new BorderLayout(0, 4));
        JPanel regexPanel = new JPanel(new BorderLayout(10, 0));
        JLabel regexLabel = new JLabel("Expression");
        regexLabel.setForeground(UiTheme.MUTED);
        regexLabel.setPreferredSize(new Dimension(86, 30));
        regexField = new JTextField();
        regexField.putClientProperty("JTextField.placeholderText", "Try (a|b)*abb");
        regexField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        regexField.setToolTipText("Educational regex syntax; matches the whole input. Use Syntax help for limits.");
        regexField.addActionListener(this::visualizeDfa);
        regexLabel.setLabelFor(regexField);
        visualizeButton = new JButton("Visualize DFA");
        visualizeButton.setFont(visualizeButton.getFont().deriveFont(Font.PLAIN));
        visualizeButton.addActionListener(this::visualizeDfa);
        regexPanel.add(regexLabel, BorderLayout.WEST);
        regexPanel.add(regexField, BorderLayout.CENTER);
        regexPanel.add(visualizeButton, BorderLayout.EAST);
        inputs.add(regexPanel, BorderLayout.NORTH);
        JPanel help = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        JLabel hint = new JLabel("Whole-string matching · literals, |, *, +, ?, groups, [a-z], .");
        hint.setForeground(UiTheme.MUTED);
        JButton syntax = new JButton("Syntax help");
        syntax.addActionListener(e -> JOptionPane.showMessageDialog(mainFrame.getFrame(),
            "Supported: literals, |, *, +, ?, ordinary parentheses, [a-z], [^a], . and escaped punctuation.\n"
            + "Examples: a(b|c)*, (a|b)*abb, [0-9]+. Use () for the empty string.\n"
            + "The entire input must match. Wildcards and negated classes cover printable ASCII (space through ~).\n"
            + "No shorthand classes (such as \\d), anchors, counted repetitions, lookarounds or special groups.\n"
            + "Use [0-9] instead of \\d, and aa instead of a{2}.\n"
            + "Generation is limited to 2,000 expression characters, 64 nested groups and 4,096 DFA subsets.\n"
            + "Construction history is capped at 200 steps / 30,000 stored states and transitions per stage.",
            "Educational regex syntax", JOptionPane.INFORMATION_MESSAGE));
        JButton examples = new JButton("Examples");
        examples.addActionListener(e -> new com.dfavisualizer.ui.dialogs.ExamplesDialog(mainFrame).setVisible(true));
        UiTheme.quietButton(examples);
        UiTheme.quietButton(syntax); help.add(hint); help.add(syntax); help.add(examples);
        inputs.add(help, BorderLayout.SOUTH);

        gettingStarted = new GettingStartedPanel(java.util.prefs.Preferences.userNodeForPackage(UiTheme.class));
        add(gettingStarted, BorderLayout.NORTH);
        add(inputs, BorderLayout.CENTER);

        JPanel testPanel = new JPanel(new BorderLayout(10, 0));
        JLabel testLabel = new JLabel("Test string");
        testLabel.setForeground(UiTheme.MUTED);
        testLabel.setPreferredSize(new Dimension(86, 30));
        testStringField = new JTextField();
        testStringField.putClientProperty("JTextField.placeholderText", "Enter a string to follow through the graph");
        testStringField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        testStringField.addActionListener(this::testString);
        testLabel.setLabelFor(testStringField);
        JPanel testButtons = new JPanel(new GridLayout(1, 2, 6, 0));
        testButton = new JButton("Test");
        testButton.addActionListener(this::testString);
        animateButton = new JButton("Animate");
        animateButton.setFont(animateButton.getFont().deriveFont(Font.PLAIN));
        animateButton.addActionListener(this::animateString);
        animateButton.setToolTipText("Follow the test string through the DFA, one symbol at a time");
        testButtons.add(testButton);
        testButtons.add(animateButton);
        testPanel.add(testLabel, BorderLayout.WEST);
        testPanel.add(testStringField, BorderLayout.CENTER);
        testPanel.add(testButtons, BorderLayout.EAST);
        add(testPanel, BorderLayout.SOUTH);
        mainFrame.getFrame().getRootPane().setDefaultButton(visualizeButton);
    }

    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu learn = new JMenu("Learn");
        learn.setMnemonic('L');
        JMenuItem walkthrough = new JMenuItem("Step-by-Step conversion");
        walkthrough.addActionListener(e -> showStepByStepConversion());
        learn.add(walkthrough);
        JMenuItem exercise = new JMenuItem("Guided exercise: recognize a suffix");
        exercise.addActionListener(e -> {
            regexField.setText("(a|b)*abb"); testStringField.setText("aabb");
            new StepByStepDialog(mainFrame, "(a|b)*abb", "aabb", true).display();
        });
        learn.add(exercise);
        JMenu tools = new JMenu("Tools");
        tools.setMnemonic('T');
        JMenuItem debug = new JMenuItem("Debug expression");
        debug.addActionListener(this::debugRegex);
        tools.add(debug);
        JMenu help = new JMenu("Help");
        help.setMnemonic('H');
        JMenuItem theory = new JMenuItem("Automata theory");
        theory.addActionListener(e -> showTheoryPopup());
        help.add(theory);
        JMenuItem gettingStartedItem = new JMenuItem("Getting started");
        gettingStartedItem.addActionListener(e -> gettingStarted.showAgain());
        help.add(gettingStartedItem);
        JMenu view = new JMenu("View");
        view.setMnemonic('V');
        JMenuItem themes = new JMenuItem("Themes…");
        themes.addActionListener(e -> mainFrame.showThemes());
        view.add(themes);
        menuBar.add(learn); menuBar.add(tools); menuBar.add(view); menuBar.add(help);
        return menuBar;
    }

    private void visualizeDfa(ActionEvent e) {
        if (e.getSource() == visualizeButton && mainFrame.getVisualizationController().isGenerating()) {
            mainFrame.getVisualizationController().cancelGeneration(); return;
        }
        String regex = regexField.getText();
        GraphControlsPanel controls = mainFrame.getGraphControls();
        mainFrame.getVisualizationController().requestVisualization(regex, controls.isDfaMinimized(), false,
                controls.isHighlightDead(), controls.getCompareToggle().isSelected());
    }
    
    private void debugRegex(ActionEvent e) {
        String regex = regexField.getText().trim();
        
        VisualizationController controller = mainFrame.getVisualizationController();
        controller.debugRegex(regex);
    }
    
    private void testString(ActionEvent e) {
        String testString = testStringField.getText();
        
        VisualizationController controller = mainFrame.getVisualizationController();
        controller.testString(testString);
    }
    
    private void animateString(ActionEvent e) {
        String testString = testStringField.getText();
        
        VisualizationController controller = mainFrame.getVisualizationController();
        controller.animateString(testString);
    }
    
    private void showStepByStepConversion() {
        String regex = regexField.getText().trim();
        if (regex.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame.getFrame(), 
                "Please enter a regular expression first.", 
                "Empty Regex", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (mainFrame.getVisualizationController().getAnimationController().isAnimationRunning())
            mainFrame.getVisualizationController().getAnimationController().togglePause();
        // Pause the main trace while the learner explores the conversion.
        new StepByStepDialog(mainFrame, regex, testStringField.getText()).display();
    }
    
    private void showTheoryPopup() {
        new TheoryPopupDialog(mainFrame).display();
    }
    
    
    public void setGenerating(boolean busy) {
        visualizeButton.setText(busy ? "Cancel generation" : "Visualize DFA");
        testButton.setEnabled(!busy); animateButton.setEnabled(!busy);
        putClientProperty("generation.busy", busy);
    }

    public JTextField getRegexField() {
        return regexField;
    }
    
    public JTextField getTestStringField() {
        return testStringField;
    }
    
    public JButton getAnimateButton() {
        return animateButton;
    }
} 