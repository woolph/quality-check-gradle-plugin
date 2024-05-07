/* Copyright 2023 ENGEL Austria GmbH */
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
        @JvmStatic
        fun supportedGradleVersions(): Stream<String> = SUPPORTED_GRADLE_VERSIONS.stream()

        @JvmStatic
        val SUPPORTED_GRADLE_VERSIONS =
            listOf(
                "8.7",
                "8.1.1",
                "7.6.4",
                "7.0",
            ) // TODO consider supporting 6.9.4 as well (but Licensecheck plugin does
        // need to be downgraded for this
    }

    @TempDir lateinit var testProjectDir: File
    lateinit var settingsFile: File
    lateinit var buildFile: File

    @BeforeEach
    fun setup() {
        settingsFile = File(testProjectDir, "settings.gradle")
        buildFile = File(testProjectDir, "build.gradle")
    }

    @ParameterizedTest
    @MethodSource("supportedGradleVersions")
    fun `project without test task`(gradleVersion: String) {
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
                .build()

        assertTrue(result.output.contains("licenseCheck will not be applied due to exception"))
        assertTrue(result.output.contains("dependencyCheck will not be applied due to exception"))
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
        assertEquals(TaskOutcome.SKIPPED, result.task(":dependencyCheckAnalyze")?.outcome)
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
        assertEquals(TaskOutcome.SKIPPED, result.task(":dependencyCheckAnalyze")?.outcome)
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
