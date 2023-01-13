# quality-check-gradle-plugin

`quality-check` is a gradle plugin which adds and preconfigures other plugins which are essential to ensure code quality
and security.

## Quick setup
Add the plugin with the id `io.github.woolph.quality-check` to your gradle build:

>####Kotlin DSL Sample *build.gradle.kts*
>```kotlin
>plugins {
>    id("io.github.woolph.quality-check") version "1.0.0"
>}
>```

>####Groovy DSL Sample *build.gradle*
>```groovy
>plugins {
>    id 'io.github.woolph.quality-check' version '1.0.0'
>}
>```