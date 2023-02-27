package io.github.woolph.gradle.licensecheck

import com.github.jk1.license.check.LicenseChecker
import com.github.jk1.license.task.CheckLicenseTask
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class CheckLicenseTaskJunitReport: DefaultTask() {
    companion object {
        const val PROJECT_JSON_FOR_LICENSE_CHECKING_FILE = "project-licenses-for-check-license-task.json"
        const val NOT_PASSED_DEPENDENCIES_FILE = "dependencies-without-allowed-license.json"
    }

    @get:Input
    abstract val allowedLicenses: SetProperty<String>

    @get:OutputDirectory
    abstract val tmpDirectory: DirectoryProperty

    @get:InputFile
    abstract val projectDependenciesData: RegularFileProperty

    @get:OutputFile
    abstract val licenseCheckReport: RegularFileProperty

    init {
        group = "verification"
        description = "Check if License could be used"

        tmpDirectory.convention(project.layout.buildDirectory.dir("tmp/license-check"))
    }

    @TaskAction
    fun checkLicense() {
        logger.info("Startup CheckLicense for ${getProject().name}")
        logger.info("Check licenses if they are allowed to use.")

        tmpDirectory.asFile.get().mkdirs()
        val checkResult = tmpDirectory.file(NOT_PASSED_DEPENDENCIES_FILE).get().asFile
        val allowedLicensesFileLocation = tmpDirectory.file("allowedLicensesFile").get().asFile

        allowedLicensesFileLocation.parentFile.mkdirs()
        allowedLicensesFileLocation.writeText(
            allowedLicenses.get().joinToString(
                ",\n",
                "{\"allowedLicenses\":[\n",
                "\n]}",
            ) { "  {\"moduleLicense\":\"$it\"}" },
        )
        val reportResult = projectDependenciesData.asFile.get()

        try {
            LicenseChecker().checkAllDependencyLicensesAreAllowed(
                allowedLicensesFileLocation,
                reportResult,
                checkResult,
            )
        } finally {
            val modulesWithUnallowedLicenses = (Json.parseToJsonElement(checkResult.readText()).jsonObject["dependenciesWithoutAllowedLicenses"]?.jsonArray ?: emptyList()).map {
                val module = "${it.jsonObject["moduleName"]?.jsonPrimitive?.content}:${it.jsonObject["moduleName"]?.jsonPrimitive?.content}"
                val license = "${it.jsonObject["moduleLicense"]?.jsonPrimitive?.content}"
                module to license
            }.groupBy(Pair<String,String>::first, Pair<String,String>::second)

            modulesWithUnallowedLicenses.forEach { module, licenses ->
                logger.error("licenses of the module $module are not allowed (licenses are $licenses)")
            }

            licenseCheckReport.asFile.get().writeText(buildString {
                appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                appendLine("<testsuites>")
                modulesWithUnallowedLicenses.forEach { module, licenses ->
                    appendLine("<testsuite name=\"$module\" tests=\"1\" skipped=\"0\" failures=\"1\">" +
                            "<testcase name=\"$module\" classname=\"license-check\"><failure message=\"none of the following licenses is allowed $licenses\" /></testcase></testsuite>")
                }
                // TODO also print accepted licenses without failure
                appendLine("</testsuites>")
            })
        }
    }
}