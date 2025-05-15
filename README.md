# Java DFA Visualizer

A comprehensive Java application that converts regular expressions to minimal Deterministic Finite Automata (DFA) and visualizes them as interactive graphs.

## Features

- Complete and accurate regex to DFA conversion
- Enhanced visualization of DFAs with:
  - Color-coded states for better readability
  - Improved layouts for different DFA sizes
  - Clean and modern styling with shadows and rounded states
  - Smart transition label simplification for complex automata
- Test strings against the generated DFA with step-by-step simulation trace
- Support for all standard regex operations:
  - Basic symbols and character classes
  - Alternation (a|b)
  - Kleene star (a*)
  - One or more (a+)
  - Zero or one (a?)
  - Grouping with parentheses
  - Character classes [...] (with ranges)
  - Dot operator (any character)

## Algorithmic Implementation

The application implements a complete regex to DFA pipeline:

1. **Thompson's Construction**: Converts regex to non-deterministic finite automaton (NFA) with epsilon transitions
2. **Subset Construction**: Converts the NFA to a DFA by computing epsilon closures
3. **Hopcroft's Algorithm**: Minimizes the DFA by combining equivalent states
4. **Graph Visualization**: Renders the minimal DFA as an interactive graph with custom styling

## Building and Running

### Prerequisites

- Java 11 or higher
- Maven

### Build

```
mvn clean package
```

This will create a JAR file with dependencies in the `target` directory.

### Run

```
java -jar target/java-dfa-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Usage

1. Enter a regular expression in the input field
2. Click "Visualize DFA"
3. The minimal DFA will be displayed as a directed graph:
   - Green states: Start states
   - Double-bordered states: Accept states
   - Colored states: Regular states with distinct colors for better visualization
   - Arrows: Transitions labeled with input symbols
   - Self-loops: Distinctively styled for better visibility
4. To test strings against the DFA:
   - Enter a test string in the "Test String" field
   - Click "Test"
   - A detailed simulation trace will show you how the DFA processes your string
   - The result (ACCEPTED/REJECTED) will be displayed

## Examples

The application can handle complex regular expressions such as:

- `a(b|c)*` - 'a' followed by zero or more occurrences of 'b' or 'c'
- `(a|b)*abb` - Any string of 'a's and 'b's ending with 'abb'
- `[a-z]+@[a-z]+\.(com|org|net)` - Simple email pattern
- `a*b?c+` - Zero or more 'a's, followed by optional 'b', followed by one or more 'c's

## Implementation Details

- **NFA class**: Represents NFAs with epsilon transitions and provides operations for NFA composition
- **RegexParser**: Implements recursive descent parsing with Thompson's construction
- **SubsetConstruction**: Implements the subset construction algorithm for NFA to DFA conversion
- **DfaMinimizer**: Implements Hopcroft's algorithm for DFA minimization
- **DfaVisualizer**: Renders DFAs using JGraphT and JGraphX with custom styling and layouts

## Future Improvements

- Performance optimizations for large regular expressions
- Support for additional regex features like negated character classes
- Step-by-step visualization of the NFA/DFA conversion process
- Export of DFA as image or SVG 