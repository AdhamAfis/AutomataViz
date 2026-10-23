package com.dfavisualizer.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JButton;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.text.DefaultHighlighter;
import com.dfavisualizer.ui.UiTheme;

/** Describes syntax fragments without claiming they correspond to individual DFA states. */
public class ExpressionGuidePanel extends JPanel {
    private final JComboBox<Part> selector = new JComboBox<>();
    private final Consumer<String> explain;
    private final JTextField source;
    private String expression = "";
    private Object highlight;

    public ExpressionGuidePanel(JTextField source, Consumer<String> explain, Consumer<Boolean> showLegend) {
        super(new BorderLayout(10, 2));
        this.source = source;
        this.explain = explain;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        JPanel choice = new JPanel(new BorderLayout(6, 0));
        JLabel caption = new JLabel("Explore expression");
        caption.setForeground(UiTheme.MUTED);
        caption.setFont(caption.getFont().deriveFont(12f));
        choice.add(caption, BorderLayout.WEST);
        choice.add(selector, BorderLayout.CENTER);
        JButton explainButton = new JButton("Explain selected part");
        UiTheme.quietButton(explainButton);
        explainButton.addActionListener(e -> selectPart());
        JPanel tools = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 4, 0));
        JCheckBox legend = new JCheckBox("Legend");
        legend.setForeground(UiTheme.MUTED);
        legend.addActionListener(e -> showLegend.accept(legend.isSelected()));
        tools.add(explainButton); tools.add(legend);
        choice.add(tools, BorderLayout.EAST);
        add(choice, BorderLayout.CENTER);
        selector.setToolTipText("Choose a fragment to highlight its source and explain it in Simulation & results");
        selector.setName("expression.parts");
        selector.getAccessibleContext().setAccessibleName("Expression part to explain");
        selector.addActionListener(e -> selectPart());
        source.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { clearHighlight(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { clearHighlight(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { clearHighlight(); }
        });
        setVisible(false);
    }

    public void setExpression(String expression) {
        this.expression = expression;
        selector.removeAllItems();
        for (Part part : parts(expression)) selector.addItem(part);
        setVisible(!expression.isEmpty());
        selectPart();
    }

    private void clearHighlight() {
        if (highlight != null) source.getHighlighter().removeHighlight(highlight);
        highlight = null;
    }

    private void selectPart() {
        clearHighlight();
        Part part = (Part) selector.getSelectedItem();
        if (part == null) return;
        explain.accept(part.text + " — " + part.description + "\nThis explains the expression source; minimized states can represent several parts.");
        highlightSource();
    }

    public void refreshHighlight() { clearHighlight(); highlightSource(); }

    private void highlightSource() {
        Part part = (Part) selector.getSelectedItem();
        if (part == null) return;
        if (source.getText().equals(expression)) {
            try { highlight = source.getHighlighter().addHighlight(part.start, part.end,
                    new DefaultHighlighter.DefaultHighlightPainter(UiTheme.SOURCE_HIGHLIGHT)); }
            catch (javax.swing.text.BadLocationException e) { throw new IllegalStateException(e); }
        }
    }

    public static List<Part> parts(String expression) {
        List<Part> parts = new ArrayList<>();
        for (int i = 0; i < expression.length();) {
            int start = i;
            char first = expression.charAt(i++);
            if (first == '\\' && i < expression.length()) i++;
            else if (first == '[') {
                while (i < expression.length()) {
                    char c = expression.charAt(i++);
                    if (c == '\\' && i < expression.length()) i++;
                    else if (c == ']') break;
                }
            } else if (first == '(') {
                int depth = 1;
                boolean inClass = false;
                while (i < expression.length() && depth > 0) {
                    char c = expression.charAt(i++);
                    if (c == '\\' && i < expression.length()) { i++; continue; }
                    if (c == '[') inClass = true;
                    if (c == ']') inClass = false;
                    if (!inClass && c == '(') depth++;
                    if (!inClass && c == ')') depth--;
                }
            } else if (first == '{') {
                while (i < expression.length() && expression.charAt(i++) != '}') { }
            }
            String quantity = "";
            if (i < expression.length() && "*+?".indexOf(expression.charAt(i)) >= 0)
                quantity = String.valueOf(expression.charAt(i++));
            String text = expression.substring(start, i);
            String description;
            if (first == '[') description = text.startsWith("[.]") ? "Match a literal dot." : text.startsWith("[^")
                    ? "Match one printable ASCII character outside this group."
                    : "Match one character from this character group; ranges such as a-z include their endpoints.";
            else if (text.equals("(?!)")) description = "Empty language: no string can match this expression.";
            else if (first == '(') description = "Grouped expression: evaluate its contents together. A | inside the group separates alternatives.";
            else if (first == '|') description = "Alternation: choose the expression on either side of |.";
            else if (first == '.') description = "Wildcard: matches the app's supported wildcard alphabet. Use [.] for a literal dot.";
            else if (first == '{' || first == '^' || first == '$') description = "Unsupported syntax in this app's automaton parser. Use *, +, ?, grouping and character classes instead.";
            else if (first == '\\') description = "Escaped punctuation matches literally. Use explicit character classes instead of shorthand escapes such as \\d.";
            else description = "Match the literal character '" + first + "'.";
            if (quantity.equals("*")) description += " Repeat zero or more times; skipping this part is allowed.";
            if (quantity.equals("+")) description += " Repeat one or more times; at least one match is required.";
            if (quantity.equals("?")) description += " Optional: match this part once or skip it.";
            parts.add(new Part(start, i, text, description));
        }
        return parts;
    }

    public static final class Part {
        public final int start, end;
        public final String text, description;
        private Part(int start, int end, String text, String description) {
            this.start = start; this.end = end; this.text = text; this.description = description;
        }
        @Override public String toString() { return text.length() > 65 ? text.substring(0, 62) + "…" : text; }
    }
}
