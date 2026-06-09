import org.gradle.plugin.compatibility.compatibility

plugins {
  `kotlin-dsl`
  `maven-publish`
  jacoco
  alias(libs.plugins.pluginPublish)
  alias(libs.plugins.spotless)
}

group = "io.github.woolph.quality-check"

version = "3.1.0"

gradlePlugin {
  website.set("https://github.com/woolph/quality-check-gradle-plugin")
  vcsUrl.set("https://github.com/woolph/quality-check-gradle-plugin")
  plugins {
    create("quality-check") {
      id = "io.github.woolph.quality-check"
      implementationClass = "io.github.woolph.gradle.QualityCheckPlugin"
      displayName = "ENGEL Quality Check"
      description = "Adds dependency check and license check to your build."
      tags.set(listOf("code quality", "security"))

      compatibility { // extension added by the Compatibility plugin
        features {
          configurationCache = true // or false if the feature isn't supported
        }
      }
    }
  }
}

repositories {
  maven(url = "https://plugins.gradle.org/m2/")
  mavenCentral()
}

dependencies {
  implementation(libs.dependencyCheck)
  implementation(libs.licenseReport)
  implementation(libs.kotlinxDatetime)
  implementation(libs.kotlinxSerialization.json)

  runtimeOnly(libs.databaseDrivers.postgres)
  runtimeOnly(libs.databaseDrivers.mssql)

  // region unit test dependencies
  testImplementation(gradleTestKit())
  testImplementation(platform(libs.test.junit.bom))
  testImplementation(libs.test.junit.params)
  testRuntimeOnly(libs.test.junit.engine)
  testRuntimeOnly(libs.test.junit.launcher)
  // endregion
}

kotlin { jvmToolchain(libs.versions.jvmTarget.map { it.toInt() }.get()) }

java {
  withSourcesJar()
  withJavadocJar()
}

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport)
}

spotless {
  kotlin {
    ratchetFrom("origin/main")
    ktfmt()
    licenseHeader("/* Copyright \$YEAR ENGEL Austria GmbH */")
  }
  kotlinGradle { ktfmt() }
}
