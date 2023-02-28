/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

class DependencyCheckTests {
    companion object {
        @JvmStatic
        fun supportedGradleVersions(): Stream<String> = QualityCheckPluginTests.SUPPORTED_GRADLE_VERSIONS.stream()
    }

    @TempDir
    lateinit var testProjectDir: File
    val settingsFile: File get() = File(testProjectDir, "settings.gradle")
    val buildFile: File get() = File(testProjectDir, "build.gradle")
    val dependencyCheckSuppression: File get() = File(testProjectDir, "dependency-check-suppression.xml")

    infix fun File.hasContent(content: String) {
        writeText(content)
    }

    fun File.doesNotExist() {
        delete()
    }

    @ParameterizedTest
    @MethodSource("supportedGradleVersions")
    fun `set dependency check to skip`(gradleVersion: String) {
        settingsFile hasContent """
            rootProject.name = 'dependency-check-skip'
            """

        buildFile hasContent """
            plugins {
                id 'java'
                id 'io.github.woolph.quality-check'
            }
            
            group = 'io.github.woolph.test'
            
            qualityCheck {
                dependencyCheck {
                    skip = true
                }
                sonarQube {
                    skip = true
                }
                licenseCheck {
                    skip = true
                }
            }
            """

        dependencyCheckSuppression.doesNotExist()

        val result: BuildResult = GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir)
            .withArguments("check")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("dependencyCheck is disabled!"))

        assertEquals(TaskOutcome.UP_TO_DATE, result.task(":check")?.outcome)
        assertEquals(TaskOutcome.SKIPPED, result.task(":dependencyCheckAnalyze")?.outcome)
        assertEquals(TaskOutcome.SKIPPED, result.task(":checkLicenses")?.outcome)
        assertEquals(TaskOutcome.SKIPPED, result.task(":sonar")?.outcome)
        assertEquals(TaskOutcome.SKIPPED, result.task(":jacocoTestReport")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, result.task(":test")?.outcome)
        println(result.task(":jacocoTestReport")?.path)
        println(result.task(":dependencyCheckAnalyze")?.path)
    }

    @ParameterizedTest
    @MethodSource("supportedGradleVersions")
    fun `when no supression xml exists then checkSuppressionFile task is skipped`(gradleVersion: String) {
        settingsFile hasContent """
            rootProject.name = 'dependency-check-skip'
            """

        buildFile hasContent """
            plugins {
                id 'java'
                id 'io.github.woolph.quality-check'
            }
            
            group = 'io.github.woolph.test'
            
            qualityCheck {
                sonarQube {
                    skip = true
                }
                licenseCheck {
                    skip = true
                }
            }
            """

        dependencyCheckSuppression.doesNotExist()

        val result: BuildResult = GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir)
            .withArguments("check")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":check")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":dependencyCheckAnalyze")?.outcome)
        assertEquals(TaskOutcome.SKIPPED, result.task(":checkSuppressionFile")?.outcome)
        assertEquals(TaskOutcome.SKIPPED, result.task(":checkLicenses")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, result.task(":test")?.outcome)
        println(result.task(":jacocoTestReport")?.path)
        println(result.task(":dependencyCheckAnalyze")?.path)
    }

    @ParameterizedTest
    @MethodSource("supportedGradleVersions")
    fun `when an empty supression xml exists then checkSuppressionFile task is successful`(gradleVersion: String) {
        settingsFile hasContent """
            rootProject.name = 'dependency-check-skip'
            """

        buildFile hasContent """
            plugins {
                id 'java'
                id 'io.github.woolph.quality-check'
            }
            
            group = 'io.github.woolph.test'
            
            qualityCheck {
                sonarQube {
                    skip = true
                }
                licenseCheck {
                    skip = true
                }
            }
            """

        dependencyCheckSuppression hasContent """<?xml version="1.0" encoding="UTF-8"?>
            <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
            </suppressions>
        """

        val result: BuildResult = GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir)
            .withArguments("check")
            .withPluginClasspath()
//            .withEnvironment(
//                listOf(
//                    "ORG_GRADLE_PROJECT_DEPENDENCY_CHECK_DB_DRIVER",
//                    "ORG_GRADLE_PROJECT_DEPENDENCY_CHECK_DB_CONNECTION",
//                    "ORG_GRADLE_PROJECT_DEPENDENCY_CHECK_DB_USER",
//                    "ORG_GRADLE_PROJECT_DEPENDENCY_CHECK_DB_PASSWORD",
//                ).associateWith { System.getenv(it) },
//            )
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":check")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":dependencyCheckAnalyze")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkSuppressionFile")?.outcome)
        assertEquals(TaskOutcome.SKIPPED, result.task(":checkLicenses")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, result.task(":test")?.outcome)
        println(result.task(":jacocoTestReport")?.path)
        println(result.task(":dependencyCheckAnalyze")?.path)
    }

    @org.junit.jupiter.api.Disabled("not yet finished")
    @ParameterizedTest
    @MethodSource("supportedGradleVersions")
    fun `using mirror database`(gradleVersion: String) {
        settingsFile hasContent """
            rootProject.name = 'dependency-check-skip'
            """

        buildFile hasContent """
            plugins {
                id 'java'
                id 'io.github.woolph.quality-check'
            }
            
            group = 'io.github.woolph.test'
            
            qualityCheck {
                sonarQube {
                    skip = true
                }
                licenseCheck {
                    skip = true
                }
            }
            """

        dependencyCheckSuppression.doesNotExist()

        val result: BuildResult = GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(testProjectDir)
            .withArguments("check", "-PDEPENDENCY_CHECK_DB_PASSWORD=abc")
            .withPluginClasspath()
            .withEnvironment(
                mapOf(
                    "ORG_GRADLE_PROJECT_DEPENDENCY_CHECK_DB_DRIVER" to "h2",
                    "ORG_GRADLE_PROJECT_DEPENDENCY_CHECK_DB_CONNECTION" = "blabla",
                    "ORG_GRADLE_PROJECT_DEPENDENCY_CHECK_DB_USER" to "user",
                )
            )
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":check")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":dependencyCheckAnalyze")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkSuppressionFile")?.outcome)
        assertEquals(TaskOutcome.SKIPPED, result.task(":checkLicenses")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, result.task(":test")?.outcome)
        println(result.task(":jacocoTestReport")?.path)
        println(result.task(":dependencyCheckAnalyze")?.path)
    }

    // TODO test database feature (maybe by providing downsized h2 file based database in the test resources
    // TODO test for checkSuppressionFile
}
