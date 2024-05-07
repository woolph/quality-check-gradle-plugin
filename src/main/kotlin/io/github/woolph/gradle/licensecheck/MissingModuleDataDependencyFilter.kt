/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.licensecheck

import com.github.jk1.license.*
import com.github.jk1.license.filter.DependencyFilter

/**
 * this filter checks for the module org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4 cause for
 * some reason it does not provide a license (it ought to be Apache License, Version 2.0)
 */
internal class MissingModuleDataDependencyFilter : DependencyFilter {
    override fun filter(source: ProjectData) =
        ProjectData(
            source.project,
            source.configurations
                .map { configurationData ->
                    ConfigurationData(
                        configurationData.name,
                        configurationData.dependencies.map(::moduleDataMapper).toSet(),
                    )
                }
                .toSet(),
            source.importedModules
                .map {
                    ImportedModuleBundle(
                        it.name,
                        it.modules.map(::importedModuleDataMapper),
                    )
                }
                .toList(),
        )

    private fun moduleDataMapper(moduleData: ModuleData): ModuleData =
        mappingEntries
            .firstOrNull { it.appliesTo(moduleData) }
            ?.moduleDataMapper
            ?.let { it(moduleData) } ?: moduleData

    private fun importedModuleDataMapper(
        importedModuleData: ImportedModuleData
    ): ImportedModuleData =
        mappingEntries
            .firstOrNull { it.appliesTo(importedModuleData) }
            ?.importedModuleDataMapper
            ?.let { it(importedModuleData) } ?: importedModuleData

    val mappingEntries =
        listOf(
            MappingEntry(
                "org.antlr",
                "antlr-runtime",
                null,
                { moduleData ->
                    ModuleData(
                        moduleData.group,
                        moduleData.name,
                        moduleData.version,
                        moduleData.hasArtifactFile,
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
                },
                { importedModuleData ->
                    ImportedModuleData(
                        importedModuleData.name,
                        importedModuleData.version,
                        importedModuleData.projectUrl,
                        "BSD licence",
                        "http://antlr.org/license.html",
                    )
                },
            ),
            MappingEntry(
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-core",
                "1.6.4",
                { moduleData ->
                    ModuleData(
                        moduleData.group,
                        moduleData.name,
                        moduleData.version,
                        moduleData.hasArtifactFile,
                        moduleData.manifests,
                        moduleData.licenseFiles,
                        setOf(
                            PomData(
                                "kotlinx-coroutines-core",
                                "Coroutines support libraries for Kotlin",
                                "https://github.com/Kotlin/kotlinx.coroutines",
                                "",
                                mutableSetOf(
                                    License(
                                        "Apache License, Version 2.0",
                                        "https://www.apache.org/licenses/LICENSE-2.0",
                                    ),
                                ),
                                null,
                                setOf(PomDeveloper("JetBrains Team", "", "")),
                            ),
                        ),
                    )
                },
                { importedModuleData ->
                    ImportedModuleData(
                        importedModuleData.name,
                        importedModuleData.version,
                        importedModuleData.projectUrl,
                        "Apache License, Version 2.0",
                        "https://www.apache.org/licenses/LICENSE-2.0",
                    )
                },
            ),
            MappingEntry(
                "org.jetbrains.kotlin",
                "kotlin-stdlib-common",
                "1.9.20",
                { moduleData ->
                    ModuleData(
                        moduleData.group,
                        moduleData.name,
                        moduleData.version,
                        moduleData.hasArtifactFile,
                        moduleData.manifests,
                        moduleData.licenseFiles,
                        setOf(
                            PomData(
                                "kotlin-stdlib-common",
                                "Kotlin Common Standard Library",
                                "https://kotlinlang.org",
                                "",
                                mutableSetOf(
                                    License(
                                        "Apache License, Version 2.0",
                                        "https://www.apache.org/licenses/LICENSE-2.0",
                                    ),
                                ),
                                null,
                                setOf(PomDeveloper("JetBrains Team", "", "")),
                            ),
                        ),
                    )
                },
                { importedModuleData ->
                    ImportedModuleData(
                        importedModuleData.name,
                        importedModuleData.version,
                        importedModuleData.projectUrl,
                        "Apache License, Version 2.0",
                        "https://www.apache.org/licenses/LICENSE-2.0",
                    )
                },
            ),
        )

    class MappingEntry(
        val group: String,
        val name: String,
        val version: String?,
        val moduleDataMapper: (ModuleData) -> ModuleData,
        val importedModuleDataMapper: (ImportedModuleData) -> ImportedModuleData,
    ) {
        fun appliesTo(moduleData: ModuleData) = moduleData.group == group && moduleData.name == name

        fun appliesTo(importedModuleData: ImportedModuleData) =
            importedModuleData.name == "$group:$name" &&
                version?.let { importedModuleData.version == it } ?: true
    }
}
