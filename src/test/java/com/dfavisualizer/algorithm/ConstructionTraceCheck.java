package com.dfavisualizer.algorithm;

import java.util.Set;
import java.util.regex.Pattern;
import com.dfavisualizer.model.DFA;
import com.dfavisualizer.model.NFA;

public class ConstructionTraceCheck {
    public static void main(String[] args) {
        for (String expression : new String[] {"a(b|c)*", "(a|b)*abb", "a+", "a?", "()", "", "a|", "(?!)", "(?!)a", "[ab]"}) {
            ConstructionTrace thompson = new ConstructionTrace(), subsets = new ConstructionTrace(), partitions = new ConstructionTrace();
            NFA nfa = new RegexParser(thompson).parse(expression);
            DFA dfa = new SubsetConstruction(subsets).convertNfaToDfa(nfa);
            DFA minimized = new DfaMinimizer(partitions).minimize(dfa);
            assert !thompson.steps().isEmpty() && !subsets.steps().isEmpty() && !partitions.steps().isEmpty();
            NFA lastNfa = thompson.steps().get(thompson.steps().size() - 1).nfa;
            DFA lastDfa = subsets.steps().get(subsets.steps().size() - 1).dfa;
            DFA lastMin = partitions.steps().get(partitions.steps().size() - 1).dfa;
            for (int length = 0; length <= 5; length++) for (int mask = 0; mask < (1 << length); mask++) {
                StringBuilder input = new StringBuilder();
                for (int bit = 0; bit < length; bit++) input.append((mask & (1 << bit)) == 0 ? 'a' : 'b');
                String word = input.toString();
                boolean expected = Pattern.matches(expression, word);
                assert accepts(lastNfa, word) == expected : expression + " Thompson " + word;
                for (DFA candidate : new DFA[] {dfa, minimized, lastDfa, lastMin})
                    assert accepts(candidate, word) == expected : expression + " DFA " + word;
            }
            if (expression.equals("a(b|c)*")) {
                assert thompson.steps().stream().anyMatch(s -> s.explanation.contains("Alternative |"));
                assert thompson.steps().stream().anyMatch(s -> s.explanation.contains("Repeat *"));
                assert thompson.steps().stream().anyMatch(s -> s.explanation.contains("Concatenation"));
            }
            if (expression.equals("(a|b)*abb")) {
                assert partitions.steps().stream().anyMatch(s -> s.explanation.contains("Split"));
                assert subsets.steps().get(0).dfa.getStates().size() == 1 : "History changed after subsequent discovery";
                assert lastDfa.getStates().size() == dfa.getStates().size();
                assert minimized.getStates().size() == 4 : "Suffix machine should have four states";
            }
            for (DFA.State state : dfa.getStates()) for (char symbol : nfa.getAlphabet())
                assert dfa.getTransitionTarget(state, symbol) != null : "Missing dead-state completion";
        }
        DFA cycle = new DFA();
        DFA.State a = new DFA.State("a"), b = new DFA.State("b"), c = new DFA.State("c");
        cycle.setStartState(a); cycle.addAcceptState(a); cycle.addAcceptState(b); cycle.addAcceptState(c);
        cycle.addTransition(a, 'a', b); cycle.addTransition(b, 'a', c); cycle.addTransition(c, 'a', a);
        assert new DfaMinimizer().minimize(cycle).getStates().size() == 1 : "Complete accepting cycle must merge";
        ConstructionTrace bounded = new ConstructionTrace();
        new RegexParser(bounded).parse("a?".repeat(150));
        assert bounded.isTruncated() && bounded.steps().size() <= 200;
        for (String expression : new String[] {"a".repeat(2001), "(".repeat(65) + "a" + ")".repeat(65)}) {
            try { new RegexParser().parse(expression); throw new AssertionError("No teaching limit"); }
            catch (IllegalArgumentException expected) { }
        }
        try { new SubsetConstruction().convertNfaToDfa(new RegexParser().parse("(a|b)*a" + "(a|b)".repeat(13)));
            throw new AssertionError("No subset budget"); }
        catch (IllegalArgumentException expected) { assert expected.getMessage().contains("4,096"); }
        System.out.println("Construction trace checks passed (snapshots, decisions, language preservation, completion, budgets)");
    }
    private static boolean accepts(NFA nfa, String word) {
        Set<Integer> active = nfa.epsilonClosure(Set.of(nfa.getStartState()));
        for (char symbol : word.toCharArray()) active = nfa.epsilonClosure(nfa.move(active, symbol));
        return active.stream().anyMatch(nfa.getAcceptStates()::contains);
    }
    private static boolean accepts(DFA dfa, String word) {
        DFA.State state = dfa.getStartState();
        for (char symbol : word.toCharArray()) { state = dfa.getTransitionTarget(state, symbol); if (state == null) return false; }
        return dfa.getAcceptStates().contains(state);
    }
}
