package com.dfavisualizer;

/**
 * Parser for regular expressions that builds NFAs using Thompson's construction algorithm.
 * Supports basic operators: concatenation, alternation (|), Kleene star (*),
 * one or more (+), zero or one (?), and grouping with parentheses.
 */
public class RegexParser {
    private String regex;
    private int position;

    /**
     * Parses a regular expression and returns the corresponding NFA.
     */
    public NFA parse(String regex) {
        if (regex == null || regex.isEmpty()) {
            return NFA.forEpsilon();
        }
        
        this.regex = preprocessRegex(regex);
        this.position = 0;
        
        return parseExpression();
    }

    /**
     * Preprocess the regex to handle escape sequences and insert explicit concatenation.
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
    private NFA parseExpression() {
        NFA term = parseTerm();
        
        while (position < regex.length() && regex.charAt(position) == '|') {
            position++;  // Skip '|'
            NFA nextTerm = parseTerm();
            term = term.union(nextTerm);
        }
        
        return term;
    }

    /**
     * Parses a term, which is one or more factors that are concatenated.
     */
    private NFA parseTerm() {
        NFA factor = parseFactor();
        
        while (position < regex.length() &&
               regex.charAt(position) != '|' &&
               regex.charAt(position) != ')') {
            
            NFA nextFactor = parseFactor();
            factor = factor.concatenate(nextFactor);
        }
        
        return factor;
    }

    /**
     * Parses a factor, which is an atom followed by an optional operator (*, +, ?).
     */
    private NFA parseFactor() {
        NFA atom = parseAtom();
        
        if (position < regex.length()) {
            char operator = regex.charAt(position);
            
            if (operator == '*') {
                position++;  // Skip '*'
                atom = atom.kleeneStar();
            } else if (operator == '+') {
                position++;  // Skip '+'
                atom = atom.oneOrMore();
            } else if (operator == '?') {
                position++;  // Skip '?'
                atom = atom.zeroOrOne();
            }
        }
        
        return atom;
    }

    /**
     * Parses an atom, which is a character, a group in parentheses, or a character class.
     */
    private NFA parseAtom() {
        if (position >= regex.length()) {
            return NFA.forEpsilon();
        }
        
        char c = regex.charAt(position);
        
        if (c == '(') {
            position++;  // Skip '('
            NFA result = parseExpression();
            
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
            return NFA.forSymbol(c);
        }
    }

    /**
     * Parses a dot (any character) by creating an NFA that matches any character
     * in the default alphabet.
     */
    private NFA parseDot() {
        NFA result = null;
        
        // Create an NFA that matches any character (a|b|c|...) for a limited alphabet
        char[] alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        
        for (char c : alphabet) {
            NFA charNFA = NFA.forSymbol(c);
            
            if (result == null) {
                result = charNFA;
            } else {
                result = result.union(charNFA);
            }
        }
        
        return result;
    }

    /**
     * Parses a character class [...] by creating an NFA that matches any character
     * in the class.
     */
    private NFA parseCharacterClass() {
        NFA result = null;
        boolean negate = false;
        
        // Check for negation
        if (position < regex.length() && regex.charAt(position) == '^') {
            negate = true;
            position++;
        }
        
        // Parse characters in the class
        while (position < regex.length() && regex.charAt(position) != ']') {
            char c = regex.charAt(position++);
            
            // Handle ranges like a-z
            if (position + 1 < regex.length() && regex.charAt(position) == '-' && regex.charAt(position + 1) != ']') {
                position++;  // Skip '-'
                char end = regex.charAt(position++);
                
                // Add all characters in the range
                for (char rangeChar = c; rangeChar <= end; rangeChar++) {
                    NFA charNFA = NFA.forSymbol(rangeChar);
                    
                    if (result == null) {
                        result = charNFA;
                    } else {
                        result = result.union(charNFA);
                    }
                }
            } else {
                // Add single character
                NFA charNFA = NFA.forSymbol(c);
                
                if (result == null) {
                    result = charNFA;
                } else {
                    result = result.union(charNFA);
                }
            }
        }
        
        // Skip closing ']'
        if (position < regex.length() && regex.charAt(position) == ']') {
            position++;
        } else {
            throw new IllegalArgumentException("Missing closing bracket in character class");
        }
        
        // If the class was negated, we would need to create an NFA that accepts
        // any character not in the class. For simplicity, we'll skip implementing
        // negation in this example.
        if (negate) {
            throw new UnsupportedOperationException("Negated character classes are not supported");
        }
        
        return result != null ? result : NFA.forEmpty();
    }
} 