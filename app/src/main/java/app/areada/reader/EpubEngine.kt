package app.areada.reader

import android.content.Context
import android.net.Uri
import app.areada.data.ReaderPreferences
import app.areada.data.ReaderRenderPalette
import app.areada.data.ReaderThemeMode
import app.areada.data.renderPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document as JsoupHtmlDocument
import org.jsoup.nodes.Document.OutputSettings.Syntax
import org.jsoup.nodes.Entities
import org.jsoup.parser.Parser
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.util.Locale
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

data class EpubBook(
    val title: String,
    val extractedRoot: File,
    val chapters: List<EpubChapter>,
    val archiveFile: File? = null,
)

data class EpubChapter(
    val title: String,
    val file: File,
    val archivePath: String = "",
)

data class RenderedChapter(
    val title: String,
    val baseUrl: String,
    val html: String,
)

object EpubEngine {
    suspend fun parse(context: Context, uri: Uri, fallbackTitle: String): EpubBook =
        withContext(Dispatchers.IO) {
            parseBlocking(context, uri, fallbackTitle)
        }

    suspend fun render(
        book: EpubBook,
        chapterIndex: Int,
        preferences: ReaderPreferences,
        paletteOverride: ReaderRenderPalette? = null,
    ): RenderedChapter =
        withContext(Dispatchers.IO) {
            renderBlocking(book, chapterIndex, preferences, paletteOverride)
        }

    private fun parseBlocking(context: Context, uri: Uri, fallbackTitle: String): EpubBook {
        val root = File(
            context.cacheDir,
            "areada/epub/${uri.toString().hashCode().toUInt().toString(16)}",
        )
        return try {
            val readyMarker = File(root, ".areada_epub_ready_v2")
            val archiveFile = File(root, "book.epub")
            if ((!readyMarker.exists() || !archiveFile.isFile) && root.exists()) {
                root.deleteRecursively()
            }
            if (!archiveFile.isFile) {
                root.mkdirs()
                copyArchive(context, uri, archiveFile)
            }

            ZipFile(archiveFile).use { zip ->
                val containerDocument = readZipXml(zip, "META-INF/container.xml")
                val rootFile = containerDocument
                    .getElementsByTagNameNS("*", "rootfile")
                    .item(0) as? Element
                    ?: throw IllegalArgumentException("Invalid or unsupported EPUB file.")

                val packagePath = rootFile.getAttribute("full-path")
                    .replace('\\', '/')
                    .trimStart('/')
                val packageDocument = readZipXml(zip, packagePath)

                val manifest = buildManifest(packageDocument)
                val packageDirectory = packagePath.substringBeforeLast('/', "")
                val chapters = buildChapters(root, zip, packageDocument, manifest, packageDirectory)
                require(chapters.isNotEmpty()) { "This EPUB does not contain readable chapters." }

                val title = packageDocument
                    .getElementsByTagNameNS("*", "title")
                    .item(0)
                    ?.textContent
                    ?.trim()
                    ?.ifBlank { null }
                    ?: fallbackTitle

                readyMarker.writeText("ready")
                EpubBook(
                    title = title,
                    extractedRoot = root,
                    chapters = chapters,
                    archiveFile = archiveFile,
                )
            }
        } catch (throwable: Throwable) {
            root.deleteRecursively()
            throw IllegalArgumentException(cleanEpubError(throwable), throwable)
        }
    }

