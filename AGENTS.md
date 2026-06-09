# AGENTS.md

This file provides guidance to AI code agents (like claude.ai/code) when working with code in this repository.

## What this project is

`quality-check` is a Gradle plugin (`io.github.woolph.quality-check`) that wraps and pre-configures two third-party plugins:

- **OWASP Dependency Check** (`org.owasp.dependencycheck`) — scans runtime dependencies for known CVEs
- **Gradle License Report** (`com.github.jk1.dependency-license-report`) — checks that all dependency licenses are on an allowlist

Applying the plugin wires both checks into the standard `check` task lifecycle automatically.

## Commands

```bash
# Build and run all checks
./gradlew build

# Run tests only
./gradlew test

# Run a single test class
./gradlew test --tests "io.github.woolph.gradle.DependencyCheckTests"

# Run a single test method
./gradlew test --tests "io.github.woolph.gradle.DependencyCheckTests.skip dependency check via cli"

# Apply code formatting (ktfmt via Spotless)
./gradlew spotlessApply

# Check formatting without applying
./gradlew spotlessCheck

# Update README.md version references to match current project version
./gradlew updateReadmeVersions

# Publish to Gradle Plugin Portal
./gradlew publishPlugins
```

## Architecture

### Plugin entry point

`QualityCheckPlugin` (`src/main/kotlin/io/github/woolph/gradle/QualityCheckPlugin.kt`) applies to a `Project` and creates one root extension `qualityCheck` (type `QualityCheckExtension`), then delegates to two companion-object functions to wire up each sub-feature.

### Extension hierarchy

```
qualityCheck (QualityCheckExtension : ExtensionAware)
├── dependencyCheck (VulnerabilityCheckExtension)
└── licenseCheck (LicenseCheckExtension)
```

Both sub-extensions implement `Skipable` (single `skip: Property<Boolean>`) and expose an `aggregatedSkip` provider that also checks a Gradle property (`skipDependencyCheck` / `skipLicenseCheck`) so checks can be skipped from the CLI without touching the build file.

### Dependency check sub-system (`dependencycheck/`)

The wiring lives in `VulnerabilityCheckExtension.Companion.applyVulnerabilityCheckExtension`. It:

1. Applies `org.owasp.dependencycheck` plugin.
2. Registers `checkSuppressionFile` (`CheckSuppressionFileTask`) — validates the suppression XML before analysis. Skipped if the file doesn't exist. Each suppression entry must have either a FALSE POSITIVE note or an expiration date ≤ 1 year away.
3. Registers `checkVulnerabilities` (`CheckVulnerabilities`) — calls `dependencyCheckAnalyze` internally, then parses the JUnit XML report to log which first-level dependency introduced each vulnerable transitive.
4. Registers `generateSuppressionFile` / `updateSuppressionFile` for suppression file maintenance.
5. Wires `check → checkVulnerabilities → checkSuppressionFile → dependencyCheckAnalyze`.

**cvssThreshold default logic**: Defaults to `0.0` when `BUILD_REASON=PullRequest` (fail on any CVE in PRs), otherwise `11.0` (never fail, just report). This allows reproducible non-PR builds.

**Mirror database**: Activated by Gradle property `DEPENDENCY_CHECK_DB_CONNECTION`. When set, `autoUpdate` is disabled and JDBC properties (`DEPENDENCY_CHECK_DB_DRIVER`, `DEPENDENCY_CHECK_DB_USER`, `DEPENDENCY_CHECK_DB_PASSWORD`) are forwarded to the OWASP plugin. Bundled JDBC drivers: `org.postgresql.Driver` and `com.microsoft.sqlserver.jdbc.SQLServerDriver`.

### License check sub-system (`licensecheck/`)

The wiring lives in `LicenseCheckExtension.Companion.applyLicenseCheckExtension`. It:

1. Applies `com.github.jk1.dependency-license-report` plugin.
2. Registers `checkLicenses` (`CheckLicenseTaskJunitReport`) as the authoritative license-checking task (the plugin's own `checkLicense` is kept but effectively disabled).
3. Registers `createLicenseBundleNormalizerConfig` to generate an additional normalizer bundle JSON at build time.
4. Wires `check → checkLicenses → generateLicenseReport`.

**Dependency filters applied to `generateLicenseReport`**:
- `WhiteListedDependencyFilter` — excludes dependencies matching `ownedDependencies` regexes (defaults to the project's own group prefix) and non-expired `whiteListedDependencies` entries.
- `MissingModuleDataDependencyFilter` — drops entries with no module data.
- `OnDemandBundleNormalizerFilter` — normalises license names via the generated bundle JSON.

**WhiteListedDependency DSL** (inside `licenseCheck { whiteListedDependencies { ... } }`):
```kotlin
add("com.example:some-dep")          // whitelisted forever
add(Regex("^com\\.example\\..*")) until "2025-12-31"  // expires
```

### Tests

Tests use **Gradle TestKit** (`GradleRunner`) with temporary project directories; each test writes its own `settings.gradle`, `build.gradle`, and optionally `gradle.properties` / `dependency-check-suppression.xml`. Tests are parameterised over all `SUPPORTED_GRADLE_VERSIONS` (currently `["9.5.1", "8.14.5"]`).

### Code style

Spotless enforces `ktfmt` formatting and a copyright header (`/* Copyright $YEAR ENGEL Austria GmbH */`) on every `.kt` file. Run `./gradlew spotlessApply` before committing to avoid CI failures.
