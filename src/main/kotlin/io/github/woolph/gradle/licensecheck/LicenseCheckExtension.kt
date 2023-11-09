/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.licensecheck

import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.render.JsonReportRenderer
import com.github.jk1.license.render.SimpleHtmlReportRenderer
import com.github.jk1.license.task.CheckLicenseTask
import io.github.woolph.gradle.Skipable
import javax.inject.Inject
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty

abstract class LicenseCheckExtension @Inject constructor(project: Project) : Skipable {
    override val skip = project.objects.property<Boolean>().convention(false)

    val reportsDirectory =
        project.objects
            .directoryProperty()
            .convention(project.layout.buildDirectory.dir("reports/dependency-license"))

    val licenseCheckReport =
        project.objects.fileProperty().convention(reportsDirectory.file("license-check-report.xml"))

    /**
     * set of regex which match the names of the modules which are considered to be owned by the
     * projects owner. By default, the set consists of one regex which is composed of the projects
     * group reduced to the first 2 segments e.g.: project.group = "com.example.subitem.subsubtiem"
     * will result in the regex "^com\.example\..*"
     */
    val ownedDependencies =
        project.objects
            .setProperty<Regex>()
            .value(
                project.providers
                    .provider { project.group }
                    .map { it.toString().split(".").take(2).joinToString(".") }
                    .map { setOf(Regex("^${Regex.escape(it)}(\\.)?.*")) },
            )

    /**
     * set of regex which match the names of the modules which are considered to be white listed,
     * which means that licenses for these modules won't be checked until they have expired By
     * default, the set consists of the owned dependencies which are valid forever
     */
    val whiteListedDependencies =
        project.objects.setProperty<WhiteListedDependency>().convention(emptySet())

    /**
     * set of allowed licenses. Defaults to setOf( "MIT License", "MIT-0", "Apache License, Version
     * 2.0", "BSD Zero Clause License", "The 2-Clause BSD License", "The 3-Clause BSD License", "GNU
     * GENERAL PUBLIC LICENSE, Version 2 + Classpath Exception", "GNU LESSER GENERAL PUBLIC LICENSE,
     * Version 2.1", "GNU Lesser General Public License v3.0", "Go License", "Indiana University
     * Extreme! Lab Software License", "COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version
     * 1.0", "Eclipse Public License - v 1.0", "Eclipse Public License - v 2.0", "PUBLIC DOMAIN",
     * "Bouncy Castle Licence", // is essentially the "MIT License" )
     */
    // FIXME check whether GPL with CE, EPL-1.0, CC0, & all are really okay
    val allowedLicenses =
        project.objects
            .setProperty<String>()
            .value(
                setOf(
                    "MIT License",
                    "MIT-0",
                    "Apache License, Version 2.0",
                    "BSD Zero Clause License",
                    "The 2-Clause BSD License",
                    "The 3-Clause BSD License",
                    "GNU GENERAL PUBLIC LICENSE, Version 2 + Classpath Exception",
                    "GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1",
                    "GNU Lesser General Public License v3.0",
                    "Go License",
                    "Indiana University Extreme! Lab Software License",
                    "COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0",
                    "Eclipse Public License - v 1.0",
                    "Eclipse Public License - v 2.0",
                    "PUBLIC DOMAIN",
                    "Bouncy Castle Licence", // is essentially the "MIT License"
                ),
            )

    inner class WhiteListedDependenciesBuilder {
        val whiteListedDependencies = mutableMapOf<Regex, Instant>()

        fun add(moduleNamePattern: Regex): Regex {
            whiteListedDependencies[moduleNamePattern] = Instant.DISTANT_FUTURE
            return moduleNamePattern
        }

        fun add(moduleNamePattern: String) = add(Regex("^${Regex.escape(moduleNamePattern)}"))

        infix fun Regex.until(validUntil: Instant) {
            whiteListedDependencies[this] = validUntil
        }

        infix fun Regex.until(validUntil: String) = until(Instant.parse(validUntil))
    }

    /** DSL helper for setting up whiteListedDependencies */
    fun whiteListedDependencies(action: Action<WhiteListedDependenciesBuilder>) {
        val whiteListedDependenciesBuilder = WhiteListedDependenciesBuilder()

        action.execute(whiteListedDependenciesBuilder)

        this@LicenseCheckExtension.whiteListedDependencies.addAll(
            whiteListedDependenciesBuilder.whiteListedDependencies.entries.map { (regex, instant) ->
                WhiteListedDependency(
                    regex,
                    instant,
                )
            },
        )
    }

