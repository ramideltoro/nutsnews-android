# T59 dependency and security automation

NutsNews protects source and dependency changes with three complementary
controls.

## Automated updates and pull-request review

Dependabot checks Gradle and GitHub Actions every Monday. Minor and patch
updates are grouped by ecosystem and production/development scope so reviewable
pull requests are created without combining unrelated Android and workflow
changes.

Every pull request runs GitHub Dependency Review. It rejects newly introduced
known vulnerabilities at moderate severity or higher and performs license
review. The action consumes GitHub's dependency graph diff and does not receive
repository secrets.

## Code scanning

CodeQL analyzes Java and Kotlin on pull requests, pushes to `main`, and manual
runs. Its manual build starts from `clean`, compiles production and
instrumentation sources with JDK 17, and enforces strict Gradle dependency
verification before analysis is uploaded.

## Dependency integrity

`gradle/verification-metadata.xml` records SHA-256 checksums for every resolved
plugin and dependency needed by compile, lint, unit-test, instrumentation-test,
and APK tasks. Normal Gradle resolution fails closed when an artifact is
missing from the metadata or its bytes do not match.

To intentionally update dependencies:

1. review the upstream release and Dependabot dependency-review result;
2. run the relevant Gradle tasks with
   `--write-verification-metadata sha256`;
3. inspect the metadata diff for only the expected coordinates and artifacts;
4. rerun the full strict build and security workflow.

## Secret and artifact boundary

All workflow actions are pinned to 40-character immutable commit SHAs.
`validate-security-automation.sh` verifies those pins and extracts every
`actions/upload-artifact` path. Only repository build logs, Gradle
reports/results/outputs, and emulator diagnostics are accepted. Home
directories, environment files, signing material, credentials, and arbitrary
workspace paths therefore cannot be added to an artifact upload unnoticed.
