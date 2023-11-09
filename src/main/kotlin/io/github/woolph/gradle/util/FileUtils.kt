/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.util

import java.io.File
import org.gradle.api.file.RegularFileProperty

fun File?.filterExists(): File? = if (this?.exists() != true) null else this

val RegularFileProperty.asFileIfExists: File?
    get() = asFile.orNull?.filterExists()
