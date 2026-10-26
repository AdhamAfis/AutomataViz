package com.dfavisualizer.ui.components;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.*;

/** A prediction before simulation, followed by an explanation students can check. */
public final class GuidedExercisePanel extends JPanel {
    private static final String[] INPUTS = {"aabb", "abba", "abb", ""};
    private static final String[] REASONS = {
        "Accepted: the final three symbols are abb. The earlier a belongs to (a|b)*.",
        "Rejected: the string ends in bba, not abb. Having abb earlier is not enough; the whole string must match.",
        "Accepted: (a|b)* can consume zero symbols, leaving exactly abb.",
        "Rejected: the empty string is missing the required abb suffix. The star makes only the prefix optional."
    };
    public GuidedExercisePanel(java.util.function.Consumer<String> simulate) {
        super(new BorderLayout(6, 4));
        setBorder(BorderFactory.createTitledBorder("Exercise: which strings end in abb?"));
        JComboBox<String> input = new JComboBox<>(new String[] {"aabb", "abba", "abb", "(empty string)"});
        JComboBox<String> prediction = new JComboBox<>(new String[] {"Choose a prediction", "Accepted", "Rejected"});
        input.getAccessibleContext().setAccessibleName("Exercise input");
        prediction.getAccessibleContext().setAccessibleName("Your prediction");
        JPanel choices = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton check = new JButton("Check prediction"), trace = new JButton("Trace in NFA");
        trace.setName("exercise.trace"); trace.setEnabled(false);
        choices.add(new JLabel("(a|b)*abb")); choices.add(input); choices.add(prediction); choices.add(check); choices.add(trace);
        JTextArea feedback = new JTextArea(3, 40);
        feedback.setName("exercise.feedback"); feedback.setEditable(false); feedback.setLineWrap(true); feedback.setWrapStyleWord(true);
        feedback.setText("Predict before tracing. What must the machine remember about the end of the input?");
        check.addActionListener(e -> {
            if (prediction.getSelectedIndex() == 0) { feedback.setText("Choose Accepted or Rejected first."); return; }
            int question = input.getSelectedIndex();
            boolean accepts = question == 0 || question == 2;
            boolean correct = (prediction.getSelectedIndex() == 1) == accepts;
            feedback.setText((correct ? "Correct. " : "Try tracing this one. ") + REASONS[question]
                + "\nIn the minimized DFA, states track how much of the suffix abb currently matches: nothing, a, ab, or abb.");
        });
        input.addActionListener(e -> { prediction.setSelectedIndex(0); feedback.setText("Predict the result, then check it or trace the input in the NFA."); });
        trace.addActionListener(e -> simulate.accept(INPUTS[input.getSelectedIndex()]));
        add(choices, BorderLayout.NORTH); add(new JScrollPane(feedback), BorderLayout.CENTER);
    }
}
