package io.github.woolph.gradle

import org.gradle.api.provider.Property

interface Skipable {
    val skip: Property<Boolean>
}
