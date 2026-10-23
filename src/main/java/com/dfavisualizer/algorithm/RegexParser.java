package com.dfavisualizer.algorithm;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import com.dfavisualizer.model.NFA;

/** Thompson construction for the app's finite-alphabet regular expression syntax. */
public class RegexParser {
    private String regex;
    private int position;

    public NFA parse(String regex) {
        if (regex == null || regex.isEmpty()) return NFA.forEpsilon();
        Pattern.compile(regex);
        this.regex = regex;
        position = 0;
        NFA result = parseExpression();
        if (position != regex.length()) throw error("Unexpected character");
        return result;
    }

    private NFA parseExpression() {
        NFA result = parseTerm();
        while (position < regex.length() && regex.charAt(position) == '|') {
            position++;
            result = result.union(parseTerm());
        }
        return result;
    }

    private NFA parseTerm() {
        NFA result = null;
        while (position < regex.length() && regex.charAt(position) != '|' && regex.charAt(position) != ')') {
            NFA factor = parseFactor();
            result = result == null ? factor : result.concatenate(factor);
        }
        return result == null ? NFA.forEpsilon() : result;
    }

    private NFA parseFactor() {
        if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
        NFA atom = parseAtom();
        if (position < regex.length()) {
            char operator = regex.charAt(position);
            if (operator == '*' || operator == '+' || operator == '?') {
                position++;
                atom = operator == '*' ? atom.kleeneStar() : operator == '+' ? atom.oneOrMore() : atom.zeroOrOne();
                if (position < regex.length() && "*+?".indexOf(regex.charAt(position)) >= 0)
                    throw error("Lazy, possessive, and repeated quantifiers are not supported");
            }
        }
        return atom;
    }

    private NFA parseAtom() {
        char c = regex.charAt(position++);
        if (c == '(') {
            if (regex.startsWith("?!)", position)) { position += 3; return NFA.forEmpty(); }
            if (position < regex.length() && regex.charAt(position) == '?')
                throw error("Special groups are not supported; use ordinary parentheses");
            NFA result = parseExpression();
            if (position >= regex.length() || regex.charAt(position++) != ')') throw error("Missing closing parenthesis");
            return result;
        }
        if (c == '\\') return NFA.forSymbol(escapedCharacter());
        if (c == '[') return parseCharacterClass();
        if (c == '.') return characterSet(printableAlphabet());
        if ("*+?{}^$)".indexOf(c) >= 0) throw error("Unsupported operator '" + c + "'; use *, +, ?, |, groups and character classes");
        if (c == NFA.EPSILON) throw error("NUL is reserved for epsilon transitions");
        return NFA.forSymbol(c);
    }

    private char escapedCharacter() {
        if (position >= regex.length()) throw error("Missing escaped character");
        char c = regex.charAt(position++);
        if (c == 'n') return '\n';
        if (c == 'r') return '\r';
        if (c == 't') return '\t';
        if (Character.isLetterOrDigit(c)) throw error("Escape \\" + c + " is not supported; use an explicit character class");
        return c;
    }

    private NFA parseCharacterClass() {
        boolean negate = position < regex.length() && regex.charAt(position) == '^';
        if (negate) position++;
        Set<Character> characters = new TreeSet<>();
        while (position < regex.length() && (regex.charAt(position) != ']' || characters.isEmpty())) {
            char first = classCharacter();
            if (position + 1 < regex.length() && regex.charAt(position) == '-' && regex.charAt(position + 1) != ']') {
                position++;
                char last = classCharacter();
                if (last < first) throw error("Reversed character range");
                for (int c = first; c <= last; c++) characters.add((char) c);
            } else characters.add(first);
        }
        if (position >= regex.length() || regex.charAt(position++) != ']') throw error("Missing closing bracket");
        if (negate) {
            Set<Character> complement = printableAlphabet();
            complement.removeAll(characters);
            characters = complement;
        }
        return characterSet(characters);
    }

    private char classCharacter() {
        char c = regex.charAt(position++);
        if (c == '\\') return escapedCharacter();
        if (c == '[' || c == '&') throw error("Nested/intersected character classes are not supported");
        return c;
    }

    private Set<Character> printableAlphabet() {
        Set<Character> alphabet = new TreeSet<>();
        for (char c = 32; c < 127; c++) alphabet.add(c);
        return alphabet;
    }

    private NFA characterSet(Set<Character> characters) {
        NFA nfa = new NFA();
        int start = nfa.createState(), accept = nfa.createState();
        nfa.setStartState(start); nfa.addAcceptState(accept);
        for (char c : characters) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            if (c == NFA.EPSILON) throw error("NUL is reserved for epsilon transitions");
            nfa.addTransition(start, c, accept);
        }
        return nfa;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at position " + position);
    }
}
