# User guide

## Getting started

Choose **Examples** below the expression for suffix recognition, parity, optional versus repeated symbols, factoring, star scope, overlapping patterns, binary multiples of three, and identifiers. Advanced examples include equivalent forms and counterexamples for similar-looking expressions. Each example includes a learning goal and accepted/rejected inputs; **Load example** fills the expression and test input and generates its graphs. Empty strings are explicitly labeled.

The first-use hint shows the basic workflow. **Dismiss** remembers your choice; **Help → Getting started** brings it back.

1. Enter a regex in the input field and click "Visualize DFA" or press Enter. Conversion and layout run in the background; Cancel generation stops the request and keeps the previous graph. Press Enter with a replacement expression to supersede the pending request. Errors also leave the previous graph intact. Syntax help below the expression explains the supported language.
2. Navigate the visualization:
   - Zoom: Ctrl+Mouse Wheel or +/- buttons
   - Pan: Click "Pan" and drag, or right-click/Alt+drag; mouse wheel pans vertically (Shift for horizontally)
   - Graph scrollbars are hidden in both the main workspace and step-by-step views
   - Reset: Fit and center the graph in the available space
   - Use "Compare NFA / DFA" above the diagrams to switch between tabs and side-by-side views
   - Choose "Original" or "Minimized" beside the DFA label to change the generated DFA immediately; unfinished expression edits are not applied
   - "View options" contains dead-state highlighting for the DFA and grid snapping for both graphs; preferences persist when generating another graph
   - The main NFA remains the original ε-NFA; self-loops always animate during playback
   - Drag the divider above "Simulation & results" to resize the results area
3. Test strings:
   - Enter a test string and click "Test"
   - View the simulation trace and result; empty input is supported
   - Click "Animate" to follow a token along each transition
   - Pause/resume, step backward or forward, restart, or change playback speed in "Simulation & results"
   - "Focus graph" fades unrelated paths and follows offscreen states; uncheck it to fit the full diagram
   - Rejection stops at an unavailable transition or a dead state and explains the viable next symbols
   - Follow the input ribbon and explanation to see what was consumed and why the string is accepted or rejected

4. Learn the expression and conversion:
   - Start with **Learn → Guided exercise: recognize a suffix**. Predict whether each input matches `(a|b)*abb`, check the explanation, then trace the input in the NFA.
   - Each conversion stage has **Construction steps…**, with Back/Next through recorded algorithm decisions. Thompson snapshots show individual fragments being combined; state names are local to each fragment and can change after combination.
   - Subset construction adds transitions as sets are discovered. The explanation shows the symbol move, ε-closure, and whether a subset is new or reused.
   - Minimization shows the original DFA alongside successive partitions and the symbol responsible for each split, followed by the merged DFA. Snapshot graphs are read-only; use the main lesson graphs for editing experiments.
   - Select a fragment in "Explore expression" to highlight its source and read its meaning in the results area
   - Use "Explain selected part" to reread the explanation; choosing a part pauses active playback
   - Open "Learn" → "Step-by-Step conversion" → "Epsilon-NFA", enter a string, and choose "Start / Reset"
   - The walkthrough opens immediately and prepares models/layouts in the background. Close cancels preparation; large graphs use a faster layered layout.
   - The introduction shows the expression breakdown. Stage navigation stays disabled until preparation finishes.
   - The NFA lesson carries over the test input; editing it requires Start / Reset. Missing transitions reject immediately.
   - Use Inspect state (or click a graph state) to see exact NFA subsets and the original DFA states merged during minimization.
   - The main graph and lesson use the same parser. Escaped punctuation is literal; wildcards and negated classes use printable ASCII. Shorthand escapes, anchors, counted repetitions, and special groups show an explicit unsupported-syntax error.
   - Alternate symbol moves and ε-closure steps; ε-transitions never advance the input
   - In "NFA to DFA", select a DFA state to inspect its exact NFA subset before minimization
   - Use the "Legend" checkbox when you need the notation reference

Generated expressions use six elimination heuristics on the original and minimized/pruned DFA. For graphs of up to five states, every elimination order is searched. Small DFAs (up to ten states) also try reverse-language conversion, which exposes compact suffix patterns such as `[ab]*abab`. The shortest equivalent result found is kept.

Generated expressions are simplified automatically: redundant groups and alternatives are removed, shared prefixes/suffixes are factored, character sets become ranges, and repetition uses `*`, `+`, and `?` where equivalent. This preserves the language; it does not guarantee the globally shortest regex. A shorter original/source expression is retained only after exact DFA language-equivalence checking, including when an edit is undone.

Choose **Regex alternatives** beside the graph (or within a Learn graph stage) to compare **Shortest found** and **Easiest to read**. Each choice shows its character count and exact DFA language-equivalence result before Copy or Use expression becomes available. Readability favors shallow nesting and fewer alternative branches; both goals may find the same expression. Applying a choice in the main workspace preserves the graph/layout. Applying from Learn fills the expression field; Visualize DFA rebuilds the main workspace if needed. Search and verification run in the background, and closing the dialog cancels preparation.

Generated expressions use `()` for the empty string and `(?!)` for the empty language (no accepted strings). Extremely large equivalent expressions report an error instead of blocking the UI.

To reconnect a transition, leave Pan mode, click the line once to select it, then drag either endpoint handle onto another state. Grouped symbols move together. Loose endpoints and conflicting DFA destinations are rejected. Testing/animation uses the edited model, and the main NFA/DFA views are rebuilt for the edited language. The regex field updates in the background to an equivalent expression for the edited language; its text may differ from the original. Visualize DFA regenerates the current expression.

In Learn, reconnecting starts a custom graph experiment: the NFA trace resets, and conversion stages/explanations based on the original graph are disabled. The visible lesson expression and main regex field update together. Choose Restore expression to return both to the original source and walkthrough.

Compact transition labels such as `[0–9, a–z]` consume one character. Double-click a transition, or select it and press Enter, to inspect its exact symbols. Only transitions with the same source and destination are grouped. Expression fragments explain syntax; they are not presented as one-to-one mappings to minimized states.

## Visualization Legend

- **States**: Incoming green arrow (start), double circle and orange fill (accept), blue/purple fill (regular)
- **Playback**: Blue outline (current state), amber arrow (active transition), moving blue token
- **Transitions**: Blue arrows (self-loops), Red dashed (epsilon), Grey (normal)

## Examples

- `a(b|c)*` - 'a' followed by zero or more occurrences of 'b' or 'c'
- `(a|b)*abb` - Any string of 'a's and 'b's ending with 'abb'
- `[a-z]+@[a-z]+\.(com|org|net)` - Simple email pattern


## Teaching limits

The app matches whole strings using its educational regex syntax. Wildcards and negated classes cover printable ASCII, not all Unicode characters. Use `[0-9]` instead of `\d`; anchors, counted repetitions, lookarounds and special groups are unsupported.

Generation accepts at most 2,000 expression characters, 64 nested groups and 4,096 discovered DFA subsets. Each stage records at most 200 construction snapshots and a combined 30,000 states/transition entries. A visible notice identifies partial histories; the completed lesson graph is still available. Smaller expressions make better construction lessons.
