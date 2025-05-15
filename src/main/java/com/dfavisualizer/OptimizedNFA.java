package com.dfavisualizer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An optimized implementation of NFA that uses more efficient data structures
 * and algorithms for memory-intensive operations.
 */
public class OptimizedNFA {
    // Maximum number of states (can be adjusted based on expected complexity)
    private static final int MAX_STATES = 10000;
    
    // Use BitSet for state sets for more efficient operations
    private BitSet states;
    private BitSet acceptStates;
    
    // Use array-based adjacency list for transitions
    private List<Map<Character, BitSet>> transitions;
    
    // Use a separate map for epsilon transitions
    private List<BitSet> epsilonTransitions;
    
    // Alphabet is stored in a HashSet for O(1) lookups
    private Set<Character> alphabet;
    
    // Start state
    private int startState;
    
    // Next state ID for creating new states
    private int nextStateId;
    
    // Epsilon constant
    public static final char EPSILON = '\0';
    
    // Cached epsilon closures for frequently accessed states
    private Map<BitSet, BitSet> cachedEpsilonClosures;
    
    /**
     * Creates a new empty OptimizedNFA.
     */
    public OptimizedNFA() {
        this.states = new BitSet(MAX_STATES);
        this.acceptStates = new BitSet(MAX_STATES);
        this.transitions = new ArrayList<>(MAX_STATES);
        this.epsilonTransitions = new ArrayList<>(MAX_STATES);
        this.alphabet = new HashSet<>();
        this.startState = 0;
        this.nextStateId = 0;
        this.cachedEpsilonClosures = new HashMap<>();
        
        // Initialize transition lists
        for (int i = 0; i < MAX_STATES; i++) {
            transitions.add(new HashMap<>());
            epsilonTransitions.add(new BitSet(MAX_STATES));
        }
    }
    
    /**
     * Creates a basic NFA that accepts a single character.
     */
    public static OptimizedNFA forSymbol(char symbol) {
        OptimizedNFA nfa = new OptimizedNFA();
        int start = nfa.createState();
        int end = nfa.createState();
        
        nfa.setStartState(start);
        nfa.addAcceptState(end);
        nfa.addTransition(start, symbol, end);
        
        return nfa;
    }
    
    /**
     * Creates a basic NFA that accepts the empty string (epsilon).
     */
    public static OptimizedNFA forEpsilon() {
        OptimizedNFA nfa = new OptimizedNFA();
        int start = nfa.createState();
        int end = nfa.createState();
        
        nfa.setStartState(start);
        nfa.addAcceptState(end);
        nfa.addEpsilonTransition(start, end);
        
        return nfa;
    }
    
    /**
     * Creates a basic NFA that accepts nothing (empty language).
     */
    public static OptimizedNFA forEmpty() {
        OptimizedNFA nfa = new OptimizedNFA();
        int start = nfa.createState();
        int end = nfa.createState();
        
        nfa.setStartState(start);
        nfa.addAcceptState(end);
        
        return nfa;
    }
    
    /**
     * Creates a new state in the NFA.
     * 
     * @return The ID of the newly created state
     */
    public int createState() {
        int newState = nextStateId++;
        states.set(newState);
        return newState;
    }
    
    /**
     * Sets the start state of the NFA.
     * 
     * @param state The state to set as the start state
     */
    public void setStartState(int state) {
        if (!states.get(state)) {
            throw new IllegalArgumentException("State " + state + " does not exist");
        }
        startState = state;
    }
    
    /**
     * Adds a state to the set of accept states.
     * 
     * @param state The state to add as an accept state
     */
    public void addAcceptState(int state) {
        if (!states.get(state)) {
            throw new IllegalArgumentException("State " + state + " does not exist");
        }
        acceptStates.set(state);
    }
    
    /**
     * Adds a transition from one state to another on a given symbol.
     */
    public void addTransition(int fromState, char symbol, int toState) {
        if (!states.get(fromState) || !states.get(toState)) {
            throw new IllegalArgumentException("One or both states do not exist");
        }
        
        // Add to alphabet (except epsilon)
        if (symbol != EPSILON) {
            alphabet.add(symbol);
        }
        
        // Add the transition
        if (symbol == EPSILON) {
            // For epsilon transitions, use the specialized storage
            addEpsilonTransition(fromState, toState);
        } else {
            // For symbol transitions, use the map-based storage
            Map<Character, BitSet> stateTransitions = transitions.get(fromState);
            BitSet targetStates = stateTransitions.computeIfAbsent(symbol, k -> new BitSet(MAX_STATES));
            targetStates.set(toState);
        }
        
        // Clear cached epsilon closures since transitions changed
        cachedEpsilonClosures.clear();
    }
    
