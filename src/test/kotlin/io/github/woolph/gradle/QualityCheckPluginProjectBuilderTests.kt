/* Copyright 2026 ENGEL Austria GmbH */
package io.github.woolph.gradle

import io.github.woolph.gradle.dependencycheck.VulnerabilityCheckExtension
import io.github.woolph.gradle.licensecheck.LicenseCheckExtension
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * In-process unit tests using [ProjectBuilder]. Unlike the TestKit-based tests, these run inside
 * the same JVM (no Gradle daemon, no separate build), which makes them very fast. They are well
 * suited for verifying the plugin's *wiring* (extensions created, tasks registered, task
 * dependencies, convention defaults) that happens eagerly at apply-time.
 *
 * Note: `afterEvaluate` blocks (which configure the OWASP / license-report extensions) are NOT
 * triggered by [ProjectBuilder], so those parts remain covered by the TestKit functional tests.
 */
class QualityCheckPluginProjectBuilderTests {
  private lateinit var project: org.gradle.api.Project

  @BeforeEach
  fun setup() {
    project =
        ProjectBuilder.builder().build().also {
          it.group = "io.github.woolph.test.sub"
          // the plugin wires into the `check` task, which is provided by the java/base plugin
          it.plugins.apply("java")
          it.plugins.apply(QualityCheckPlugin::class.java)
        }
  }

  @Test
  fun `applying the plugin creates the qualityCheck extension`() {
    val qualityCheck = project.extensions.findByName("qualityCheck")
    assertNotNull(qualityCheck)
    assertTrue(qualityCheck is QualityCheckExtension)
  }

  @Test
  fun `applying the plugin creates the dependencyCheck and licenseCheck sub-extensions`() {
    val qualityCheck = project.extensions.getByName("qualityCheck") as ExtensionAware

    val dependencyCheck = qualityCheck.extensions.findByName("dependencyCheck")
    val licenseCheck = qualityCheck.extensions.findByName("licenseCheck")

    assertNotNull(dependencyCheck)
    assertNotNull(licenseCheck)
    assertTrue(dependencyCheck is VulnerabilityCheckExtension)
    assertTrue(licenseCheck is LicenseCheckExtension)
  }

  @Test
  fun `applying the plugin registers all dependency-check tasks`() {
    listOf(
            "checkVulnerabilities",
            "checkSuppressionFile",
            "generateSuppressionFile",
            "updateSuppressionFile",
        )
        .forEach { taskName ->
          assertNotNull(project.tasks.findByName(taskName), "task '$taskName' should be registered")
        }
  }

  @Test
  fun `applying the plugin registers all license-check tasks`() {
    listOf(
            "checkLicenses",
            "createLicenseBundleNormalizerConfig",
        )
        .forEach { taskName ->
          assertNotNull(project.tasks.findByName(taskName), "task '$taskName' should be registered")
        }
  }

  @Test
  fun `check task depends on checkVulnerabilities and checkLicenses`() {
    val check = project.tasks.getByName("check")

    val checkDependencyNames =
        check.taskDependencies.getDependencies(check).map(Task::getName).toSet()

    assertTrue(
        checkDependencyNames.containsAll(setOf("checkVulnerabilities", "checkLicenses")),
        "check should depend on checkVulnerabilities and checkLicenses, but depends on $checkDependencyNames",
    )
  }

  @Test
  fun `checkVulnerabilities depends on checkSuppressionFile`() {
    val checkVulnerabilities = project.tasks.getByName("checkVulnerabilities")

    val dependencyNames =
        checkVulnerabilities.taskDependencies
            .getDependencies(checkVulnerabilities)
            .map(Task::getName)
            .toSet()

    assertTrue(
        dependencyNames.contains("checkSuppressionFile"),
        "checkVulnerabilities should depend on checkSuppressionFile, but depends on $dependencyNames",
    )
  }

  @Test
  fun `dependencyCheck extension exposes the expected convention defaults`() {
    val dependencyCheck =
        (project.extensions.getByName("qualityCheck") as ExtensionAware)
            .extensions
            .getByName("dependencyCheck") as VulnerabilityCheckExtension

    assertFalse(dependencyCheck.skip.get(), "skip should default to false")
    // no BUILD_REASON=PullRequest in the test environment => threshold defaults to 11.0 (never
    // fail)
    assertEquals(11.0f, dependencyCheck.cvssThreshold.get())
    assertEquals(
        project.layout.projectDirectory.file("dependency-check-suppression.xml").asFile,
        dependencyCheck.suppressionFile.get().asFile,
    )
  }

  @Test
  fun `licenseCheck ownedDependencies default is derived from the first two group segments`() {
    val licenseCheck =
        (project.extensions.getByName("qualityCheck") as ExtensionAware)
            .extensions
            .getByName("licenseCheck") as LicenseCheckExtension

    assertFalse(licenseCheck.skip.get(), "skip should default to false")

    val ownedPatterns = licenseCheck.ownedDependencies.get().map { it.pattern }
    // group = "io.github.woolph.test.sub" => first two segments => "io.github"
    assertEquals(listOf("^\\Qio.github\\E(\\.)?.*"), ownedPatterns)
  }
}
