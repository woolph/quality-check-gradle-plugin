/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.licensecheck

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class WhiteListedDependency(
    val moduleNamePattern: Regex,
    val validUntil: Instant = Instant.DISTANT_FUTURE,
) {
  fun isValid(clock: Clock) = validUntil >= clock.now()

  fun isExpired(clock: Clock) = !isValid(clock)

  override fun toString() =
      "WhiteListedDependency(moduleNamePattern=$moduleNamePattern, validUntil=$validUntil)"
}
