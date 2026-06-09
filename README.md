# quality-check-gradle-plugin


[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-io.github.woolph.quality--check-blue.svg)](https://plugins.gradle.org/plugin/io.github.woolph.quality-check)
[![Changelog](https://img.shields.io/badge/changelog-3.1.0-blue.svg)](CHANGELOG.md)

`quality-check` is a Gradle plugin that wraps and pre-configures two third-party plugins to enforce dependency security
and license compliance:

* **[OWASP Dependency Check](https://jeremylong.github.io/DependencyCheck/)** — scans runtime dependencies for known CVEs (Common Vulnerabilities and Exposures)
* **[Gradle License Report](https://github.com/jk1/Gradle-License-Report)** — verifies that all dependency licenses are on an allowlist

Applying the plugin wires both checks into the standard `check` task automatically, so running `./gradlew build` is all
that is required.

## Quick setup

Add the plugin to your Gradle build:

````kotlin
plugins {
    id("io.github.woolph.quality-check") version "3.1.0"
}
````

That is all. Both checks run as part of `./gradlew check` (and therefore `./gradlew build`) without any further
configuration.

## Task wiring

The plugin registers the following task chain:

```
check
├── checkVulnerabilities         (parses the OWASP report and logs which direct dependency pulled in each CVE)
│   └── checkSuppressionFile     (validates the suppression XML before the scan runs)
│       └── dependencyCheckAnalyze
└── checkLicenses                (fails the build if any dependency license is not on the allowlist)
    └── generateLicenseReport
```

## Configuration

All options live inside a `qualityCheck {}` block:

````kotlin
qualityCheck {
    dependencyCheck {
        skip = true                          // disable the vulnerability check entirely
        cvssThreshold = 7.0f                 // fail only on CVSS score ≥ 7.0 (default: 0.0 on PRs, 11.0 otherwise)
        suppressionFile = file("dependency-check-suppression.xml")  // default location
    }
    licenseCheck {
        skip = true                          // disable the license check entirely
        allowedLicenses.add("MIT License")   // add a license to the allowlist
        allowedLicenses.set(setOf("Apache License, Version 2.0", "MIT License"))  // replace the whole allowlist

        whiteListedDependencies {
            add("com.example:some-dep")                            // skip license check for this dependency forever
            add(Regex("^com\\.example\\..*")) until "2026-12-31"  // skip until the given date, then re-evaluate
        }
    }
}
````

### Default allowed licenses

The following licenses are allowed by default:

* MIT License, MIT-0
* Apache License, Version 2.0
* BSD Zero Clause License, The 2-Clause BSD License, The 3-Clause BSD License
* GNU GENERAL PUBLIC LICENSE, Version 2 + Classpath Exception
* GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1 / v3.0
* Go License
* Indiana University Extreme! Lab Software License
* COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0
* Eclipse Public License - v 1.0 / v 2.0
* PUBLIC DOMAIN
* Bouncy Castle Licence

### Skipping checks from the command line

Both checks can be skipped without touching the build file:

````shell
./gradlew build -PskipDependencyCheck=true
./gradlew build -PskipLicenseCheck=true
````

### CVSS threshold and PR builds

The `cvssThreshold` controls at which CVSS score the build fails:

| Scenario | Default threshold | Effect |
|---|---|---|
| `BUILD_REASON=PullRequest` (CI) | `0.0` | Any CVE fails the build |
| All other builds | `11.0` | Build never fails (CVEs are reported only) |

Override by setting `cvssThreshold` explicitly in the `dependencyCheck {}` block.

## Dependency Check

### Suppression file

Vulnerabilities that are false positives or cannot be fixed immediately can be suppressed via
`dependency-check-suppression.xml` (default location: `{projectDir}/dependency-check-suppression.xml`).

The `checkSuppressionFile` task validates the suppression file before each scan. An entry is considered valid if it
meets **one** of these conditions:

* It has a note containing the text `FALSE POSITIVE`, indicating the vulnerability does not affect the application.
* It has an expiration date no more than one year in the future. Once the date passes, the entry is ignored, forcing
  you to re-evaluate the vulnerability.

#### Generating or updating the suppression file

After running `dependencyCheckAnalyze`, you can generate a new suppression file that removes obsolete entries and adds
entries for all newly found vulnerabilities:

````shell
./gradlew generateSuppressionFile -PsuppressUntil=2027-01-01
````

The result is written to `dependency-check-suppression.new.xml`. Review it, then replace the original file.

To update the existing file in place instead, use `updateSuppressionFile`.

#### Highlighting suppressed dependencies in the dependency tree

The following command prints the full dependency tree and highlights every dependency that currently has a suppression
entry. It works by extracting the `pkg:maven` coordinates from the suppression file and using them as a `grep` pattern:

````shell
./gradlew dependencies --configuration runtimeClasspath \
  | grep --color=auto -E "$(grep '<packageUrl>' dependency-check-suppression.xml \
      | sed 's_.*pkg:maven/\(.*\)/\(.*\)@.*_\1:\2_' \
      | sort -u | tr '\n' '|')\$"
````

### Using a mirror database

By default, `dependencyCheckAnalyze` downloads the NVD vulnerability data into the local Gradle cache and refreshes it
on each run. On ephemeral build agents the cache is lost between builds, causing a full NVD download every time.

To point the plugin at a shared JDBC database instead, set these Gradle properties (e.g. in `gradle.properties` or as
environment variables passed to the build):

| Property | Description |
|---|---|
| `DEPENDENCY_CHECK_DB_CONNECTION` | JDBC connection string — setting this activates mirror mode and disables `autoUpdate` |
| `DEPENDENCY_CHECK_DB_DRIVER` | Fully-qualified JDBC driver class name |
| `DEPENDENCY_CHECK_DB_USER` | Database username |
| `DEPENDENCY_CHECK_DB_PASSWORD` | Database password |

The following JDBC drivers are bundled with the plugin:

* `org.postgresql.Driver` (from `org.postgresql:postgresql:42.7.7`)
* `com.microsoft.sqlserver.jdbc.SQLServerDriver` (from `com.microsoft.sqlserver:mssql-jdbc:12.2.0.jre11`)

## License Check

Some open-source licenses (e.g. AGPL, GPL without Classpath Exception) require you to publish your own source code when
distributing software that links against them. The license check prevents unlicensed dependencies from silently entering
a closed-source project.

The `checkLicenses` task fails the build if any dependency's license is not in the `allowedLicenses` set. See the
[Configuration](#configuration) section above for how to extend the allowlist or whitelist individual dependencies.

Dependencies matching the project's own group prefix (derived from the first two segments of `project.group`) are
excluded automatically, so your own multi-module artifacts are never flagged.

## Gradle version support

This plugin supports Gradle versions starting with Gradle 8.14.5.

This plugin fully supports [Build Cache](https://docs.gradle.org/current/userguide/build_cache.html) and [Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html).