    private fun renderBlocking(
        book: EpubBook,
        chapterIndex: Int,
        preferences: ReaderPreferences,
        paletteOverride: ReaderRenderPalette?,
    ): RenderedChapter {
        val chapter = book.chapters.getOrNull(chapterIndex) ?: error("Chapter not found.")
        ensureChapterExtracted(book, chapter)
        val rawHtml = chapter.file.readText()
        val baseUrl = chapter.file.parentFile?.toURI()?.toString().orEmpty()
        val palette = paletteOverride ?: preferences.themeMode.renderPalette()
        val fontSize = preferences.fontSizeSp.coerceIn(14, 30)
        val lineSpacing = preferences.lineSpacing.coerceIn(1.2f, 2.4f)
        val colorScheme = if (preferences.themeMode == ReaderThemeMode.DARK) "dark" else "light"
        val normalizedDocument = Jsoup.parse(rawHtml, baseUrl, Parser.htmlParser())
        ensureReferencedAssets(book, chapter, normalizedDocument)
        val renderedDocument = buildRenderedDocument(
            sourceDocument = normalizedDocument,
            baseUrl = baseUrl,
        )

        renderedDocument.outputSettings()
            .prettyPrint(false)
            .syntax(Syntax.html)
            .escapeMode(Entities.EscapeMode.base)
            .charset(Charsets.UTF_8)

        val head = renderedDocument.head()
        head.prependElement("meta")
            .attr("name", "viewport")
            .attr("content", "width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes")
        head.prependElement("meta")
            .attr("charset", "utf-8")
        head.appendElement("style").appendText(
            """
            :root {
              color-scheme: $colorScheme;
            }
            html {
              -webkit-text-size-adjust: none !important;
              text-size-adjust: none !important;
            }
            html, body {
              margin: 0;
              padding: 0;
              background: ${palette.backgroundHex};
              color: ${palette.textHex};
              font-family: ${preferences.fontChoice.cssFamily};
              font-size: ${fontSize}px !important;
              line-height: $lineSpacing !important;
            }
            body {
              padding: 76px 18px 132px;
              word-break: break-word;
              overflow-wrap: anywhere;
            }
            body * {
              box-sizing: border-box;
              max-width: 100%;
              font-family: inherit !important;
              font-size: inherit !important;
              line-height: inherit !important;
              text-decoration: none !important;
              text-decoration-line: none !important;
            }
            p, div, span, li, td, th, blockquote, pre, em, strong, i, b, u, a, font, small, sup, sub, ins, section, article {
              font-size: inherit !important;
              line-height: inherit !important;
              font-family: inherit !important;
              text-decoration: none !important;
              text-decoration-line: none !important;
            }
            h1 {
              font-size: ${fontSize + 8}px !important;
            }
            h2 {
              font-size: ${fontSize + 5}px !important;
            }
            h3, h4, h5, h6 {
              font-size: ${fontSize + 3}px !important;
            }
            p, li {
              margin-top: 0;
              margin-bottom: 1em;
            }
            img, svg, video, picture, figure {
              display: block;
              max-width: 100% !important;
              height: auto !important;
              margin: 1.25rem auto;
              object-fit: contain;
              image-rendering: auto;
            }
            a, a:link, a:visited {
              color: ${palette.accentHex};
              text-decoration: none !important;
              text-decoration-line: none !important;
              border-bottom: 0 !important;
            }
            a * {
              color: inherit !important;
            }
            u, ins {
              text-decoration: none !important;
              text-decoration-line: none !important;
              border-bottom: 0 !important;
            }
            table {
              display: block;
              width: 100% !important;
              overflow-x: auto;
              border-collapse: collapse;
            }
            blockquote, pre {
              background: ${palette.surfaceHex};
              color: ${palette.textHex};
              border-radius: 0;
              padding: 16px;
              overflow: auto;
            }
            hr {
              border: 0;
              border-top: 1px solid ${palette.mutedHex};
              margin: 1.5rem 0;
            }
            """.trimIndent(),
        )
        val html = renderedDocument.outerHtml()

        return RenderedChapter(
            title = chapter.title,
            baseUrl = chapter.file.parentFile?.toURI()?.toString() ?: book.extractedRoot.toURI().toString(),
            html = html,
        )
    }

    private fun buildRenderedDocument(
        sourceDocument: JsoupHtmlDocument,
        baseUrl: String,
    ): JsoupHtmlDocument {
        sourceDocument.select("script").remove()
        sourceDocument.select("[style]").removeAttr("style")

        val renderedDocument = Jsoup.parse(
            "<!doctype html><html><head></head><body></body></html>",
            baseUrl,
            Parser.htmlParser(),
        )
        val sourceHtml = sourceDocument.selectFirst("html")
        val sourceHead = sourceDocument.head()
        val sourceBody = sourceDocument.body()
        val renderedHtml = renderedDocument.selectFirst("html")
        val renderedBody = renderedDocument.body()
        val renderedHead = renderedDocument.head()

        sourceHtml?.attributes()?.forEach { attribute ->
            renderedHtml?.attr(attribute.key, attribute.value)
        }
        sourceBody.attributes().forEach { attribute ->
            renderedBody.attr(attribute.key, attribute.value)
        }

        sourceHead.children()
            .filterNot { child ->
                when (child.normalName()) {
                    "meta" -> child.hasAttr("charset") || child.hasAttr("http-equiv")
                    "script", "style" -> true
                    "link" -> child.attr("rel").contains("stylesheet", ignoreCase = true)
                    else -> false
                }
            }
            .forEach { child ->
                renderedHead.appendChild(child.clone())
            }

        renderedBody.html(sourceBody.html())
        return renderedDocument
    }

