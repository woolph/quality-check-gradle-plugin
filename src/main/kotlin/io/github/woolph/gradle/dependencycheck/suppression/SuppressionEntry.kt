/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.dependencycheck.suppression

import io.github.woolph.gradle.util.children
import io.github.woolph.gradle.util.get
import io.github.woolph.gradle.util.processXml
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.temporal.TemporalQueries
import java.util.*

data class SuppressionEntry(
    val packageUrl: String,
    val vulnerabilities: List<Vulnerability>,
    val notes: String? = null,
    val suppressUntil: ZonedDateTime? = null,
    val packageUrlPattern: Boolean = true,
) {
    fun asXmlTag(desiredZoneId: ZoneId) = buildString {
        if (suppressUntil != null) {
            appendLine(
                "    <suppress until=\"${DATE_FORMATTER_SUPPRESS_UNTIL.format(suppressUntil.withZoneSameInstant(desiredZoneId))}\">")
        } else {
            appendLine("    <suppress>")
        }
        if (notes != null) {
            appendLine(
                """        <notes><![CDATA[
            ${notes.trim().replace(Regex("(\r?\n)\\s*")) { "${it.groups[1]?.value!!}            " }}
        ]]></notes>""",
            )
        }
        if (packageUrlPattern) {
            appendLine("        <packageUrl regex=\"true\">$packageUrl</packageUrl>")
        } else {
            appendLine("        <packageUrl>$packageUrl</packageUrl>")
        }
        vulnerabilities.forEach {
            appendLine("        <${it.type.nodeName}>${it.name}</${it.type.nodeName}>")
        }
        appendLine("    </suppress>")
    }

    companion object {
        val DATE_FORMATTER_SUPPRESS_UNTIL = DateTimeFormatter.ISO_DATE

        private val DATE_FORMATTER_SUPPRESS_UNTIL_PARSER =
            DateTimeFormatterBuilder()
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0L)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0L)
                .parseDefaulting(ChronoField.SECOND_OF_DAY, 0L)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0L)
                .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0L)
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .optionalStart()
                .appendLiteral("T")
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .optionalEnd()
                .optionalStart()
                .appendOffsetId()
                .parseDefaulting(ChronoField.NANO_OF_DAY, 0L)
                .toFormatter(Locale.getDefault(Locale.Category.FORMAT))

        val DEFAULT_ZONE_ID = ZoneId.of("UTC")

        fun parseToZoneDateTime(dateTime: String): ZonedDateTime {
            val temporalAccessor = DATE_FORMATTER_SUPPRESS_UNTIL_PARSER.parse(dateTime)
            return LocalDateTime.from(temporalAccessor)
                .atZone(temporalAccessor.query<ZoneId>(TemporalQueries.zone()) ?: DEFAULT_ZONE_ID)
        }
    }
}

fun Sequence<SuppressionEntry>.writeTo(file: File, desiredZoneId: ZoneId) {
    file.writeText(
        buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine(
                "<suppressions xmlns=\"https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd\">")
            this@writeTo.sortedBy { it.suppressUntil }
                .forEach {
                    append(it.asXmlTag(desiredZoneId))
                    appendLine()
                }
            appendLine("</suppressions>")
        },
    )
}

fun File.parseAsDependencyCheckSuppressionFile() = processXml { doc ->
    doc.children().flatMap { suppressions ->
        suppressions.children().mapNotNull { suppression ->
            val suppressUntil =
                suppression.attributes["until"]?.value?.let {
                    SuppressionEntry.parseToZoneDateTime(it)
                }
            val (packageUrlPattern: Boolean?, packageUrl: String?) =
                suppression
                    .children()
                    .firstOrNull { it.nodeName == "packageUrl" }
                    ?.let { it.attributes["regex"]?.value?.let { it == "true" } to it.textContent }
                    ?: (null to null)
            val suppressionNote =
                suppression.children().filter { it.nodeName == "notes" }.firstOrNull()?.textContent

            if (packageUrl != null) {
                SuppressionEntry(
                    packageUrl,
                    suppression.children().mapNotNull { Vulnerability.from(it) }.toList(),
                    notes = suppressionNote,
                    suppressUntil = suppressUntil,
                    packageUrlPattern = packageUrlPattern ?: false,
                )
            } else {
                null
            }
        }
    }
}
