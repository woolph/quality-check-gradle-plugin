/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.dependencycheck.suppression

import io.github.woolph.gradle.util.children
import io.github.woolph.gradle.util.get
import io.github.woolph.gradle.util.processXml
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.time.ZoneId
import java.time.ZonedDateTime

abstract class GenerateSuppressionFileTask : DefaultTask() {
    @get:InputFile
    abstract val dependencyCheckXmlReport: RegularFileProperty

    @get:InputFile
    @get:Optional
    abstract val originalSuppressionFile: RegularFileProperty

    @get:OutputFile
    abstract val suppressionFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val suppressUntil: Property<ZonedDateTime>

    @get:Input
    abstract val desiredZoneId: Property<ZoneId>

    init {
        dependencyCheckXmlReport.convention(
            project.layout.buildDirectory.file("reports/dependency-check-junit.xml"),
        )

        suppressionFile.convention(
            project.layout.projectDirectory.file("dc-suppress.xml"),
        )

        suppressUntil.convention(
            project.providers.gradleProperty("suppressUntil").map { SuppressionEntry.parseToZoneDateTime(it) },
        )

        desiredZoneId.convention(SuppressionEntry.DEFAULT_ZONE_ID)
    }

    @TaskAction
    fun generateSuppressionFile() {
        val includeAlreadySuppressed = true
        val suppressUntil = suppressUntil.orNull

        val newSuppressionEntries = dependencyCheckXmlReport.asFile.get().processXml { doc ->
            doc.children().flatMap { testsuites ->
                testsuites.children().filter {
                    (it.attributes["failures"]?.value?.toInt() ?: 0) > 0
                }.children().mapNotNull { testcase ->
                    testcase.attributes["classname"]?.value?.let { vulnerability ->
                        val packageUrl = testcase.attributes["name"]?.value ?: "unknown"
                        val suppressionNote = testcase.children().filter { it.nodeName == "skipped" }.firstOrNull()?.let {
                            it.attributes["message"]?.value
                        } ?: "TODO enter reason why this can be suppressed or otherwise fix it!\n" +
                            "see details on http://web.nvd.nist.gov/view/vuln/detail?vulnId=$vulnerability"

                        SuppressionEntry(
                            packageUrl,
                            listOf(Vulnerability(VulnerabilityType.VulnerabilityName, vulnerability)), // TODO determine correct VulnerabilityType
                            notes = suppressionNote,
                            suppressUntil = suppressUntil,
                            packageUrlPattern = false,
                        )
                    }
                }
            }
        }

        val usedOriginalSuppressionEntries = if (includeAlreadySuppressed && originalSuppressionFile.isPresent) {
            val skippedVulnerabilities = dependencyCheckXmlReport.asFile.get().processXml { doc ->
                doc.children().flatMap { testsuites ->
                    testsuites.children().filter {
                        (it.attributes["skipped"]?.value?.toInt() ?: 0) > 0
                    }.children().mapNotNull { testcase ->
                        val vulnerability = testcase.attributes["classname"]?.value
                        vulnerability
                    }
                }.toList()
            }

            val originalSuppressionEntries = originalSuppressionFile.asFile.get()
                .parseAsDependencyCheckSuppressionFile()

            originalSuppressionEntries.filter {
                it.vulnerabilities.any { it.name in skippedVulnerabilities }
            }.mapNotNull {
                val suppressedVulnerabilities = it.vulnerabilities.filter { it.name in skippedVulnerabilities }
                if (suppressedVulnerabilities.isNotEmpty()) {
                    it.copy(vulnerabilities = suppressedVulnerabilities, suppressUntil = if (it.suppressUntil == null) null else (suppressUntil ?: it.suppressUntil))
                } else {
                    null
                }
            }
        } else {
            emptySequence()
        }

        sequenceOf(usedOriginalSuppressionEntries, newSuppressionEntries).flatMap { it }
            .writeTo(suppressionFile.asFile.get(), desiredZoneId.get())
    }
}