    private fun buildManifest(document: Document): Map<String, ManifestItem> {
        val nodes = document.getElementsByTagNameNS("*", "item")
        return buildMap {
            repeat(nodes.length) { index ->
                val element = nodes.item(index) as? Element ?: return@repeat
                val id = element.getAttribute("id").trim()
                val href = element.getAttribute("href").trim()
                val mediaType = element.getAttribute("media-type").trim()
                if (id.isNotEmpty() && href.isNotEmpty()) {
                    put(id, ManifestItem(href, mediaType))
                }
            }
        }
    }

    private fun buildChapters(
        root: File,
        zip: ZipFile,
        document: Document,
        manifest: Map<String, ManifestItem>,
        packageDirectory: String,
    ): List<EpubChapter> {
        val nodes = document.getElementsByTagNameNS("*", "itemref")
        return buildList {
            repeat(nodes.length) { index ->
                val element = nodes.item(index) as? Element ?: return@repeat
                val idRef = element.getAttribute("idref").trim()
                val manifestItem = manifest[idRef] ?: return@repeat

                if (!isReadableChapter(manifestItem)) {
                    return@repeat
                }

                val archivePath = normalizeArchivePath(packageDirectory, manifestItem.href)
                if (zip.getEntry(archivePath) == null) {
                    return@repeat
                }

                add(
                    EpubChapter(
                        title = prettyChapterTitle(manifestItem.href, index),
                        file = resolveInside(root, archivePath),
                        archivePath = archivePath,
                    ),
                )
            }
        }
    }

