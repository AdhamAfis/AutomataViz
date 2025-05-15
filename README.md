# Java DFA Visualizer

A comprehensive Java application that converts regular expressions to minimal Deterministic Finite Automata (DFA) and visualizes them as interactive graphs.

## Features

- Complete and accurate regex to DFA conversion
- Enhanced visualization of DFAs with:
  - Color-coded states for better readability
  - Improved layouts for different DFA sizes
  - Clean and modern styling with shadows and rounded states
  - Smart transition label simplification for complex automata
  - Enhanced loop visualization with distinct styling and positioning
  - Intuitive color scheme with consistent meanings across NFA and DFA
- Split view to visualize both NFA and DFA side by side
- Advanced visualization controls:
  - Zoom in/out with mouse wheel (Ctrl+scroll) or buttons
  - Pan around large automata by dragging
  - Reset view to original state
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
3. The automata will be displayed:
   - If "Show NFA/DFA Split View" is checked, you'll see both the NFA (left) and minimized DFA (right)
   - Green states: Start states
   - Orange states: Accept states
   - Light blue/purple states: Regular states
   - Grey arrows: Standard transitions labeled with input symbols
   - Blue self-loops: More prominently displayed with clear positioning
   - Red arrows (NFA only): Epsilon transitions
4. Navigate the visualization:
   - Zoom in/out using Ctrl+Mouse Wheel or the +/- buttons
   - Click the "🖐" (Pan Mode) button to enable drag-to-move navigation
   - When Pan Mode is active, click and drag anywhere to move the visualization
   - Alt+Drag or middle/right button drag also works for panning
   - Reset the view to original size with the Reset button
5. To test strings against the DFA:
   - Enter a test string in the "Test String" field
   - Click "Test"
   - A detailed simulation trace will show you how the DFA processes your string
   - The result (ACCEPTED/REJECTED) will be displayed

## Color and Visualization Guide

- **States**:
  - Green border: Start states
  - Orange fill: Accept states
  - Green border with orange fill: Both start and accept states
  - Blue/purple fill: Regular states
  
- **Transitions**:
  - Blue arrows with clear looping: Self-loops (when a state transitions to itself)
  - Red dashed arrows (NFA only): Epsilon transitions
  - Grey arrows: Normal transitions between different states
  
- **Tooltips**:
  - Hover over the visualization for information about states and transitions

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
- **NfaVisualizer**: Renders NFAs with improved loop visualization and consistent color scheme

## Recent Improvements

- Enhanced loop visualization with distinctive blue styling and improved positioning
- Consistent color scheme across NFA and DFA with intuitive meaning
- Special styling for epsilon transitions in NFA view
- Tooltips with valuable state and transition information
- Optimized edge label positioning for better readability
- Enhanced visual distinction between different types of states and transitions

## Future Improvements

- Performance optimizations for large regular expressions
- Support for additional regex features like negated character classes
- Step-by-step visualization of the NFA/DFA conversion process
- Improved layout algorithms for complex automata
- Animation of string acceptance testing
- Dark mode support
- Ability to save/load automata configurations
- Export of DFA as image or SVG 