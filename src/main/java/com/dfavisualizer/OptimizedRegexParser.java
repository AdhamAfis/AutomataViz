package com.dfavisualizer;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
 * An optimized parser for regular expressions that incorporates:
 * 1. More efficient NFA construction using OptimizedNFA
 * 2. Direct DFA construction for certain pattern types
 * 3. Memory-efficient algorithms for large regex patterns
 */
public class OptimizedRegexParser {
    private String regex;
    private int position;
    private RegexOptimizer optimizer;
    
    /**
     * Creates a new optimized regex parser.
     */
    public OptimizedRegexParser() {
        this.optimizer = new RegexOptimizer();
    }

    /**
     * Parses a regular expression and returns the corresponding DFA directly.
     * This method tries to optimize by:
     * 1. Checking if direct DFA construction is possible
     * 2. Using optimized NFA construction if direct construction is not possible
     * 3. Minimizing memory usage throughout the process
     * 
     * @param regex The regular expression to parse
     * @return A DFA that recognizes the language defined by the regex
     */
    public DFA parseToDfa(String regex) {
        if (regex == null || regex.isEmpty()) {
            // Create a simple DFA that accepts only the empty string
            DFA dfa = new DFA();
            DFA.State start = new DFA.State("q0");
            dfa.addState(start);
            dfa.setStartState(start);
            dfa.addAcceptState(start);
            return dfa;
        }
        
        // Preprocess the regex
        String processedRegex = preprocessRegex(regex);
        
        // Try direct DFA construction for recognized patterns
        if (optimizer.canDirectlyConvertToDfa(processedRegex)) {
            try {
                return optimizer.constructDfaDirectly(processedRegex);
            } catch (Exception e) {
                // If direct construction fails, fall back to NFA-based approach
                System.out.println("Direct DFA construction failed, falling back to NFA: " + e.getMessage());
            }
        }
        
        // Use optimized NFA construction
        try {
            OptimizedNFA optNfa = parseToOptimizedNfa(processedRegex);
            
            // Convert OptimizedNFA to conventional NFA for compatibility with SubsetConstruction
            NFA nfa = optNfa.toConventionalNFA();
            
            // Convert NFA to DFA using subset construction
            SubsetConstruction subsetConstruction = new SubsetConstruction();
            return subsetConstruction.convertNfaToDfa(nfa);
        } catch (Exception e) {
            // Print error and rethrow
            System.err.println("Error parsing regex: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Parses a regular expression and returns the corresponding optimized NFA.
     * 
     * @param regex The regular expression to parse
     * @return An OptimizedNFA that recognizes the language defined by the regex
     */
    public OptimizedNFA parseToOptimizedNfa(String regex) {
        if (regex == null || regex.isEmpty()) {
            return OptimizedNFA.forEpsilon();
        }
        
        this.regex = preprocessRegex(regex);
        this.position = 0;
        
        try {
            return parseExpression();
        } catch (Exception e) {
            // Print error and rethrow
            System.err.println("Error parsing regex: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Preprocess the regex to handle escape sequences and other special cases.
     */
    private String preprocessRegex(String regex) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < regex.length(); i++) {
            char current = regex.charAt(i);
            
            // Handle escape sequences
            if (current == '\\' && i + 1 < regex.length()) {
                result.append(regex.charAt(++i));
                continue;
            }
            
            result.append(current);
        }
        
        return result.toString();
    }
    
    /**
     * Parses an expression, which is one or more terms separated by '|'.
     */
    private OptimizedNFA parseExpression() {
        OptimizedNFA term = null;
        try {
            term = parseTerm();
            
            while (position < regex.length() && regex.charAt(position) == '|') {
                position++;  // Skip '|'
                OptimizedNFA nextTerm = parseTerm();
                term = term.union(nextTerm);
            }
            
            return term;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing expression at position " + position + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses a term, which is one or more factors that are concatenated.
     */
    private OptimizedNFA parseTerm() {
        OptimizedNFA factor = null;
        try {
            factor = parseFactor();
            
            while (position < regex.length() &&
                   regex.charAt(position) != '|' &&
                   regex.charAt(position) != ')') {
                
                OptimizedNFA nextFactor = parseFactor();
                factor = factor.concatenate(nextFactor);
            }
            
            return factor;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing term at position " + position + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses a factor, which is an atom followed by an optional operator (*, +, ?).
     */
    private OptimizedNFA parseFactor() {
        OptimizedNFA atom = null;
        try {
            atom = parseAtom();
            
            if (position < regex.length()) {
                char operator = regex.charAt(position);
                
                if (operator == '*') {
                    position++;  // Skip '*'
                    atom = atom.kleeneStar();
                } else if (operator == '+') {
                    position++;  // Skip '+'
                    
                    // a+ = aa*
                    OptimizedNFA copy = atom.concatenate(atom.kleeneStar());
                    atom = copy;
                } else if (operator == '?') {
                    position++;  // Skip '?'
                    
                    // Create a new NFA for a?
                    OptimizedNFA result = new OptimizedNFA();
                    int newStart = result.createState();
                    int newAccept = result.createState();
                    
                    result.setStartState(newStart);
                    result.addAcceptState(newAccept);
                    
                    // Connect new start to atom's start
                    result.addEpsilonTransition(newStart, atom.getStartState());
                    
                    // Connect new start to new accept (empty string case)
                    result.addEpsilonTransition(newStart, newAccept);
                    
                    // Connect atom's accept states to new accept
                    BitSet atomAccepts = atom.getAcceptStates();
                    for (int state = atomAccepts.nextSetBit(0); state >= 0; state = atomAccepts.nextSetBit(state + 1)) {
                        result.addEpsilonTransition(state, newAccept);
                    }
                    
                    atom = result;
                }
            }
            
            return atom;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing factor at position " + position + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses an atom, which is a character, a group in parentheses, or a character class.
     */
    private OptimizedNFA parseAtom() {
        if (position >= regex.length()) {
            return OptimizedNFA.forEpsilon();
        }
        
        char c = regex.charAt(position);
        
        try {
            if (c == '(') {
                position++;  // Skip '('
                OptimizedNFA result = parseExpression();
                
                if (position >= regex.length() || regex.charAt(position) != ')') {
                    throw new IllegalArgumentException("Expected ')' at position " + position);
                }
                
                position++;  // Skip ')'
                return result;
            } else if (c == '.') {
                position++;  // Skip '.'
                return parseDot();
            } else if (c == '[') {
                position++; // Skip '['
                return parseCharacterClass();
            } else {
                position++;  // Skip the character
                return OptimizedNFA.forSymbol(c);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing atom at position " + position + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses the dot operator (.) which matches any character.
     */
    private OptimizedNFA parseDot() {
        OptimizedNFA result = new OptimizedNFA();
        int start = result.createState();
        int accept = result.createState();
        
        result.setStartState(start);
        result.addAcceptState(accept);
        
        // Add transitions for all ASCII printable characters
        for (char c = 32; c < 127; c++) {
            result.addTransition(start, c, accept);
        }
        
        return result;
    }
    
    /**
     * Parses a character class [...] into an NFA.
     */
    private OptimizedNFA parseCharacterClass() {
        OptimizedNFA result = new OptimizedNFA();
        int start = result.createState();
        int accept = result.createState();
        
        result.setStartState(start);
        result.addAcceptState(accept);
        
        boolean isNegated = false;
        if (position < regex.length() && regex.charAt(position) == '^') {
            isNegated = true;
            position++; // Skip '^'
        }
        
        Set<Character> includedChars = new HashSet<>();
        
        // Parse the character class content
        while (position < regex.length() && regex.charAt(position) != ']') {
            char current = regex.charAt(position++);
            
            // Handle character range (e.g., a-z)
            if (position + 1 < regex.length() && regex.charAt(position) == '-' && regex.charAt(position + 1) != ']') {
                position++; // Skip '-'
                char end = regex.charAt(position++);
                
                // Add all characters in the range
                for (char c = current; c <= end; c++) {
                    includedChars.add(c);
                }
            } else {
                // Add single character
                includedChars.add(current);
            }
        }
        
        if (position >= regex.length() || regex.charAt(position) != ']') {
            throw new IllegalArgumentException("Expected ']' at position " + position);
        }
        
        position++; // Skip ']'
        
        if (isNegated) {
            // For negated character class, add transitions for all characters not in the class
            for (char c = 32; c < 127; c++) {
                if (!includedChars.contains(c)) {
                    result.addTransition(start, c, accept);
                }
            }
        } else {
            // For normal character class, add transitions for all characters in the class
            for (char c : includedChars) {
                result.addTransition(start, c, accept);
            }
        }
        
        return result;
    }
} 
