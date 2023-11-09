/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.dependencycheck.suppression

private val pattern = Regex("^pkg:maven/(?<group>.*)/(?<name>.*)@(?<version>.*)$")

fun String.toModuleString(): String? =
    pattern.matchEntire(this)?.let {
        "${it.groups["group"]?.value}:${it.groups["name"]?.value}:${it.groups["version"]?.value}"
    }
