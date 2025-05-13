package com.dfavisualizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a Deterministic Finite Automaton (DFA).
 */
public class DFA {
    private Set<State> states;
    private Set<Character> alphabet;
    private Map<StateTransition, State> transitions;
    private State startState;
    private Set<State> acceptStates;

    public DFA() {
        this.states = new HashSet<>();
        this.alphabet = new HashSet<>();
        this.transitions = new HashMap<>();
        this.acceptStates = new HashSet<>();
    }

    public Set<State> getStates() {
        return states;
    }

    public Set<Character> getAlphabet() {
        return alphabet;
    }

    public Map<StateTransition, State> getTransitions() {
        return transitions;
    }

    public State getStartState() {
        return startState;
    }

    public void setStartState(State startState) {
        this.startState = startState;
        this.states.add(startState);
    }

    public Set<State> getAcceptStates() {
        return acceptStates;
    }

    public void addState(State state) {
        this.states.add(state);
    }

    public void addSymbol(char symbol) {
        this.alphabet.add(symbol);
    }

    public void addTransition(State fromState, char symbol, State toState) {
        this.states.add(fromState);
        this.states.add(toState);
        this.alphabet.add(symbol);
        this.transitions.put(new StateTransition(fromState, symbol), toState);
    }

    public void addAcceptState(State state) {
        this.acceptStates.add(state);
        this.states.add(state);
    }

    public State getTransitionTarget(State currentState, char symbol) {
        return transitions.get(new StateTransition(currentState, symbol));
    }

    /**
     * Represents a state in the DFA.
     */
    public static class State {
        private final String name;

        public State(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State state = (State) o;
            return Objects.equals(name, state.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Represents a transition in the DFA.
     */
    public static class StateTransition {
        private final State state;
        private final char symbol;

        public StateTransition(State state, char symbol) {
            this.state = state;
            this.symbol = symbol;
        }

        public State getState() {
            return state;
        }

        public char getSymbol() {
            return symbol;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StateTransition that = (StateTransition) o;
            return symbol == that.symbol && Objects.equals(state, that.state);
        }

        @Override
        public int hashCode() {
            return Objects.hash(state, symbol);
        }

        @Override
        public String toString() {
            return "(" + state + ", " + symbol + ")";
        }
    }
} 