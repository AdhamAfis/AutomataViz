package com.dfavisualizer;

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
    
    /**
     * Minimizes an NFA by:
     * 1. Converting it to a DFA using subset construction
     * 2. Minimizing the DFA using Hopcroft's algorithm
     * 3. Converting the minimized DFA back to a simplified NFA representation
     * 
     * @param nfa The NFA to minimize
     * @return A minimized NFA that is equivalent to the input NFA
     */
    public NFA minimizeNfa(NFA nfa) {
        if (nfa == null) {
            throw new IllegalArgumentException("NFA cannot be null");
        }
        
        // Step 1: Convert NFA to DFA using subset construction
        DFA dfa = subsetConstruction.convertNfaToDfa(nfa);
        
        // Step 2: Minimize the DFA using Hopcroft's algorithm
        DFA minimizedDfa = dfaMinimizer.minimize(dfa);
        
        // Step 3: Convert the minimized DFA back to an NFA representation
        return createSimpleNfaFromDfa(minimizedDfa);
    }
    
    /**
     * Creates a simple NFA representation from a DFA for visualization purposes.
     * This is used to represent the minimized DFA as an NFA.
     */
    private NFA createSimpleNfaFromDfa(DFA dfa) {
        NFA nfa = new NFA();
        
        // Create states for each DFA state
        for (DFA.State dfaState : dfa.getStates()) {
            int nfaState = Integer.parseInt(dfaState.getName().substring(1));
            
            // Ensure state exists in NFA
            while (nfa.getStates().size() <= nfaState) {
                nfa.createState();
            }
            
            // Mark start and accept states
            if (dfaState.equals(dfa.getStartState())) {
                nfa.setStartState(nfaState);
            }
            
            if (dfa.getAcceptStates().contains(dfaState)) {
                nfa.addAcceptState(nfaState);
            }
        }
        
        // Add NFA transitions for each DFA transition
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