package com.dfavisualizer.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.dfavisualizer.model.DFA;
import com.dfavisualizer.model.NFA;

/** Optional, bounded snapshots of the algorithms' actual decisions. */
public final class ConstructionTrace {
    public static final class Step {
        public final String explanation;
        public final NFA nfa;
        public final DFA dfa;
        private Step(String explanation, NFA nfa, DFA dfa) {
            this.explanation = explanation; this.nfa = nfa; this.dfa = dfa;
        }
    }
    private final List<Step> steps = new ArrayList<>();
    private int cells;
    private boolean truncated;
    public List<Step> steps() { return Collections.unmodifiableList(steps); }
    public boolean isTruncated() { return truncated; }
    private boolean reserve(int count) {
        if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
        // Keep lesson history bounded; the final automaton remains available for larger examples.
        if (steps.size() >= 200 || cells + count > 30000) { truncated = true; return false; }
        cells += count;
        return true;
    }
    public void nfa(String explanation, NFA nfa) {
        if (reserve(nfa.getStates().size() + nfa.getTransitions().size()))
            steps.add(new Step(explanation, nfa.copy(), null));
    }
    public void dfa(String explanation, DFA dfa) {
        if (!reserve(dfa.getStates().size() + dfa.getTransitions().size())) return;
        DFA copy = new DFA();
        copy.getStates().addAll(dfa.getStates()); copy.getAlphabet().addAll(dfa.getAlphabet());
        copy.setStartState(dfa.getStartState()); copy.getAcceptStates().addAll(dfa.getAcceptStates());
        copy.getTransitions().putAll(dfa.getTransitions());
        steps.add(new Step(explanation, null, copy));
    }
}
