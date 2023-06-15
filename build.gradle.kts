import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberFunctions

plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.pluginPublish)

    alias(libs.plugins.spotless)
}

group = "io.github.woolph.quality-check"
version = "1.2.5"

gradlePlugin {
    website.set("https://github.com/woolph/quality-check-gradle-plugin")
    vcsUrl.set("https://github.com/woolph/quality-check-gradle-plugin")
    plugins {
        create("quality-check") {
            id = "io.github.woolph.quality-check"
            implementationClass = "io.github.woolph.gradle.QualityCheckPlugin"
            displayName = "ENGEL Quality Check"
            description = "Adds dependency check, license check, and sonarqube to your build."
            tags.set(listOf("code quality", "security"))
        }
    }
}

repositories {
    maven(url = "https://plugins.gradle.org/m2/")
    mavenCentral()
}

dependencies {
    implementation(libs.dependencyCheck)
    implementation(libs.sonarQube)
    implementation(libs.licenseReport)
    implementation(libs.kotlinxDatetime)
    implementation(libs.kotlinxSerialization.json)

    runtimeOnly(libs.databaseDrivers.postgres)
    runtimeOnly(libs.databaseDrivers.mssql)

    //region unit test dependencies
    testImplementation(gradleTestKit())
    testImplementation(libs.test.junit.params)
    testRuntimeOnly(libs.test.junit.engine)
    //endregion
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.map { it.toInt() }.get())
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    kotlin {
        ktlint(libs.versions.ktlint.get())
        licenseHeader("/* Copyright \$YEAR ENGEL Austria GmbH */")
    }
    kotlinGradle {
        ktlint(libs.versions.ktlint.get())
    }
}

tasks.create("updateReadmeVersions") {
    val readmeFile = layout.projectDirectory.file("README.md")

    inputs.file(readmeFile)
    inputs.property("version", version)
    outputs.file(readmeFile)

    doFirst {
        val readmeContent = readmeFile.asFile.readText()

        val versionPatternString = "\\d+(\\.\\w+)*(-\\w+(\\.\\w+)*)?"
        val versionPattern = Regex("(?<=:)$versionPatternString")
        val replacements = concat(
            project.extensions.findByType<GradlePluginDevelopmentExtension>()?.let {
                it.plugins.asSequence().flatMap { plugin ->
                    sequenceOf(
                        Regex("id\\(\"${Regex.escape(plugin.id)}\"\\) version \"$versionPatternString\"") to "id(\"${plugin.id}\") version \"$version\"",
                        Regex("id '${Regex.escape(plugin.id)}' version '$versionPatternString'") to "id '${plugin.id}' version '$version'",
                    )
                }
            } ?: emptySequence(),
            project.extensions.findByName("libs")?.let { libs ->
                libs.libSequence()
                    .map { it.toString() }.map { dependencyString ->
                        val dependencyStringWithoutVersion = dependencyString.replace(versionPattern, "")
                        Regex("${Regex.escape(dependencyStringWithoutVersion)}($versionPatternString)?") to dependencyString
                    }
            } ?: emptySequence(),
        )

        readmeFile.asFile.writeText(
            replacements.onEach { println(it) }
                .fold(readmeContent) { currentReadmeContent, (pattern, replacement) ->
                    currentReadmeContent.replace(pattern, replacement)
                },
        )
    }
}

fun <T> concat(vararg sequences: Sequence<T>): Sequence<T> = sequenceOf(*sequences).flatMap { it }

val providerType = Provider::class.createType(listOf(KTypeProjection.STAR))
val subDependencyFactoryType = org.gradle.api.internal.catalog.AbstractExternalDependencyFactory.SubDependencyFactory::class.createType()

fun Any.libSequence(): Sequence<MinimalExternalModuleDependency> {
    val memberFunctions = this::class.memberFunctions

    return concat(
        memberFunctions.asSequence()
            .filter { it.parameters.size == 1 && it.returnType.isSubtypeOf(providerType) }
            .map { (it.call(this) as Provider<*>).get() }
            .filterIsInstance<MinimalExternalModuleDependency>(),
        memberFunctions.asSequence()
            .filter { it.parameters.size == 1 && it.returnType.isSubtypeOf(subDependencyFactoryType) }
            .flatMap { it.call(this)?.libSequence() ?: emptySequence() },
    )
}
