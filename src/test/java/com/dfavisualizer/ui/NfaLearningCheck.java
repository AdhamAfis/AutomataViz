package com.dfavisualizer.ui;

import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;
import com.dfavisualizer.algorithm.RegexParser;
import com.dfavisualizer.algorithm.SubsetConstruction;
import com.dfavisualizer.model.DFA;
import com.dfavisualizer.model.NFA;
import com.dfavisualizer.ui.components.NfaLessonPanel;
import com.dfavisualizer.ui.components.ExpressionGuidePanel;

public class NfaLearningCheck {
    public static void main(String[] args) throws Exception {
        UiTheme.install();
        SwingUtilities.invokeAndWait(() -> {
            NFA nfa = new RegexParser().parse("a(b|c)*");
            SubsetConstruction converter = new SubsetConstruction();
            DFA dfa = converter.convertNfaToDfa(nfa);
            Map<DFA.State, Set<Integer>> subsets = converter.getStateSubsets();
            assert subsets.get(dfa.getStartState()).equals(nfa.epsilonClosure(Set.of(nfa.getStartState())));
            for (Map.Entry<DFA.StateTransition, DFA.State> entry : dfa.getTransitions().entrySet()) {
                Set<Integer> moved = nfa.epsilonClosure(nfa.move(subsets.get(entry.getKey().getState()), entry.getKey().getSymbol()));
                assert moved.equals(subsets.get(entry.getValue())) : "DFA subset provenance does not match actual transitions";
            }
            NfaLessonPanel lesson = new NfaLessonPanel(nfa, subsets);
            lesson.start("ab");
            lesson.next();
            assert lesson.consumed() == 0 && lesson.activeStates().equals(nfa.epsilonClosure(Set.of(nfa.getStartState()))) : "Epsilon consumed input";
            lesson.next();
            Set<Integer> afterRead = lesson.activeStates();
            assert lesson.consumed() == 1;
            lesson.next();
            assert lesson.activeStates().equals(nfa.epsilonClosure(afterRead)) && lesson.consumed() == 1;
            lesson.back();
            assert lesson.activeStates().equals(afterRead) && lesson.consumed() == 1;
            lesson.next(); lesson.next(); lesson.next();
            assert lesson.explanation().contains("ACCEPTED") : lesson.explanation();
            lesson.start("x"); lesson.next(); lesson.next(); lesson.next();
            assert lesson.explanation().contains("REJECTED");
            NfaLessonPanel empty = new NfaLessonPanel(new RegexParser().parse("a*"), java.util.Collections.emptyMap());
            empty.start(""); empty.next();
            assert empty.explanation().contains("ACCEPTED") : "Empty input must accept through epsilon closure";
            String expression = "[a-z]+@[a-z]+[.][a-z][a-z]+";
            int end = 0;
            for (ExpressionGuidePanel.Part part : ExpressionGuidePanel.parts(expression)) {
                assert part.start == end && part.text.equals(expression.substring(part.start, part.end));
                end = part.end;
            }
            assert end == expression.length() : "Guide lost an expression fragment";
            assert ExpressionGuidePanel.parts("[a-z]{2,}").stream().anyMatch(p -> p.description.contains("Unsupported"));
        });
        System.out.println("NFA lesson, subset mapping and expression guide checks passed");
    }
}
