package com.dfavisualizer.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

/**
 * Represents a Non-deterministic Finite Automaton (NFA) with epsilon transitions.
 * Used as an intermediate representation during regex to DFA conversion.
 */
public class NFA {
    public static final char EPSILON = '\0';  // Represents epsilon/empty transition
    
    private Set<Integer> states;
    private Set<Character> alphabet;
    private Map<NFATransition, Set<Integer>> transitions;
    private int startState;
    private Set<Integer> acceptStates;
    private int nextStateId;

    public NFA() {
        this.states = new HashSet<>();
        this.alphabet = new HashSet<>();
        this.transitions = new HashMap<>();
        this.acceptStates = new HashSet<>();
        this.nextStateId = 0;
    }

    public static NFA forSymbol(char symbol) {
        NFA nfa = new NFA();
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
    public static NFA forEpsilon() {
        NFA nfa = new NFA();
        int start = nfa.createState();
        int end = nfa.createState();
        
        nfa.setStartState(start);
        nfa.addAcceptState(end);
        nfa.addTransition(start, EPSILON, end);
        
        return nfa;
    }

    /**
     * Creates a basic NFA that accepts nothing (empty language).
     */
    public static NFA forEmpty() {
        NFA nfa = new NFA();
        int start = nfa.createState();
        int end = nfa.createState();
        
        nfa.setStartState(start);
        nfa.addAcceptState(end);
        
        return nfa;
    }

    public NFA concatenate(NFA other) {
        if (other == null) {
            return this;
        }
        
        NFA result = new NFA();
        
        Map<Integer, Integer> stateMap1 = copyStatesAndTransitions(this, result);
        
        Map<Integer, Integer> stateMap2 = copyStatesAndTransitions(other, result);
        
        for (int acceptState : this.acceptStates) {
            result.addTransition(stateMap1.get(acceptState), EPSILON, stateMap2.get(other.startState));
        }
        
        result.setStartState(stateMap1.get(this.startState));
        
        for (int acceptState : other.acceptStates) {
            result.addAcceptState(stateMap2.get(acceptState));
        }
        
        return result;
    }

    public NFA union(NFA other) {
        if (other == null) {
            return this;
        }
        
        NFA result = new NFA();
        
        int newStart = result.createState();
        result.setStartState(newStart);
        
        int newAccept = result.createState();
        result.addAcceptState(newAccept);
        
        Map<Integer, Integer> stateMap1 = copyStatesAndTransitions(this, result);
        
        Map<Integer, Integer> stateMap2 = copyStatesAndTransitions(other, result);
        
        result.addTransition(newStart, EPSILON, stateMap1.get(this.startState));
        result.addTransition(newStart, EPSILON, stateMap2.get(other.startState));
        
        for (int acceptState : this.acceptStates) {
            result.addTransition(stateMap1.get(acceptState), EPSILON, newAccept);
        }
        for (int acceptState : other.acceptStates) {
            result.addTransition(stateMap2.get(acceptState), EPSILON, newAccept);
        }
        
        return result;
    }

    /**
     * Creates an NFA that is the Kleene closure (star) of this NFA.
     */
    public NFA kleeneStar() {
        NFA result = new NFA();
        
        int newStart = result.createState();
        result.setStartState(newStart);
        
        int newAccept = result.createState();
        result.addAcceptState(newAccept);
        
        Map<Integer, Integer> stateMap = copyStatesAndTransitions(this, result);
        
        result.addTransition(newStart, EPSILON, stateMap.get(this.startState));
        
        result.addTransition(newStart, EPSILON, newAccept);
        
        for (int acceptState : this.acceptStates) {
            result.addTransition(stateMap.get(acceptState), EPSILON, stateMap.get(this.startState));
        }
        
        for (int acceptState : this.acceptStates) {
            result.addTransition(stateMap.get(acceptState), EPSILON, newAccept);
        }
        
        return result;
    }

    /**
     * Creates an NFA that accepts one or more repetitions of this NFA.
     */
    public NFA oneOrMore() {
        NFA copy = copy();
        return copy.concatenate(this.kleeneStar());
    }

    /**
     * Creates an NFA that accepts zero or one occurrence of this NFA.
     */
    public NFA zeroOrOne() {
        NFA result = new NFA();
        
        int newStart = result.createState();
        result.setStartState(newStart);
        
        int newAccept = result.createState();
        result.addAcceptState(newAccept);
        
        Map<Integer, Integer> stateMap = copyStatesAndTransitions(this, result);
        
        result.addTransition(newStart, EPSILON, stateMap.get(this.startState));
        
        result.addTransition(newStart, EPSILON, newAccept);
        
        for (int acceptState : this.acceptStates) {
            result.addTransition(stateMap.get(acceptState), EPSILON, newAccept);
        }
        
        return result;
    }

    public NFA copy() {
        NFA result = new NFA();
        
        Map<Integer, Integer> stateMap = copyStatesAndTransitions(this, result);
        
        result.setStartState(stateMap.get(this.startState));
        
        for (int acceptState : this.acceptStates) {
            result.addAcceptState(stateMap.get(acceptState));
        }
        
        return result;
    }

    /**
     * Helper method to copy states and transitions from one NFA to another.
     * Returns a mapping from original state IDs to new state IDs.
     */
    private Map<Integer, Integer> copyStatesAndTransitions(NFA source, NFA target) {
        Map<Integer, Integer> stateMap = new HashMap<>();
        
        for (int state : source.states) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            int newState = target.createState();
            stateMap.put(state, newState);
        }
        
        for (Map.Entry<NFATransition, Set<Integer>> entry : source.transitions.entrySet()) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            NFATransition transition = entry.getKey();
            int fromState = stateMap.get(transition.getState());
            char symbol = transition.getSymbol();
            
            for (int toState : entry.getValue()) {
                target.addTransition(fromState, symbol, stateMap.get(toState));
            }
        }
        
