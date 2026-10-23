# AutomataViz website

Static HTML, CSS, and JavaScript, deployed from this repository with GitHub Pages. No frontend dependencies or installation step are needed. The tag-based desktop release workflow remains separate.

## Local workflow

Requirements: Java 11+, Maven, Node.js 22+, and Python 3.9+.

From the repository root:

```sh
mvn clean package
npm --prefix website run verify
npm --prefix website run preview
```

Open http://127.0.0.1:4173/. Stop any previous server on that port first. Preview serves `website/build`, the same directory uploaded to Pages.

Edit `dist/index.html`, `dist/styles.css`, and `dist/site.js`. After a website edit, rerun `npm --prefix website run verify`, then refresh the browser. After Java changes, rerun Maven first. Never edit generated files in `build`.

The build copies the dependency-bundled JAR from `target` into `build/downloads/AutomataViz.jar` and updates its displayed SHA-256 and size. Generated JARs and `build` are ignored by Git. The source page's download metadata is only a local fallback; the built page always derives it from the actual JAR.

Verification runs the gallery test, validator tests, JavaScript syntax checking, and checks all local HTML links, anchors, gallery image paths, download size, and checksum. URLs must be project-relative so they work beneath `/AutomataViz/`. External sites are not contacted by the checker. CI also explicitly runs `LearningRegexCheck` and `RegexAlternativesCheck` with assertions enabled in headless mode. Maven compiles the Swing check harnesses, but these require separate desktop runs; `mvn package` does not exercise the desktop UI.

## GitHub Pages setup

1. Commit the website and `.github/workflows/pages.yml` together with the app source it builds. `website` is a regular directory in the main repository, not a submodule or separate repository.
2. In the repository's **Settings → Pages → Build and deployment**, choose **GitHub Actions** as the source.
3. In the `github-pages` environment, allow deployment from the repository's default branch only.
4. Merge to the default branch, or run **Website checks and GitHub Pages** manually on that branch.

All pull requests and branch pushes run the build and checks, including documentation-only changes so a required CI status never remains pending due to a path filter. They do not publish. Successful default-branch runs publish only `website/build`, using GitHub's Pages artifact/deploy actions and the `github-pages` environment. No personal token, external hosting account, or `gh-pages` branch is required. The deployment URL appears in the Actions run's environment link. The usual project URL is `https://adhamafis.github.io/AutomataViz/` unless a custom domain is configured.

This is a development snapshot download built from the same commit as the website, not a stable release. Tagged desktop releases continue through `.github/workflows/release.yml`. Re-run an earlier successful Pages workflow to restore its source revision; this rebuilds its JAR rather than retrieving a permanently retained binary.

## Screenshots

The gallery uses original macOS PNGs supplied by the project owner. Preserve transparency and keep image dimensions, descriptions, and captions in `dist/site.js` synchronized. Run verification after changing images; then inspect desktop and mobile layouts locally. Automated structural checks do not replace visual review.
