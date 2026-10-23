package com.dfavisualizer.algorithm;

import java.util.*;

/** Immutable expressions keep algebraic simplifications separate from regex text/escaping. */
final class RegexExpression {
    private enum Kind { EPSILON, SYMBOLS, ALTERNATION, SEQUENCE, REPETITION }
    static final RegexExpression EPSILON = new RegexExpression(Kind.EPSILON, Collections.emptyList(), Collections.emptySet(), '\0');
    private final Kind kind;
    private final List<RegexExpression> children;
    private final Set<Character> symbols;
    private final char operator;
    private final int weight;
    private final int hash;

    private RegexExpression(Kind kind, List<RegexExpression> children, Set<Character> symbols, char operator) {
        this.kind = kind; this.children = List.copyOf(children);
        this.symbols = Collections.unmodifiableSet(new TreeSet<>(symbols)); this.operator = operator;
        long size = symbols.size() + 2;
        for (RegexExpression child : children) size += child.weight + 2L;
        if (size > 2_000_000) throw new IllegalStateException("Equivalent expression exceeds the search size limit; try another elimination order.");
        weight = (int) Math.min(1_000_001, size);
        hash = Objects.hash(kind, this.children, this.symbols, operator);
    }

    static RegexExpression symbols(Set<Character> symbols) {
        return new RegexExpression(Kind.SYMBOLS, Collections.emptyList(), symbols, '\0');
    }

    static RegexExpression union(RegexExpression left, RegexExpression right) {
        if (left == null) return right;
        if (right == null || left.equals(right)) return left;
        LinkedHashSet<RegexExpression> choices = new LinkedHashSet<>();
        addChoices(choices, left); addChoices(choices, right);
        Set<Character> merged = new TreeSet<>();
        for (RegexExpression choice : choices) if (choice.kind == Kind.SYMBOLS) merged.addAll(choice.symbols);
        if (!merged.isEmpty()) {
            choices.removeIf(choice -> choice.kind == Kind.SYMBOLS);
            choices.add(symbols(merged));
        }
        for (RegexExpression choice : new ArrayList<>(choices)) {
            if (choice.kind != Kind.REPETITION) continue;
            RegexExpression base = choice.children.get(0);
            choices.remove(base);
            if (choice.operator == '*') {
                choices.remove(repeat(base, '+'));
                choices.remove(repeat(base, '?'));
            }
            if (choice.operator != '+') choices.remove(EPSILON);
        }
        boolean optional = choices.remove(EPSILON);
        List<RegexExpression> parts = new ArrayList<>(choices);
        RegexExpression result;
        if (parts.isEmpty()) return EPSILON;
        if (parts.size() == 1) result = parts.get(0);
        else {
            List<List<RegexExpression>> sequences = new ArrayList<>();
            for (RegexExpression part : parts) sequences.add(new ArrayList<>(part.sequence()));
            RegexExpression prefix = sequences.get(0).get(0);
            RegexExpression suffix = sequences.get(0).get(sequences.get(0).size() - 1);
            if (sequences.stream().allMatch(sequence -> sequence.get(0).equals(prefix))) {
                RegexExpression rest = null;
                for (List<RegexExpression> sequence : sequences) rest = union(rest, sequence(sequence.subList(1, sequence.size())));
                result = concat(prefix, rest);
            } else if (sequences.stream().allMatch(sequence -> sequence.get(sequence.size() - 1).equals(suffix))) {
                RegexExpression rest = null;
                for (List<RegexExpression> sequence : sequences) rest = union(rest, sequence(sequence.subList(0, sequence.size() - 1)));
                result = concat(rest, suffix);
            } else {
                result = factorRepetitionSuffix(parts);
                if (result == null) result = new RegexExpression(Kind.ALTERNATION, parts, Collections.emptySet(), '\0');
            }
        }
        return optional ? repeat(result, '?') : result;
    }

    private static RegexExpression factorRepetitionSuffix(List<RegexExpression> parts) {
        List<List<RegexExpression>> expanded = new ArrayList<>();
        for (RegexExpression part : parts) {
            List<RegexExpression> sequence = new ArrayList<>(part.sequence());
            RegexExpression last = sequence.get(sequence.size() - 1);
            if (last.isRepeat('+')) {
                sequence.remove(sequence.size() - 1);
                RegexExpression base = last.children.get(0);
                sequence.addAll(base.sequence());
                sequence.add(star(base));
            }
            expanded.add(sequence);
        }
        RegexExpression suffix = expanded.get(0).get(expanded.get(0).size() - 1);
        if (!suffix.isRepeat('*') || !expanded.stream().allMatch(sequence -> sequence.get(sequence.size() - 1).equals(suffix))) return null;
        RegexExpression rest = null;
        for (List<RegexExpression> sequence : expanded)
            rest = union(rest, sequence(sequence.subList(0, sequence.size() - 1)));
        return concat(rest, suffix);
    }

    private static void addChoices(Set<RegexExpression> choices, RegexExpression expression) {
        if (expression.kind == Kind.ALTERNATION) choices.addAll(expression.children);
        else choices.add(expression);
    }

