/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle

import io.github.woolph.gradle.dependencycheck.suppression.toModuleString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StringUtilTests {
    @Test
    fun test() {
        assertEquals(
            "commons-fileupload:commons-fileupload:1.4",
            "pkg:maven/commons-fileupload/commons-fileupload@1.4".toModuleString())
        assertEquals(
            "com.fasterxml.jackson.core:jackson-core:2.15.1",
            "pkg:maven/com.fasterxml.jackson.core/jackson-core@2.15.1".toModuleString())
    }
}
