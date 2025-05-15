# AutomataViz

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
  - Export visualizations as PNG images
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

## Project Structure

The project follows a standard Maven structure with the following organization:

```
java-dfa/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/dfavisualizer/
│   │   │       ├── algorithm/   # Algorithm implementations for automata
│   │   │       ├── controller/  # Controllers for application logic
│   │   │       ├── model/       # Domain models (DFA, NFA)
│   │   │       ├── ui/          # User interface components
│   │   │       │   ├── components/ # Reusable UI components
│   │   │       │   └── dialogs/    # Dialog windows
│   │   │       └── util/        # Utility classes
│   │   └── resources/           # Application resources (images, config)
│   └── test/
│       ├── java/                # Test classes mirroring main structure
│       └── resources/           # Test resources
└── docs/                        # Documentation
```

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
java -jar target/AutomataViz-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Or run directly with Maven:

```
mvn exec:java -Dexec.mainClass="com.dfavisualizer.ui.MainApp"
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
5. Export the visualization:
   - Click the "📷" (Export) button to save the current visualization as a PNG image
   - Choose a location and filename in the save dialog
   - The status bar will show the path where the image was saved
6. To test strings against the DFA:
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

## Understanding Graph Visualization

The visualization of automata in this application uses directed graphs to represent both NFAs and DFAs, with special attention to making the graphs clear and intuitive.

### Graph Structure

- **States (Nodes)**: Each state in the automaton is represented as a circular node labeled with its name (e.g., "q0", "q1", etc.)
- **Transitions (Edges)**: Transitions between states are shown as directed arrows labeled with the input symbols that trigger the transition

### Layout Algorithms

The application automatically selects the best layout algorithm based on the size and complexity of the automaton:

1. **Circle Layout**: For small automata (≤5 states), states are arranged in a circle for a compact representation
2. **Hierarchical Layout**: For medium and large automata (>5 states), states are organized in a left-to-right or top-to-bottom flow, with the start state typically at the left/top and accept states toward the right/bottom

### Special Visualizations

- **Self-Loops**: When a state transitions to itself (common with Kleene star operations like `a*`), the loop is drawn with a distinctive blue color and positioned with clear spacing to make it more visible
- **Multi-Symbol Transitions**: When multiple symbols can trigger the same transition, they are grouped together (e.g., "a,b,c" or "a-z" for ranges)
- **Epsilon Transitions (NFA only)**: Empty transitions are shown as red dashed lines labeled with "ε"

### Layout Optimization

For complex automata:
- Edge crossings are minimized where possible
- State spacing is optimized for readability
- Similar transitions are grouped and labeled efficiently
- Long labels for transitions with many symbols are simplified to ranges or counts

### Interactive Features

- **Zoom**: Use the zoom controls to focus on specific parts of larger automata
- **Pan**: Drag the visualization to move around when viewing large automata
- **Reset View**: Return to the original view with the Reset button
- **Tooltips**: Hover over states and transitions to see additional information

### Visual Distinction

The clear visual distinction between different types of states and transitions helps highlight the structure and behavior of the automaton:
- Start and accept states are immediately identifiable by their distinctive colors
- Self-loops are visually emphasized to highlight repetition patterns
- Epsilon transitions in NFAs stand out with their red dashed appearance

## Visualization Examples

*This section will include screenshots demonstrating various visualizations:*

1. **Simple DFA Example**: A visualization of a small DFA for a simple regex like `a(b|c)*`
2. **NFA and DFA Comparison**: Side-by-side visualization showing an NFA and its equivalent DFA
3. **Complex Automaton**: Visualization of a larger automaton with many states and transitions
4. **Loop Visualization**: Close-up example of how loops are displayed
5. **Epsilon Transitions**: Example showing epsilon transitions in an NFA

*Screenshots will be added in a future update.*

## Examples

The application can handle complex regular expressions such as:

- `a(b|c)*` - 'a' followed by zero or more occurrences of 'b' or 'c'
- `(a|b)*abb` - Any string of 'a's and 'b's ending with 'abb'
- `[a-z]+@[a-z]+\.(com|org|net)` - Simple email pattern
- `a*b?c+` - Zero or more 'a's, followed by optional 'b', followed by one or more 'c's

## Implementation Details

The application is organized into a structured package hierarchy following a modern architecture:

- **com.dfavisualizer.model**: Contains the domain model classes (DFA, NFA, OptimizedNFA)
- **com.dfavisualizer.algorithm**: Houses algorithms for regex parsing, automata construction, and minimization
- **com.dfavisualizer.ui**: Holds UI components including visualizers and the main application
- **com.dfavisualizer.controller**: Contains controllers that coordinate between UI and model layers
- **com.dfavisualizer.util**: Provides utility classes used across the application

### Core Components

- **NFA class**: Represents NFAs with epsilon transitions and provides operations for NFA composition
- **RegexParser**: Implements recursive descent parsing with Thompson's construction
- **SubsetConstruction**: Implements the subset construction algorithm for NFA to DFA conversion
- **DfaMinimizer**: Implements Hopcroft's algorithm for DFA minimization
- **DfaVisualizer**: Renders DFAs using JGraphT and JGraphX with custom styling and layouts
- **NfaVisualizer**: Renders NFAs with improved loop visualization and consistent color scheme

## Technical Graph Implementation

The visualization components leverage several libraries and techniques to create clear, interactive representations of the automata:

### Libraries and Technologies

- **JGraphT**: Used for the underlying graph data structure
- **JGraphX (mxGraph)**: Provides the visualization and rendering capabilities
- **Swing**: Used for the UI components and integration

### Graph Construction Process

1. **Data Structure Conversion**: The NFA/DFA is converted to a JGraphT `DefaultDirectedGraph` with custom `LabeledEdge` objects
2. **Edge Aggregation**: Multiple transitions between the same states are combined with intelligent labeling
3. **Range Detection**: Consecutive character transitions are automatically detected and displayed as ranges
4. **Adapter Creation**: A `JGraphXAdapter` bridges between the JGraphT graph and the mxGraph visualization

### Style Implementation

- **mxStylesheet**: Custom style sheets define the visual appearance of states and transitions
- **Style Mapping**: State types (start, accept, regular) are mapped to appropriate styles
- **Edge Style Selection**: Different edge types (normal, loop, epsilon) use distinct style configurations

### Loop Visualization Enhancement

Self-loops are specially handled through multiple techniques:
- **Detection**: Self-loops are identified during graph construction
- **Custom Geometry**: Special offset points ensure loops are clearly visible
- **Entity Relation Style**: The `entityRelation` edge style creates clearer loop renderings
- **Color Coding**: A distinctive blue color makes loops immediately identifiable

### Layout Selection Logic

The application uses the following logic to determine the best layout:
- For graphs with ≤5 states: Circle layout with optimized radius
- For graphs with 6-10 states: Hierarchical layout with medium spacing
- For graphs with >10 states: Hierarchical layout with larger spacing and orientation optimizations

### Transition Label Optimization

- For transitions with ≤5 symbols: Display all symbols separated by commas
- For transitions with >5 symbols: Detect and display character ranges when possible (e.g., "a-z")
- For very large transitions: Display symbol count rather than all symbols "[35 symbols]"

## Recent Improvements

- Enhanced loop visualization with distinctive blue styling and improved positioning
- Consistent color scheme across NFA and DFA with intuitive meaning
- Special styling for epsilon transitions in NFA view
- Tooltips with valuable state and transition information
- Optimized edge label positioning for better readability
- Enhanced visual distinction between different types of states and transitions
- Added ability to export automata visualization as PNG images

## Future Improvements

- Performance optimizations for large regular expressions
- Support for additional regex features like negated character classes
- Step-by-step visualization of the NFA/DFA conversion process
- Improved layout algorithms for complex automata
- Animation of string acceptance testing
- Dark mode support
- Ability to save/load automata configurations
- Export to additional formats (SVG, PDF)
