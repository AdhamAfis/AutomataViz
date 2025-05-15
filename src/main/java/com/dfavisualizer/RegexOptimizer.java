package com.dfavisualizer;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optimizes regular expressions by recognizing common patterns
 * and providing direct DFA construction for them.
 */
public class RegexOptimizer {
    
    // Common regex pattern types that can be optimized
    private static final Pattern LITERAL_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern EXACT_STRING_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern STARTS_WITH_PATTERN = Pattern.compile("^\\^([a-zA-Z0-9]+).*$");
    private static final Pattern ENDS_WITH_PATTERN = Pattern.compile("^.*([a-zA-Z0-9]+)\\$$");
    private static final Pattern SIMPLE_ALTERNATION_PATTERN = Pattern.compile("^([a-zA-Z0-9]+)\\|([a-zA-Z0-9]+)$");
    private static final Pattern SIMPLE_STAR_PATTERN = Pattern.compile("^([a-zA-Z0-9])\\*$");
    private static final Pattern CHARACTER_CLASS_PATTERN = Pattern.compile("^\\[([a-zA-Z0-9-]+)\\]$");
    
    /**
     * Analyzes a regex pattern to determine if it can be directly converted to a DFA
     * without going through the NFA construction process.
     * 
     * @param regex The regular expression to analyze
     * @return true if the regex can be directly converted to a DFA
     */
    public boolean canDirectlyConvertToDfa(String regex) {
        return isLiteral(regex) || 
               isExactString(regex) || 
               isStartsWith(regex) || 
               isEndsWith(regex) || 
               isSimpleAlternation(regex) || 
               isSimpleStar(regex) ||
               isSimpleCharacterClass(regex);
    }
    
    /**
     * Directly constructs a DFA for optimized regex patterns.
     * This bypasses the NFA construction and subset construction for specific patterns.
     * 
     * @param regex The regular expression to convert
     * @return A DFA that recognizes the language defined by the regex
     * @throws IllegalArgumentException if the regex can't be directly converted
     */
    public DFA constructDfaDirectly(String regex) {
        if (isLiteral(regex)) {
            return constructLiteralDfa(regex);
        } else if (isExactString(regex)) {
            return constructExactStringDfa(regex);
        } else if (isStartsWith(regex)) {
            return constructStartsWithDfa(extractStartsWithPrefix(regex));
        } else if (isEndsWith(regex)) {
            return constructEndsWithDfa(extractEndsWithSuffix(regex));
        } else if (isSimpleAlternation(regex)) {
            String[] alternatives = extractAlternatives(regex);
            return constructAlternationDfa(alternatives[0], alternatives[1]);
        } else if (isSimpleStar(regex)) {
            return constructStarDfa(extractStarCharacter(regex));
        } else if (isSimpleCharacterClass(regex)) {
            Set<Character> chars = extractCharacterClass(regex);
            return constructCharacterClassDfa(chars);
        }
        
        throw new IllegalArgumentException("Regex pattern cannot be directly converted to DFA: " + regex);
    }
    
    /**
     * Checks if the regex is a simple literal (sequence of characters).
     */
    private boolean isLiteral(String regex) {
        return LITERAL_PATTERN.matcher(regex).matches();
    }
    
    /**
     * Checks if the regex is an exact string match (beginning to end).
     */
    private boolean isExactString(String regex) {
        return EXACT_STRING_PATTERN.matcher(regex).matches();
    }
    
    /**
     * Checks if the regex is a "starts with" pattern (^abc).
     */
    private boolean isStartsWith(String regex) {
        return STARTS_WITH_PATTERN.matcher(regex).matches();
    }
    
