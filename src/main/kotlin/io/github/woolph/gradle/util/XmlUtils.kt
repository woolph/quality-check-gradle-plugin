/* Copyright 2023 ENGEL Austria GmbH */
package io.github.woolph.gradle.util

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

fun <R> File.processXml(
    dbFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance(),
    block: (org.w3c.dom.Document) -> R,
): R =
    inputStream().use { inputStream ->
        block(dbFactory.newDocumentBuilder().parse(InputSource(inputStream)))
    }

fun org.w3c.dom.NodeList.asSequence(): Sequence<org.w3c.dom.Node> =
    (0 until length).asSequence().map(this::item)

fun org.w3c.dom.Node.children(): Sequence<org.w3c.dom.Node> = childNodes.asSequence()

fun Sequence<org.w3c.dom.Node>.children(): Sequence<org.w3c.dom.Node> =
    flatMap(org.w3c.dom.Node::children)

operator fun org.w3c.dom.NamedNodeMap?.get(name: String): org.w3c.dom.Attr? =
    this?.getNamedItem(name)?.let { it as org.w3c.dom.Attr }
