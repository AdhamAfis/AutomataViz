package com.dfavisualizer.algorithm;

import java.util.Set;
import java.util.regex.Pattern;
import com.dfavisualizer.model.NFA;
import com.dfavisualizer.model.DFA;

/** Learning graphs must recognize the expression shown to the learner. */
public class LearningRegexCheck {
    public static void main(String[] args) {
        String[] expressions = {"a\\+", "\\.", "(a|)", "()", "a?", "[a-c]+", "[^a]", "[\\-a]", "[a\\]]", "a(b|c)*"};
        String[] inputs = {"", "a", "aa", "+", "a+", ".", "!", "b", "c", "-", "]", "ab", "acb"};
        for (String expression : expressions) {
            NFA nfa = new RegexParser().parse(expression);
            DFA mainDfa = new RegexToDfaConverter().convertRegexToDfa(expression);
            for (String input : inputs) {
                Set<Integer> active = nfa.epsilonClosure(Set.of(nfa.getStartState()));
                for (char c : input.toCharArray()) active = nfa.epsilonClosure(nfa.move(active, c));
                boolean actual = active.stream().anyMatch(nfa.getAcceptStates()::contains);
                DFA.State state = mainDfa.getStartState();
                for (char c : input.toCharArray()) state = mainDfa.getTransitions().get(new DFA.StateTransition(state, c));
                assert mainDfa.getAcceptStates().contains(state) == actual : "Main visualization disagrees with Learn: " + expression + " input=" + input;
                assert actual == Pattern.matches(expression, input) : expression + " incorrectly handles '" + input + "'";
            }
        }
        for (String invalid : new String[] {"a)", "*a", "[z-a]", "[a", "(a", "a{2}", "\\d", "(?=a)", "a*?"}) {
            try { new RegexParser().parse(invalid); throw new AssertionError("Silently accepted unsupported/invalid syntax: " + invalid); }
            catch (IllegalArgumentException expected) { }
        }
        for (String expression : new String[] {"a(b|c)*", "a*", "a|b", "()"}) {
            DFA original = new SubsetConstruction().convertNfaToDfa(new RegexParser().parse(expression));
            DfaMinimizer minimizer = new DfaMinimizer();
            DFA minimized = minimizer.minimize(original);
            java.util.Map<DFA.State, Set<DFA.State>> groups = minimizer.getStateGroups();
            Set<DFA.State> seen = new java.util.HashSet<>();
            assert groups.keySet().equals(minimized.getStates());
            for (java.util.Map.Entry<DFA.State, Set<DFA.State>> entry : groups.entrySet()) {
                for (DFA.State member : entry.getValue()) {
                    assert seen.add(member) : "Original state appears in several merged groups";
                    assert original.getAcceptStates().contains(member) == minimized.getAcceptStates().contains(entry.getKey());
                    for (char symbol : original.getAlphabet()) {
                        DFA.State sourceTarget = original.getTransitions().get(new DFA.StateTransition(member, symbol));
                        DFA.State mergedTarget = minimized.getTransitions().get(new DFA.StateTransition(entry.getKey(), symbol));
                        assert sourceTarget == null ? mergedTarget == null : groups.get(mergedTarget).contains(sourceTarget);
                    }
                }
            }
            assert seen.equals(original.getStates()) : "Merge inspector loses original states";
        }
        NFA group = new RegexParser().parse("[a-zA-Z0-9]+");
        assert group.getStates().size() < 20 : "A character class produces an unnecessarily enormous teaching graph";
        System.out.println("Learning regex checks passed (language equivalence, empty alternatives, escapes, classes, invalid syntax)");
    }
}
