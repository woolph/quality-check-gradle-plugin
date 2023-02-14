plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.pluginPublish)

    alias(libs.plugins.spotless)
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
    implementation(libs.kotlinxDatetime)

    runtimeOnly(libs.databaseDrivers.postgres)
    runtimeOnly(libs.databaseDrivers.mssql)

    //region unit test dependencies
    testImplementation(gradleTestKit())
    testImplementation(libs.test.junit)
    //endregion
}

java {
    sourceCompatibility = libs.versions.jvmTarget.map { JavaVersion.toVersion(it) }.get()

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
