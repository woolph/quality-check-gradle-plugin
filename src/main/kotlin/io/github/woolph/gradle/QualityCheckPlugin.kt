package io.github.woolph.gradle

import io.github.woolph.gradle.DependencyCheckExtension.Companion.applyDependencyCheckExtension
import io.github.woolph.gradle.SonarqubeExtension.Companion.applySonarQubeExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

import org.gradle.kotlin.dsl.*

@Suppress("unused")
class QualityCheckPlugin: Plugin<Project> {
    private lateinit var qualityCheckExtension: QualityCheckExtension
    private lateinit var dependencyCheckExtension: DependencyCheckExtension
    private lateinit var sonarqubeExtension: SonarqubeExtension

    override fun apply(project: Project): Unit = project.run {
        qualityCheckExtension = extensions.create("qualityCheck", QualityCheckExtension::class)

        applyDependencyCheckExtension(qualityCheckExtension as ExtensionAware)
        applySonarQubeExtension(qualityCheckExtension as ExtensionAware)
    }
}
