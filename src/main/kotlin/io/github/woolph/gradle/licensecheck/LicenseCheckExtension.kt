/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.licensecheck

import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.render.JsonReportRenderer
import com.github.jk1.license.render.SimpleHtmlReportRenderer
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

    /**
     * set of regex which match the names of the modules which are considered to be owned by the projects owner.
     * By default the set consists of one regex which is composed of the projects group reduced to the first 2 segments
     * e.g.: project.group = "com.example.subitem.subsubtiem" will result in the regex "^com\.example\..*"
     */
    val ownedDependencies = project.objects.setProperty<Regex>().convention(
        project.providers.provider { project.group }
            .map { it.toString().split(".").take(2).joinToString(".") }
            .map { setOf(Regex("^${Regex.escape(it)}\\..*")) }
    )

    /**
     * set of allowed licenses.
     * Defaults to setOf(
     * "MIT License",
     * "MIT-0",
     * "Apache License, Version 2.0",
     * "The 2-Clause BSD License",
     * "The 3-Clause BSD License",
     * "GNU GENERAL PUBLIC LICENSE, Version 2 + Classpath Exception",
     * "COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0",
     * "Eclipse Public License - v 1.0",
     * "PUBLIC DOMAIN")
     */
    // FIXME check whether GPL with CE, EPL-1.0, CC0, & all are really okay
    val allowedLicenses = project.objects.setProperty<String>().convention(
        setOf(
            "MIT License",
            "MIT-0",
            "Apache License, Version 2.0",
            "The 2-Clause BSD License",
            "The 3-Clause BSD License",
            "GNU GENERAL PUBLIC LICENSE, Version 2 + Classpath Exception",
            "COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0",
            "Eclipse Public License - v 1.0",
            "PUBLIC DOMAIN",
        )
    )

    companion object {
        fun Project.applyLicenseCheckExtension(baseExtension: ExtensionAware) {
            val allowedLicensesFileLocation = File(buildDir, "tmp/allowed-licenses.json")

            val thisExtension = baseExtension.extensions.create("licenseCheck", LicenseCheckExtension::class, project)

            try {
                val check = tasks.named("check")

                plugins.apply("com.github.jk1.dependency-license-report")

                tasks.named<com.github.jk1.license.task.CheckLicensePreparationTask>("checkLicensePreparation") {
                    onlyIf { false } // skip this task cause it only adds a renderer
                }

                tasks.named<com.github.jk1.license.task.ReportTask>("generateLicenseReport") {
                    inputs.property("ownedDependencies", thisExtension.ownedDependencies)
                }

                val checkLicense = tasks.named<com.github.jk1.license.task.CheckLicenseTask>("checkLicense") {
                    inputs.property("allowedLicenses", thisExtension.allowedLicenses)
                    inputs.property("ownedDependencies", thisExtension.ownedDependencies)

                    onlyIf {
                        thisExtension.skip.map { !it }.get()
                    }

                    doFirst {
                        allowedLicensesFileLocation.parentFile.mkdirs()
                        allowedLicensesFileLocation.writeText(
                            thisExtension.allowedLicenses.get()
                                .joinToString(
                                    ",\n",
                                    "{\"allowedLicenses\":[\n",
                                    "\n]}"
                                ) { "  {\"moduleLicense\":\"$it\"}" }
                        )
                    }
                }

                check {
                    dependsOn(checkLicense)
                }

                afterEvaluate {
                    if (thisExtension.skip.get()) {
                        logger.warn("licenseCheck is disabled!")
                    }

                    extensions.getByName<com.github.jk1.license.LicenseReportExtension>("licenseReport").apply {
                        renderers = arrayOf(
                            SimpleHtmlReportRenderer(),
                            JsonReportRenderer("project-licenses-for-check-license-task.json", false),
                        )
                        excludeBoms = true
                        excludes = arrayOf(
                            "com.engelglobal*",
                            "org.springframework.boot:spring-boot-dependencies", // otherwise this bom is not excluded!
                        )

                        filters = arrayOf<DependencyFilter>(
                            OwnedDependencyFilter(thisExtension.ownedDependencies.get()),
                            CoroutinesFilter(),
                            com.github.jk1.license.filter.LicenseBundleNormalizer(),
                        )

                        allowedLicensesFile = allowedLicensesFileLocation
                    }
                }
            } catch (e: Exception) {
                println(e)
                logger.error("licenseCheck will not be applied due to exception", e)
            }
        }
    }
}
