package com.dfavisualizer.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.*;
import com.dfavisualizer.ui.MainFrame;

public final class ExamplesDialog extends JDialog {
    public static final class Example {
        public final String title, expression, goal;
        public final List<String> accepted, rejected;
        private Example(String title, String expression, String goal, List<String> accepted, List<String> rejected) {
            this.title = title; this.expression = expression; this.goal = goal;
            this.accepted = accepted; this.rejected = rejected;
        }
        @Override public String toString() { return title; }
    }
    public static final List<Example> EXAMPLES = List.of(
        new Example("Ends in abb", "(a|b)*abb",
            "Learn what a suffix machine needs to remember. The prefix can be any sequence of a and b, but the final three symbols must be abb.",
            List.of("abb", "aabb", "babb"), List.of("", "ab", "abba")),
        new Example("Even number of a's", "b*(ab*ab*)*",
            "Learn how states can track parity over the alphabet {a, b}. Each repeated group contributes two a's, with any number of b's between them. Zero a's is even.",
            List.of("", "bb", "abba", "aa"), List.of("a", "abaa", "aaa")),
        new Example("Optional b", "ab?",
            "Compare ? with *. After the required a, b may occur zero or one time. Try abb and compare it with the Repeated b example.",
            List.of("a", "ab"), List.of("", "abb", "b")),
        new Example("Repeated b", "ab*",
            "Compare * with ?. After the required a, b may occur zero or more times. Both ab? and ab* accept a; only ab* accepts abb.",
            List.of("a", "ab", "abb"), List.of("", "b", "ba")),
        new Example("Repeated alternatives", "((ab|ac)d?)*",
            "Combine alternation, an optional suffix and repetition. Each block is ab or ac, optionally followed by d. Compare with Factored blocks: (a[bc]d?)* accepts exactly the same strings.",
            List.of("", "ab", "acd", "abacd", "abdacd"), List.of("a", "ad", "abcd", "abdd")),
        new Example("Factored blocks", "(a[bc]d?)*",
            "Factor the shared a out of ab|ac. This is equivalent to ((ab|ac)d?)*, even though Thompson's construction can produce different NFAs. Compare their minimized DFAs and try the same inputs.",
            List.of("", "ab", "acd", "abacd", "abdacd"), List.of("a", "ad", "abcd", "abdd")),
        new Example("Moving the star", "a[bc]*d?",
            "Compare with (a[bc]d?)*. Here only [bc] repeats: there is exactly one initial a and at most one final d. Counterexample: abc matches this expression but not the repeated blocks; abac matches the blocks but not this expression.",
            List.of("a", "ab", "abc", "abbd", "acccd"), List.of("", "abac", "abdacd", "d")),
        new Example("Runs versus arbitrary choices", "a*(ba*)*",
            "This describes every string over {a, b}, just like [ab]*. Split any input into initial a's followed by blocks of b and then a's. Compare with a*|b*, which rejects mixed strings such as ab.",
            List.of("", "a", "b", "abba", "aababaab"), List.of("c", "abca", "1")),
        new Example("Even a's AND even b's", "(aa|bb|(ab|ba)(aa|bb)*(ab|ba))*",
            "Track two parity conditions over {a, b}. A block aa or bb preserves both parities. A mixed pair ab or ba flips both, so mixed pairs must be paired. The minimized DFA needs four parity combinations. Compare with Even number of a's: bb satisfies both, but b satisfies only the earlier example.",
            List.of("", "aa", "bb", "abab", "abba", "aabb", "baba"), List.of("a", "b", "ab", "aba", "aaab")),
        new Example("Contains aba, including overlaps", "[ab]*aba[ab]*",
            "Track a pattern inside a string, not only at its end. Compare with [ab]*aba. Both accept aba and baba, but abab matches only the contains expression. After finding aba, the remaining input still has to stay within {a, b}.",
            List.of("aba", "baba", "abab", "abaab", "aababb"), List.of("", "ab", "abb", "bbb", "abac")),
        new Example("Binary multiples of three", "(0|1(01*0)*1)+",
            "Interpret a nonempty input as a binary integer divisible by three. Leading zeros are allowed. States can represent the remainder modulo 3; reading bit x changes remainder r to (2r + x) mod 3. The empty string is excluded by +.",
            List.of("0", "00", "11", "110", "1001", "10101", "0011"), List.of("", "1", "10", "100", "111")),
        new Example("Identifier with a restricted first symbol", "[a-zA-Z_][a-zA-Z0-9_]*",
            "Separate the first-symbol rule from the repeating rule. Letters or underscore can start this ASCII identifier; digits are allowed only afterwards. Compare with [a-zA-Z0-9_]+, which also accepts 2item.",
            List.of("a", "_item", "item2", "A_9"), List.of("", "2item", "a-b", "name.space")),
        new Example("Email address (simplified)", "[a-zA-Z0-9_%+-]+([.][a-zA-Z0-9_%+-]+)*@[a-zA-Z0-9]+([.][a-zA-Z0-9]+)*[.][a-zA-Z][a-zA-Z]+",
            "Follow a local part, a literal @, domain labels and a final dot with at least two letters. [.] matches a literal dot; . alone is a wildcard. Local dots separate nonempty groups, so consecutive dots are rejected. This ASCII teaching pattern deliberately excludes hyphenated domains, quoted local parts and internationalized addresses; it is not a complete email validator.",
            List.of("adham@gmail.com", "adham.said+notes@example.org", "student@cs.example.edu", "USER42@mail.net"),
            List.of("", "adhamgmail.com", "@gmail.com", "adham@", "a..b@example.com", "a@example.", "a@example.c", "a b@example.com"))
    );