    companion object {
        internal fun Project.applyLicenseCheckExtension(baseExtension: ExtensionAware) {
            val thisExtension =
                baseExtension.extensions.create(
                    "licenseCheck", LicenseCheckExtension::class, project)
            val tmpDirectory = project.layout.buildDirectory.dir("tmp/license-check")
            val additonalLicenseNormalizerBundle =
                tmpDirectory.map { it.file("additional-license-normalizer-bundle.json") }

            try {
                val check = tasks.named("check")

                plugins.apply("com.github.jk1.dependency-license-report")

                tasks.named<com.github.jk1.license.task.CheckLicensePreparationTask>(
                    "checkLicensePreparation") {
                        group = "verification/license-check"
                        onlyIf { false } // skip this task cause it only adds a renderer
                }

                val createLicenseBundleNormalizerConfig =
                    tasks.create<CreateLicenseBundleNormalizerConfigTask>(
                        "createLicenseBundleNormalizerConfig") {
                            this.additonalLicenseNormalizerBundle.set(
                                additonalLicenseNormalizerBundle)
                        }

                val generateLicenseReport =
                    tasks.named<com.github.jk1.license.task.ReportTask>("generateLicenseReport") {
                        group = "verification/license-check"
                        inputs.property("ownedDependencies", thisExtension.ownedDependencies)
                        inputs.property(
                            "whiteListedDependencies",
                            thisExtension.whiteListedDependencies.map { it.map { it.toString() } },
                        )
                        inputs.file(
                            createLicenseBundleNormalizerConfig.additonalLicenseNormalizerBundle)
                    }

                val checkLicense =
                    tasks.create<CheckLicenseTaskJunitReport>("checkLicenses") {
                        //                val checkLicense =
                        // tasks.replace<CheckLicenseTaskJunitReport>("checkLicense",
                        // CheckLicenseTaskJunitReport::class.java) {

                        allowedLicenses.set(thisExtension.allowedLicenses)
                        licenseCheckReport.set(
                            thisExtension.reportsDirectory.file("license-check-report.xml"))
                        projectDependenciesData.set(
                            thisExtension.reportsDirectory.file(
                                CheckLicenseTaskJunitReport.PROJECT_JSON_FOR_LICENSE_CHECKING_FILE))
                        this.tmpDirectory.set(tmpDirectory)

                        onlyIf { thisExtension.skip.map { !it }.get() }

                        dependsOn(generateLicenseReport)
                    }

                tasks.named("checkLicense") {
                    group = "verification/license-check"
                    description =
                        "task from license-check plugin (but it is replaced by checkLicenses)"
                }

                check { dependsOn(checkLicense) }

                afterEvaluate {
                    if (thisExtension.skip.get()) {
                        logger.warn("licenseCheck is disabled!")
                    }

                    thisExtension.whiteListedDependencies
                        .get()
                        .filter(::expiredWhiteListedDependencies)
                        .map { it.moduleNamePattern }
                        .let { expiredWhiteListDependencies ->
                            if (expiredWhiteListDependencies.isNotEmpty()) {
                                logger.warn(
                                    "The following whiteListedDependencies have expired: [${
                                        expiredWhiteListDependencies.joinToString(
                                            ", ",
                                        )
                                    }]",
                                )
                            }
                        }

                    extensions
                        .getByName<com.github.jk1.license.LicenseReportExtension>("licenseReport")
                        .apply {
                            renderers =
                                arrayOf(
                                    SimpleHtmlReportRenderer(),
                                    JsonReportRenderer(
                                        CheckLicenseTask
                                            .getPROJECT_JSON_FOR_LICENSE_CHECKING_FILE(),
                                        false),
                                )
                            outputDir = thisExtension.reportsDirectory.get().asFile.toString()
                            excludeBoms = true
                            excludes =
                                arrayOf(
                                    // otherwise this bom is not excluded!
                                    "org.springframework.boot:spring-boot-dependencies",
                                    "org.springframework.cloud:spring-cloud-dependencies",
                                    "org.springframework.cloud:spring-cloud-sleuth-otel-dependencies",
                                )

                            val whiteListedDependencies =
                                thisExtension.ownedDependencies.zip(
                                    thisExtension.whiteListedDependencies.map {
                                        it.filter(not(::expiredWhiteListedDependencies)).map {
                                            it.moduleNamePattern
                                        }
                                    },
                                ) { a, b ->
                                    a union b
                                }

                            val additonalLicenseNormalizerBundleFile =
                                additonalLicenseNormalizerBundle.get().asFile

                            additonalLicenseNormalizerBundleFile.parentFile.mkdirs()
                            //
                            // additonalLicenseNormalizerBundleFile.writeText("""
                            //                            {
                            //                              "bundles" : [
                            //                                { "bundleName" : "Antlr BSD-3-Clause",
                            // "licenseName" : "The 3-Clause BSD License", "licenseUrl" :
                            // "https://www.antlr.org/license.html" },
                            //                              ],
                            //                              "transformationRules" : [
                            //                                { "bundleName" : "Antlr BSD-3-Clause",
                            // "licenseUrlPattern" : ".*www\\.antlr\\.org/license\\.(html|htm)" },
                            //                              ]
                            //                            }
                            //
                            //                        """.trimIndent())

                            filters =
                                arrayOf<DependencyFilter>(
                                    WhiteListedDependencyFilter(logger, whiteListedDependencies),
                                    MissingModuleDataDependencyFilter(),
                                    OnDemandBundleNormalizerFilter(
                                        createLicenseBundleNormalizerConfig
                                            .additonalLicenseNormalizerBundle,
                                    ),
                                )
                        }
                }
            } catch (e: Exception) {
                logger.error("licenseCheck will not be applied due to exception", e)
            }
        }

        private fun <T> not(predicate: (T) -> Boolean): (T) -> Boolean = { !predicate(it) }

        private fun expiredWhiteListedDependencies(item: WhiteListedDependency): Boolean =
            item.isExpired(Clock.System)
    }
}
