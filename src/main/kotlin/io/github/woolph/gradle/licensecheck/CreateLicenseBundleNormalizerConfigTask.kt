package io.github.woolph.gradle.licensecheck

import com.github.jk1.license.check.LicenseChecker
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class CreateLicenseBundleNormalizerConfigTask: DefaultTask() {
    @get:OutputFile
    abstract val additonalLicenseNormalizerBundle: RegularFileProperty

    @TaskAction
    fun createLicenseBundleNormalizerConfigTask() {
        CreateLicenseBundleNormalizerConfigTask::class.java.getResourceAsStream("/additional-license-normalizer-bundle.json")
            ?.let { additonalLicenseNormalizerBundle.get().asFile.writeBytes(it.readAllBytes()) }
    }
}