    /**
     * Adds an epsilon transition from one state to another.
     */
    public void addEpsilonTransition(int fromState, int toState) {
        if (!states.get(fromState) || !states.get(toState)) {
            throw new IllegalArgumentException("One or both states do not exist");
        }
        
        // Add the epsilon transition
        epsilonTransitions.get(fromState).set(toState);
        
        // Clear cached epsilon closures since transitions changed
        cachedEpsilonClosures.clear();
    }
    
    /**
     * Computes the epsilon closure of a set of states.
     * This is optimized to use BitSets for efficient set operations.
     */
    public BitSet epsilonClosure(BitSet stateSet) {
        // Check cache first
        if (cachedEpsilonClosures.containsKey(stateSet)) {
            return (BitSet) cachedEpsilonClosures.get(stateSet).clone();
        }
        
        // Create result set with initial states
        BitSet result = (BitSet) stateSet.clone();
        
        // Use a stack-based approach to avoid recursion
        BitSet stack = (BitSet) stateSet.clone();
        
        while (!stack.isEmpty()) {
            // Pop a state from the stack
            int state = stack.nextSetBit(0);
            stack.clear(state);
            
            // Get epsilon transitions for this state
            BitSet epsilonTargets = epsilonTransitions.get(state);
            
            // For each target state not already in result, add it and push to stack
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                if (!result.get(target)) {
                    result.set(target);
                    stack.set(target);
                }
            }
        }
        
        // Cache the result for future use
        cachedEpsilonClosures.put((BitSet) stateSet.clone(), (BitSet) result.clone());
        
