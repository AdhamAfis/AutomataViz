package com.dfavisualizer.algorithm;

import java.util.*;
import java.util.concurrent.CancellationException;
import com.dfavisualizer.model.DFA;
import com.dfavisualizer.model.NFA;

/** Snapshots an automaton, then eliminates states to produce the same whole-string language. */
public final class AutomatonToRegex {
    private final Map<Object, Integer> ids = new LinkedHashMap<>();
    private final Map<Long, RegexExpression> edges = new HashMap<>();
    private final int start, finish;
    private DFA source;
    private String easiestToRead;

    public static final class Alternatives {
        public final String shortest, readable;
        private Alternatives(String shortest, String readable) { this.shortest = shortest; this.readable = readable; }
    }

    /** Both returned choices have passed an exact DFA language-equivalence check. */
    public Alternatives alternatives(String... preferredExpressions) {
        if (source == null) throw new IllegalStateException("Regex alternatives require a DFA snapshot");
        String shortest = convert(preferredExpressions);
        String readable = easiestToRead == null ? shortest : easiestToRead;
        if (!verified(shortest) || !verified(readable)) throw new IllegalStateException("An expression failed language-equivalence verification");
        return new Alternatives(shortest, readable);
    }

    private boolean verified(String expression) {
        return equivalent(new SubsetConstruction().convertNfaToDfa(new RegexParser().parse(expression)));
    }

    private void considerReadable(String expression) {
        if (expression != null && (easiestToRead == null || readabilityScore(expression) < readabilityScore(easiestToRead)))
            easiestToRead = expression;
    }

