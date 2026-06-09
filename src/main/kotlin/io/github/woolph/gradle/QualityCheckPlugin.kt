/* Copyright 2023-2026 ENGEL Austria GmbH */
package io.github.woolph.gradle

import io.github.woolph.gradle.dependencycheck.VulnerabilityCheckExtension
import io.github.woolph.gradle.dependencycheck.VulnerabilityCheckExtension.Companion.applyVulnerabilityCheckExtension
import io.github.woolph.gradle.licensecheck.LicenseCheckExtension
import io.github.woolph.gradle.licensecheck.LicenseCheckExtension.Companion.applyLicenseCheckExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

@Suppress("unused")
class QualityCheckPlugin : Plugin<Project> {
  private lateinit var qualityCheckExtension: QualityCheckExtension
  private lateinit var vulnerabilityCheckExtension: VulnerabilityCheckExtension
  private lateinit var licenseCheckExtension: LicenseCheckExtension

  override fun apply(project: Project) {
    project.run {
      /*
            plugins.apply("jacoco")

            val jacocoTestReport =
                tasks.named<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoTestReport") {
                  reports { xml.required.set(true) }
                }
            tasks.named<org.gradle.api.tasks.testing.Test>("test") { finalizedBy(jacocoTestReport) }
      */
      qualityCheckExtension = extensions.create("qualityCheck", QualityCheckExtension::class)
      applyVulnerabilityCheckExtension(qualityCheckExtension)
      applyLicenseCheckExtension(qualityCheckExtension)
    }
  }
}
