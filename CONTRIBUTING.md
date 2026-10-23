# Contributing

Open a bug report with reproduction steps or describe the learning problem behind an improvement. For regex issues, include the expression and input in fenced code blocks so formatting preserves every operator and backslash.

Create a focused branch and open a pull request against `main`. Describe the problem, the resulting behavior, and validation. Include before/after screenshots for visual changes. Do not commit generated JARs, `target/`, `website/build/`, local credentials, or browser automation logs.

## Checks

From the repository root:

```sh
mvn clean package
java -ea -Djava.awt.headless=true -cp 'target/test-classes:target/*' com.dfavisualizer.algorithm.LearningRegexCheck
java -ea -Djava.awt.headless=true -cp 'target/test-classes:target/*' com.dfavisualizer.algorithm.RegexAlternativesCheck
npm --prefix website run verify
```

On Windows, use `;` instead of `:` in Java classpaths. These algorithm checks do not replace the desktop interaction harnesses described in the [testing guide](docs/TESTING.md). Run the relevant Swing checks after UI changes; report what remains unverified. For website presentation changes, inspect desktop and mobile sizes and preserve screenshot transparency.

The required-check candidate is **Build and verify**. It runs on every pull request, including documentation-only changes. Deployment runs only on the default branch after those checks pass. See [website instructions](website/README.md) for preview and Pages setup.

## Reviews and releases

Resolve review discussions before merging. The repository supports a single maintainer, so an additional approval is not mandatory. CODEOWNERS identifies the maintainer without making self-approval necessary. Do not force-push or delete `main`.

Tags beginning with `v` trigger the desktop release workflow. Create release tags only after validating the intended commit. Hyphenated version tags are published as prereleases. Stable releases and the website's development snapshot are separate artifacts.

Report security issues through the process in [SECURITY.md](SECURITY.md).
