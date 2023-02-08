package io.github.woolph.gradle.licensecheck

import com.github.jk1.license.ConfigurationData
import com.github.jk1.license.ImportedModuleBundle
import com.github.jk1.license.ProjectData
import com.github.jk1.license.filter.DependencyFilter

/**
 * this filter removes all the dependencies which are considered to be owned by the projects owner
 */
internal class OwnedDependencyFilter(val regexs: Set<Regex>): DependencyFilter {
    override fun filter(source: ProjectData) =
        ProjectData(
            source.project,
            source.configurations.map { configurationData ->
                ConfigurationData(
                    configurationData.name,
                    configurationData.dependencies.filter { moduleData -> !regexs.any { it.matches(moduleData.group) } }.toSet()
                )
            }.toSet(),
            source.importedModules.map {
                ImportedModuleBundle(
                    it.name,
                    it.modules.filter { moduleData -> !regexs.any { it.matches(moduleData.name) } })
            }.toList(),
        )
}
