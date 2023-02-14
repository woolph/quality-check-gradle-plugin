/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.licensecheck

import com.github.jk1.license.ConfigurationData
import com.github.jk1.license.ImportedModuleBundle
import com.github.jk1.license.ProjectData
import com.github.jk1.license.filter.DependencyFilter
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider

/**
 * this filter removes all the dependencies which are considered to be owned by the projects owner
 */
internal class WhiteListedDependencyFilter(val logger: Logger, val regexProvider: Provider<Set<Regex>>) :
    DependencyFilter {
    override fun filter(source: ProjectData) = regexProvider.orNull?.let { regexs ->
        logger.warn("white-listed dependencies are: ${regexs.joinToString(", ")}")
        ProjectData(
            source.project,
            source.configurations.map { configurationData ->
                ConfigurationData(
                    configurationData.name,
                    configurationData.dependencies.filter { moduleData ->
                        !regexs.any { it.matches("${moduleData.group}:${moduleData.name}") }
                    }.toSet()
                )
            }.toSet(),
            source.importedModules.map {
                ImportedModuleBundle(
                    it.name,
                    it.modules.filter { moduleData -> !regexs.any { it.matches(moduleData.name) } }
                )
            }.toList(),
        )
    } ?: source
}
