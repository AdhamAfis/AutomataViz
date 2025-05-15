# Project Structure Documentation

## Overview

The AutomataViz application follows a clean separation of concerns with the following packages:

### Main Components

1. **Algorithm Package** (`com.dfavisualizer.algorithm`)
   - Contains all algorithms related to automata theory
   - Includes parsers, converters, and minimization algorithms
   - Examples: `RegexParser`, `DfaMinimizer`, `SubsetConstruction`

2. **Model Package** (`com.dfavisualizer.model`)
   - Contains domain objects that represent the core entities
   - Examples: `DFA`, `NFA`, `OptimizedNFA`

3. **Controller Package** (`com.dfavisualizer.controller`)
   - Manages the application flow and logic
   - Coordinates between UI and model components
   - Examples: `AnimationController`, `VisualizationController`

4. **UI Package** (`com.dfavisualizer.ui`)
   - Contains all user interface components
   - Subpackages:
     - `components`: Reusable UI elements
     - `dialogs`: Dialog windows for user interaction
   - Main application entry point (`MainApp.java`)

5. **Utility Package** (`com.dfavisualizer.util`)
   - Contains helper classes and utilities used across the application

### Resources

- **Main Resources** (`src/main/resources`)
   - Application icons, images, and configuration files
   - CSS files for styling (if applicable)

- **Test Resources** (`src/test/resources`)
   - Test data files and mock configurations

### Test Structure

The test directory mirrors the main application structure:
- `src/test/java/com/dfavisualizer/algorithm/` - For algorithm tests
- `src/test/java/com/dfavisualizer/model/` - For model tests
- `src/test/java/com/dfavisualizer/controller/` - For controller tests

## Best Practices

1. **Package organization**: Each package should have a clear responsibility
2. **Package-info.java**: Each package includes documentation through package-info.java
3. **Resources**: Resource files should be placed in the appropriate resources directory
4. **Testing**: Test classes should follow the same structure as the main code
5. **Documentation**: Code should be well-documented with Javadoc comments

## Build System

Maven is used for dependency management and build automation. The main configuration file (`pom.xml`) defines:
- Project coordinates
- Dependencies
- Build plugins
- Resource directories
- Repository configurations 