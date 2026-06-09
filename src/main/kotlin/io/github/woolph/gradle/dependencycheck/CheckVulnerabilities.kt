/* Copyright 2025-2026 ENGEL Austria GmbH */
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
import org.gradle.api.artifacts.result.ComponentSelectionCause
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
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
      project.tasks.named<org.owasp.dependencycheck.gradle.tasks.Analyze>("dependencyCheckAnalyze")

  init {
    group = "verification/dependency-check"

    classpath.convention(project.configurations.getByName("runtimeClasspath"))
    reportJunit.convention(project.layout.buildDirectory.file("reports/dependency-check-junit.xml"))
  }

  @TaskAction
  fun checkVulnerabilities() {
    try {
      dependencyCheckAnalyze.get().analyze()
    } finally {
      val resolutionResult = classpath.get().incoming.resolutionResult

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
                          testcase.attributes["name"]?.value?.toModuleString() ?: "unknown",
                          listOf(
                              Vulnerability(VulnerabilityType.VulnerabilityName, vulnerability)
                          ), // TODO determine correct
                          // VulnerabilityType
                      )
                    }
                  }
            }
          }

      logger.debug("printVulnerabilityCause: starting to process the xml of dependency issues")

      vulnerabilities?.forEach {
        val causes = resolutionResult.getRequestedParentDependencies(it.moduleString)
        logger.warn(
            "$causes introduced ${it.moduleString} with the ${it.vulnerabilities.map { it.name }}"
        )
      }
    }
  }

  data class VulnerabilityCause(
      val moduleString: String,
      val vulnerabilities: List<Vulnerability>,
  )

  fun ResolutionResult.getRequestedParentDependencies(moduleName: String): String {
    fun ResolvedComponentResult.findFirstLevelParents(): List<ResolvedComponentResult> {
      val isRootComponents =
          selectionReason.descriptions.any { description ->
            description.cause == ComponentSelectionCause.ROOT
          }
      val isFirstLevelComponent =
          selectionReason.descriptions.any { description ->
            description.cause == ComponentSelectionCause.REQUESTED
          }

      return if (isRootComponents) {
        emptyList()
      } else if (isFirstLevelComponent) {
        listOf(this)
      } else {
        dependents
            .filter { it.resolvedVariant.attributes.getAttribute(ATTRIBUTE_CATEGORY) != "platform" }
            .flatMap { it.from.findFirstLevelParents() }
      }
    }

    val component =
        allComponents.first {
          it.moduleVersion != null &&
              "${it.moduleVersion?.group}:${it.moduleVersion?.name}:${it.moduleVersion?.version}" ==
                  moduleName
        }
    return component.findFirstLevelParents().joinToString(",") {
      "${it.moduleVersion?.group}:${it.moduleVersion?.name}:${it.moduleVersion?.version}"
    }
  }

  companion object {
    val ATTRIBUTE_CATEGORY =
        org.gradle.api.attributes.Attribute.of("org.gradle.category", String::class.java)
  }
}
