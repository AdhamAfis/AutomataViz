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
        OptimizedNFA optNfa = parseToOptimizedNfa(processedRegex);
        
        // Convert OptimizedNFA to conventional NFA for compatibility with SubsetConstruction
        NFA nfa = optNfa.toConventionalNFA();
        
        // Convert NFA to DFA using subset construction
        SubsetConstruction subsetConstruction = new SubsetConstruction();
        return subsetConstruction.convertNfaToDfa(nfa);
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
        
        return parseExpression();
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
        OptimizedNFA term = parseTerm();
        
        while (position < regex.length() && regex.charAt(position) == '|') {
            position++;  // Skip '|'
            OptimizedNFA nextTerm = parseTerm();
            term = term.union(nextTerm);
        }
        
        return term;
    }
    
    /**
     * Parses a term, which is one or more factors that are concatenated.
     */
    private OptimizedNFA parseTerm() {
        OptimizedNFA factor = parseFactor();
        
        while (position < regex.length() &&
               regex.charAt(position) != '|' &&
               regex.charAt(position) != ')') {
            
            OptimizedNFA nextFactor = parseFactor();
            factor = factor.concatenate(nextFactor);
        }
        
        return factor;
    }
    
    /**
     * Parses a factor, which is an atom followed by an optional operator (*, +, ?).
     */
    private OptimizedNFA parseFactor() {
        OptimizedNFA atom = parseAtom();
        
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
                
                // Create a new start and accept state
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
    }
    
    /**
     * Parses an atom, which is a character, a group in parentheses, or a character class.
     */
    private OptimizedNFA parseAtom() {
        if (position >= regex.length()) {
            return OptimizedNFA.forEpsilon();
        }
        
        char c = regex.charAt(position);
        
        if (c == '(') {
            position++;  // Skip '('
            OptimizedNFA result = parseExpression();
            
            if (position < regex.length() && regex.charAt(position) == ')') {
                position++;  // Skip ')'
            } else {
                throw new IllegalArgumentException("Missing closing parenthesis");
            }
            
            return result;
        } else if (c == '.') {
            // Handle dot (any character)
            position++;
            return parseDot();
        } else if (c == '[') {
            // Handle character class [...]
            position++;
            return parseCharacterClass();
        } else {
            // Handle single character
            position++;
            return OptimizedNFA.forSymbol(c);
        }
    }
    
    /**
     * Parses a dot (any character) by creating an NFA that matches any character.
     * This uses an optimized approach to avoid creating too many states.
     */
    private OptimizedNFA parseDot() {
        OptimizedNFA result = new OptimizedNFA();
        
        // Create just one start and one accept state
        int startState = result.createState();
        int acceptState = result.createState();
        
        result.setStartState(startState);
        result.addAcceptState(acceptState);
        
        // Add transitions for a limited alphabet
        // This is more efficient than creating a separate state for each character
        for (char c = 'a'; c <= 'z'; c++) {
            result.addTransition(startState, c, acceptState);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            result.addTransition(startState, c, acceptState);
        }
        for (char c = '0'; c <= '9'; c++) {
            result.addTransition(startState, c, acceptState);
        }
        
        // Add some common special characters
        char[] specialChars = {'_', '-', '+', '*', '/', '=', '<', '>', '!', '@', '#', '$', '%', '^', '&', ' '};
        for (char c : specialChars) {
            result.addTransition(startState, c, acceptState);
        }
        
        return result;
    }
    
    /**
     * Parses a character class [...] by creating an NFA that matches any character
     * in the class. Uses an optimized approach with fewer states.
     */
    private OptimizedNFA parseCharacterClass() {
        // Create an NFA with a single start and accept state
        OptimizedNFA result = new OptimizedNFA();
        int startState = result.createState();
        int acceptState = result.createState();
        
        result.setStartState(startState);
        result.addAcceptState(acceptState);
        
        boolean negate = false;
        
        // Check for negation
        if (position < regex.length() && regex.charAt(position) == '^') {
            negate = true;
            position++;
        }
        
        // Build the set of characters in this class
        Set<Character> charSet = new HashSet<>();
        
        // Parse characters in the class
        while (position < regex.length() && regex.charAt(position) != ']') {
            char c = regex.charAt(position++);
            
            // Handle ranges like a-z
            if (position + 1 < regex.length() && regex.charAt(position) == '-' && regex.charAt(position + 1) != ']') {
                position++;  // Skip '-'
                char end = regex.charAt(position++);
                
                // Add all characters in the range
                for (char rangeChar = c; rangeChar <= end; rangeChar++) {
                    charSet.add(rangeChar);
                }
            } else {
                // Add single character
                charSet.add(c);
            }
        }
        
        // Skip closing ']'
        if (position < regex.length() && regex.charAt(position) == ']') {
            position++;
        } else {
            throw new IllegalArgumentException("Missing closing bracket in character class");
        }
        
        // If negated, invert the character set
        if (negate) {
            Set<Character> allChars = new HashSet<>();
            
            // Add standard alphabet characters
            for (char ch = 'a'; ch <= 'z'; ch++) allChars.add(ch);
            for (char ch = 'A'; ch <= 'Z'; ch++) allChars.add(ch);
            for (char ch = '0'; ch <= '9'; ch++) allChars.add(ch);
            
            // Add common special characters
            char[] specialChars = {'_', '-', '+', '*', '/', '=', '<', '>', '!', '@', '#', '$', '%', '^', '&', ' '};
            for (char ch : specialChars) allChars.add(ch);
            
            // Remove characters in the class
            allChars.removeAll(charSet);
            charSet = allChars;
        }
        
        // Add transitions for all characters in the set
        for (char c : charSet) {
            result.addTransition(startState, c, acceptState);
        }
        
        return result;
    }
} 