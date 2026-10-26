package com.dfavisualizer.algorithm;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import com.dfavisualizer.model.NFA;

/** Thompson construction for the app's finite-alphabet regular expression syntax. */
public class RegexParser {
    private final ConstructionTrace trace;
    public RegexParser() { this(null); }
    public RegexParser(ConstructionTrace trace) { this.trace = trace; }
    private void record(String explanation, NFA nfa) {
        if (trace != null) trace.nfa(explanation + "\nThis is a fragment, with local state names; combining fragments renumbers states.", nfa);
    }
    private String regex;
    private int position;

    public NFA parse(String regex) {
        if (regex == null || regex.isEmpty()) {
            NFA empty = NFA.forEpsilon(); record("Empty expression: an ε-edge accepts the empty string.", empty); return empty;
        }
        if (regex.length() > 2000) throw new IllegalArgumentException("Expression exceeds the 2,000 character teaching limit. Try a smaller example.");
        int nesting = 0;
        boolean escaped = false, inClass = false;
        for (char c : regex.toCharArray()) {
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '[') inClass = true;
            if (c == ']') inClass = false;
            if (!inClass && c == '(' && ++nesting > 64)
                throw new IllegalArgumentException("Use at most 64 nested groups in a teaching example.");
            if (!inClass && c == ')') nesting--;
        }
        Pattern.compile(regex);
        this.regex = regex;
        position = 0;
        NFA result = parseExpression();
        if (position != regex.length()) throw error("Unexpected character");
        record("Completed Thompson NFA for " + regex + ". ε-edges consume no input.", result);
        return result;
    }

    private NFA parseExpression() {
        NFA result = parseTerm();
        while (position < regex.length() && regex.charAt(position) == '|') {
            position++;
            result = result.union(parseTerm());
            record("Alternative |: add a new start and accept state, with ε-edges to choose either branch and join their exits.", result);
        }
        return result;
    }

    private NFA parseTerm() {
        NFA result = null;
        while (position < regex.length() && regex.charAt(position) != '|' && regex.charAt(position) != ')') {
            NFA factor = parseFactor();
            if (result == null) result = factor;
            else {
                result = result.concatenate(factor);
                record("Concatenation: connect each left exit to the right entry with an ε-edge. Read the left fragment, then the right.", result);
            }
        }
        if (result == null) { result = NFA.forEpsilon(); record("Empty branch: an ε-edge allows this branch to consume no input.", result); }
        return result;
    }

    private NFA parseFactor() {
        if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
        int start = position;
        NFA atom = parseAtom();
        record("Fragment " + regex.substring(start, position) + ": enter at the green arrow and finish at the double ring. A character edge consumes one symbol.", atom);
        if (position < regex.length()) {
            char operator = regex.charAt(position);
            if (operator == '*' || operator == '+' || operator == '?') {
                position++;
                atom = operator == '*' ? atom.kleeneStar() : operator == '+' ? atom.oneOrMore() : atom.zeroOrOne();
                record(operator == '*' ? "Repeat *: add ε-edges to skip the fragment or loop from its exit to its entry. Zero repetitions are allowed."
                    : operator == '+' ? "Repeat +: concatenate one copy with a starred copy. At least one repetition is required."
                    : "Optional ?: add an ε-path around the fragment, allowing zero or one occurrence.", atom);
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
