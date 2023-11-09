/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.dependencycheck.suppression

import java.time.ZonedDateTime
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * This task checks the suppression file provided for "inappropriate" suppression entries. An entry
 * is considered appropriate it
 * - it contains a FALSE POSTIVE "tag" in the notes, which indicates that the suppressed
 *   vulnerability is not affecting the application.
 * - or it has a expiration date which is not farther way than the maxSuppressionUntil (usually a
 *   year from now). Passing the expiration date disables the suppression entry, forcing you to
 *   reevaluate the vulnerability. Therefore this expiring suppression may be useful for
 *   vulnerabilities that, for some reason, cannot be fixed right away.
 */
abstract class CheckSuppressionFileTask : DefaultTask() {
    @get:InputFile @get:Optional abstract val originalSuppressionFile: RegularFileProperty

    @get:Input abstract val maxSuppressUntil: Property<ZonedDateTime>

    @get:Input abstract val falsePositivePattern: Property<Regex>

    init {
        group = "verification/dependency-check"

        maxSuppressUntil.convention(ZonedDateTime.now().plusDays(365))

        falsePositivePattern.convention(Regex("false[\\s-_]positive", RegexOption.IGNORE_CASE))
    }

    @TaskAction
    fun checkSuppressionFile() {
        originalSuppressionFile.asFile.orNull?.parseAsDependencyCheckSuppressionFile()?.let {
            originalSuppressionEntries ->
            val falsePositivePattern = falsePositivePattern.get()
            val maxSuppressUntil = maxSuppressUntil.get()

            val inappropriateEntries =
                originalSuppressionEntries
                    .filter {
                        it.notes?.contains(falsePositivePattern) != true &&
                            it.suppressUntil?.isBefore(maxSuppressUntil) != true
                    }
                    .toList()

            if (inappropriateEntries.isNotEmpty()) {
                inappropriateEntries.forEach {
                    logger.error(
                        "the suppression file entry for ${it.packageUrl} (${it.vulnerabilities.joinToString { it.name }}) does neither contain FALSE POSITIVE note nor an appropriate expiration date")
                }
                throw GradleException(
                    "Some entries in the DC suppression file ${originalSuppressionFile.get()} do neither have a FALSE POSITIVE note nor an appropriate expiration date set (at max the suppression expiration should be one year)")
            }
        }
    }
}
