package com.dfavisualizer.algorithm;

import com.dfavisualizer.model.DFA;
import com.dfavisualizer.ui.dialogs.ExamplesDialog;
import java.util.regex.Pattern;

public class LearningExamplesCheck {
    public static void main(String[] args) {
        for (ExamplesDialog.Example example : ExamplesDialog.EXAMPLES) {
            DFA dfa = new RegexToDfaConverter().convertRegexToDfa(example.expression);
            for (boolean expected : new boolean[] {true, false}) {
                for (String word : expected ? example.accepted : example.rejected) {
                    assert Pattern.matches(example.expression, word) == expected : example.title + ": mislabeled input " + word;
                    DFA.State state = dfa.getStartState();
                    for (char c : word.toCharArray()) { state = dfa.getTransitionTarget(state, c); if (state == null) break; }
                    assert dfa.getAcceptStates().contains(state) == expected : example.title + ": automaton disagrees";
                }
            }
            assert !example.goal.isBlank() && !example.accepted.isEmpty() && !example.rejected.isEmpty();
        }
        assert equivalent("((ab|ac)d?)*", "(a[bc]d?)*");
        assert equivalent("a*(ba*)*", "[ab]*");
        assert !equivalent("a[bc]*d?", "(a[bc]d?)*");
        assert !equivalent("[ab]*aba[ab]*", "[ab]*aba");
        assert !equivalent("a*|b*", "[ab]*");
        for (int length = 0; length <= 8; length++) for (int bits = 0; bits < (1 << length); bits++) {
            StringBuilder letters = new StringBuilder(), binary = new StringBuilder();
            int aCount = 0;
            for (int bit = 0; bit < length; bit++) {
                boolean one = (bits & (1 << bit)) != 0;
                letters.append(one ? 'a' : 'b'); binary.append(one ? '1' : '0');
                if (one) aCount++;
            }
            assert Pattern.matches("(aa|bb|(ab|ba)(aa|bb)*(ab|ba))*", letters)
                == (aCount % 2 == 0 && (length - aCount) % 2 == 0) : "Incorrect double parity";
            assert Pattern.matches("(0|1(01*0)*1)+", binary)
                == (length > 0 && Integer.parseInt(binary.toString(), 2) % 3 == 0) : "Incorrect binary divisibility";
        }
        System.out.println("Learning example checks passed (goals, inputs, DFA agreement, exact comparisons, parity and binary arithmetic)");
    }
    private static boolean equivalent(String left, String right) {
        DFA a = new RegexToDfaConverter().convertRegexToDfa(left);
        DFA b = new RegexToDfaConverter().convertRegexToDfa(right);
        java.util.Set<Character> alphabet = new java.util.HashSet<>(a.getAlphabet()); alphabet.addAll(b.getAlphabet());
        java.util.Queue<java.util.List<DFA.State>> queue = new java.util.ArrayDeque<>();
        java.util.Set<java.util.List<DFA.State>> seen = new java.util.HashSet<>();
        queue.add(java.util.Arrays.asList(a.getStartState(), b.getStartState()));
        while (!queue.isEmpty()) {
            java.util.List<DFA.State> pair = queue.remove();
            if (!seen.add(pair)) continue;
            if (a.getAcceptStates().contains(pair.get(0)) != b.getAcceptStates().contains(pair.get(1))) return false;
            for (char symbol : alphabet) queue.add(java.util.Arrays.asList(
                pair.get(0) == null ? null : a.getTransitionTarget(pair.get(0), symbol),
                pair.get(1) == null ? null : b.getTransitionTarget(pair.get(1), symbol)));
        }
        return true;
    }
}
