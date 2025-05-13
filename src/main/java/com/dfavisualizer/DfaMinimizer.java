package com.dfavisualizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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
        // Get all states and alphabet
        Set<DFA.State> allStates = dfa.getStates();
        Set<Character> alphabet = dfa.getAlphabet();
        
        // Group states into accepting and non-accepting sets (initial partition)
        Set<DFA.State> acceptStates = dfa.getAcceptStates();
        Set<DFA.State> nonAcceptStates = new HashSet<>(allStates);
        nonAcceptStates.removeAll(acceptStates);
        
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
            // Use one state as the representative for this partition
            // (arbitrary choice within the partition)
            DFA.State representativeState = new DFA.State("q" + stateCounter++);
            minimizedDfa.addState(representativeState);
            
            // Map all original states in this partition to the new representative state
            for (DFA.State originalState : partition) {
                stateMap.put(originalState, representativeState);
            }
            
            // If the partition contains the start state, set the representative as the new start state
            if (partition.contains(originalDfa.getStartState())) {
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