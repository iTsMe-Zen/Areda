package app.areada.data

import android.content.Context
import android.net.Uri
import org.w3c.dom.Node
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object LocalTextExtractor {
    private const val MAX_EXTRACTED_CHARS = 1_200_000

    fun readFb2(
        context: Context,
        uri: Uri,
    ): String =
        context.contentResolver.openInputStream(uri)?.use { input ->
            parseFb2OrZip(input)
        }.orEmpty()

    private fun parseFb2OrZip(input: InputStream): String {
        val buffered = BufferedInputStream(input)
        buffered.mark(4)
        val first = buffered.read()
        val second = buffered.read()
        buffered.reset()
        if (first == 'P'.code && second == 'K'.code) {
            ZipInputStream(buffered).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && entry.name.endsWith(".fb2", ignoreCase = true)) {
                        return parseFb2(zip)
                    }
                    zip.closeEntry()
                }
            }
            return ""
        }
        return parseFb2(buffered)
    }

    private fun parseFb2(input: InputStream): String {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        }
        val document = factory.newDocumentBuilder().parse(input)
        val builder = StringBuilder()

        firstElementText(document, "book-title")?.let { title ->
            appendBlock(builder, title)
        }
        val bodies = document.getElementsByTagName("body")
        if (bodies.length == 0) {
            appendBlock(builder, document.documentElement?.textContent.orEmpty())
        } else {
            for (index in 0 until bodies.length) {
                appendFb2Children(bodies.item(index), builder)
            }
        }

        return builder.toString().trim().take(MAX_EXTRACTED_CHARS)
    }

    private fun appendFb2Children(
        node: Node,
        builder: StringBuilder,
    ) {
        val children = node.childNodes
        for (index in 0 until children.length) {
            if (builder.length >= MAX_EXTRACTED_CHARS) {
                return
            }
            val child = children.item(index)
            val tag = child.nodeName.substringAfter(':').lowercase()
            when (tag) {
                "binary", "description" -> Unit
                "title", "subtitle", "p", "v" -> appendBlock(builder, child.textContent.orEmpty())
                "empty-line" -> appendNewline(builder)
                else -> appendFb2Children(child, builder)
            }
        }
    }

    private fun firstElementText(
        document: org.w3c.dom.Document,
        tagName: String,
    ): String? {
        val nodes = document.getElementsByTagName(tagName)
        for (index in 0 until nodes.length) {
            val text = nodes.item(index).textContent.orEmpty().compactWhitespace()
            if (text.isNotBlank()) {
                return text
            }
        }
        return null
    }

    private fun appendBlock(
        builder: StringBuilder,
        rawText: String,
    ) {
        val text = rawText.compactWhitespace()
        if (text.isBlank()) {
            return
        }
        if (builder.isNotEmpty() && !builder.endsWith("\n\n")) {
            builder.append("\n\n")
        }
        builder.append(text)
    }

    private fun appendNewline(builder: StringBuilder) {
        if (builder.isNotEmpty() && !builder.endsWith("\n\n")) {
            builder.append("\n\n")
        }
    }

    private fun String.compactWhitespace(): String =
        replace(Regex("\\s+"), " ").trim()

    private fun StringBuilder.endsWith(suffix: String): Boolean =
        length >= suffix.length && substring(length - suffix.length, length) == suffix
}
