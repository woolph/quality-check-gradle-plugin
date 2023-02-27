package io.github.woolph.gradle.licensecheck

import com.github.jk1.license.ProjectData
import com.github.jk1.license.filter.DependencyFilter
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

class OnDemandBundleNormalizerFilter(
    val additonalLicenseNormalizerBundle: Provider<RegularFile>,
): DependencyFilter {
    override fun filter(source: ProjectData?): ProjectData {
        val licenseBundleNormalizer = com.github.jk1.license.filter.LicenseBundleNormalizer(mapOf(
            "bundlePath" to additonalLicenseNormalizerBundle.get().asFile.toString(),
            "createDefaultTransformationRules" to true,
        ))

        return licenseBundleNormalizer.filter(source)
    }
}
