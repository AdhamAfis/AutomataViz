# Testing

See [Contributing](../CONTRIBUTING.md) for the checks required before a pull request.

## Visual regression checks

After `mvn package`, run these assertion-based Swing checks on a machine with a desktop session:

```sh
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.VisualRegressionCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.UiInteractionCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.GraphNavigationCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.GraphDrawingCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.PlaybackCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.TextShortcutCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.GuidedPlaybackCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.NfaLearningCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.LearningWorkspaceCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.GraphControlsCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.GraphThemeCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.ThemeSwitchCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.StepResponsivenessCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.LearningReliabilityCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.LearningDialogCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.ReconnectionCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.ReconnectionInteractionCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.algorithm.LearningRegexCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.algorithm.AutomatonToRegexCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.algorithm.RegexSimplificationCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.algorithm.PreferredRegexCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.algorithm.RegexOptimizationCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.algorithm.GraphExampleRegexCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.algorithm.RegexAlternativesCheck
java -ea -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.RegexAlternativesDialogCheck
```

On Windows, replace the classpath separator `:` with `;`. The checks cover repeated generation, split-view restoration, keyboard visualization, resizable results, graph visibility, label readability, and control clipping at three window sizes and divider limits. Rendered windows are written to `target/visual-checks/`. These checks run separately from Maven's test phase because they require a desktop session. Synthetic pan and zoom gestures are checked in the main workspace and step-by-step graph tabs. Playback checks cover stepping, pause/resume, restart, self-loops, whitespace, empty input, rejection, grouped symbols, and cancellation when replacing graphs. The guided-learning checks also cover backward stepping after completion, empty-input safety, early dead-state rejection, exact subset mappings, ε-closure without input consumption, expression-source highlighting, single-view/comparison switching, and visible controls and active states with the email example at 900×650 and 1200×820. Physical mouse/trackpad gestures and the export file chooser still need manual checks.

The scrollbar appearance check can also run without a desktop session:

```sh
java -ea -Djava.awt.headless=true -cp target/test-classes:target/AutomataViz-1.1.0-jar-with-dependencies.jar com.dfavisualizer.ui.ScrollbarAppearanceCheck
```

It checks thumb contrast in both orientations, including hover and pressed states, at 1× and 2× scale.