        return result;
    }
    
    /**
     * Computes the set of states reachable from a set of states on a given symbol.
     */
    public BitSet move(BitSet stateSet, char symbol) {
        BitSet result = new BitSet(MAX_STATES);
        
        // For each state in the input set
        for (int state = stateSet.nextSetBit(0); state >= 0; state = stateSet.nextSetBit(state + 1)) {
            // Get transitions for this state and symbol
            Map<Character, BitSet> stateTransitions = transitions.get(state);
            BitSet targetStates = stateTransitions.get(symbol);
            
            // If there are transitions, add them to the result
            if (targetStates != null) {
                result.or(targetStates);
            }
        }
        
        return result;
    }
    
    /**
     * Creates an NFA that is the concatenation of this NFA and another.
     * Uses optimized approach to avoid creating too many intermediate states.
     */
    public OptimizedNFA concatenate(OptimizedNFA other) {
        if (other == null) {
            return this;
        }
        
        // Compute max state ID to avoid conflicts
        int maxState = Math.max(this.nextStateId, other.nextStateId);
        int offset = maxState;
        
        OptimizedNFA result = new OptimizedNFA();
        
        // Copy states from this NFA
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            result.states.set(state);
            
            // Copy accept states
            if (acceptStates.get(state)) {
                // Accept states from this NFA become regular states
                // We'll connect them to the other NFA's start state
            }
            
            // Copy transitions
            Map<Character, BitSet> stateTransitions = transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                result.alphabet.add(symbol);
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(state, symbol, target);
                }
            }
            
            // Copy epsilon transitions
            BitSet epsilonTargets = epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(state, target);
            }
        }
        
        // Copy states from other NFA with offset
        for (int state = other.states.nextSetBit(0); state >= 0; state = other.states.nextSetBit(state + 1)) {
            int newState = state + offset;
            result.states.set(newState);
            
            // Copy accept states
            if (other.acceptStates.get(state)) {
                result.addAcceptState(newState);
            }
            
            // Copy transitions
            Map<Character, BitSet> stateTransitions = other.transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                result.alphabet.add(symbol);
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(newState, symbol, target + offset);
                }
            }
            
            // Copy epsilon transitions
            BitSet epsilonTargets = other.epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(newState, target + offset);
            }
        }
        
        // Set start state
        result.setStartState(this.startState);
        
        // Connect accept states of this NFA to start state of other NFA
        for (int state = this.acceptStates.nextSetBit(0); state >= 0; state = this.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(state, other.startState + offset);
        }
        
        result.nextStateId = Math.max(this.nextStateId, other.nextStateId + offset);
        
        return result;
    }
    
    /**
     * Creates an NFA that is the union (alternation) of this NFA and another.
     * Uses optimized approach to minimize state creation.
     */
    public OptimizedNFA union(OptimizedNFA other) {
        if (other == null) {
            return this;
        }
        
        // Compute max state ID to avoid conflicts
        int maxState = Math.max(this.nextStateId, other.nextStateId);
        int offset = maxState;
        
        OptimizedNFA result = new OptimizedNFA();
        
        // Create new start and accept states
        int newStart = result.createState();
        int newAccept = result.createState();
        
        result.setStartState(newStart);
        result.addAcceptState(newAccept);
        
        // Copy states from this NFA
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            result.states.set(state);
            
            // Copy transitions
            Map<Character, BitSet> stateTransitions = transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                result.alphabet.add(symbol);
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(state, symbol, target);
                }
            }
            
            // Copy epsilon transitions
            BitSet epsilonTargets = epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(state, target);
            }
        }
        
        // Copy states from other NFA with offset
        for (int state = other.states.nextSetBit(0); state >= 0; state = other.states.nextSetBit(state + 1)) {
            int newState = state + offset;
            result.states.set(newState);
            
            // Copy transitions
            Map<Character, BitSet> stateTransitions = other.transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                result.alphabet.add(symbol);
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(newState, symbol, target + offset);
                }
            }
            
            // Copy epsilon transitions
            BitSet epsilonTargets = other.epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(newState, target + offset);
            }
        }
        
        // Connect new start state to start states of both NFAs
        result.addEpsilonTransition(newStart, this.startState);
        result.addEpsilonTransition(newStart, other.startState + offset);
        
        // Connect accept states of both NFAs to new accept state
        for (int state = this.acceptStates.nextSetBit(0); state >= 0; state = this.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(state, newAccept);
        }
        
        for (int state = other.acceptStates.nextSetBit(0); state >= 0; state = other.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(state + offset, newAccept);
        }
        
        result.nextStateId = Math.max(result.nextStateId, Math.max(this.nextStateId, other.nextStateId + offset));
        
        return result;
    }
    
    /**
     * Creates an NFA that is the Kleene closure (star) of this NFA.
     * Optimized to minimize state creation.
     */
    public OptimizedNFA kleeneStar() {
        OptimizedNFA result = new OptimizedNFA();
        
        // Create new start and accept states
        int newStart = result.createState();
        int newAccept = result.createState();
        
        result.setStartState(newStart);
        result.addAcceptState(newAccept);
        
        // Copy states from this NFA
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            result.states.set(state);
            
            // Copy transitions
            Map<Character, BitSet> stateTransitions = transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                result.alphabet.add(symbol);
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(state, symbol, target);
                }
            }
            
            // Copy epsilon transitions
            BitSet epsilonTargets = epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(state, target);
            }
        }
        
        // Connect new start state to start state of this NFA
        result.addEpsilonTransition(newStart, this.startState);
        
        // Connect new start state to new accept state (empty string case)
        result.addEpsilonTransition(newStart, newAccept);
        
        // Connect accept states of this NFA to new accept state
        for (int state = this.acceptStates.nextSetBit(0); state >= 0; state = this.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(state, newAccept);
        }
        
        // Connect accept states of this NFA back to start state for repetition
        for (int state = this.acceptStates.nextSetBit(0); state >= 0; state = this.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(state, this.startState);
        }
        
        return result;
    }
    
    /**
     * Gets the start state of this NFA.
     */
    public int getStartState() {
        return startState;
    }
    
    /**
     * Gets all states in this NFA.
     */
    public BitSet getStates() {
        return (BitSet) states.clone();
    }
    
    /**
     * Gets all accept states in this NFA.
     */
    public BitSet getAcceptStates() {
        return (BitSet) acceptStates.clone();
    }
    
    /**
     * Gets the alphabet of this NFA.
     */
    public Set<Character> getAlphabet() {
        return new HashSet<>(alphabet);
    }
    
    /**
     * Creates a conventional NFA from this optimized NFA.
     * This is useful for compatibility with existing code.
     */
    public NFA toConventionalNFA() {
        NFA nfa = new NFA();
        
        // Create states
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            while (nfa.getStates().size() <= state) {
                nfa.createState();
            }
        }
        
        // Set start state
        nfa.setStartState(startState);
        
        // Set accept states
        for (int state = acceptStates.nextSetBit(0); state >= 0; state = acceptStates.nextSetBit(state + 1)) {
            nfa.addAcceptState(state);
        }
        
        // Add transitions
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            // Add symbol transitions
            Map<Character, BitSet> stateTransitions = transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    nfa.addTransition(state, symbol, target);
                }
            }
            
            // Add epsilon transitions
            BitSet epsilonTargets = epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                nfa.addTransition(state, EPSILON, target);
            }
        }
        
        return nfa;
    }
} 