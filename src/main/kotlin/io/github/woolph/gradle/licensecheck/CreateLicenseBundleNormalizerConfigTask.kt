/* Copyright 2023-2026 ENGEL Austria GmbH */
package io.github.woolph.gradle.licensecheck

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class CreateLicenseBundleNormalizerConfigTask : DefaultTask() {
  @get:OutputFile abstract val additionalLicenseNormalizerBundle: RegularFileProperty

  init {
    group = "verification/license-check"
    description = "Create License Bundle Normalizer Config Task"
  }

  @TaskAction
  fun createLicenseBundleNormalizerConfigTask() {
    CreateLicenseBundleNormalizerConfigTask::class
        .java
        .getResourceAsStream("/additional-license-normalizer-bundle.json")
        ?.let { additionalLicenseNormalizerBundle.get().asFile.writeBytes(it.readAllBytes()) }
  }
}
