/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.licensecheck

import com.github.jk1.license.ConfigurationData
import com.github.jk1.license.ImportedModuleBundle
import com.github.jk1.license.ImportedModuleData
import com.github.jk1.license.License
import com.github.jk1.license.ModuleData
import com.github.jk1.license.PomData
import com.github.jk1.license.PomDeveloper
import com.github.jk1.license.ProjectData
import com.github.jk1.license.filter.DependencyFilter

/**
 * this filter checks for the module org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
 * cause for some reason it does not provide a license (it ought to be Apache License, Version 2.0)
 */
internal class AntlrRuntimeFilter : DependencyFilter {
    override fun filter(source: ProjectData) =
        ProjectData(
            source.project,
            source.configurations.map { configurationData ->
                ConfigurationData(
                    configurationData.name,
                    configurationData.dependencies.map { moduleData ->
                        if (moduleData.group == "org.antlr" && moduleData.name == "antlr-runtime") {
                            ModuleData(
                                moduleData.group,
                                moduleData.name,
                                moduleData.version,
                                moduleData.manifests,
                                moduleData.licenseFiles,
                                setOf(
                                    PomData(
                                        "antlr-runtime",
                                        "",
                                        "",
                                        "",
                                        mutableSetOf(
                                            License(
                                                "BSD licence",
                                                "http://antlr.org/license.html",
                                            ),
                                        ),
                                        null,
                                        setOf(PomDeveloper("", "", "")),
                                    ),
                                ),
                            )
                        } else {
                            moduleData
                        }
                    }.toSet(),
                )
            }.toSet(),
            source.importedModules.map {
                ImportedModuleBundle(
                    it.name,
                    it.modules.map { moduleData ->
                        if (moduleData.name == "org.antlr:antlr-runtime") {
                            ImportedModuleData(
                                moduleData.name,
                                moduleData.version,
                                moduleData.projectUrl,
                                "BSD licence",
                                "http://antlr.org/license.html",
                            )
                        } else {
                            moduleData
                        }
                    },
                )
            }.toList(),
        )
}
