package com.dfavisualizer.algorithm;

import java.util.Map;
import java.util.regex.Pattern;
import com.dfavisualizer.model.DFA;
import com.dfavisualizer.model.NFA;
import com.dfavisualizer.model.OptimizedNFA;

public class RegexToDfaConverter {

    private final RegexParser parser;
    private final OptimizedRegexParser optimizedParser;
    private final SubsetConstruction subsetConstruction;
    private final DfaMinimizer minimizer;
    private final RegexOptimizer optimizer;
    
    private NFA lastNfa; // Store the last created NFA
    private DFA lastDfa; // Store the non-minimized DFA
    
    // Keep the main graph and learning walkthrough on the same Thompson construction.
    private boolean useOptimizedImplementation = false;

    public RegexToDfaConverter() {
        this.parser = new RegexParser();
        this.optimizedParser = new OptimizedRegexParser();
        this.subsetConstruction = new SubsetConstruction();
        this.minimizer = new DfaMinimizer();
        this.optimizer = new RegexOptimizer();
    }
    
    public void setUseOptimizedImplementation(boolean useOptimized) {
        this.useOptimizedImplementation = useOptimized;
    }

    /**
     * Gets the last NFA created during conversion
     * 
     * @return The last NFA created, or null if none exists
     */
    public NFA getLastNfa() {
        return lastNfa;
    }
    
    /**
     * Gets the last non-minimized DFA created during conversion
     * 
     * @return The last non-minimized DFA created, or null if none exists
     */
    public DFA getLastDfa() {
        return lastDfa;
    }

    /**
     * Converts a regular expression to a DFA using Thompson's construction,
     * the subset construction algorithm, and DFA minimization.
     * 
     * @param regex The regular expression to convert
     * @return A minimal DFA that accepts the language defined by the regex
     * @throws IllegalArgumentException if the regex is invalid
     */
    public DFA convertRegexToDfa(String regex) {
        if (regex == null || regex.isEmpty()) {
            throw new IllegalArgumentException("Regular expression cannot be null or empty.");
        }

        try {
            Pattern.compile(regex);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid regular expression: " + e.getMessage());
        }

        try {
            if (useOptimizedImplementation) {
                return convertRegexToDfaOptimized(regex);
            } else {
                return convertRegexToDfaOriginal(regex);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting regex to DFA: " + e.getMessage(), e);
        }
    }
    
    private DFA convertRegexToDfaOriginal(String regex) {
        lastNfa = parser.parse(regex);
        
        lastDfa = subsetConstruction.convertNfaToDfa(lastNfa);
        
        DFA minimizedDfa = minimizer.minimize(lastDfa);
        
        return minimizedDfa;
    }
    
    private DFA convertRegexToDfaOptimized(String regex) {
        if (optimizer.canDirectlyConvertToDfa(regex)) {
            try {
                DFA directDfa = optimizer.constructDfaDirectly(regex);
                
                // For intermediate visualization, create a simple NFA and DFA
                // that corresponds to the direct DFA (simplified representation)
                lastNfa = createSimpleNfaFromDfa(directDfa);
                lastDfa = directDfa;
                
                return minimizer.minimize(directDfa);
            } catch (Exception e) {
                // If direct construction fails, fall back to the optimized NFA approach
                System.out.println("Direct DFA construction failed, falling back to NFA: " + e.getMessage());
            }
        }
        
        try {
            DFA optimizedDfa = optimizedParser.parseToDfa(regex);
            
            // For intermediate visualization, we need to create a conventional NFA
            // We'll create a simple NFA that approximates the optimized one
            OptimizedNFA optNfa = optimizedParser.parseToOptimizedNfa(regex);
            lastNfa = optNfa.toConventionalNFA();
            lastDfa = optimizedDfa;
            
            return minimizer.minimize(optimizedDfa);
        } catch (Exception e) {
            // If optimized parsing fails, fall back to original implementation
            System.out.println("Optimized parsing failed, falling back to original: " + e.getMessage());
            return convertRegexToDfaOriginal(regex);
        }
    }
    
    /**
     * Creates a simple NFA representation from a DFA for visualization purposes.
     * This is used when direct DFA construction bypasses the NFA creation.
     */
    private NFA createSimpleNfaFromDfa(DFA dfa) {
        NFA nfa = new NFA();
        
        for (DFA.State dfaState : dfa.getStates()) {
            int nfaState = Integer.parseInt(dfaState.getName().substring(1));
            
            while (nfa.getStates().size() <= nfaState) {
                nfa.createState();
            }
            
            if (dfaState.equals(dfa.getStartState())) {
                nfa.setStartState(nfaState);
            }
            
            if (dfa.getAcceptStates().contains(dfaState)) {
                nfa.addAcceptState(nfaState);
            }
        }
        
        for (Map.Entry<DFA.StateTransition, DFA.State> entry : dfa.getTransitions().entrySet()) {
            DFA.StateTransition transition = entry.getKey();
            DFA.State targetState = entry.getValue();
            
            int fromState = Integer.parseInt(transition.getState().getName().substring(1));
            int toState = Integer.parseInt(targetState.getName().substring(1));
            char symbol = transition.getSymbol();
            
            nfa.addTransition(fromState, symbol, toState);
        }
        
        return nfa;
    }

    /**
     * Converts a regular expression to a DFA without applying minimization.
     * This is useful for visualizing the intermediate DFA before minimization.
     * 
     * @param regex The regular expression to convert
     * @return A non-minimized DFA that accepts the language defined by the regex
     * @throws IllegalArgumentException if the regex is invalid
     */
    public DFA convertRegexToDfaWithoutMinimization(String regex) {
        if (regex == null || regex.isEmpty()) {
            throw new IllegalArgumentException("Regular expression cannot be null or empty.");
        }

        try {
            Pattern.compile(regex);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid regular expression: " + e.getMessage());
        }

        try {
            if (useOptimizedImplementation) {
                try {
                    DFA optimizedDfa = optimizedParser.parseToDfa(regex);
                    
                    OptimizedNFA optNfa = optimizedParser.parseToOptimizedNfa(regex);
                    lastNfa = optNfa.toConventionalNFA();
                    lastDfa = optimizedDfa;
                    
                    return optimizedDfa;
                } catch (Exception e) {
                    // If optimized parsing fails, fall back to original implementation
                    System.out.println("Optimized parsing failed, falling back to original: " + e.getMessage());
                    return convertRegexToDfaOriginalWithoutMinimization(regex);
                }
            } else {
                return convertRegexToDfaOriginalWithoutMinimization(regex);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting regex to DFA: " + e.getMessage(), e);
        }
    }

    private DFA convertRegexToDfaOriginalWithoutMinimization(String regex) {
        lastNfa = parser.parse(regex);
        
        lastDfa = subsetConstruction.convertNfaToDfa(lastNfa);
        
        return lastDfa;
    }
} 