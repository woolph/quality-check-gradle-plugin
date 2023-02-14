# quality-check-gradle-plugin

`quality-check` is a gradle plugin which adds and preconfigures other plugins which are essential to ensure code quality
and security.

## Quick setup
Add the plugin with the id `io.github.woolph.quality-check` to your gradle build:

>####Kotlin DSL Sample *build.gradle.kts*
>```kotlin
>plugins {
>    id("io.github.woolph.quality-check") version "1.1.0"
>}
>```

>####Groovy DSL Sample *build.gradle*
>```groovy
>plugins {
>    id 'io.github.woolph.quality-check' version '1.1.0'
>}
>```

## What does it do?
The quality check adds several other plugins to your gradle build with the intent to ensure code quality and code 
security. More precisely, it adds the following plugins:
* `org.owasp:dependency-check-gradle:7.4.4` which checks your 3rd party dependencies for known vulnerabilities
* `org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.5.0.2730` which contacts a sonarQube server to do code analysis
* `com.github.jk1:gradle-license-report:2.1` which checks the licenses of your 3rd party dependencies