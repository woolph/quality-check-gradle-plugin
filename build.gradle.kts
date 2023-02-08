@Suppress("DSL_SCOPE_VIOLATION") // remove once https://youtrack.jetbrains.com/issue/KTIJ-19369 is fixed
plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.pluginPublish)
}

group = "io.github.woolph.quality-check"
version = "1.1.0"

pluginBundle {
    website = "https://github.com/woolph/quality-check-gradle-plugin"
    vcsUrl = "https://github.com/woolph/quality-check-gradle-plugin"
    tags = listOf("code quality", "security")
}

gradlePlugin {
    plugins {
        create("quality-check") {
            id = "io.github.woolph.quality-check"
            implementationClass = "io.github.woolph.gradle.QualityCheckPlugin"
            displayName = "ENGEL Quality Check"
            description = "Adds dependency check, license check, and sonarqube to your build."
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

    runtimeOnly(libs.databaseDrivers.postgres)
    runtimeOnly(libs.databaseDrivers.mssql)

    //region unit test dependencies
    testImplementation(gradleTestKit())
    testImplementation(libs.test.junit)
    //endregion
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8

    withSourcesJar()
    withJavadocJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
