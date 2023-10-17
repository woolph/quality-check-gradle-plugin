/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.dependencycheck.suppression

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

abstract class UpdateSuppressionFileTask : DefaultTask() {
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
        group = "verification/dependency-check"

        suppressionFile.convention(
            project.layout.projectDirectory.file("dc-suppress-updated.xml"),
        )

        suppressUntil.convention(
            project.providers.gradleProperty("suppressUntil").map { SuppressionEntry.parseToZoneDateTime(it) },
        )

        desiredZoneId.convention(SuppressionEntry.DEFAULT_ZONE_ID)
    }

    @TaskAction
    fun updateSuppressionFile() {
        originalSuppressionFile.asFile.orNull
            ?.parseAsDependencyCheckSuppressionFile()
            ?.let { originalSuppressionEntries ->
                val suppressUntil = suppressUntil.orNull

                originalSuppressionEntries
                    .map {
                        it.copy(suppressUntil = if (it.suppressUntil == null) null else (suppressUntil ?: it.suppressUntil))
                    }.writeTo(suppressionFile.asFile.get(), desiredZoneId.get())
            }
    }
}
