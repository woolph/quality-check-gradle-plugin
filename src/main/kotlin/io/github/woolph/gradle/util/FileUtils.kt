/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.util

import org.gradle.api.file.RegularFileProperty
import java.io.File

fun File?.filterExists(): File? = if (this?.exists() != true) null else this

val RegularFileProperty.asFileIfExists: File?
    get() = asFile.orNull?.filterExists()
