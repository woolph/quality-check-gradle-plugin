/* Copyright 2023-2026 ENGEL Austria GmbH */
package io.github.woolph.gradle

import java.io.File
import java.util.stream.Stream
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class QualityCheckPluginTests {
  companion object {
    @JvmStatic fun supportedGradleVersions(): Stream<String> = SUPPORTED_GRADLE_VERSIONS.stream()

    @JvmStatic
    val SUPPORTED_GRADLE_VERSIONS =
        listOf(
            "9.5.1",
            "8.14.5",
        )
  }

  @TempDir lateinit var testProjectDir: File
  lateinit var settingsFile: File
  lateinit var buildFile: File
  lateinit var gradleProperties: File

  @BeforeEach
  fun setup() {
    settingsFile = File(testProjectDir, "settings.gradle")
    buildFile = File(testProjectDir, "build.gradle")
    gradleProperties = File(testProjectDir, "gradle.properties")
  }

  @ParameterizedTest
  @MethodSource("supportedGradleVersions")
  fun `project without check or test task`(gradleVersion: String) {
    gradleProperties.writeText(
        """
        org.gradle.configuration-cache=true
        """
            .trimIndent(),
    )
    settingsFile.writeText(
        """
        rootProject.name = 'dependency-check-skip'
        """
            .trimIndent(),
    )
    buildFile.writeText(
        """
        plugins {
            id 'io.github.woolph.quality-check'
        }

        group = 'io.github.woolph.test'

        qualityCheck {
            dependencyCheck {
                skip = true
            }
            licenseCheck {
                skip = true
            }
        }
        """
            .trimIndent(),
    )

    val result: BuildResult =
        GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .buildAndFail()

    println(result.output)
    assertTrue(
        result.output.contains(
            "Task with name 'check' not found in root project 'dependency-check-skip'."
        )
    )
  }

  @ParameterizedTest
  @MethodSource("supportedGradleVersions")
  fun `set dependency check to skip`(gradleVersion: String) {
    settingsFile.writeText(
        """
        rootProject.name = 'dependency-check-skip'
        """
            .trimIndent(),
    )
    buildFile.writeText(
        """
        plugins {
            id 'java'
            id 'io.github.woolph.quality-check'
        }

        group = 'io.github.woolph.test'

        qualityCheck {
            dependencyCheck {
                skip = true
            }
            licenseCheck {
                skip = true
            }
        }
        """
            .trimIndent(),
    )

    val result: BuildResult =
        GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir)
            .withArguments("check")
            .withPluginClasspath()
            .build()

    assertTrue(result.output.contains("licenseCheck is disabled!"))
    assertTrue(result.output.contains("dependencyCheck is disabled!"))

    assertEquals(TaskOutcome.UP_TO_DATE, result.task(":check")?.outcome)
    assertEquals(TaskOutcome.SKIPPED, result.task(":checkVulnerabilities")?.outcome)
    assertEquals(TaskOutcome.SKIPPED, result.task(":checkLicenses")?.outcome)
    assertEquals(TaskOutcome.NO_SOURCE, result.task(":test")?.outcome)
    println(result.task(":dependencyCheckAnalyze")?.path)
  }

  @ParameterizedTest
  @MethodSource("supportedGradleVersions")
  fun `licenseCheck succeeds for allowed license`(gradleVersion: String) {
    settingsFile.writeText(
        """
        rootProject.name = 'licenseCheckOnly'
        """
            .trimIndent(),
    )
    buildFile.writeText(
        """
        plugins {
            id 'java'
            id 'io.github.woolph.quality-check'
        }

        group = 'io.github.woolph.test'

        repositories {
            mavenCentral()
        }

        dependencies {
            implementation("javax.annotation:javax.annotation-api:1.3.2")
        }

        qualityCheck {
            dependencyCheck {
                skip = true
            }
        }
        """
            .trimIndent(),
    )

    val result: BuildResult =
        GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir)
            .withArguments("check")
            .withPluginClasspath()
            .build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":check")?.outcome)
    assertEquals(TaskOutcome.SUCCESS, result.task(":checkLicenses")?.outcome)
    assertEquals(TaskOutcome.SKIPPED, result.task(":checkVulnerabilities")?.outcome)
    assertEquals(TaskOutcome.NO_SOURCE, result.task(":test")?.outcome)
    println(result.task(":dependencyCheckAnalyze")?.path)
  }

  @ParameterizedTest
  @MethodSource("supportedGradleVersions")
  fun `licenseCheck fails for not allowed license`(gradleVersion: String) {
    settingsFile.writeText(
        """
        rootProject.name = 'licenseCheckOnly'
        """
            .trimIndent(),
    )
    buildFile.writeText(
        """
        plugins {
            id 'java'
            id 'io.github.woolph.quality-check'
        }

        group = 'io.github.woolph.test'

        repositories {
            mavenCentral()
        }

        dependencies {
            implementation("javax.annotation:javax.annotation-api:1.3.2")
        }

        qualityCheck {
            dependencyCheck {
                skip = true
            }
            licenseCheck {
                allowedLicenses = ["MIT-0"]
            }
        }
        """
            .trimIndent(),
    )

    val result: BuildResult =
        GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir)
            .withArguments("check")
            .withPluginClasspath()
            .buildAndFail()

    assertEquals(TaskOutcome.FAILED, result.task(":checkLicenses")?.outcome)
  }

  // TODO also test kotlin dsl

  // TODO test for dependencyCheck
}
