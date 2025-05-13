/**
 * Provides classes for converting regular expressions to Deterministic Finite Automata (DFA)
 * and visualizing them as directed graphs.
 * 
 * <p>The main components are:</p>
 * <ul>
 *   <li>{@link com.dfavisualizer.Main} - Main application entry point with UI</li>
 *   <li>{@link com.dfavisualizer.DFA} - Represents a Deterministic Finite Automaton</li>
 *   <li>{@link com.dfavisualizer.NFA} - Represents a Non-deterministic Finite Automaton with epsilon transitions</li>
 *   <li>{@link com.dfavisualizer.RegexParser} - Parses regular expressions into NFAs using Thompson's construction</li>
 *   <li>{@link com.dfavisualizer.SubsetConstruction} - Converts NFAs to DFAs using subset construction algorithm</li>
 *   <li>{@link com.dfavisualizer.DfaMinimizer} - Minimizes DFAs using Hopcroft's algorithm</li>
 *   <li>{@link com.dfavisualizer.RegexToDfaConverter} - Orchestrates the conversion of regex to minimized DFA</li>
 *   <li>{@link com.dfavisualizer.DfaVisualizer} - Renders DFAs as interactive graphs</li>
 * </ul>
 * 
 * <p>The conversion process follows these steps:</p>
 * <ol>
 *   <li>Parse the regex and build an NFA using Thompson's construction</li>
 *   <li>Convert the NFA to a DFA using subset construction</li>
 *   <li>Minimize the DFA using Hopcroft's algorithm</li>
 *   <li>Visualize the resulting minimal DFA</li>
 * </ol>
 * 
 * <p>This implementation supports a wide range of regex operations: concatenation, alternation (|),
 * Kleene star (*), plus (+), question mark (?), character classes, and grouping with parentheses.</p>
 */
package com.dfavisualizer; 