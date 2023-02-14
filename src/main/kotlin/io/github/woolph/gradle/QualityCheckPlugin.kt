/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle

import io.github.woolph.gradle.dependencycheck.DependencyCheckExtension
import io.github.woolph.gradle.dependencycheck.DependencyCheckExtension.Companion.applyDependencyCheckExtension
import io.github.woolph.gradle.licensecheck.LicenseCheckExtension.Companion.applyLicenseCheckExtension
import io.github.woolph.gradle.sonar.SonarQubeExtension
import io.github.woolph.gradle.sonar.SonarQubeExtension.Companion.applySonarQubeExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.create

@Suppress("unused")
class QualityCheckPlugin : Plugin<Project> {
    private lateinit var qualityCheckExtension: QualityCheckExtension
    private lateinit var dependencyCheckExtension: DependencyCheckExtension
    private lateinit var sonarqubeExtension: SonarQubeExtension

    override fun apply(project: Project): Unit = project.run {
        qualityCheckExtension = extensions.create("qualityCheck", QualityCheckExtension::class)

        applyDependencyCheckExtension(qualityCheckExtension as ExtensionAware)
        applySonarQubeExtension(qualityCheckExtension as ExtensionAware)
        applyLicenseCheckExtension(qualityCheckExtension as ExtensionAware)
    }
}
