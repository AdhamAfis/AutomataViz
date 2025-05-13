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

    public RegexToDfaConverter() {
        this.parser = new RegexParser();
        this.subsetConstruction = new SubsetConstruction();
        this.minimizer = new DfaMinimizer();
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
            NFA nfa = parser.parse(regex);
            
            // Step 2: Convert the NFA to a DFA using subset construction
            DFA dfa = subsetConstruction.convertNfaToDfa(nfa);
            
            // Step 3: Minimize the DFA using Hopcroft's algorithm
            DFA minimizedDfa = minimizer.minimize(dfa);
            
            return minimizedDfa;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting regex to DFA: " + e.getMessage(), e);
        }
    }
} 