    /**
     * Extracts the prefix from a "starts with" pattern.
     */
    private String extractStartsWithPrefix(String regex) {
        Matcher matcher = STARTS_WITH_PATTERN.matcher(regex);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Checks if the regex is an "ends with" pattern (abc$).
     */
    private boolean isEndsWith(String regex) {
        return ENDS_WITH_PATTERN.matcher(regex).matches();
    }
    
    /**
     * Extracts the suffix from an "ends with" pattern.
     */
    private String extractEndsWithSuffix(String regex) {
        Matcher matcher = ENDS_WITH_PATTERN.matcher(regex);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Checks if the regex is a simple alternation (a|b).
     */
    private boolean isSimpleAlternation(String regex) {
        return SIMPLE_ALTERNATION_PATTERN.matcher(regex).matches();
    }
    
    /**
     * Extracts the alternatives from an alternation pattern.
     */
    private String[] extractAlternatives(String regex) {
        Matcher matcher = SIMPLE_ALTERNATION_PATTERN.matcher(regex);
        if (matcher.matches()) {
            return new String[] { matcher.group(1), matcher.group(2) };
        }
        return null;
    }
    
    /**
     * Checks if the regex is a simple Kleene star (a*).
     */
    private boolean isSimpleStar(String regex) {
        return SIMPLE_STAR_PATTERN.matcher(regex).matches();
    }
    
    /**
     * Extracts the character from a Kleene star pattern.
     */
    private char extractStarCharacter(String regex) {
        Matcher matcher = SIMPLE_STAR_PATTERN.matcher(regex);
        if (matcher.matches()) {
            return matcher.group(1).charAt(0);
        }
        return 0;
    }
    
    /**
     * Checks if the regex is a simple character class [a-z].
     */
    private boolean isSimpleCharacterClass(String regex) {
        return CHARACTER_CLASS_PATTERN.matcher(regex).matches();
    }
    
    /**
     * Extracts the characters from a character class pattern.
     */
    private Set<Character> extractCharacterClass(String regex) {
        Set<Character> chars = new HashSet<>();
        Matcher matcher = CHARACTER_CLASS_PATTERN.matcher(regex);
        
        if (matcher.matches()) {
            String content = matcher.group(1);
            for (int i = 0; i < content.length(); i++) {
                if (i + 2 < content.length() && content.charAt(i + 1) == '-') {
                    // Handle range (e.g., a-z)
                    char start = content.charAt(i);
                    char end = content.charAt(i + 2);
                    for (char c = start; c <= end; c++) {
                        chars.add(c);
                    }
                    i += 2; // Skip the range
                } else {
                    // Handle single character
                    chars.add(content.charAt(i));
                }
            }
        }
        
        return chars;
    }
    
    /**
     * Constructs a DFA for a literal pattern (sequence of characters).
     */
    private DFA constructLiteralDfa(String literal) {
        DFA dfa = new DFA();
        
        // Create states (one for each step in the literal, plus final state)
        DFA.State[] states = new DFA.State[literal.length() + 1];
        for (int i = 0; i <= literal.length(); i++) {
            states[i] = new DFA.State("q" + i);
            dfa.addState(states[i]);
        }
        
        // Set start state
        dfa.setStartState(states[0]);
        
        // Add transitions for each character in the literal
        for (int i = 0; i < literal.length(); i++) {
            dfa.addTransition(states[i], literal.charAt(i), states[i + 1]);
        }
        
        // Mark the final state as accepting
        dfa.addAcceptState(states[literal.length()]);
        
        return dfa;
    }
    
    /**
     * Constructs a DFA for an exact string pattern.
     */
    private DFA constructExactStringDfa(String str) {
        return constructLiteralDfa(str);
    }
    
    /**
     * Constructs a DFA for a "starts with" pattern.
     */
    private DFA constructStartsWithDfa(String prefix) {
        DFA dfa = new DFA();
        
        // Create states (one for each step in the prefix, plus a "rest" state)
        DFA.State[] states = new DFA.State[prefix.length() + 1];
        for (int i = 0; i <= prefix.length(); i++) {
            states[i] = new DFA.State("q" + i);
            dfa.addState(states[i]);
        }
        
        // Set start state
        dfa.setStartState(states[0]);
        
        // Add transitions for each character in the prefix
        for (int i = 0; i < prefix.length(); i++) {
            dfa.addTransition(states[i], prefix.charAt(i), states[i + 1]);
        }
        
        // Mark the final prefix state as accepting
        dfa.addAcceptState(states[prefix.length()]);
        
        // Add self-loop transitions for the final state to accept any character after the prefix
        for (char c = 'a'; c <= 'z'; c++) {
            dfa.addTransition(states[prefix.length()], c, states[prefix.length()]);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            dfa.addTransition(states[prefix.length()], c, states[prefix.length()]);
        }
        for (char c = '0'; c <= '9'; c++) {
            dfa.addTransition(states[prefix.length()], c, states[prefix.length()]);
        }
        
        return dfa;
    }
    
    /**
     * Constructs a DFA for an "ends with" pattern.
     */
    private DFA constructEndsWithDfa(String suffix) {
        DFA dfa = new DFA();
        
        // Create states (one for each step in the suffix, plus start state)
        DFA.State[] states = new DFA.State[suffix.length() + 1];
        for (int i = 0; i <= suffix.length(); i++) {
            states[i] = new DFA.State("q" + i);
            dfa.addState(states[i]);
        }
        
        // Set start state
        dfa.setStartState(states[0]);
        
        // The start state can loop back to itself for any character
        for (char c = 'a'; c <= 'z'; c++) {
            dfa.addTransition(states[0], c, states[0]);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            dfa.addTransition(states[0], c, states[0]);
        }
        for (char c = '0'; c <= '9'; c++) {
            dfa.addTransition(states[0], c, states[0]);
        }
        
        // Add transitions for each character in the suffix
        dfa.addTransition(states[0], suffix.charAt(0), states[1]);
        for (int i = 1; i < suffix.length(); i++) {
            dfa.addTransition(states[i], suffix.charAt(i), states[i + 1]);
        }
        
        // Mark the final state as accepting
        dfa.addAcceptState(states[suffix.length()]);
        
        return dfa;
    }
    
    /**
     * Constructs a DFA for a simple alternation pattern (a|b).
     */
    private DFA constructAlternationDfa(String alt1, String alt2) {
        DFA dfa = new DFA();
        
        // Create start state and final state
        DFA.State startState = new DFA.State("q0");
        DFA.State acceptState = new DFA.State("qAccept");
        
        dfa.addState(startState);
        dfa.addState(acceptState);
        
        dfa.setStartState(startState);
        dfa.addAcceptState(acceptState);
        
        // If the alternation is just single characters
        if (alt1.length() == 1 && alt2.length() == 1) {
            dfa.addTransition(startState, alt1.charAt(0), acceptState);
            dfa.addTransition(startState, alt2.charAt(0), acceptState);
        } else {
            // For longer alternatives, we need intermediate states
            DFA dfa1 = constructLiteralDfa(alt1);
            DFA dfa2 = constructLiteralDfa(alt2);
            
            // Merge the two DFAs (simplified for brevity)
            // In a real implementation, you would need to properly merge the states and transitions
            
            // Note: This is just a simplified example; handling arbitrary alternation
            // properly would require more complex state merging
            return null;
        }
        
        return dfa;
    }
    
    /**
     * Constructs a DFA for a simple Kleene star pattern (a*).
     */
    private DFA constructStarDfa(char c) {
        DFA dfa = new DFA();
        
        // Create single state that is both start and accept
        DFA.State state = new DFA.State("q0");
        dfa.addState(state);
        dfa.setStartState(state);
        dfa.addAcceptState(state);
        
        // Add self-loop for the character
        dfa.addTransition(state, c, state);
        
        return dfa;
    }
    
    /**
     * Constructs a DFA for a character class [a-z].
     */
    private DFA constructCharacterClassDfa(Set<Character> chars) {
        DFA dfa = new DFA();
        
        // Create start and accept states
        DFA.State startState = new DFA.State("q0");
        DFA.State acceptState = new DFA.State("q1");
        
        dfa.addState(startState);
        dfa.addState(acceptState);
        
        dfa.setStartState(startState);
        dfa.addAcceptState(acceptState);
        
        // Add transitions for each character in the class
        for (char c : chars) {
            dfa.addTransition(startState, c, acceptState);
        }
        
        return dfa;
    }
} 