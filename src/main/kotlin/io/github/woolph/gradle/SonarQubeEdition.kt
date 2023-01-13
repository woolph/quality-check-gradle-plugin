package io.github.woolph.gradle

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