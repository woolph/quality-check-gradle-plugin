package io.github.woolph.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import javax.inject.Inject

abstract class SonarqubeExtension @Inject constructor(project: Project): Skipable {
    override val skip: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(false)

    /**
     * defines the edition of the sonarqube server (currently, this information is used to skip the sonarqube analyzis
     * on Community edition servers for Pull Request builds, because pull request analyzis is not supported by the
     * Community edition)
     *
     * This property can also be set by calling the gradle build with the following argument
     * -Psonarqube.edition={edition}.
     * It is set to UNKNOWN by default.
     */
    val edition: Property<SonarQubeEdition> = project.objects.property(SonarQubeEdition::class.java)
        .convention(SonarQubeEdition.of(project.properties["sonarqube.edition"].toString()) ?: SonarQubeEdition.UNKNOWN)

    companion object {
        fun Project.applySonarQubeExtension(baseExtension: ExtensionAware) {
            val sonarqubeExtension = baseExtension.extensions.create("sonarQube", SonarqubeExtension::class, project)

            try {
                val check = tasks.named("check")
                val test = tasks.named<org.gradle.api.tasks.testing.Test>("test")

                plugins.apply("jacoco")

                val jacocoTestReport = tasks.named<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoTestReport")

                plugins.apply("org.sonarqube")

                val sonarqube = tasks.named<org.sonarqube.gradle.SonarTask>("sonar")

                jacocoTestReport {
                    dependsOn(test)

                    reports {
                        html.required.set(true)
                        xml.required.set(true)
                    }
                }

                sonarqube {
                    dependsOn(jacocoTestReport)
                }

                check {
                    dependsOn(jacocoTestReport)
                    dependsOn(sonarqube)
                }

                afterEvaluate {
                    if (sonarqubeExtension.skip.get()) {
                        logger.warn("sonarqube is disabled!")
                    } else if (sonarqubeExtension.edition.get() == SonarQubeEdition.COMMUNITY && System.getenv("BUILD_REASON") == "PullRequest") {
                        logger.warn("sonarqube is running on Community Edition and build reason is PullRequest => skipping sonarqube!")
                        sonarqubeExtension.skip.set(true)
                    }

                    extensions.getByName<org.sonarqube.gradle.SonarExtension>("sonar").apply {
                        isSkipProject = sonarqubeExtension.skip.get()

                        properties {
                            property("sonar.projectKey", project.name)
                            property("sonar.jacoco.reportPaths", "build/jacoco/test.exec")
                            property("sonar.qualitygate.wait", true)
                        }
                    }
                }
            } catch (e: Exception) {
                println(e)
                logger.error("sonarqube will not be applied due to exception", e)
            }
        }
    }
}
