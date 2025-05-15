package com.dfavisualizer;

import java.util.regex.Pattern;

/**
 * Converts regular expressions to DFAs using a complete implementation of:
 * 1. Thompson's construction (regex -> NFA)
 * 2. Subset Construction (NFA -> DFA)
 * 3. Hopcroft's algorithm (DFA minimization)
 */
public class RegexToDfaConverter {

    private final RegexParser parser;
    private final SubsetConstruction subsetConstruction;
    private final DfaMinimizer minimizer;
    
    private NFA lastNfa; // Store the last created NFA
    private DFA lastDfa; // Store the non-minimized DFA

    public RegexToDfaConverter() {
        this.parser = new RegexParser();
        this.subsetConstruction = new SubsetConstruction();
        this.minimizer = new DfaMinimizer();
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
            // Step 1: Parse the regex and build an NFA using Thompson's construction
            lastNfa = parser.parse(regex);
            
            // Step 2: Convert the NFA to a DFA using subset construction
            lastDfa = subsetConstruction.convertNfaToDfa(lastNfa);
            
            // Step 3: Minimize the DFA using Hopcroft's algorithm
            DFA minimizedDfa = minimizer.minimize(lastDfa);
            
            return minimizedDfa;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting regex to DFA: " + e.getMessage(), e);
        }
    }
} 