    public ExamplesDialog(MainFrame app) {
        super(app.getFrame(), "Learning examples", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(720, 430); setMinimumSize(new java.awt.Dimension(600, 350));
        setLocationRelativeTo(app.getFrame());
        JPanel content = new JPanel(new BorderLayout(12, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setContentPane(content);
        JList<Example> choices = new JList<>(EXAMPLES.toArray(new Example[0]));
        choices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        choices.getAccessibleContext().setAccessibleName("Learning example");
        content.add(new JScrollPane(choices), BorderLayout.WEST);
        JTextArea details = new JTextArea();
        details.setName("examples.details"); details.setEditable(false); details.setLineWrap(true); details.setWrapStyleWord(true);
        details.setMargin(new java.awt.Insets(8, 8, 8, 8));
        choices.addListSelectionListener(e -> {
            Example example = choices.getSelectedValue();
            if (example == null) return;
            details.setText("Expression: " + example.expression + "\n\nLearning goal\n" + example.goal
                + "\n\nAccepted inputs: " + display(example.accepted)
                + "\nRejected inputs: " + display(example.rejected)
                + "\n\nPredict a result first. Load the example, then choose Animate or Learn → Step-by-Step conversion.");
            details.setCaretPosition(0);
        });
        content.add(new JScrollPane(details), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton close = new JButton("Close"), load = new JButton("Load example");
        close.addActionListener(e -> dispose());
        load.addActionListener(e -> {
            Example example = choices.getSelectedValue();
            if (example == null) return;
            app.getInputPanel().getRegexField().setText(example.expression);
            app.getInputPanel().getTestStringField().setText(example.accepted.get(0));
            com.dfavisualizer.ui.components.GraphControlsPanel controls = app.getGraphControls();
            app.getVisualizationController().requestVisualization(example.expression, controls.isDfaMinimized(), false,
                controls.isHighlightDead(), controls.getCompareToggle().isSelected());
            dispose();
        });
        buttons.add(close); buttons.add(load); content.add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(load);
        choices.setSelectedIndex(0);
    }
    private static String display(List<String> words) {
        java.util.List<String> quoted = new java.util.ArrayList<>();
        for (String word : words) quoted.add(word.isEmpty() ? "(empty string)" : "\"" + word + "\"");
        return String.join(", ", quoted);
    }
}
