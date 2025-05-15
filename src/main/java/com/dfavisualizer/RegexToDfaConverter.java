package com.dfavisualizer;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Converts regular expressions to DFAs using a complete implementation of:
 * 1. Thompson's construction (regex -> NFA)
 * 2. Subset Construction (NFA -> DFA)
 * 3. Hopcroft's algorithm (DFA minimization)
 * 
 * This implementation includes:
 * - Memory-efficient algorithms for large regex patterns
 * - Direct DFA construction for certain regex patterns
 * - Optimized NFA construction
 */
public class RegexToDfaConverter {

    private final RegexParser parser;
    private final OptimizedRegexParser optimizedParser;
    private final SubsetConstruction subsetConstruction;
    private final DfaMinimizer minimizer;
    private final RegexOptimizer optimizer;
    
    private NFA lastNfa; // Store the last created NFA
    private DFA lastDfa; // Store the non-minimized DFA
    
    // Flag to control whether to use optimized implementation
    private boolean useOptimizedImplementation = true;

    public RegexToDfaConverter() {
        this.parser = new RegexParser();
        this.optimizedParser = new OptimizedRegexParser();
        this.subsetConstruction = new SubsetConstruction();
        this.minimizer = new DfaMinimizer();
        this.optimizer = new RegexOptimizer();
    }
    
    /**
     * Sets whether to use the optimized implementation.
     * 
     * @param useOptimized true to use optimized algorithms, false to use original
     */
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

        // Validate regex syntax
        try {
            Pattern.compile(regex);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid regular expression: " + e.getMessage());
        }

        try {
            // Use optimized implementation if enabled
            if (useOptimizedImplementation) {
                return convertRegexToDfaOptimized(regex);
            } else {
                return convertRegexToDfaOriginal(regex);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting regex to DFA: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts a regular expression to a DFA using the original implementation.
     */
    private DFA convertRegexToDfaOriginal(String regex) {
        // Step 1: Parse the regex and build an NFA using Thompson's construction
        lastNfa = parser.parse(regex);
        
        // Step 2: Convert the NFA to a DFA using subset construction
        lastDfa = subsetConstruction.convertNfaToDfa(lastNfa);
        
        // Step 3: Minimize the DFA using Hopcroft's algorithm
        DFA minimizedDfa = minimizer.minimize(lastDfa);
        
        return minimizedDfa;
    }
    
    /**
     * Converts a regular expression to a DFA using the optimized implementation.
     */
    private DFA convertRegexToDfaOptimized(String regex) {
        // Check if we can directly convert to DFA
        if (optimizer.canDirectlyConvertToDfa(regex)) {
            try {
                // Direct construction without going through NFA
                DFA directDfa = optimizer.constructDfaDirectly(regex);
                
                // For intermediate visualization, create a simple NFA and DFA
                // that corresponds to the direct DFA (simplified representation)
                lastNfa = createSimpleNfaFromDfa(directDfa);
                lastDfa = directDfa;
                
                // Minimize the DFA
                return minimizer.minimize(directDfa);
            } catch (Exception e) {
                // If direct construction fails, fall back to the optimized NFA approach
                System.out.println("Direct DFA construction failed, falling back to NFA: " + e.getMessage());
            }
        }
        
        // Try using the optimized parser for complex regexes
        try {
            // Use optimized parsing that creates fewer states and transitions
            DFA optimizedDfa = optimizedParser.parseToDfa(regex);
            
            // For intermediate visualization, we need to create a conventional NFA
            // We'll create a simple NFA that approximates the optimized one
            OptimizedNFA optNfa = optimizedParser.parseToOptimizedNfa(regex);
            lastNfa = optNfa.toConventionalNFA();
            lastDfa = optimizedDfa;
            
            // Minimize the DFA and return
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
        
        // Create states for each DFA state
        for (DFA.State dfaState : dfa.getStates()) {
            int nfaState = Integer.parseInt(dfaState.getName().substring(1));
            
            // Ensure state exists in NFA
            while (nfa.getStates().size() <= nfaState) {
                nfa.createState();
            }
            
            // Mark start and accept states
            if (dfaState.equals(dfa.getStartState())) {
                nfa.setStartState(nfaState);
            }
            
            if (dfa.getAcceptStates().contains(dfaState)) {
                nfa.addAcceptState(nfaState);
            }
        }
        
        // Add NFA transitions for each DFA transition
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
} 