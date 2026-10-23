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
    private final Map<DFA.State, Set<DFA.State>> stateGroups = new HashMap<>();

    /** Exact original-state membership of each result state from the most recent conversion. */
    public Map<DFA.State, Set<DFA.State>> getStateGroups() {
        return java.util.Collections.unmodifiableMap(stateGroups);
    }


    public DFA minimize(DFA dfa) {
        if (dfa == null) {
            throw new IllegalArgumentException("Cannot minimize null DFA");
        }
        
        stateGroups.clear();
        for (DFA.State state : dfa.getStates()) stateGroups.put(state, java.util.Collections.singleton(state));

        if (dfa.getStates().size() <= 1) {
            return copyDfa(dfa);
        }
        
        Set<DFA.State> allStates = dfa.getStates();
        Set<Character> alphabet = dfa.getAlphabet();
        
        if (alphabet.isEmpty()) {
            return copyDfa(dfa);
        }
        
        Set<DFA.State> acceptStates = dfa.getAcceptStates();
        Set<DFA.State> nonAcceptStates = new HashSet<>(allStates);
        nonAcceptStates.removeAll(acceptStates);
        
        if (acceptStates.isEmpty() || nonAcceptStates.isEmpty()) {
            return minimizeWithSingleInitialPartition(dfa, allStates, alphabet);
        }
        
        List<Set<DFA.State>> partitions = new ArrayList<>();
        if (!acceptStates.isEmpty()) {
            partitions.add(new HashSet<>(acceptStates));
        }
        if (!nonAcceptStates.isEmpty()) {
            partitions.add(new HashSet<>(nonAcceptStates));
        }
        
        Queue<Set<DFA.State>> queue = new LinkedList<>(partitions);
        
        while (!queue.isEmpty()) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            Set<DFA.State> setA = queue.poll();
            
            for (char symbol : alphabet) {
                Set<DFA.State> leadingToA = getStatesThatLeadTo(dfa, setA, symbol);
                
                List<Set<DFA.State>> newPartitions = new ArrayList<>();
                
                for (Set<DFA.State> partition : partitions) {
                    if (partition.equals(setA)) {
                        newPartitions.add(partition);
                        continue;
                    }
                    
                    Set<DFA.State> intersection = new HashSet<>(partition);
                    intersection.retainAll(leadingToA);
                    
                    Set<DFA.State> difference = new HashSet<>(partition);
                    difference.removeAll(leadingToA);
                    
                    if (!intersection.isEmpty() && !difference.isEmpty()) {
                        newPartitions.add(intersection);
                        newPartitions.add(difference);
                        
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
                        newPartitions.add(partition);
                    }
                }
                
                partitions = newPartitions;
            }
        }
        
        return createMinimizedDfa(dfa, partitions);
    }
    
    /**
     * Special case minimization when all states are accepting or all are non-accepting.
     * In this case, we need a different strategy for the initial partition.
     */
    private DFA minimizeWithSingleInitialPartition(DFA dfa, Set<DFA.State> allStates, Set<Character> alphabet) {
        List<Set<DFA.State>> partitions = new ArrayList<>();
        for (DFA.State state : allStates) {
            Set<DFA.State> partition = new HashSet<>();
            partition.add(state);
            partitions.add(partition);
        }
        
        boolean changed;
        do {
            changed = false;
            
            for (int i = 0; i < partitions.size(); i++) {
                if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
                for (int j = i + 1; j < partitions.size(); j++) {
                    if (areEquivalentPartitions(dfa, partitions.get(i), partitions.get(j), partitions, alphabet)) {
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
    
    private boolean areEquivalentPartitions(DFA dfa, Set<DFA.State> p1, Set<DFA.State> p2, 
                                           List<Set<DFA.State>> partitions, Set<Character> alphabet) {
        DFA.State s1 = p1.iterator().next();
        DFA.State s2 = p2.iterator().next();
        
        boolean s1Accepting = dfa.getAcceptStates().contains(s1);
        boolean s2Accepting = dfa.getAcceptStates().contains(s2);
        if (s1Accepting != s2Accepting) {
            return false;
        }
        
        for (char symbol : alphabet) {
            DFA.State target1 = dfa.getTransitionTarget(s1, symbol);
            DFA.State target2 = dfa.getTransitionTarget(s2, symbol);
            
            if ((target1 == null) != (target2 == null)) {
                return false;
            }
            
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
    
    private int findPartitionIndex(List<Set<DFA.State>> partitions, DFA.State state) {
        for (int i = 0; i < partitions.size(); i++) {
            if (partitions.get(i).contains(state)) {
                return i;
            }
        }
        return -1;
    }
    
    private DFA copyDfa(DFA dfa) {
        DFA copy = new DFA();
        
        Map<DFA.State, DFA.State> stateMap = new HashMap<>();
        for (DFA.State state : dfa.getStates()) {
            DFA.State newState = new DFA.State(state.getName());
            copy.addState(newState);
            stateMap.put(state, newState);
        }
        
        if (dfa.getStartState() != null) {
            copy.setStartState(stateMap.get(dfa.getStartState()));
        }
        
        for (DFA.State state : dfa.getAcceptStates()) {
            copy.addAcceptState(stateMap.get(state));
        }
        
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

    private DFA createMinimizedDfa(DFA originalDfa, List<Set<DFA.State>> partitions) {
        DFA minimizedDfa = new DFA();
        stateGroups.clear();
        
        Map<DFA.State, DFA.State> stateMap = new HashMap<>();
        
        int stateCounter = 0;
        for (Set<DFA.State> partition : partitions) {
            if (partition.isEmpty()) continue;
            
            DFA.State representativeState = new DFA.State("q" + stateCounter++);
            minimizedDfa.addState(representativeState);
            stateGroups.put(representativeState, java.util.Collections.unmodifiableSet(new HashSet<>(partition)));
            
            for (DFA.State originalState : partition) {
                stateMap.put(originalState, representativeState);
            }
            
            if (originalDfa.getStartState() != null && partition.contains(originalDfa.getStartState())) {
                minimizedDfa.setStartState(representativeState);
            }
            
            for (DFA.State originalState : partition) {
                if (originalDfa.getAcceptStates().contains(originalState)) {
                    minimizedDfa.addAcceptState(representativeState);
                    break;
                }
            }
        }
        
        for (Set<DFA.State> partition : partitions) {
            if (partition.isEmpty()) continue;
            
            // Take any state from the partition (they all have the same transitions)
            DFA.State anyOriginalState = partition.iterator().next();
            DFA.State fromState = stateMap.get(anyOriginalState);
            
            for (char symbol : originalDfa.getAlphabet()) {
                DFA.State originalTarget = originalDfa.getTransitionTarget(anyOriginalState, symbol);
                
                if (originalTarget == null) {
                    continue;
                }
                
                DFA.State toState = stateMap.get(originalTarget);
                
                minimizedDfa.addTransition(fromState, symbol, toState);
            }
        }
        
        return minimizedDfa;
    }
} 