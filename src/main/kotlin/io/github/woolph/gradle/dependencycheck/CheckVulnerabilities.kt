/* Copyright 2025 ENGEL Austria GmbH */
package io.github.woolph.gradle.dependencycheck

import io.github.woolph.gradle.dependencycheck.suppression.Vulnerability
import io.github.woolph.gradle.dependencycheck.suppression.VulnerabilityType
import io.github.woolph.gradle.dependencycheck.suppression.toModuleString
import io.github.woolph.gradle.util.asFileIfExists
import io.github.woolph.gradle.util.children
import io.github.woolph.gradle.util.get
import io.github.woolph.gradle.util.processXml
import java.time.LocalDate
import kotlin.sequences.forEach
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.named

@CacheableTask
abstract class CheckVulnerabilities : DefaultTask() {
    @get:Classpath abstract val classpath: Property<Configuration>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val suppressionFile: RegularFileProperty

    @get:Input val today: LocalDate = LocalDate.now()

    @get:OutputFile abstract val reportJunit: RegularFileProperty

    @get:Internal
    val dependencyCheckAnalyze =
        project.tasks.named<org.owasp.dependencycheck.gradle.tasks.Analyze>(
            "dependencyCheckAnalyze")

    init {
        group = "verification/dependency-check"

        classpath.convention(project.configurations.getByName("runtimeClasspath"))
        reportJunit.convention(
            project.layout.buildDirectory.file("reports/dependency-check-junit.xml"))
    }

    @TaskAction
    fun checkVulnerabilities() {
        try {
            dependencyCheckAnalyze.get().analyze()
        } finally {
            val dependencyCauses =
                classpath.get().resolvedConfiguration.getDependencyCause().mapKeys {
                    it.key.module.toString()
                }

            val vulnerabilities =
                reportJunit.asFileIfExists?.processXml { doc ->
                    doc.children().flatMap { testsuites ->
                        testsuites
                            .children()
                            .filter { (it.attributes["failures"]?.value?.toInt() ?: 0) > 0 }
                            .children()
                            .mapNotNull { testcase ->
                                testcase.attributes["classname"]?.value?.let { vulnerability ->
                                    VulnerabilityCause(
                                        testcase.attributes["name"]?.value?.toModuleString()
                                            ?: "unknown",
                                        listOf(
                                            Vulnerability(
                                                VulnerabilityType.VulnerabilityName,
                                                vulnerability)), // TODO determine correct
                                        // VulnerabilityType
                                    )
                                }
                            }
                    }
                }

            logger.debug(
                "printVulnerabilityCause: starting to process the xml of dependency issues")

            vulnerabilities?.forEach {
                val cause = dependencyCauses[it.moduleString] ?: emptySet()
                logger.warn(
                    "${cause.map { it.module }} introduced ${it.moduleString} with the ${it.vulnerabilities.map { it.name }}")
            }
        }
    }

    data class VulnerabilityCause(
        val moduleString: String,
        val vulnerabilities: List<Vulnerability>,
    )

    fun ResolvedConfiguration.getDependencyCause():
        Map<ResolvedDependency, Set<ResolvedDependency>> {
        return sequence {
                firstLevelModuleDependencies.forEach { causingDependency ->
                    causingDependency.children.forEach { addChildren(it, causingDependency) }
                }
            }
            .groupBy(
                Pair<ResolvedDependency, ResolvedDependency>::first,
                Pair<ResolvedDependency, ResolvedDependency>::second)
            .mapValues { it.value.toSet() }
    }

    suspend fun SequenceScope<Pair<ResolvedDependency, ResolvedDependency>>.addChildren(
        childDependency: ResolvedDependency,
        causingDependency: ResolvedDependency
    ) {
        yield(childDependency to causingDependency)
        childDependency.children.forEach { addChildren(it, causingDependency) }
    }
}
