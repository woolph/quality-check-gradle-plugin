package io.github.woolph.gradle.licensecheck

import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.render.JsonReportRenderer
import com.github.jk1.license.render.SimpleHtmlReportRenderer
import com.github.jk1.license.task.CheckLicenseTask
import io.github.woolph.gradle.Skipable
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import java.io.File
import javax.inject.Inject

abstract class LicenseCheckExtension @Inject constructor(project: Project) : Skipable {
    override val skip = project.objects.property<Boolean>().convention(false)

    val ownedDependencies = project.objects.setProperty<Regex>().convention(setOf(
        Regex("^com\\.engelglobal\\..*")
    ))

    val allowedLicenses = project.objects.setProperty<String>().convention(setOf(
            "MIT License",
            "MIT-0",
            "Apache License, Version 2.0",
            "The 2-Clause BSD License",
            "The 3-Clause BSD License",
            "GNU GENERAL PUBLIC LICENSE, Version 2 + Classpath Exception",
            "COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0",
            "Eclipse Public License - v 1.0",
            "PUBLIC DOMAIN",
        ))
    // FIXME check whether GPL with CE is, EPL-1.0, CC0, & all are really okay

    companion object {
        fun Project.applyLicenseCheckExtension(baseExtension: ExtensionAware) {
            val allowedLicensesFileLocation = File("${buildDir}/tmp/allowed-licenses.json")

            val licenseCheckExtension =
                baseExtension.extensions.create("licenseCheck", LicenseCheckExtension::class, project)

            try {
                val check = tasks.named("check")

                plugins.apply("com.github.jk1.dependency-license-report")

                tasks.named<com.github.jk1.license.task.CheckLicensePreparationTask>("checkLicensePreparation") {
                    onlyIf { false } // skip this task cause it only adds a renderer
                }

                tasks.named<com.github.jk1.license.task.ReportTask>("generateLicenseReport") {
                    inputs.property("ownedDependencies", licenseCheckExtension.ownedDependencies)
                }

                val checkLicense = tasks.named<com.github.jk1.license.task.CheckLicenseTask>("checkLicense")

                checkLicense {
                    inputs.property("allowedLicenses", licenseCheckExtension.allowedLicenses)
                    inputs.property("ownedDependencies", licenseCheckExtension.ownedDependencies)

                    onlyIf {
                        licenseCheckExtension.skip.map { !it }.get()
                    }

                    doFirst {
                        allowedLicensesFileLocation.parentFile.mkdirs()
                        allowedLicensesFileLocation.writeText(
                            licenseCheckExtension.allowedLicenses.get()
                                .joinToString(",\n", "{\"allowedLicenses\":[\n", "\n]}") { "  {\"moduleLicense\":\"$it\"}" }
                        )
                    }
                }

                check {
                    dependsOn(checkLicense)
                }

                afterEvaluate {
                    if (licenseCheckExtension.skip.get()) {
                        logger.warn("checkLicense is disabled!")
                    }

                    extensions.getByName<com.github.jk1.license.LicenseReportExtension>("licenseReport").apply {
                        renderers = arrayOf(
                            SimpleHtmlReportRenderer(),
                            JsonReportRenderer("project-licenses-for-check-license-task.json",false),
                        )
                        excludeBoms = true
                        excludes = arrayOf(
                            "com.engelglobal*",
                            "org.springframework.boot:spring-boot-dependencies", // otherwise this bom is not excluded!
                        )

                        filters = arrayOf<DependencyFilter>(
                            OwnedDependencyFilter(licenseCheckExtension.ownedDependencies.get()),
                            CoroutinesFilter(),
                            com.github.jk1.license.filter.LicenseBundleNormalizer(),
                        )

                        allowedLicensesFile = allowedLicensesFileLocation
                    }
                }
            } catch (e: Exception) {
                println(e)
                logger.error("dependencyCheck will not be applied due to exception", e)
            }
        }
    }
}
