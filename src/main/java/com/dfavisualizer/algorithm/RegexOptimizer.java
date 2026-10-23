package com.dfavisualizer.algorithm;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dfavisualizer.model.DFA;

/**
 * Optimizes regular expressions by recognizing common patterns
 * and providing direct DFA construction for them.
 */
public class RegexOptimizer {
    
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
    
    private boolean isLiteral(String regex) {
        return LITERAL_PATTERN.matcher(regex).matches();
    }
    
    private boolean isExactString(String regex) {
        return EXACT_STRING_PATTERN.matcher(regex).matches();
    }
    
    private boolean isStartsWith(String regex) {
        return STARTS_WITH_PATTERN.matcher(regex).matches();
    }
    
    private String extractStartsWithPrefix(String regex) {
        Matcher matcher = STARTS_WITH_PATTERN.matcher(regex);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private boolean isEndsWith(String regex) {
        return ENDS_WITH_PATTERN.matcher(regex).matches();
    }
    
    private String extractEndsWithSuffix(String regex) {
        Matcher matcher = ENDS_WITH_PATTERN.matcher(regex);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private boolean isSimpleAlternation(String regex) {
        return SIMPLE_ALTERNATION_PATTERN.matcher(regex).matches();
    }
    
    private String[] extractAlternatives(String regex) {
        Matcher matcher = SIMPLE_ALTERNATION_PATTERN.matcher(regex);
        if (matcher.matches()) {
            return new String[] { matcher.group(1), matcher.group(2) };
        }
        return null;
    }
    
    private boolean isSimpleStar(String regex) {
        return SIMPLE_STAR_PATTERN.matcher(regex).matches();
    }
    
    private char extractStarCharacter(String regex) {
        Matcher matcher = SIMPLE_STAR_PATTERN.matcher(regex);
        if (matcher.matches()) {
            return matcher.group(1).charAt(0);
        }
        return 0;
    }
    
    private boolean isSimpleCharacterClass(String regex) {
        return CHARACTER_CLASS_PATTERN.matcher(regex).matches();
    }
    
    private Set<Character> extractCharacterClass(String regex) {
        Set<Character> chars = new HashSet<>();
        Matcher matcher = CHARACTER_CLASS_PATTERN.matcher(regex);
        
        if (matcher.matches()) {
            String content = matcher.group(1);
            for (int i = 0; i < content.length(); i++) {
                if (i + 2 < content.length() && content.charAt(i + 1) == '-') {
                    char start = content.charAt(i);
                    char end = content.charAt(i + 2);
                    for (char c = start; c <= end; c++) {
                        chars.add(c);
                    }
                    i += 2; // Skip the range
                } else {
                    chars.add(content.charAt(i));
                }
            }
        }
        
        return chars;
    }
    
    private DFA constructLiteralDfa(String literal) {
        DFA dfa = new DFA();
        
        DFA.State[] states = new DFA.State[literal.length() + 1];
        for (int i = 0; i <= literal.length(); i++) {
            states[i] = new DFA.State("q" + i);
            dfa.addState(states[i]);
        }
        
        dfa.setStartState(states[0]);
        
        for (int i = 0; i < literal.length(); i++) {
            dfa.addTransition(states[i], literal.charAt(i), states[i + 1]);
        }
        
        dfa.addAcceptState(states[literal.length()]);
        
        return dfa;
    }
    
    private DFA constructExactStringDfa(String str) {
        return constructLiteralDfa(str);
    }
    
    private DFA constructStartsWithDfa(String prefix) {
        DFA dfa = new DFA();
        
        DFA.State[] states = new DFA.State[prefix.length() + 1];
        for (int i = 0; i <= prefix.length(); i++) {
            states[i] = new DFA.State("q" + i);
            dfa.addState(states[i]);
        }
        
        dfa.setStartState(states[0]);
        
        for (int i = 0; i < prefix.length(); i++) {
            dfa.addTransition(states[i], prefix.charAt(i), states[i + 1]);
        }
        
        dfa.addAcceptState(states[prefix.length()]);
        
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
    
    private DFA constructEndsWithDfa(String suffix) {
        DFA dfa = new DFA();
        
        DFA.State[] states = new DFA.State[suffix.length() + 1];
        for (int i = 0; i <= suffix.length(); i++) {
            states[i] = new DFA.State("q" + i);
            dfa.addState(states[i]);
        }
        
        dfa.setStartState(states[0]);
        
        for (char c = 'a'; c <= 'z'; c++) {
            dfa.addTransition(states[0], c, states[0]);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            dfa.addTransition(states[0], c, states[0]);
        }
        for (char c = '0'; c <= '9'; c++) {
            dfa.addTransition(states[0], c, states[0]);
        }
        
        dfa.addTransition(states[0], suffix.charAt(0), states[1]);
        for (int i = 1; i < suffix.length(); i++) {
            dfa.addTransition(states[i], suffix.charAt(i), states[i + 1]);
        }
        
        dfa.addAcceptState(states[suffix.length()]);
        
        return dfa;
    }
    
    private DFA constructAlternationDfa(String alt1, String alt2) {
        DFA dfa = new DFA();
        
        DFA.State startState = new DFA.State("q0");
        DFA.State acceptState = new DFA.State("qAccept");
        
        dfa.addState(startState);
        dfa.addState(acceptState);
        
        dfa.setStartState(startState);
        dfa.addAcceptState(acceptState);
        
        if (alt1.length() == 1 && alt2.length() == 1) {
            dfa.addTransition(startState, alt1.charAt(0), acceptState);
            dfa.addTransition(startState, alt2.charAt(0), acceptState);
        } else {
            DFA dfa1 = constructLiteralDfa(alt1);
            DFA dfa2 = constructLiteralDfa(alt2);
            
            // TODO: Implement proper state merging
            return null;
        }
        
        return dfa;
    }
    
    private DFA constructStarDfa(char c) {
        DFA dfa = new DFA();
        
        DFA.State state = new DFA.State("q0");
        dfa.addState(state);
        dfa.setStartState(state);
        dfa.addAcceptState(state);
        
        dfa.addTransition(state, c, state);
        
        return dfa;
    }
    
    private DFA constructCharacterClassDfa(Set<Character> chars) {
        DFA dfa = new DFA();
        
        DFA.State startState = new DFA.State("q0");
        DFA.State acceptState = new DFA.State("q1");
        
        dfa.addState(startState);
        dfa.addState(acceptState);
        
        dfa.setStartState(startState);
        dfa.addAcceptState(acceptState);
        
        for (char c : chars) {
            dfa.addTransition(startState, c, acceptState);
        }
        
        return dfa;
    }
} 