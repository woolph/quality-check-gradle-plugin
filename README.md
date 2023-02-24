# quality-check-gradle-plugin

`quality-check` is a gradle plugin which adds and preconfigures other plugins which are essential to ensure code quality
and security.

## Quick setup
Add the plugin with the id `io.github.woolph.quality-check` to your gradle build:

>####Kotlin DSL Sample *build.gradle.kts*
>```kotlin
>plugins {
>    id("io.github.woolph.quality-check") version "1.1.1"
>}
>```

>####Groovy DSL Sample *build.gradle*
>```groovy
>plugins {
>    id 'io.github.woolph.quality-check' version '1.1.1'
>}
>```

## What does it do?
The quality check adds several other plugins to your gradle build with the intent to ensure code quality and code 
security. More precisely, it adds the following plugins:
* `org.owasp:dependency-check-gradle:8.1.0` which checks your 3rd party dependencies for known vulnerabilities
* `org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:4.0.0.2929` which contacts a sonarQube server to do code analysis
* `com.github.jk1:gradle-license-report:2.1` which checks the licenses of your 3rd party dependencies

Your gradle build is automatically configured in a way that the `check` of your build depends upon 
all the relevant tasks introduced by these plugins. Here is what it would look like, if you'd have to do this
explicitly (but you DON'T have to do that, 'cause it is already setup this way!):

````kotlin
tasks.check {
    dependsOn(
        tasks.dependencyCheckAnalyze,
        tasks.sonar,
        tasks.checkLicense,
    )
}
````

The `check` task (which also depends on the `test` task executing the unit tests of your project) will be executed 
with every execution of the `build` task, so you do not have to do anything except for adding the plugin to your build.

In some cases, some tweaking of the checks performed may be necessary to better fit your projects purpose.

### Dependency Check
### Sonarqube
### License Report
In software projects, you may want to take a close look on the licenses of your 3rd party dependencies, because some of 
the commonly used licenses could force you to disclose your source code, which may be detrimental especially for closed 
source projects. The license report plugin allows you to define a set of licenses which you consider "safe" and the 
`checkLicense` (which the `check` task will depend upon) checks whether the licenses of your 3rd party dependencies are
within this set. If not, the build fails.