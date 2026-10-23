package com.dfavisualizer.model;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OptimizedNFA {
    private static final int MAX_STATES = 10000;

    private BitSet states;
    private BitSet acceptStates;

    private List<Map<Character, BitSet>> transitions;
    
    private List<BitSet> epsilonTransitions;

    private Set<Character> alphabet;
    
    private int startState;
    
    private int nextStateId;
    
    public static final char EPSILON = '\0';
    
    private Map<BitSet, BitSet> cachedEpsilonClosures;
    
    public OptimizedNFA() {
        this.states = new BitSet(MAX_STATES);
        this.acceptStates = new BitSet(MAX_STATES);
        this.transitions = new ArrayList<>(MAX_STATES);
        this.epsilonTransitions = new ArrayList<>(MAX_STATES);
        this.alphabet = new HashSet<>();
        this.startState = 0;
        this.nextStateId = 0;
        this.cachedEpsilonClosures = new HashMap<>();
        
        for (int i = 0; i < MAX_STATES; i++) {
            transitions.add(new HashMap<>());
            epsilonTransitions.add(new BitSet(MAX_STATES));
        }
    }
    
    public static OptimizedNFA forSymbol(char symbol) {
        OptimizedNFA nfa = new OptimizedNFA();
        try {
            int start = nfa.createState();
            int end = nfa.createState();
            
            nfa.setStartState(start);
            nfa.addAcceptState(end);
            nfa.addTransition(start, symbol, end);
            
            return nfa;
        } catch (Exception e) {
            System.err.println("Error creating NFA for symbol '" + symbol + "': " + e.getMessage());
            // Create a minimal valid NFA as fallback
            int start = nfa.createState();
            nfa.setStartState(start);
            nfa.addAcceptState(start); // Same state is both start and accept
            return nfa;
        }
    }
    
    public static OptimizedNFA forEpsilon() {
        OptimizedNFA nfa = new OptimizedNFA();
        try {
            int start = nfa.createState();
            int end = nfa.createState();
            
            nfa.setStartState(start);
            nfa.addAcceptState(end);
            nfa.addEpsilonTransition(start, end);
            
            return nfa;
        } catch (Exception e) {
            System.err.println("Error creating NFA for epsilon: " + e.getMessage());
            // Create a minimal valid NFA as fallback
            int start = nfa.createState();
            nfa.setStartState(start);
            nfa.addAcceptState(start); // Same state is both start and accept
            return nfa;
        }
    }
    
    public static OptimizedNFA forEmpty() {
        OptimizedNFA nfa = new OptimizedNFA();
        int start = nfa.createState();
        int end = nfa.createState();
        
        nfa.setStartState(start);
        nfa.addAcceptState(end);
        
        return nfa;
    }
    
    public int createState() {
        int newState = nextStateId++;
        states.set(newState);
        return newState;
    }
    
    public void setStartState(int state) {
        if (!states.get(state)) {
            throw new IllegalArgumentException("State " + state + " does not exist");
        }
        startState = state;
    }
    
    public void addAcceptState(int state) {
        if (!states.get(state)) {
            throw new IllegalArgumentException("State " + state + " does not exist");
        }
        acceptStates.set(state);
    }
    
    public void addTransition(int fromState, char symbol, int toState) {
        try {
            if (!states.get(fromState) || !states.get(toState)) {
                throw new IllegalArgumentException("One or both states do not exist: fromState=" + fromState + ", toState=" + toState);
            }
            
            if (symbol != EPSILON) {
                alphabet.add(symbol);
            }
            
            if (symbol == EPSILON) {
                addEpsilonTransition(fromState, toState);
            } else {
                Map<Character, BitSet> stateTransitions = transitions.get(fromState);
                BitSet targetStates = stateTransitions.computeIfAbsent(symbol, k -> new BitSet(MAX_STATES));
                targetStates.set(toState);
            }
            
            // Clear cached epsilon closures since transitions changed
            cachedEpsilonClosures.clear();
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("State index out of bounds: fromState=" + fromState + ", toState=" + toState + 
                ". Maximum state index allowed is " + (MAX_STATES - 1), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error adding transition from state " + fromState + 
                " to state " + toState + " on symbol '" + symbol + "': " + e.getMessage(), e);
        }
    }
    
    public void addEpsilonTransition(int fromState, int toState) {
        try {
            if (!states.get(fromState) || !states.get(toState)) {
                throw new IllegalArgumentException("One or both states do not exist: fromState=" + fromState + ", toState=" + toState);
            }
            
            epsilonTransitions.get(fromState).set(toState);
            
            // Clear cached epsilon closures since transitions changed
            cachedEpsilonClosures.clear();
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("State index out of bounds: fromState=" + fromState + ", toState=" + toState + 
                ". Maximum state index allowed is " + (MAX_STATES - 1), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error adding epsilon transition from state " + fromState + 
                " to state " + toState + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Computes the epsilon closure of a set of states.
     * This is optimized to use BitSets for efficient set operations.
     */
    public BitSet epsilonClosure(BitSet stateSet) {
        if (cachedEpsilonClosures.containsKey(stateSet)) {
            return (BitSet) cachedEpsilonClosures.get(stateSet).clone();
        }
        
        BitSet result = (BitSet) stateSet.clone();
        
        // Use a stack-based approach to avoid recursion
        BitSet stack = (BitSet) stateSet.clone();
        
        while (!stack.isEmpty()) {
            int state = stack.nextSetBit(0);
            stack.clear(state);
            
            BitSet epsilonTargets = epsilonTransitions.get(state);
            
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                if (!result.get(target)) {
                    result.set(target);
                    stack.set(target);
                }
            }
        }
        
        cachedEpsilonClosures.put((BitSet) stateSet.clone(), (BitSet) result.clone());
        
        return result;
    }
    
    public BitSet move(BitSet stateSet, char symbol) {
        BitSet result = new BitSet(MAX_STATES);
        
        for (int state = stateSet.nextSetBit(0); state >= 0; state = stateSet.nextSetBit(state + 1)) {
            Map<Character, BitSet> stateTransitions = transitions.get(state);
            BitSet targetStates = stateTransitions.get(symbol);
            
            if (targetStates != null) {
                result.or(targetStates);
            }
        }
        
        return result;
    }
    
    public OptimizedNFA concatenate(OptimizedNFA other) {
        if (other == null) {
            return this;
        }
        
        OptimizedNFA result = new OptimizedNFA();
        
        Map<Integer, Integer> thisStateMap = new HashMap<>();
        Map<Integer, Integer> otherStateMap = new HashMap<>();
        
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            int newState = result.createState();
            thisStateMap.put(state, newState);
        }
        
        for (int state = other.states.nextSetBit(0); state >= 0; state = other.states.nextSetBit(state + 1)) {
            int newState = result.createState();
            otherStateMap.put(state, newState);
        }
        
        result.setStartState(thisStateMap.get(this.startState));
        
        for (int state = other.acceptStates.nextSetBit(0); state >= 0; state = other.acceptStates.nextSetBit(state + 1)) {
            result.addAcceptState(otherStateMap.get(state));
        }
        
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            Map<Character, BitSet> stateTransitions = transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(thisStateMap.get(state), symbol, thisStateMap.get(target));
                }
            }
            
            BitSet epsilonTargets = epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(thisStateMap.get(state), thisStateMap.get(target));
            }
        }
        
        for (int state = other.states.nextSetBit(0); state >= 0; state = other.states.nextSetBit(state + 1)) {
            Map<Character, BitSet> stateTransitions = other.transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(otherStateMap.get(state), symbol, otherStateMap.get(target));
                }
            }
            
            BitSet epsilonTargets = other.epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(otherStateMap.get(state), otherStateMap.get(target));
            }
        }
        
        for (int state = this.acceptStates.nextSetBit(0); state >= 0; state = this.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(thisStateMap.get(state), otherStateMap.get(other.startState));
        }
        
        return result;
    }
    
    public OptimizedNFA union(OptimizedNFA other) {
        if (other == null) {
            return this;
        }
        
        OptimizedNFA result = new OptimizedNFA();
        
        int newStart = result.createState();
        int newAccept = result.createState();
        
        result.setStartState(newStart);
        result.addAcceptState(newAccept);
        
        Map<Integer, Integer> thisStateMap = new HashMap<>();
        Map<Integer, Integer> otherStateMap = new HashMap<>();
        
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            int newState = result.createState();
            thisStateMap.put(state, newState);
        }
        
        for (int state = other.states.nextSetBit(0); state >= 0; state = other.states.nextSetBit(state + 1)) {
            int newState = result.createState();
            otherStateMap.put(state, newState);
        }
        
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            Map<Character, BitSet> stateTransitions = transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(thisStateMap.get(state), symbol, thisStateMap.get(target));
                }
            }
            
            BitSet epsilonTargets = epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(thisStateMap.get(state), thisStateMap.get(target));
            }
        }
        
        for (int state = other.states.nextSetBit(0); state >= 0; state = other.states.nextSetBit(state + 1)) {
            Map<Character, BitSet> stateTransitions = other.transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(otherStateMap.get(state), symbol, otherStateMap.get(target));
                }
            }
            
            BitSet epsilonTargets = other.epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(otherStateMap.get(state), otherStateMap.get(target));
            }
        }
        
        result.addEpsilonTransition(newStart, thisStateMap.get(this.startState));
        result.addEpsilonTransition(newStart, otherStateMap.get(other.startState));
        
        for (int state = this.acceptStates.nextSetBit(0); state >= 0; state = this.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(thisStateMap.get(state), newAccept);
        }
        
        for (int state = other.acceptStates.nextSetBit(0); state >= 0; state = other.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(otherStateMap.get(state), newAccept);
        }
        
        return result;
    }
    
    public OptimizedNFA kleeneStar() {
        OptimizedNFA result = new OptimizedNFA();
        
        int newStart = result.createState();
        int newAccept = result.createState();
        
        result.setStartState(newStart);
        result.addAcceptState(newAccept);
        
        Map<Integer, Integer> stateMap = new HashMap<>();
        
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            int newState = result.createState();
            stateMap.put(state, newState);
        }
        
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            Map<Character, BitSet> stateTransitions = transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(stateMap.get(state), symbol, stateMap.get(target));
                }
            }
            
            BitSet epsilonTargets = epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(stateMap.get(state), stateMap.get(target));
            }
        }
        
        result.addEpsilonTransition(newStart, stateMap.get(this.startState));
        
        result.addEpsilonTransition(newStart, newAccept);
        
        for (int state = this.acceptStates.nextSetBit(0); state >= 0; state = this.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(stateMap.get(state), newAccept);
        }
        
        for (int state = this.acceptStates.nextSetBit(0); state >= 0; state = this.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(stateMap.get(state), stateMap.get(this.startState));
        }
        
        return result;
    }
    
    public int getStartState() {
        return startState;
    }
    
    public BitSet getStates() {
        return (BitSet) states.clone();
    }
    
    public BitSet getAcceptStates() {
        return (BitSet) acceptStates.clone();
    }
    
    public Set<Character> getAlphabet() {
        return new HashSet<>(alphabet);
    }
    
    /**
     * Creates a conventional NFA from this optimized NFA.
     * This is useful for compatibility with existing code.
     */
    public NFA toConventionalNFA() {
        NFA nfa = new NFA();
        
        try {
            int maxState = states.length();
            for (int i = 0; i < maxState; i++) {
                nfa.createState();
            }
            
            nfa.setStartState(startState);
            
            for (int state = acceptStates.nextSetBit(0); state >= 0; state = acceptStates.nextSetBit(state + 1)) {
                try {
                    nfa.addAcceptState(state);
                } catch (Exception e) {
                    System.err.println("Warning: Could not add accept state " + state + ": " + e.getMessage());
                }
            }
            
            for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
                Map<Character, BitSet> stateTransitions = transitions.get(state);
                for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                    char symbol = entry.getKey();
                    BitSet targets = entry.getValue();
                    
                    for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                        try {
                            nfa.addTransition(state, symbol, target);
                        } catch (Exception e) {
                            System.err.println("Warning: Could not add transition from " + state + 
                                " to " + target + " on symbol '" + symbol + "': " + e.getMessage());
                        }
                    }
                }
                
                BitSet epsilonTargets = epsilonTransitions.get(state);
                for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                    try {
                        nfa.addTransition(state, EPSILON, target);
                    } catch (Exception e) {
                        System.err.println("Warning: Could not add epsilon transition from " + state + 
                            " to " + target + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error converting OptimizedNFA to conventional NFA: " + e.getMessage());
            e.printStackTrace();
        }
        
        return nfa;
    }
} 