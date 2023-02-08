package io.github.woolph.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class QualityCheckPluginTests {
    @TempDir
    lateinit var testProjectDir: File
    lateinit var settingsFile: File
    lateinit var buildFile: File

    @BeforeEach
    fun setup() {
        settingsFile = File(testProjectDir, "settings.gradle")
        buildFile = File(testProjectDir, "build.gradle")
    }

    @Test
    fun `project without test task`() {
        settingsFile.writeText("""
            rootProject.name = 'dependency-check-skip'
        """.trimIndent())
        buildFile.writeText("""
            plugins {
                id 'io.github.woolph.quality-check'
            }
            
            qualityCheck {
                dependencyCheck {
                    skip = true
                }
                sonarQube {
                    skip = true
                }
            }
        """.trimIndent())

        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("dependencyCheck will not be applied due to exception"))
        assertTrue(result.output.contains("sonarqube will not be applied due to exception"))
    }

    @Test
    fun `set dependency check to skip`() {
        settingsFile.writeText("""
            rootProject.name = 'dependency-check-skip'
        """.trimIndent())
        buildFile.writeText("""
            plugins {
                id 'java'
                id 'io.github.woolph.quality-check'
            }
            
            qualityCheck {
                dependencyCheck {
                    skip = true
                }
                sonarQube {
                    skip = true
                }
            }
        """.trimIndent())

        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("check")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("dependencyCheck is disabled!"))
        assertTrue(result.output.contains("sonarqube is disabled!"))

        assertEquals(TaskOutcome.SUCCESS, result.task(":check")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":dependencyCheckAnalyze")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":sonar")?.outcome)
        assertEquals(TaskOutcome.SKIPPED, result.task(":jacocoTestReport")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, result.task(":test")?.outcome)
        println(result.task(":jacocoTestReport")?.path)
        println(result.task(":dependencyCheckAnalyze")?.path)

    }

    @Test
    fun `set sonarqube edition to community for non pull request`() {
        settingsFile.writeText("""
            rootProject.name = 'dependency-check-skip'
        """.trimIndent())
        buildFile.writeText("""
            plugins {
                id 'java'
                id 'io.github.woolph.quality-check'
            }
            
            qualityCheck {
                dependencyCheck {
                    skip = true
                }
            }
        """.trimIndent())

        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("check", "--stacktrace", "-Psonarqube.edition=community")
            .withEnvironment(mapOf("BUILD_REASON" to "Scheduled"))
            .withPluginClasspath()
            .buildAndFail()

        assertFalse(result.output.contains("sonarqube is running on Community Edition and build reason is PullRequest => skipping sonarqube!"))

//        assertEquals(TaskOutcome.SUCCESS, result.task(":check")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":dependencyCheckAnalyze")?.outcome)
        assertEquals(TaskOutcome.FAILED, result.task(":sonar")?.outcome)
        assertEquals(TaskOutcome.SKIPPED, result.task(":jacocoTestReport")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, result.task(":test")?.outcome)
    }

    @Test
    fun `set sonarqube edition to community for pull request`() {
        settingsFile.writeText("""
            rootProject.name = 'dependency-check-skip'
        """.trimIndent())
        buildFile.writeText("""
            plugins {
                id 'java'
                id 'io.github.woolph.quality-check'
            }
            
            qualityCheck {
                dependencyCheck {
                    skip = true
                }
            }
        """.trimIndent())

        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("check", "--stacktrace", "-Psonarqube.edition=community")
            .withEnvironment(mapOf("BUILD_REASON" to "PullRequest"))
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("sonarqube is running on Community Edition and build reason is PullRequest => skipping sonarqube!"))

        assertEquals(TaskOutcome.SUCCESS, result.task(":check")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":dependencyCheckAnalyze")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":sonar")?.outcome)
        assertEquals(TaskOutcome.SKIPPED, result.task(":jacocoTestReport")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, result.task(":test")?.outcome)
        println(result.task(":jacocoTestReport")?.path)
        println(result.task(":dependencyCheckAnalyze")?.path)
    }
}
