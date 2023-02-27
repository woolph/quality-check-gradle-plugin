package io.github.woolph.gradle.dependencycheck.suppression

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.time.ZoneId
import java.time.ZonedDateTime
import org.gradle.api.tasks.Optional

/**
 * This task checks whether the suppression file provided is valid. It shall not contain suppression with an expiration
 * date beyond a year unless they are marked with an FALSE POSITIVE note.
 */
abstract class CheckSuppressionFileTask: DefaultTask() {
    @get:InputFile
    abstract val originalSuppressionFile: RegularFileProperty

    @get:Input
    abstract val maxSuppressUntil: Property<ZonedDateTime>

    @get:Input
    abstract val falsePositivePattern: Property<Regex>

    init {
        maxSuppressUntil.convention(ZonedDateTime.now().plusDays(365))
        falsePositivePattern.convention(Regex("false[\\s-_]positive", RegexOption.IGNORE_CASE))
    }

    @TaskAction
    fun updateSuppressionFile() {
        val falsePositivePattern = falsePositivePattern.get()
        val maxSuppressUntil = maxSuppressUntil.get()
        val originalSuppressionEntries = originalSuppressionFile.asFile.get()
            .parseAsDependencyCheckSuppressionFile()

        val entriesWhichDoNotContainAFalsePositiveNoteNorAnAppropiateSuppressionExpiration = originalSuppressionEntries
            .filter {
                it.notes?.contains(falsePositivePattern) != true && it.suppressUntil?.isBefore(maxSuppressUntil) != true
            }.toList()

        if (entriesWhichDoNotContainAFalsePositiveNoteNorAnAppropiateSuppressionExpiration.isNotEmpty()) {
            entriesWhichDoNotContainAFalsePositiveNoteNorAnAppropiateSuppressionExpiration.forEach {
                logger.error("the suppresison file entry for ${it.packageUrl} (${it.vulnerabilities.joinToString { it.name }}) does not contain FALSE POSITIVE note nor an appropriate expiriation date")
            }
            throw GradleException("Some entries in the DC suppression file ${originalSuppressionFile.get()} do neither have a FALSE POSITIVE note nor an appropriate expiriation date set (at max the suppression expiration should be one year)")
        }
    }
}
