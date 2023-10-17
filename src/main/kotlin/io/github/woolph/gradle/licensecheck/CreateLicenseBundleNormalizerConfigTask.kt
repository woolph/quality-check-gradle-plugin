/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.licensecheck

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class CreateLicenseBundleNormalizerConfigTask : DefaultTask() {
    @get:OutputFile
    abstract val additonalLicenseNormalizerBundle: RegularFileProperty

    init {
        group = "verification/license-check"
        description = "Create License Bundle Normalizer Config Task"
    }

    @TaskAction
    fun createLicenseBundleNormalizerConfigTask() {
        CreateLicenseBundleNormalizerConfigTask::class.java.getResourceAsStream("/additional-license-normalizer-bundle.json")
            ?.let { additonalLicenseNormalizerBundle.get().asFile.writeBytes(it.readAllBytes()) }
    }
}
