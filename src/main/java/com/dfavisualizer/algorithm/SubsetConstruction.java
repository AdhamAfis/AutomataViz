package com.dfavisualizer.algorithm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.dfavisualizer.model.DFA;
import com.dfavisualizer.model.NFA;

/**
 * Implements the subset construction algorithm to convert an NFA to a DFA.
 */
public class SubsetConstruction {

    /**
     * Converts an NFA to a DFA using the subset construction algorithm.
     * 
     * @param nfa The NFA to convert
     * @return The resulting DFA
     */
    public DFA convertNfaToDfa(NFA nfa) {
        if (nfa == null) {
            throw new IllegalArgumentException("NFA cannot be null");
        }
        
        DFA dfa = new DFA();
        
        // Handle empty alphabet case
        if (nfa.getAlphabet().isEmpty()) {
            // Create a single-state DFA
            DFA.State singleState = new DFA.State("q0");
            dfa.addState(singleState);
            dfa.setStartState(singleState);
            
            // If NFA's start state is accepting, make DFA's start state accepting
            Set<Integer> startStateSet = new HashSet<>();
            startStateSet.add(nfa.getStartState());
            Set<Integer> eClosureStart = nfa.epsilonClosure(startStateSet);
            
            for (int acceptState : nfa.getAcceptStates()) {
                if (eClosureStart.contains(acceptState)) {
                    dfa.addAcceptState(singleState);
                    break;
                }
            }
            
            return dfa;
        }
        
        // Track which DFA states correspond to which NFA state subsets
        Map<Set<Integer>, DFA.State> dfaStates = new HashMap<>();
        
        // Queue of NFA state subsets to process
        Queue<Set<Integer>> queue = new LinkedList<>();
        
        // Start with the epsilon closure of the NFA start state
        Set<Integer> startStateSet = new HashSet<>();
        startStateSet.add(nfa.getStartState());
        Set<Integer> startStateEClosure = nfa.epsilonClosure(startStateSet);
        
        // Create start state in DFA
        DFA.State dfaStartState = new DFA.State("q0");
        dfa.setStartState(dfaStartState);
        dfa.addState(dfaStartState);
        
        // Add start state to queue and mapping
        dfaStates.put(startStateEClosure, dfaStartState);
        queue.add(startStateEClosure);
        
        // Check if start state is accepting
        boolean isStartAccepting = false;
        for (int acceptState : nfa.getAcceptStates()) {
            if (startStateEClosure.contains(acceptState)) {
                dfa.addAcceptState(dfaStartState);
                isStartAccepting = true;
                break;
            }
        }
        
        // Track state ID counter for naming states
        int stateCounter = 1;
        
        // Process all state subsets
        while (!queue.isEmpty()) {
            Set<Integer> currentStateSet = queue.poll();
            DFA.State currentDfaState = dfaStates.get(currentStateSet);
            
            // For each input symbol
            for (char symbol : nfa.getAlphabet()) {
                // Compute next state set using e-closure(move(currentStateSet, symbol))
                Set<Integer> moveResult = nfa.move(currentStateSet, symbol);
                Set<Integer> nextStateSet = nfa.epsilonClosure(moveResult);
                
                // Skip if there are no next states
                if (nextStateSet.isEmpty()) {
                    continue;
                }
                
                // Get or create DFA state for this set
                DFA.State nextDfaState;
                if (dfaStates.containsKey(nextStateSet)) {
                    nextDfaState = dfaStates.get(nextStateSet);
                } else {
                    nextDfaState = new DFA.State("q" + stateCounter++);
                    dfa.addState(nextDfaState);
                    dfaStates.put(nextStateSet, nextDfaState);
                    queue.add(nextStateSet);
                    
                    // Check if this new state is accepting
                    for (int acceptState : nfa.getAcceptStates()) {
                        if (nextStateSet.contains(acceptState)) {
                            dfa.addAcceptState(nextDfaState);
                            break;
                        }
                    }
                }
                
                // Add transition to DFA
                dfa.addTransition(currentDfaState, symbol, nextDfaState);
            }
        }
        
        // Ensure complete transition function by adding a "dead state" for missing transitions
        ensureCompleteTransitionFunction(dfa);
        
        return dfa;
    }

    /**
     * Makes the DFA's transition function complete by adding a "dead state" 
     * and transitions to it for any missing transitions.
     */
    private void ensureCompleteTransitionFunction(DFA dfa) {
        // Skip if alphabet is empty
        Set<Character> alphabet = dfa.getAlphabet();
        if (alphabet == null || alphabet.isEmpty()) {
            return;
        }
        
        Set<DFA.State> states = dfa.getStates();
        if (states == null || states.isEmpty()) {
            return;
        }
        
        // Create a "dead state" if needed
        DFA.State deadState = null;
        boolean needDeadState = false;
        
        // Check if we need a dead state
        for (DFA.State state : states) {
            if (state == null) continue;
            
            for (char symbol : alphabet) {
                DFA.StateTransition transition = new DFA.StateTransition(state, symbol);
                if (!dfa.getTransitions().containsKey(transition)) {
                    needDeadState = true;
                    break;
                }
            }
            if (needDeadState) break;
        }
        
        // Create dead state if needed
        if (needDeadState) {
            deadState = new DFA.State("qDead");
            dfa.addState(deadState);
            
            // Make sure the dead state loops back to itself for all symbols
            for (char deadSymbol : alphabet) {
                dfa.addTransition(deadState, deadSymbol, deadState);
            }
            
            // Add missing transitions to the dead state
            for (DFA.State state : states) {
                if (state == null || state == deadState) continue; // Skip null or the dead state itself
                
                for (char symbol : alphabet) {
                    DFA.StateTransition transition = new DFA.StateTransition(state, symbol);
                    
                    // If no transition exists for this state and symbol
                    if (!dfa.getTransitions().containsKey(transition)) {
                        // Add transition to the dead state
                        dfa.addTransition(state, symbol, deadState);
                    }
                }
            }
        }
    }
} 