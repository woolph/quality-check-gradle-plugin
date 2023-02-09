/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle

import org.gradle.api.provider.Property

interface Skipable {
    /**
     * skips the corresponding check task if set to true.
     */
    val skip: Property<Boolean>
}
