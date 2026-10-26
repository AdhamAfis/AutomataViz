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

To publish a version:

1. Update the project version in `pom.xml` (for example `1.2.0`), the versioned JAR commands in the docs, and `CHANGELOG.md` in a pull request.
2. Merge it into `main`. **Build and Release** builds on Java 11 and runs the core algorithm checks, then creates `v1.2.0` at that verified commit.
3. The workflow publishes the dependency-bundled JAR, `SHA256SUMS.txt`, and `README.txt` with launch and checksum instructions. Release notes include the matching changelog section when present plus GitHub's generated changes. GitHub also supplies source archives.

`1.2.0-rc.1` produces a prerelease; `-SNAPSHOT` versions do not publish. Existing published releases are left unchanged. Existing tags must point to the checked-out commit and are never moved. Releases stay drafts until all assets have uploaded.

For a retry, use **Actions → Build and Release → Run workflow** on `main` if the release commit is still its tip, or select the existing version tag. You can also push a matching `v*` tag manually; the commit must already belong to `main`. A tag/version mismatch or a permissions/network failure stops the job. No personal access token is needed; the release job uses `contents: write` on `GITHUB_TOKEN`.

Tag creation and publication happen in the same workflow because [token-created pushes do not trigger another workflow](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow). Stable releases and the website's development snapshot remain separate artifacts.

Check release helpers locally with `python3 -m unittest discover -s scripts -p 'test_*.py'`.

Report security issues through the process in [SECURITY.md](SECURITY.md).
