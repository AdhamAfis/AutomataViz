# AutomataViz

A Java application that converts regular expressions to minimal Deterministic Finite Automata (DFA) and visualizes them as interactive graphs.

## Features

- Regex to DFA conversion pipeline: Thompson's Construction → Subset Construction → Hopcroft's Algorithm
- Interactive visualization of NFAs and DFAs with color-coded states and transitions
- Split view to compare NFA and DFA side by side
- Navigation controls: zoom, pan, reset view, and export as PNG
- Test strings against the DFA with step-by-step simulation
- Support for standard regex operations: basic symbols, alternation (|), Kleene star (*), plus (+), optional (?), grouping, character classes, dot operator

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

1. Enter a regex in the input field and click "Visualize DFA"
2. Navigate the visualization:
   - Zoom: Ctrl+Mouse Wheel or +/- buttons
   - Pan: Click the "🖐" button and drag, or Alt+Drag
3. Test strings:
   - Enter a test string and click "Test"
   - View the simulation trace and result

## Visualization Legend

- **States**: Green border (start), Orange fill (accept), Blue/purple (regular)
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
- **Visualizers**: Render automata using JGraphT and JGraphX with custom styling