    private long readabilityScore(String expression) {
        int depth = 0, maxDepth = 0, groups = 0, branches = 0;
        boolean characterClass = false;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '\\') { i++; continue; }
            if (c == '[') characterClass = true;
            else if (c == ']') characterClass = false;
            else if (!characterClass) {
                if (c == '(') { groups++; maxDepth = Math.max(maxDepth, ++depth); }
                else if (c == ')') depth--;
                else if (c == '|') branches++;
            }
        }
        return expression.length() + maxDepth * 12L + groups * 3L + branches * 8L;
    }


    public AutomatonToRegex(DFA dfa) {
        source = new DFA();
        for (DFA.State state : dfa.getStates()) source.addState(state);
        source.setStartState(dfa.getStartState());
        source.getAcceptStates().addAll(dfa.getAcceptStates());
        source.getAlphabet().addAll(dfa.getAlphabet());
        source.getTransitions().putAll(dfa.getTransitions());
        initialize(dfa.getStates());
        start = ids.size(); finish = start + 1;
        add(start, ids.get(dfa.getStartState()), RegexExpression.EPSILON);
        for (DFA.State state : dfa.getAcceptStates()) add(ids.get(state), finish, RegexExpression.EPSILON);
        Map<Long, SortedSet<Character>> symbols = new HashMap<>();
        for (Map.Entry<DFA.StateTransition, DFA.State> edge : dfa.getTransitions().entrySet())
            symbols.computeIfAbsent(key(ids.get(edge.getKey().getState()), ids.get(edge.getValue())), k -> new TreeSet<>()).add(edge.getKey().getSymbol());
        addSymbols(symbols);
    }

    public AutomatonToRegex(NFA nfa) {
        initialize(nfa.getStates());
        start = ids.size(); finish = start + 1;
        add(start, ids.get(nfa.getStartState()), RegexExpression.EPSILON);
        for (int state : nfa.getAcceptStates()) add(ids.get(state), finish, RegexExpression.EPSILON);
        Map<Long, SortedSet<Character>> symbols = new HashMap<>();
        for (Map.Entry<NFA.NFATransition, Set<Integer>> edge : nfa.getTransitions().entrySet()) {
            int from = ids.get(edge.getKey().getState());
            for (int target : edge.getValue()) {
                int to = ids.get(target);
                if (edge.getKey().getSymbol() == NFA.EPSILON) add(from, to, RegexExpression.EPSILON);
                else symbols.computeIfAbsent(key(from, to), k -> new TreeSet<>()).add(edge.getKey().getSymbol());
            }
        }
        addSymbols(symbols);
    }

    private void initialize(Set<?> states) {
        List<Object> ordered = new ArrayList<>(states);
        ordered.sort(Comparator.comparing(Object::toString));
        for (Object state : ordered) ids.put(state, ids.size());
    }

    private void addSymbols(Map<Long, SortedSet<Character>> symbols) {
        for (Map.Entry<Long, SortedSet<Character>> edge : symbols.entrySet()) {
            edges.put(edge.getKey(), RegexExpression.union(edges.get(edge.getKey()), RegexExpression.symbols(edge.getValue())));
        }
    }

    /** Run on a worker thread; null edges denote the empty language; epsilon has its own expression node. */
    public String convert() { return convert(new String[0]); }

    public String convert(String... preferredExpressions) {
        easiestToRead = null;
        String expression = bestExpression();
        if (source != null) for (String preferred : preferredExpressions) {
            checkCancelled();
            if (preferred == null || preferred.isEmpty()) continue;
            try {
                DFA candidate = new SubsetConstruction().convertNfaToDfa(new RegexParser().parse(preferred));
                if (equivalent(candidate)) {
                    if (preferred.length() < expression.length()) expression = preferred;
                    considerReadable(preferred);
                }
            } catch (IllegalArgumentException ex) {
                // A manually edited or unsupported source must not prevent graph conversion.
            }
        }
        considerReadable(expression);
        return expression;
    }

    private String bestExpression() {
        String best = searchOrders(false);
        if (source == null) return best;
        checkCancelled();
        DFA minimized = new DfaMinimizer().minimize(source);
        if (!equivalent(minimized)) return best;
        minimized = trim(minimized);
        AutomatonToRegex reduced = new AutomatonToRegex(minimized);
        best = shorter(best, reduced.searchOrders(false));
        considerReadable(reduced.easiestToRead);
        // shortcut: reverse determinization can be exponential; use it for small DFAs until a bounded subset builder exists.
        if (minimized.getStates().size() <= 10) {
            NFA reversed = reverse(minimized);
            DFA reversedDfa = new SubsetConstruction().convertNfaToDfa(reversed);
            reversedDfa = new DfaMinimizer().minimize(reversedDfa);
            AutomatonToRegex reversedConverter = new AutomatonToRegex(trim(reversedDfa));
            best = shorter(best, reversedConverter.searchOrders(true));
            considerReadable(reversedConverter.easiestToRead);
        }
        return best;
    }

    private String searchOrders(boolean reverseResult) {
        Map<Long, RegexExpression> initial = new HashMap<>(edges);
        String best = null;
        try {
            int attempts = 6;
            if (ids.size() <= 5) {
                int permutations = 1;
                for (int i = 2; i <= ids.size(); i++) permutations *= i;
                attempts += permutations;
            }
            for (int strategy = 0; strategy < attempts; strategy++) {
                checkCancelled();
                edges.clear(); edges.putAll(initial);
                try {
                    RegexExpression result = eliminate(strategy);
                    if (result != null && reverseResult) result = result.reversed();
                    String expression = result == null ? "(?!)" : result.equals(RegexExpression.EPSILON) ? "()" : result.render();
                    best = shorter(best, expression);
                    considerReadable(expression);
                } catch (IllegalStateException tooLarge) {
                    // Another elimination order can stay small even when this one expands past the size limit.
                }
            }
        } finally { edges.clear(); edges.putAll(initial); }
        if (best == null) throw new IllegalStateException("Equivalent expression exceeds one million characters; simplify the graph first.");
        return best;
    }

    private RegexExpression eliminate(int strategy) {
        Set<Integer> remaining = new LinkedHashSet<>();
        for (int i = 0; i < ids.size() + 2; i++) remaining.add(i);
        List<Integer> order = new ArrayList<>();
        if (strategy >= 6) {
            List<Integer> available = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) available.add(i);
            int permutation = strategy - 6;
            while (!available.isEmpty()) {
                int index = permutation % available.size();
                permutation /= available.size();
                order.add(available.remove(index));
            }
        }
        while (remaining.size() > 2) {
            checkCancelled();
            int chosen = -1;
            long best = Long.MAX_VALUE;
            for (int candidate : remaining) {
                if (candidate == start || candidate == finish) continue;
                long incoming = 0, outgoing = 0, inWeight = 0, outWeight = 0;
                for (int other : remaining) if (other != candidate) {
                    if (get(other, candidate) != null) { incoming++; inWeight += get(other, candidate).weight(); }
                    if (get(candidate, other) != null) { outgoing++; outWeight += get(candidate, other).weight(); }
                }
                long cost;
                if (strategy >= 6) cost = order.indexOf(candidate);
                else if (strategy == 0) cost = incoming * outgoing;
                else if (strategy == 1) cost = incoming * outWeight + outgoing * inWeight;
                else if (strategy == 2) cost = incoming;
                else if (strategy == 3) cost = outgoing;
                else cost = strategy == 4 ? candidate : -candidate;
                if (cost < best) { best = cost; chosen = candidate; }
            }
            RegexExpression repeated = RegexExpression.star(get(chosen, chosen));
            List<Integer> incoming = new ArrayList<>(), outgoing = new ArrayList<>();
            for (int other : remaining) if (other != chosen) {
                if (get(other, chosen) != null) incoming.add(other);
                if (get(chosen, other) != null) outgoing.add(other);
            }
            for (int from : incoming) for (int to : outgoing) {
                checkCancelled();
                RegexExpression through = RegexExpression.concat(get(from, chosen), repeated, get(chosen, to));
                add(from, to, through);
            }
            for (int other : remaining) { edges.remove(key(other, chosen)); edges.remove(key(chosen, other)); }
            remaining.remove(chosen);
        }
        return get(start, finish);
    }

    private String shorter(String left, String right) {
        return left == null || right.length() < left.length() ? right : left;
    }

    private DFA trim(DFA dfa) {
        Set<DFA.State> dead = dfa.getDeadStates();
        DFA result = new DFA();
        result.setStartState(dfa.getStartState());
        Queue<DFA.State> queue = new ArrayDeque<>();
        Set<DFA.State> seen = new HashSet<>();
        queue.add(dfa.getStartState()); seen.add(dfa.getStartState());
        while (!queue.isEmpty()) {
            checkCancelled();
            DFA.State from = queue.remove();
            if (dfa.getAcceptStates().contains(from)) result.addAcceptState(from);
            for (char symbol : dfa.getAlphabet()) {
                DFA.State to = dfa.getTransitionTarget(from, symbol);
                if (to == null || dead.contains(to)) continue;
                result.addTransition(from, symbol, to);
                if (seen.add(to)) queue.add(to);
            }
        }
        return result;
    }

    private NFA reverse(DFA dfa) {
        NFA result = new NFA();
        Map<DFA.State, Integer> states = new HashMap<>();
        for (DFA.State state : dfa.getStates()) states.put(state, result.createState());
        int newStart = result.createState(); result.setStartState(newStart);
        result.addAcceptState(states.get(dfa.getStartState()));
        for (DFA.State accepting : dfa.getAcceptStates()) result.addTransition(newStart, NFA.EPSILON, states.get(accepting));
        for (Map.Entry<DFA.StateTransition, DFA.State> edge : dfa.getTransitions().entrySet())
            result.addTransition(states.get(edge.getValue()), edge.getKey().getSymbol(), states.get(edge.getKey().getState()));
        return result;
    }

    private boolean equivalent(DFA candidate) {
        Set<Character> alphabet = new HashSet<>(source.getAlphabet());
        alphabet.addAll(candidate.getAlphabet());
        Set<List<DFA.State>> seen = new HashSet<>();
        Queue<List<DFA.State>> queue = new ArrayDeque<>();
        List<DFA.State> initial = Arrays.asList(source.getStartState(), candidate.getStartState());
        seen.add(initial); queue.add(initial);
        while (!queue.isEmpty()) {
            checkCancelled();
            List<DFA.State> pair = queue.remove();
            if (source.getAcceptStates().contains(pair.get(0)) != candidate.getAcceptStates().contains(pair.get(1))) return false;
            for (char symbol : alphabet) {
                List<DFA.State> next = Arrays.asList(pair.get(0) == null ? null : source.getTransitionTarget(pair.get(0), symbol),
                        pair.get(1) == null ? null : candidate.getTransitionTarget(pair.get(1), symbol));
                if (seen.add(next)) queue.add(next);
            }
        }
        return true;
    }

    private long key(int from, int to) { return ((long) from << 32) | (to & 0xffffffffL); }
    private RegexExpression get(int from, int to) { return edges.get(key(from, to)); }
    private void add(int from, int to, RegexExpression label) { edges.put(key(from, to), RegexExpression.union(get(from, to), label)); }
    private void checkCancelled() { if (Thread.currentThread().isInterrupted()) throw new CancellationException(); }
}
