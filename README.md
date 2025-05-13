# Java DFA Visualizer

A comprehensive Java application that converts regular expressions to minimal Deterministic Finite Automata (DFA) and visualizes them as interactive graphs.

## Features

- Complete and accurate regex to DFA conversion
- Visualization of DFAs as interactive graphs
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
4. **Graph Visualization**: Renders the minimal DFA as an interactive graph

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
   - Double-outlined states: Accept states
   - Arrows: Transitions labeled with input symbols

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
- **DfaVisualizer**: Renders DFAs using JGraphT and JGraphX

## Future Improvements

- Performance optimizations for large regular expressions
- Support for additional regex features like negated character classes
- Interactive testing of strings against the generated DFA
- Step-by-step visualization of the NFA/DFA conversion process
- Export of DFA as image or SVG 