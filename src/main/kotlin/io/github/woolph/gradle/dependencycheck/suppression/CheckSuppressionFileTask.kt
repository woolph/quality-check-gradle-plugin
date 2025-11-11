/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.dependencycheck.suppression

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
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
@CacheableTask
abstract class CheckSuppressionFileTask : DefaultTask() {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:Optional
  abstract val originalSuppressionFile: RegularFileProperty

  @get:Input abstract val maxSuppressUntil: Property<Duration>

  @get:Input abstract val falsePositivePattern: Property<Regex>

  @get:Input
  abstract val today:
      Property<LocalDate> // FIXME LocalDate causes issues with configuration-cache due to unallowed
  // reflective access

  @get:OutputFile abstract val suppressionFileCheckResult: RegularFileProperty

  init {
    group = "verification/dependency-check"

    maxSuppressUntil.convention(365.days.toJavaDuration())

    suppressionFileCheckResult.convention(
        project.layout.buildDirectory.file(
            "reports/dependency-check/suppression-file-check-result.txt"))

    falsePositivePattern.convention(Regex("false[\\s-_]positive", RegexOption.IGNORE_CASE))

    today.set(LocalDate.now())
    today.finalizeValue()
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
                    it.suppressUntil?.isBefore(
                        today.get().atStartOfDay(ZoneId.systemDefault()).plus(maxSuppressUntil)) !=
                        true
              }
              .toList()

      suppressionFileCheckResult.get().asFile.writeText(inappropriateEntries.joinToString("\n"))

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