    private fun copyArchive(context: Context, uri: Uri, archiveFile: File) {
        archiveFile.parentFile?.mkdirs()
        val tempFile = File(archiveFile.parentFile, "${archiveFile.name}.tmp")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output, DEFAULT_BUFFER_SIZE * 4)
            }
        } ?: throw IllegalArgumentException("Unable to read that EPUB.")

        if (archiveFile.exists()) {
            archiveFile.delete()
        }
        if (!tempFile.renameTo(archiveFile)) {
            tempFile.copyTo(archiveFile, overwrite = true)
            tempFile.delete()
        }
    }

    private fun readZipXml(zip: ZipFile, archivePath: String): Document {
        val normalizedPath = normalizeArchivePath("", archivePath)
        val entry = zip.getEntry(normalizedPath)
            ?: throw IllegalArgumentException("Invalid or unsupported EPUB file.")
        return zip.getInputStream(entry).use { input ->
            parseXml(input)
        }
    }

    private fun ensureChapterExtracted(book: EpubBook, chapter: EpubChapter) {
        if (chapter.file.isFile) {
            return
        }
        val archiveFile = book.archiveFile ?: return
        val archivePath = chapter.archivePath.ifBlank {
            chapter.file.relativeToOrNull(book.extractedRoot)?.invariantSeparatorsPath.orEmpty()
        }
        if (archivePath.isBlank()) {
            return
        }

        ZipFile(archiveFile).use { zip ->
            extractEntry(zip, book.extractedRoot, archivePath)
                ?: throw IllegalArgumentException("Invalid or unsupported EPUB file.")
        }
    }

    private fun ensureReferencedAssets(
        book: EpubBook,
        chapter: EpubChapter,
        document: JsoupHtmlDocument,
    ) {
        val archiveFile = book.archiveFile ?: return
        val chapterDirectory = chapter.archivePath.substringBeforeLast('/', "")
        val archivePaths = linkedSetOf<String>()

        document.select("[src], [href], [poster]").forEach { element ->
            archiveReferenceFrom(chapterDirectory, element.attr("src"))?.let { archivePaths += it }
            archiveReferenceFrom(chapterDirectory, element.attr("href"))?.let { archivePaths += it }
            archiveReferenceFrom(chapterDirectory, element.attr("poster"))?.let { archivePaths += it }
        }
        document.select("[srcset]").forEach { element ->
            element.attr("srcset")
                .split(',')
                .map { candidate -> candidate.trim().substringBefore(' ').trim() }
                .forEach { candidate ->
                    archiveReferenceFrom(chapterDirectory, candidate)?.let { archivePaths += it }
                }
        }

        if (archivePaths.isEmpty()) {
            return
        }

        ZipFile(archiveFile).use { zip ->
            archivePaths.forEach { archivePath ->
                extractEntry(zip, book.extractedRoot, archivePath)
            }
        }
    }

    private fun extractEntry(zip: ZipFile, root: File, archivePath: String): File? {
        val normalizedPath = normalizeArchivePath("", archivePath)
        if (normalizedPath.isBlank()) {
            return null
        }
        val target = resolveInside(root, normalizedPath)
        if (target.isFile || target.isDirectory) {
            return target
        }

        val entry = zip.getEntry(normalizedPath) ?: return null
        if (entry.isDirectory) {
            target.mkdirs()
            return target
        }

        target.parentFile?.mkdirs()
        val tempFile = File(target.parentFile, "${target.name}.tmp")
        zip.getInputStream(entry).use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output, DEFAULT_BUFFER_SIZE * 4)
            }
        }
        if (target.exists()) {
            target.delete()
        }
        if (!tempFile.renameTo(target)) {
            tempFile.copyTo(target, overwrite = true)
            tempFile.delete()
        }
        return target
    }

    private fun archiveReferenceFrom(basePath: String, rawReference: String): String? {
        val reference = rawReference
            .trim()
            .substringBefore('#')
            .substringBefore('?')
            .trim()
        if (reference.isBlank() || isExternalReference(reference)) {
            return null
        }

        return normalizeArchivePath(basePath, decodeArchivePath(reference))
            .takeIf { it.isNotBlank() }
    }

    private fun decodeArchivePath(reference: String): String =
        runCatching { URI(reference).path ?: reference }.getOrDefault(reference)

    private fun isExternalReference(reference: String): Boolean {
        val lower = reference.lowercase(Locale.ROOT)
        return lower.startsWith("//") ||
            lower.startsWith("data:") ||
            lower.startsWith("http:") ||
            lower.startsWith("https:") ||
            lower.startsWith("mailto:") ||
            lower.startsWith("tel:") ||
            lower.startsWith("javascript:") ||
            lower.startsWith("file:") ||
            lower.startsWith("android.resource:")
    }    private fun parseXml(file: File): Document =
        file.inputStream().use { input -> parseXml(input) }

    private fun parseXml(inputStream: InputStream): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        return factory.newDocumentBuilder().parse(inputStream)
    }

    private fun resolveInside(root: File, archivePath: String): File {
        val rootPath = root.canonicalFile.toPath()
        val candidate = File(root, archivePath.replace('/', File.separatorChar)).canonicalFile
        require(candidate.toPath().startsWith(rootPath)) { "Unsafe EPUB path detected." }
        return candidate
    }

    private fun normalizeArchivePath(basePath: String, relativePath: String): String {
        val segments = mutableListOf<String>()
        val combined = listOf(basePath, relativePath)
            .filter { it.isNotBlank() }
            .joinToString("/")
            .replace('\\', '/')

        combined.split("/").forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
                else -> segments += part
            }
        }

        return segments.joinToString("/")
    }

    private fun isReadableChapter(item: ManifestItem): Boolean {
        val lowerHref = item.href.lowercase(Locale.ROOT)
        return item.mediaType.contains("html", ignoreCase = true) ||
            lowerHref.endsWith(".xhtml") ||
            lowerHref.endsWith(".html") ||
            lowerHref.endsWith(".htm")
    }

    private fun cleanEpubError(throwable: Throwable): String {
        val message = throwable.message.orEmpty()
        return when {
            message.contains("readable chapters", ignoreCase = true) -> "This EPUB does not contain readable chapters."
            message.contains("Unable to read", ignoreCase = true) -> "Unable to read that EPUB."
            else -> "Invalid or unsupported EPUB file."
        }
    }

    private fun prettyChapterTitle(href: String, index: Int): String {
        val stem = href
            .substringAfterLast('/')
            .substringBeforeLast('.', "")
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()

        if (stem.isBlank() || stem.equals("index", ignoreCase = true)) {
            return "Chapter ${index + 1}"
        }

        return stem.split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.replaceFirstChar { char ->
                    if (char.isLowerCase()) {
                        char.titlecase(Locale.ROOT)
                    } else {
                        char.toString()
                    }
                }
            }
    }

    private data class ManifestItem(
        val href: String,
        val mediaType: String,
    )
}
