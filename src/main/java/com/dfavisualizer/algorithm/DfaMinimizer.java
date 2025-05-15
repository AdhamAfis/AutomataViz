package com.dfavisualizer.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.dfavisualizer.model.DFA;

/**
 * Implements Hopcroft's algorithm for DFA minimization.
 */
public class DfaMinimizer {

    /**
     * Minimizes the given DFA by combining equivalent states.
     * 
     * @param dfa The DFA to minimize
     * @return A minimized equivalent DFA
     */
    public DFA minimize(DFA dfa) {
        if (dfa == null) {
            throw new IllegalArgumentException("Cannot minimize null DFA");
        }
        
        // For tiny DFAs (0-1 states), just return a copy
        if (dfa.getStates().size() <= 1) {
            return copyDfa(dfa);
        }
        
        // Get all states and alphabet
        Set<DFA.State> allStates = dfa.getStates();
        Set<Character> alphabet = dfa.getAlphabet();
        
        // No minimization possible with empty alphabet
        if (alphabet.isEmpty()) {
            return copyDfa(dfa);
        }
        
        // Group states into accepting and non-accepting sets (initial partition)
        Set<DFA.State> acceptStates = dfa.getAcceptStates();
        Set<DFA.State> nonAcceptStates = new HashSet<>(allStates);
        nonAcceptStates.removeAll(acceptStates);
        
        // Special case: if all states are accepting or all are non-accepting,
        // we need a different initial partition based on transitions
        if (acceptStates.isEmpty() || nonAcceptStates.isEmpty()) {
            return minimizeWithSingleInitialPartition(dfa, allStates, alphabet);
        }
        
        // Partitioning of states into sets of equivalent states
        List<Set<DFA.State>> partitions = new ArrayList<>();
        if (!acceptStates.isEmpty()) {
            partitions.add(new HashSet<>(acceptStates));
        }
        if (!nonAcceptStates.isEmpty()) {
            partitions.add(new HashSet<>(nonAcceptStates));
        }
        
        // Refinement queue
        Queue<Set<DFA.State>> queue = new LinkedList<>(partitions);
        
        // Refine partitions until no more refinements are possible
        while (!queue.isEmpty()) {
            Set<DFA.State> setA = queue.poll();
            
            // For each input symbol
            for (char symbol : alphabet) {
                // Find states that lead to setA on input symbol
                Set<DFA.State> leadingToA = getStatesThatLeadTo(dfa, setA, symbol);
                
                // Make a copy of current partitions to work with
                List<Set<DFA.State>> newPartitions = new ArrayList<>();
                
                // For each existing partition
                for (Set<DFA.State> partition : partitions) {
                    // Skip if this is the set being used to refine
                    if (partition.equals(setA)) {
                        newPartitions.add(partition);
                        continue;
                    }
                    
                    // Intersect
                    Set<DFA.State> intersection = new HashSet<>(partition);
                    intersection.retainAll(leadingToA);
                    
                    // Difference
                    Set<DFA.State> difference = new HashSet<>(partition);
                    difference.removeAll(leadingToA);
                    
                    // If both sets are non-empty, the partition has been split
                    if (!intersection.isEmpty() && !difference.isEmpty()) {
                        newPartitions.add(intersection);
                        newPartitions.add(difference);
                        
                        // Add to queue based on which is smaller
                        if (queue.contains(partition)) {
                            queue.remove(partition);
                            queue.add(intersection);
                            queue.add(difference);
                        } else {
                            if (intersection.size() <= difference.size()) {
                                queue.add(intersection);
                            } else {
                                queue.add(difference);
                            }
                        }
                    } else {
                        // No refinement for this partition
                        newPartitions.add(partition);
                    }
                }
                
                // Update partitions
                partitions = newPartitions;
            }
        }
        
        // Create a new minimized DFA
        return createMinimizedDfa(dfa, partitions);
    }
    
    /**
     * Special case minimization when all states are accepting or all are non-accepting.
     * In this case, we need a different strategy for the initial partition.
     */
    private DFA minimizeWithSingleInitialPartition(DFA dfa, Set<DFA.State> allStates, Set<Character> alphabet) {
        // Start with each state in its own partition
        List<Set<DFA.State>> partitions = new ArrayList<>();
        for (DFA.State state : allStates) {
            Set<DFA.State> partition = new HashSet<>();
            partition.add(state);
            partitions.add(partition);
        }
        
        // Iteratively merge equivalent states
        boolean changed;
        do {
            changed = false;
            
            // Try to merge partitions
            for (int i = 0; i < partitions.size(); i++) {
                for (int j = i + 1; j < partitions.size(); j++) {
                    if (areEquivalentPartitions(dfa, partitions.get(i), partitions.get(j), partitions, alphabet)) {
                        // Merge j into i
                        partitions.get(i).addAll(partitions.get(j));
                        partitions.remove(j);
                        changed = true;
                        break;
                    }
                }
                if (changed) break;
            }
        } while (changed);
        
        return createMinimizedDfa(dfa, partitions);
    }
    
