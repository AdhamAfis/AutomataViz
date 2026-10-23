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
    private final Map<DFA.State, Set<Integer>> stateSubsets = new java.util.LinkedHashMap<>();

    public Map<DFA.State, Set<Integer>> getStateSubsets() {
        return java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(stateSubsets));
    }

    private void remember(DFA.State state, Set<Integer> subset) {
        stateSubsets.put(state, java.util.Collections.unmodifiableSet(new java.util.TreeSet<>(subset)));
    }

    public DFA convertNfaToDfa(NFA nfa) {
        if (nfa == null) {
            throw new IllegalArgumentException("NFA cannot be null");
        }
        
        stateSubsets.clear();
        DFA dfa = new DFA();
        
        if (nfa.getAlphabet().isEmpty()) {
            DFA.State singleState = new DFA.State("q0");
            dfa.addState(singleState);
            dfa.setStartState(singleState);
            
            Set<Integer> startStateSet = new HashSet<>();
            startStateSet.add(nfa.getStartState());
            Set<Integer> eClosureStart = nfa.epsilonClosure(startStateSet);
            remember(singleState, eClosureStart);
            
            for (int acceptState : nfa.getAcceptStates()) {
                if (eClosureStart.contains(acceptState)) {
                    dfa.addAcceptState(singleState);
                    break;
                }
            }
            
            return dfa;
        }
        
        Map<Set<Integer>, DFA.State> dfaStates = new HashMap<>();
        
        Queue<Set<Integer>> queue = new LinkedList<>();
        
        Set<Integer> startStateSet = new HashSet<>();
        startStateSet.add(nfa.getStartState());
        Set<Integer> startStateEClosure = nfa.epsilonClosure(startStateSet);
        
        DFA.State dfaStartState = new DFA.State("q0");
        dfa.setStartState(dfaStartState);
        dfa.addState(dfaStartState);
        
        dfaStates.put(startStateEClosure, dfaStartState);
        remember(dfaStartState, startStateEClosure);
        queue.add(startStateEClosure);
        
        boolean isStartAccepting = false;
        for (int acceptState : nfa.getAcceptStates()) {
            if (startStateEClosure.contains(acceptState)) {
                dfa.addAcceptState(dfaStartState);
                isStartAccepting = true;
                break;
            }
        }
        
        int stateCounter = 1;
        
        while (!queue.isEmpty()) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            Set<Integer> currentStateSet = queue.poll();
            DFA.State currentDfaState = dfaStates.get(currentStateSet);
            
            for (char symbol : new java.util.TreeSet<>(nfa.getAlphabet())) {
                Set<Integer> moveResult = nfa.move(currentStateSet, symbol);
                Set<Integer> nextStateSet = nfa.epsilonClosure(moveResult);
                
                if (nextStateSet.isEmpty()) {
                    continue;
                }
                
                DFA.State nextDfaState;
                if (dfaStates.containsKey(nextStateSet)) {
                    nextDfaState = dfaStates.get(nextStateSet);
                } else {
                    nextDfaState = new DFA.State("q" + stateCounter++);
                    dfa.addState(nextDfaState);
                    dfaStates.put(nextStateSet, nextDfaState);
                    remember(nextDfaState, nextStateSet);
                    queue.add(nextStateSet);
                    
                    for (int acceptState : nfa.getAcceptStates()) {
                        if (nextStateSet.contains(acceptState)) {
                            dfa.addAcceptState(nextDfaState);
                            break;
                        }
                    }
                }
                
                dfa.addTransition(currentDfaState, symbol, nextDfaState);
            }
        }
        
        ensureCompleteTransitionFunction(dfa);
        
        return dfa;
    }

    /**
     * Makes the DFA's transition function complete by adding a "dead state" 
     * and transitions to it for any missing transitions.
     */
    private void ensureCompleteTransitionFunction(DFA dfa) {
        Set<Character> alphabet = dfa.getAlphabet();
        if (alphabet == null || alphabet.isEmpty()) {
            return;
        }
        
        Set<DFA.State> states = dfa.getStates();
        if (states == null || states.isEmpty()) {
            return;
        }
        
        DFA.State deadState = null;
        boolean needDeadState = false;
        
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
        
        if (needDeadState) {
            deadState = new DFA.State("qDead");
            dfa.addState(deadState);
            remember(deadState, java.util.Collections.emptySet());
            
            for (char deadSymbol : alphabet) {
                dfa.addTransition(deadState, deadSymbol, deadState);
            }
            
            for (DFA.State state : states) {
                if (state == null || state == deadState) continue; // Skip null or the dead state itself
                
                for (char symbol : alphabet) {
                    DFA.StateTransition transition = new DFA.StateTransition(state, symbol);
                    
                    if (!dfa.getTransitions().containsKey(transition)) {
                        dfa.addTransition(state, symbol, deadState);
                    }
                }
            }
        }
    }
} 