        return stateMap;
    }

    /**
     * Computes the epsilon closure of a set of states.
     * The epsilon closure is the set of states reachable from any state in the input set
     * by following zero or more epsilon transitions.
     */
    public Set<Integer> epsilonClosure(Set<Integer> states) {
        if (states == null || states.isEmpty()) {
            return new HashSet<>();
        }
        
        Set<Integer> result = new HashSet<>(states);
        Stack<Integer> stack = new Stack<>();
        
        stack.addAll(states);
        
        while (!stack.isEmpty()) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            int state = stack.pop();
            
            NFATransition transition = new NFATransition(state, EPSILON);
            Set<Integer> targets = transitions.get(transition);
            
            if (targets != null) {
                for (int target : targets) {
                    if (!result.contains(target)) {
                        result.add(target);
                        stack.push(target);
                    }
                }
            }
        }
        
        return result;
    }

    public Set<Integer> move(Set<Integer> states, char symbol) {
        if (states == null || states.isEmpty()) {
            return new HashSet<>();
        }
        
        Set<Integer> result = new HashSet<>();
        
        for (int state : states) {
            NFATransition transition = new NFATransition(state, symbol);
            Set<Integer> targets = transitions.get(transition);
            
            if (targets != null) {
                result.addAll(targets);
            }
        }
        
        return result;
    }

    public int createState() {
        int stateId = nextStateId++;
        states.add(stateId);
        return stateId;
    }

    public void addTransition(int fromState, char symbol, int toState) {
        try {
            if (!states.contains(fromState)) {
                throw new IllegalArgumentException("Source state " + fromState + " does not exist in the NFA");
            }
            
            if (!states.contains(toState)) {
                throw new IllegalArgumentException("Target state " + toState + " does not exist in the NFA");
            }
            
            if (symbol != EPSILON) {
                alphabet.add(symbol);
            }
            
            NFATransition transition = new NFATransition(fromState, symbol);
            transitions.computeIfAbsent(transition, k -> new HashSet<>()).add(toState);
        } catch (IllegalArgumentException e) {
            throw e; // Rethrow the original exception
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Error adding transition from state " + fromState + 
                " to state " + toState + " on symbol '" + symbol + "': " + e.getMessage(), e);
        }
    }

    public void setStartState(int startState) {
        if (!states.contains(startState)) {
            throw new IllegalArgumentException("State " + startState + " does not exist in the NFA");
        }
        this.startState = startState;
    }

    public void addAcceptState(int acceptState) {
        if (!states.contains(acceptState)) {
            throw new IllegalArgumentException("State " + acceptState + " does not exist in the NFA");
        }
        this.acceptStates.add(acceptState);
    }

    public Set<Integer> getStates() {
        return states;
    }

    public Set<Character> getAlphabet() {
        return alphabet;
    }

    public Map<NFATransition, Set<Integer>> getTransitions() {
        return transitions;
    }

    public int getStartState() {
        return startState;
    }

    public Set<Integer> getAcceptStates() {
        return acceptStates;
    }

    public static class NFATransition {
        private final int state;
        private final char symbol;

        public NFATransition(int state, char symbol) {
            this.state = state;
            this.symbol = symbol;
        }

        public int getState() {
            return state;
        }

        public char getSymbol() {
            return symbol;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NFATransition that = (NFATransition) o;
            return state == that.state && symbol == that.symbol;
        }

        @Override
        public int hashCode() {
            return Objects.hash(state, symbol);
        }

        @Override
        public String toString() {
            return "(" + state + ", " + (symbol == EPSILON ? "ε" : symbol) + ")";
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NFA with ").append(states.size()).append(" states\n");
        sb.append("Start state: ").append(startState).append("\n");
        sb.append("Accept states: ").append(acceptStates).append("\n");
        sb.append("Alphabet: ").append(alphabet).append("\n");
        sb.append("Transitions:\n");
        
        for (Map.Entry<NFATransition, Set<Integer>> entry : transitions.entrySet()) {
            NFATransition transition = entry.getKey();
            Set<Integer> targets = entry.getValue();
            
            for (int target : targets) {
                sb.append("  ").append(transition.getState())
                  .append(" --[").append(transition.getSymbol() == EPSILON ? "ε" : transition.getSymbol()).append("]--> ")
                  .append(target).append("\n");
            }
        }
        
        return sb.toString();
    }
} 