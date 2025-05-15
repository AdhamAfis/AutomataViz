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
    
    /**
     * Creates a basic NFA that accepts the empty string (epsilon).
     */
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
        try {
            if (!states.get(fromState) || !states.get(toState)) {
                throw new IllegalArgumentException("One or both states do not exist: fromState=" + fromState + ", toState=" + toState);
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
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("State index out of bounds: fromState=" + fromState + ", toState=" + toState + 
                ". Maximum state index allowed is " + (MAX_STATES - 1), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error adding transition from state " + fromState + 
                " to state " + toState + " on symbol '" + symbol + "': " + e.getMessage(), e);
        }
    }
    
    /**
     * Adds an epsilon transition from one state to another.
     */
    public void addEpsilonTransition(int fromState, int toState) {
        try {
            if (!states.get(fromState) || !states.get(toState)) {
                throw new IllegalArgumentException("One or both states do not exist: fromState=" + fromState + ", toState=" + toState);
            }
            
            // Add the epsilon transition
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
        
        // Create a new NFA for the result
        OptimizedNFA result = new OptimizedNFA();
        
        // First, create all the states needed
        Map<Integer, Integer> thisStateMap = new HashMap<>();
        Map<Integer, Integer> otherStateMap = new HashMap<>();
        
        // Map states from this NFA
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            int newState = result.createState();
            thisStateMap.put(state, newState);
        }
        
        // Map states from other NFA
        for (int state = other.states.nextSetBit(0); state >= 0; state = other.states.nextSetBit(state + 1)) {
            int newState = result.createState();
            otherStateMap.put(state, newState);
        }
        
        // Set start state from this NFA
        result.setStartState(thisStateMap.get(this.startState));
        
        // Set accept states from other NFA
        for (int state = other.acceptStates.nextSetBit(0); state >= 0; state = other.acceptStates.nextSetBit(state + 1)) {
            result.addAcceptState(otherStateMap.get(state));
        }
        
        // Copy transitions from this NFA
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            // Copy symbol transitions
            Map<Character, BitSet> stateTransitions = transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(thisStateMap.get(state), symbol, thisStateMap.get(target));
                }
            }
            
            // Copy epsilon transitions
            BitSet epsilonTargets = epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(thisStateMap.get(state), thisStateMap.get(target));
            }
        }
        
        // Copy transitions from other NFA
        for (int state = other.states.nextSetBit(0); state >= 0; state = other.states.nextSetBit(state + 1)) {
            // Copy symbol transitions
            Map<Character, BitSet> stateTransitions = other.transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(otherStateMap.get(state), symbol, otherStateMap.get(target));
                }
            }
            
            // Copy epsilon transitions
            BitSet epsilonTargets = other.epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(otherStateMap.get(state), otherStateMap.get(target));
            }
        }
        
        // Connect accept states of this NFA to start state of other NFA
        for (int state = this.acceptStates.nextSetBit(0); state >= 0; state = this.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(thisStateMap.get(state), otherStateMap.get(other.startState));
        }
        
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
        
        // Create a new NFA for the result
        OptimizedNFA result = new OptimizedNFA();
        
        // Create new start and accept states
        int newStart = result.createState();
        int newAccept = result.createState();
        
        result.setStartState(newStart);
        result.addAcceptState(newAccept);
        
        // Create state mappings
        Map<Integer, Integer> thisStateMap = new HashMap<>();
        Map<Integer, Integer> otherStateMap = new HashMap<>();
        
        // Map states from this NFA
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            int newState = result.createState();
            thisStateMap.put(state, newState);
        }
        
        // Map states from other NFA
        for (int state = other.states.nextSetBit(0); state >= 0; state = other.states.nextSetBit(state + 1)) {
            int newState = result.createState();
            otherStateMap.put(state, newState);
        }
        
        // Copy transitions from this NFA
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            // Copy symbol transitions
            Map<Character, BitSet> stateTransitions = transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(thisStateMap.get(state), symbol, thisStateMap.get(target));
                }
            }
            
            // Copy epsilon transitions
            BitSet epsilonTargets = epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(thisStateMap.get(state), thisStateMap.get(target));
            }
        }
        
        // Copy transitions from other NFA
        for (int state = other.states.nextSetBit(0); state >= 0; state = other.states.nextSetBit(state + 1)) {
            // Copy symbol transitions
            Map<Character, BitSet> stateTransitions = other.transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(otherStateMap.get(state), symbol, otherStateMap.get(target));
                }
            }
            
            // Copy epsilon transitions
            BitSet epsilonTargets = other.epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(otherStateMap.get(state), otherStateMap.get(target));
            }
        }
        
        // Connect new start state to start states of both NFAs
        result.addEpsilonTransition(newStart, thisStateMap.get(this.startState));
        result.addEpsilonTransition(newStart, otherStateMap.get(other.startState));
        
        // Connect accept states of both NFAs to new accept state
        for (int state = this.acceptStates.nextSetBit(0); state >= 0; state = this.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(thisStateMap.get(state), newAccept);
        }
        
        for (int state = other.acceptStates.nextSetBit(0); state >= 0; state = other.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(otherStateMap.get(state), newAccept);
        }
        
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
        
        // Create state mapping
        Map<Integer, Integer> stateMap = new HashMap<>();
        
        // Map states from this NFA
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            int newState = result.createState();
            stateMap.put(state, newState);
        }
        
        // Copy transitions from this NFA
        for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
            // Copy symbol transitions
            Map<Character, BitSet> stateTransitions = transitions.get(state);
            for (Map.Entry<Character, BitSet> entry : stateTransitions.entrySet()) {
                char symbol = entry.getKey();
                BitSet targets = entry.getValue();
                
                for (int target = targets.nextSetBit(0); target >= 0; target = targets.nextSetBit(target + 1)) {
                    result.addTransition(stateMap.get(state), symbol, stateMap.get(target));
                }
            }
            
            // Copy epsilon transitions
            BitSet epsilonTargets = epsilonTransitions.get(state);
            for (int target = epsilonTargets.nextSetBit(0); target >= 0; target = epsilonTargets.nextSetBit(target + 1)) {
                result.addEpsilonTransition(stateMap.get(state), stateMap.get(target));
            }
        }
        
        // Connect new start state to start state of this NFA
        result.addEpsilonTransition(newStart, stateMap.get(this.startState));
        
        // Connect new start state to new accept state (empty string case)
        result.addEpsilonTransition(newStart, newAccept);
        
        // Connect accept states of this NFA to new accept state
        for (int state = this.acceptStates.nextSetBit(0); state >= 0; state = this.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(stateMap.get(state), newAccept);
        }
        
        // Connect accept states of this NFA back to start state for repetition
        for (int state = this.acceptStates.nextSetBit(0); state >= 0; state = this.acceptStates.nextSetBit(state + 1)) {
            result.addEpsilonTransition(stateMap.get(state), stateMap.get(this.startState));
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
        
        try {
            // Create states with proper indexing
            int maxState = states.length();
            for (int i = 0; i < maxState; i++) {
                nfa.createState();
            }
            
            // Set start state
            nfa.setStartState(startState);
            
            // Set accept states
            for (int state = acceptStates.nextSetBit(0); state >= 0; state = acceptStates.nextSetBit(state + 1)) {
                try {
                    nfa.addAcceptState(state);
                } catch (Exception e) {
                    System.err.println("Warning: Could not add accept state " + state + ": " + e.getMessage());
                }
            }
            
            // Add transitions
            for (int state = states.nextSetBit(0); state >= 0; state = states.nextSetBit(state + 1)) {
                // Add symbol transitions
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
                
                // Add epsilon transitions
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