    /**
     * Checks if two partitions are equivalent (have same transitions to same partitions).
     */
    private boolean areEquivalentPartitions(DFA dfa, Set<DFA.State> p1, Set<DFA.State> p2, 
                                           List<Set<DFA.State>> partitions, Set<Character> alphabet) {
        DFA.State s1 = p1.iterator().next();
        DFA.State s2 = p2.iterator().next();
        
        // States must have same acceptance status
        boolean s1Accepting = dfa.getAcceptStates().contains(s1);
        boolean s2Accepting = dfa.getAcceptStates().contains(s2);
        if (s1Accepting != s2Accepting) {
            return false;
        }
        
        // For each input symbol, transitions must go to same partition
        for (char symbol : alphabet) {
            DFA.State target1 = dfa.getTransitionTarget(s1, symbol);
            DFA.State target2 = dfa.getTransitionTarget(s2, symbol);
            
            // Both must transition to null or both to non-null
            if ((target1 == null) != (target2 == null)) {
                return false;
            }
            
            // If both have transitions, check if they go to the same partition
            if (target1 != null && target2 != null) {
                int partition1 = findPartitionIndex(partitions, target1);
                int partition2 = findPartitionIndex(partitions, target2);
                
                if (partition1 != partition2) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Find the index of the partition containing the given state.
     */
    private int findPartitionIndex(List<Set<DFA.State>> partitions, DFA.State state) {
        for (int i = 0; i < partitions.size(); i++) {
            if (partitions.get(i).contains(state)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Creates a direct copy of the DFA.
     */
    private DFA copyDfa(DFA dfa) {
        DFA copy = new DFA();
        
        // Copy states
        Map<DFA.State, DFA.State> stateMap = new HashMap<>();
        for (DFA.State state : dfa.getStates()) {
            DFA.State newState = new DFA.State(state.getName());
            copy.addState(newState);
            stateMap.put(state, newState);
        }
        
        // Set start state
        if (dfa.getStartState() != null) {
            copy.setStartState(stateMap.get(dfa.getStartState()));
        }
        
        // Set accept states
        for (DFA.State state : dfa.getAcceptStates()) {
            copy.addAcceptState(stateMap.get(state));
        }
        
        // Copy transitions
        for (Map.Entry<DFA.StateTransition, DFA.State> entry : dfa.getTransitions().entrySet()) {
            DFA.StateTransition transition = entry.getKey();
            DFA.State targetState = entry.getValue();
            
            copy.addTransition(
                stateMap.get(transition.getState()),
                transition.getSymbol(),
                stateMap.get(targetState)
            );
        }
        
        return copy;
    }

    /**
     * Find states that lead to target states on a given input symbol.
     */
    private Set<DFA.State> getStatesThatLeadTo(DFA dfa, Set<DFA.State> targetStates, char symbol) {
        Set<DFA.State> result = new HashSet<>();
        
        for (DFA.State state : dfa.getStates()) {
            DFA.State target = dfa.getTransitionTarget(state, symbol);
            if (target != null && targetStates.contains(target)) {
                result.add(state);
            }
        }
        
        return result;
    }

    /**
     * Create a new minimized DFA from the original DFA and partitions of equivalent states.
     */
    private DFA createMinimizedDfa(DFA originalDfa, List<Set<DFA.State>> partitions) {
        DFA minimizedDfa = new DFA();
        
        // Map from original state to representative state in minimized DFA
        Map<DFA.State, DFA.State> stateMap = new HashMap<>();
        
        // Create new states and build the mapping
        int stateCounter = 0;
        for (Set<DFA.State> partition : partitions) {
            if (partition.isEmpty()) continue;
            
            // Use one state as the representative for this partition
            // (arbitrary choice within the partition)
            DFA.State representativeState = new DFA.State("q" + stateCounter++);
            minimizedDfa.addState(representativeState);
            
            // Map all original states in this partition to the new representative state
            for (DFA.State originalState : partition) {
                stateMap.put(originalState, representativeState);
            }
            
            // If the partition contains the start state, set the representative as the new start state
            if (originalDfa.getStartState() != null && partition.contains(originalDfa.getStartState())) {
                minimizedDfa.setStartState(representativeState);
            }
            
            // If the partition contains any accept state, make the representative an accept state
            for (DFA.State originalState : partition) {
                if (originalDfa.getAcceptStates().contains(originalState)) {
                    minimizedDfa.addAcceptState(representativeState);
                    break;
                }
            }
        }
        
        // Create transitions in the minimized DFA
        for (Set<DFA.State> partition : partitions) {
            if (partition.isEmpty()) continue;
            
            // Take any state from the partition (they all have the same transitions)
            DFA.State anyOriginalState = partition.iterator().next();
            DFA.State fromState = stateMap.get(anyOriginalState);
            
            // For each input symbol
            for (char symbol : originalDfa.getAlphabet()) {
                DFA.State originalTarget = originalDfa.getTransitionTarget(anyOriginalState, symbol);
                
                // Skip if there's no transition
                if (originalTarget == null) {
                    continue;
                }
                
                // Map to the representative target state
                DFA.State toState = stateMap.get(originalTarget);
                
                // Add the transition
                minimizedDfa.addTransition(fromState, symbol, toState);
            }
        }
        
        return minimizedDfa;
    }
} 