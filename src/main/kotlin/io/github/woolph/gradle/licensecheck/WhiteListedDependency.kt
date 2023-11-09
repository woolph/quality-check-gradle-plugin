/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.licensecheck

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class WhiteListedDependency(
    val moduleNamePattern: Regex,
    val validUntil: Instant = Instant.DISTANT_FUTURE,
) {
    fun isValid(clock: Clock) = validUntil >= clock.now()

    fun isExpired(clock: Clock) = !isValid(clock)

    override fun toString() =
        "WhiteListedDependency(moduleNamePattern=$moduleNamePattern, validUntil=$validUntil)"
}