    static RegexExpression concat(RegexExpression... expressions) {
        List<RegexExpression> parts = new ArrayList<>();
        for (RegexExpression expression : expressions) {
            if (expression == null) return null;
            if (expression.kind != Kind.EPSILON) parts.addAll(expression.sequence());
        }
        for (int i = 0; i + 1 < parts.size();) {
            RegexExpression left = parts.get(i), right = parts.get(i + 1), replacement = null;
            if (left.isRepeat('*') && right.equals(left)) replacement = left;
            else if (right.isRepeat('*') && left.equals(right.children.get(0))) replacement = repeat(left, '+');
            else if (left.isRepeat('*') && right.equals(left.children.get(0))) replacement = repeat(right, '+');
            else if (left.kind == Kind.REPETITION && right.kind == Kind.REPETITION
                    && left.children.equals(right.children) && (left.operator == '*' || right.operator == '*'))
                replacement = repeat(left.children.get(0), left.operator == '+' || right.operator == '+' ? '+' : '*');
            if (replacement != null) { parts.set(i, replacement); parts.remove(i + 1); if (i > 0) i--; }
            else i++;
        }
        return sequence(parts);
    }

    private List<RegexExpression> sequence() { return kind == Kind.SEQUENCE ? children : Collections.singletonList(this); }
    private static RegexExpression sequence(List<RegexExpression> parts) {
        if (parts.isEmpty()) return EPSILON;
        return parts.size() == 1 ? parts.get(0) : new RegexExpression(Kind.SEQUENCE, parts, Collections.emptySet(), '\0');
    }

    static RegexExpression star(RegexExpression expression) { return expression == null ? EPSILON : repeat(expression, '*'); }
    private static RegexExpression repeat(RegexExpression expression, char operator) {
        if (expression.kind == Kind.EPSILON) return EPSILON;
        if (expression.kind == Kind.REPETITION) {
            if (expression.operator == operator || expression.operator == '*') return expression;
            return repeat(expression.children.get(0), '*');
        }
        return new RegexExpression(Kind.REPETITION, Collections.singletonList(expression), Collections.emptySet(), operator);
    }
    private boolean isRepeat(char operator) { return kind == Kind.REPETITION && this.operator == operator; }
    private int precedence() { return kind == Kind.ALTERNATION ? 1 : kind == Kind.SEQUENCE ? 2 : kind == Kind.REPETITION ? 3 : 4; }

    int weight() { return weight; }

    RegexExpression reversed() {
        if (kind == Kind.SYMBOLS || kind == Kind.EPSILON) return this;
        if (kind == Kind.REPETITION) return repeat(children.get(0).reversed(), operator);
        if (kind == Kind.ALTERNATION) {
            RegexExpression result = null;
            for (RegexExpression child : children) result = union(result, child.reversed());
            return result;
        }
        List<RegexExpression> reversed = new ArrayList<>();
        for (int i = children.size() - 1; i >= 0; i--) reversed.add(children.get(i).reversed());
        return concat(reversed.toArray(new RegexExpression[0]));
    }

    String render() { StringBuilder result = new StringBuilder(); render(result, 0); return result.toString(); }
    private void render(StringBuilder result, int parentPrecedence) {
        boolean group = precedence() < parentPrecedence;
        if (group) result.append('(');
        if (kind == Kind.SYMBOLS) {
            List<Character> ordered = new ArrayList<>(symbols);
            boolean characterClass = ordered.size() > 1 || ordered.get(0) <= 32 && "\n\r\t".indexOf(ordered.get(0)) < 0;
            if (characterClass) result.append('[');
            for (int i = 0; i < ordered.size(); i++) {
                char first = ordered.get(i); int last = i;
                while (last + 1 < ordered.size() && ordered.get(last + 1) == ordered.get(last) + 1
                        && sameRange(first, ordered.get(last + 1))) last++;
                if (characterClass && last - i >= 2) { literal(result, first, true); result.append('-'); literal(result, ordered.get(last), true); i = last; }
                else literal(result, first, characterClass);
            }
            if (characterClass) result.append(']');
        } else if (kind == Kind.REPETITION) {
            children.get(0).render(result, 4); result.append(operator);
        } else {
            for (int i = 0; i < children.size(); i++) {
                if (kind == Kind.ALTERNATION && i > 0) result.append('|');
                children.get(i).render(result, precedence());
            }
        }
        if (group) result.append(')');
        if (result.length() > 1_000_000) throw new IllegalStateException("Equivalent expression exceeds one million characters; simplify the graph first.");
        if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
    }
    private boolean sameRange(char first, char last) {
        return first >= 'a' && last <= 'z' || first >= 'A' && last <= 'Z' || first >= '0' && last <= '9';
    }
    private void literal(StringBuilder result, char c, boolean inClass) {
        if (c == '\n') { result.append("\\n"); return; }
        if (c == '\r') { result.append("\\r"); return; }
        if (c == '\t') { result.append("\\t"); return; }
        if ((inClass ? "\\[]^-&" : "\\.[](){}*+?|^$").indexOf(c) >= 0) result.append('\\');
        result.append(c);
    }
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RegexExpression)) return false;
        RegexExpression expression = (RegexExpression) other;
        return kind == expression.kind && operator == expression.operator && children.equals(expression.children) && symbols.equals(expression.symbols);
    }
    @Override public int hashCode() { return hash; }
}
