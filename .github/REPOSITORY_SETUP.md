# Repository settings

The baseline default-branch ruleset is stored in [rulesets/main.json](rulesets/main.json). It blocks deletion and force pushes, requires pull requests, and requires resolved review conversations. Zero mandatory approvals keeps the repository usable by its single maintainer. No bypass actor is configured.

The live repository has this baseline ruleset and the settings below configured. The configuration files themselves still need to be committed and pushed. Pages uses Actions with HTTPS and a deployment policy allowing the `main` branch only; no website deployment has run yet.

## Required CI check

After the updated Pages workflow is pushed and has reported its first successful check, add **Build and verify** as a required status check in the default-branch ruleset. Select GitHub Actions as its source and require the branch to be up to date. Do not require the deployment job: it intentionally does not run on pull requests.

Do not add a check that has not run yet. Do not restore path filters to the required workflow: a skipped workflow can leave a PR permanently waiting for its required status. Keep required CI available for documentation and configuration changes as well as source changes.

## Settings to maintain

- Default branch: `main`.
- Automatically delete merged feature branches and allow updating PR branches.
- Squash commit titles default to the pull request title; other merge methods remain available.
- Default Actions token permissions: read-only. Workflows explicitly request writes only for Pages deployment or release publishing. Actions must not approve pull requests.
- Enable Dependabot alerts and security updates, secret scanning, push protection, and private vulnerability reporting where available.
- Pages source: GitHub Actions. Restrict the `github-pages` deployment environment to the default branch.

Weekly Maven and Actions dependency-update configuration is in `dependabot.yml`. Issue forms, the PR template, CODEOWNERS, CONTRIBUTING.md, and SECURITY.md take effect after they are committed and pushed.

No license has been selected here. The maintainer must choose one before advertising the repository as licensed open-source software.
