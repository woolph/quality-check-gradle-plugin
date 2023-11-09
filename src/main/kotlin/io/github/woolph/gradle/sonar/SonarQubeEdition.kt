/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.sonar

enum class SonarQubeEdition {
    UNKNOWN,
    COMMUNITY,
    DEVELOPER,
    ENTERPRISE,
    ;

    companion object {
        fun of(string: String?): SonarQubeEdition? =
            values().asSequence().firstOrNull { it.name.equals(string, true) }
    }
}
