# AutomataViz

A Java application that converts regular expressions to minimal Deterministic Finite Automata (DFA) and visualizes them as interactive graphs.

## Features

- Regex to DFA conversion pipeline: Thompson's Construction → Subset Construction → Hopcroft's Algorithm
- Interactive visualization of NFAs and DFAs with color-coded states and transitions
- Separate NFA and DFA tabs by default, with optional side-by-side comparison
- Navigation controls: zoom, pan, reset view, and export as PNG
- Guided DFA playback with decision previews, backward stepping, focused paths, and precise failure explanations
- Selectable regex fragments and an interactive ε-NFA lesson with exact DFA subset mappings
- Support for regex operations: basic symbols, alternation (|), Kleene star (*), plus (+), optional (?), grouping, character classes, dot operator

The desktop interface uses [FlatLaf](https://www.formdev.com/flatlaf/) with the native Flat Dark theme shown in the FlatLaf demo, compact editor controls, charcoal toolbars, and a resizable simulation console. Graphs, teaching highlights, and exported PNGs use a coordinated dark palette. Learning, debugging, and theory actions are available through the Learn, Tools, and Help menus. Choose **View → Themes** for the six core themes and the bundled IntelliJ theme collection, matching FlatLaf Demo 3.7.2. Search by name or filter core/light/dark themes; selection previews immediately and is saved for the next launch. Graph palettes and PNG backgrounds follow light/dark mode. Switching pauses playback without changing the input position or graph layout. FlatLaf is bundled in the executable JAR; no extra installation is needed.

## Prerequisites

- Java 11+
- Maven

## Build & Run

```
# Build
mvn clean package

# Run JAR
java -jar target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar

# Or run with Maven
mvn exec:java -Dexec.mainClass="com.dfavisualizer.ui.MainApp"
```

## Application Flow

1. **Regex Parsing**: Input regex is parsed into tokens
2. **NFA Construction**: Thompson's Construction builds an NFA with epsilon transitions
3. **DFA Conversion**: Subset Construction algorithm converts NFA to DFA
4. **DFA Minimization**: Hopcroft's Algorithm reduces the DFA to minimal form
5. **Visualization**: The automata are rendered as interactive graphs
6. **Testing**: User input strings are evaluated against the DFA with step-by-step tracing

## Algorithms

1. **Thompson's Construction**:
   - Converts regex to NFA with epsilon transitions
   - Each regex operation maps to a specific NFA structure
   - Builds the NFA bottom-up from the regex parse tree

2. **Subset Construction**:
   - Converts NFA to DFA by computing epsilon closures
   - Each DFA state corresponds to a set of NFA states
   - Eliminates nondeterminism and epsilon transitions

3. **Hopcroft's Algorithm**:
   - Minimizes DFA by combining equivalent states
   - Uses partition refinement to identify states with identical behavior
   - Results in the smallest possible DFA for the language

## Package Structure

```
com.dfavisualizer/
├── algorithm/
│   ├── RegexParser.java       # Parses regex into tokens
│   ├── ThompsonConstruction.java # Builds NFA from regex
│   ├── SubsetConstruction.java   # Converts NFA to DFA
│   └── DfaMinimizer.java      # Implements Hopcroft's algorithm
├── model/
│   ├── NFA.java               # NFA representation with epsilon transitions
│   ├── DFA.java               # DFA representation
│   └── State.java             # State representation for automata
├── controller/
│   ├── AutomataController.java # Manages conversion pipeline
│   └── SimulationController.java # Handles string testing
├── ui/
│   ├── MainApp.java           # Main application entry point
│   ├── components/
│   │   ├── AutomataPanel.java # Base visualization panel
│   │   └── ControlPanel.java  # UI controls for the application
│   └── visualizer/
│       ├── NfaVisualizer.java # NFA visualization
│       └── DfaVisualizer.java # DFA visualization
└── util/
    ├── GraphUtil.java         # Graph construction utilities
    └── VisualStyleProvider.java # Styling for visualization
```

## Project Structure

```
src/
├── main/
│   ├── java/com/dfavisualizer/
│   │   ├── algorithm/   # Automata algorithms
│   │   ├── controller/  # Application logic
│   │   ├── model/       # Domain models (DFA, NFA)
│   │   ├── ui/          # UI components and visualizers
│   │   └── util/        # Utility classes
│   └── resources/       # Application resources
└── test/                # Test classes and resources
```

## Usage

1. Enter a regex in the input field and click "Visualize DFA" or press Enter
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

## Implementation

- **NFA class**: Represents NFAs with epsilon transitions
- **RegexParser**: Implements recursive descent parsing with Thompson's construction
- **SubsetConstruction**: Converts NFA to DFA
- **DfaMinimizer**: Implements Hopcroft's algorithm
- **Visualizers**: Render automata using a shared JGraphX renderer with left-to-right layout, real self-loops, and grouped transition labels

## Visual regression checks

After `mvn package`, run these assertion-based Swing checks on a machine with a desktop session:

```sh
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.VisualRegressionCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.UiInteractionCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.GraphNavigationCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.GraphDrawingCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.PlaybackCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.TextShortcutCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.GuidedPlaybackCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.NfaLearningCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.LearningWorkspaceCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.GraphControlsCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.GraphThemeCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.ThemeSwitchCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.StepResponsivenessCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.LearningReliabilityCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.LearningDialogCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.ReconnectionCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.ReconnectionInteractionCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.algorithm.LearningRegexCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.algorithm.AutomatonToRegexCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.algorithm.RegexSimplificationCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.algorithm.PreferredRegexCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.algorithm.RegexOptimizationCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.algorithm.GraphExampleRegexCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.algorithm.RegexAlternativesCheck
java -ea -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.RegexAlternativesDialogCheck
```

On Windows, replace the classpath separator `:` with `;`. The checks cover repeated generation, split-view restoration, keyboard visualization, resizable results, graph visibility, label readability, and control clipping at three window sizes and divider limits. Rendered windows are written to `target/visual-checks/`. These checks run separately from Maven's test phase because they require a desktop session. Synthetic pan and zoom gestures are checked in the main workspace and step-by-step graph tabs. Playback checks cover stepping, pause/resume, restart, self-loops, whitespace, empty input, rejection, grouped symbols, and cancellation when replacing graphs. The guided-learning checks also cover backward stepping after completion, empty-input safety, early dead-state rejection, exact subset mappings, ε-closure without input consumption, expression-source highlighting, single-view/comparison switching, and visible controls and active states with the email example at 900×650 and 1200×820. Physical mouse/trackpad gestures and the export file chooser still need manual checks.

The scrollbar appearance check can also run without a desktop session:

```sh
java -ea -Djava.awt.headless=true -cp target/test-classes:target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar com.dfavisualizer.ui.ScrollbarAppearanceCheck
```

It checks thumb contrast in both orientations, including hover and pressed states, at 1× and 2× scale.

## Project website

The static project/download website is in [`website/`](website/README.md). GitHub Actions builds its Java download, verifies the site, and deploys `website/build` to GitHub Pages after successful default-branch checks. See the website README for local preview commands and one-time Pages setup.

## Contributing and repository setup

See [CONTRIBUTING.md](CONTRIBUTING.md) for pull requests and checks, [SECURITY.md](SECURITY.md) for private vulnerability reports, and [repository setup](.github/REPOSITORY_SETUP.md) for branch protection and Actions configuration.
