package com.dfavisualizer.algorithm;

import com.dfavisualizer.model.DFA;
import com.dfavisualizer.model.NFA;

/**
 * Minimizes an NFA by first converting it to a DFA, minimizing the DFA,
 * and then converting the minimized DFA back to a simplified NFA representation.
 * 
 * This approach produces a minimal NFA that is equivalent to the original NFA
 * but with potentially fewer states.
 */
public class NfaMinimizer {
    
    private final SubsetConstruction subsetConstruction;
    private final DfaMinimizer dfaMinimizer;
    
    public NfaMinimizer() {
        this.subsetConstruction = new SubsetConstruction();
        this.dfaMinimizer = new DfaMinimizer();
    }
    
    public NFA minimizeNfa(NFA nfa) {
        if (nfa == null) {
            throw new IllegalArgumentException("NFA cannot be null");
        }
        
        DFA dfa = subsetConstruction.convertNfaToDfa(nfa);
        
        DFA minimizedDfa = dfaMinimizer.minimize(dfa);
        
        return createSimpleNfaFromDfa(minimizedDfa);
    }
    
    /**
     * Creates a simple NFA representation from a DFA for visualization purposes.
     * This is used to represent the minimized DFA as an NFA.
     */
    private NFA createSimpleNfaFromDfa(DFA dfa) {
        NFA nfa = new NFA();
        
        for (DFA.State dfaState : dfa.getStates()) {
            int nfaState = Integer.parseInt(dfaState.getName().substring(1));
            
            while (nfa.getStates().size() <= nfaState) {
                nfa.createState();
            }
            
            if (dfaState.equals(dfa.getStartState())) {
                nfa.setStartState(nfaState);
            }
            
            if (dfa.getAcceptStates().contains(dfaState)) {
                nfa.addAcceptState(nfaState);
            }
        }
        
        for (DFA.StateTransition transition : dfa.getTransitions().keySet()) {
            DFA.State sourceState = transition.getState();
            char symbol = transition.getSymbol();
            DFA.State targetState = dfa.getTransitions().get(transition);
            
            int fromState = Integer.parseInt(sourceState.getName().substring(1));
            int toState = Integer.parseInt(targetState.getName().substring(1));
            
            nfa.addTransition(fromState, symbol, toState);
        }
        
        return nfa;
    }
} 