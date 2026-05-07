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
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

data class EpubBook(
    val title: String,
    val extractedRoot: File,
    val chapters: List<EpubChapter>,
)

data class EpubChapter(
    val title: String,
    val file: File,
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
            val readyMarker = File(root, ".areada_epub_ready")
            val containerFile = resolveInside(root, "META-INF/container.xml")
            if ((!readyMarker.exists() || !containerFile.isFile) && root.exists()) {
                root.deleteRecursively()
            }
            if (!readyMarker.exists()) {
                root.mkdirs()

                context.contentResolver.openInputStream(uri)?.use { input ->
                    extractArchive(root, input)
                } ?: throw IllegalArgumentException("Unable to read that EPUB.")

                if (!containerFile.isFile) {
                    throw IllegalArgumentException("Invalid or unsupported EPUB file.")
                }
                readyMarker.writeText("ready")
            }

            val containerDocument = parseXml(containerFile)
            val rootFile = containerDocument
                .getElementsByTagNameNS("*", "rootfile")
                .item(0) as? Element
                ?: throw IllegalArgumentException("Invalid or unsupported EPUB file.")

            val packagePath = rootFile.getAttribute("full-path")
                .replace('\\', '/')
                .trimStart('/')
            val packageFile = resolveInside(root, packagePath)
            if (!packageFile.isFile) {
                throw IllegalArgumentException("Invalid or unsupported EPUB file.")
            }
            val packageDocument = parseXml(packageFile)

            val manifest = buildManifest(packageDocument)
            val packageDirectory = packagePath.substringBeforeLast('/', "")
            val chapters = buildChapters(root, packageDocument, manifest, packageDirectory)
            require(chapters.isNotEmpty()) { "This EPUB does not contain readable chapters." }

            val title = packageDocument
                .getElementsByTagNameNS("*", "title")
                .item(0)
                ?.textContent
                ?.trim()
                ?.ifBlank { null }
                ?: fallbackTitle

            EpubBook(
                title = title,
                extractedRoot = root,
                chapters = chapters,
            )
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
        val rawHtml = chapter.file.readText()
        val baseUrl = chapter.file.parentFile?.toURI()?.toString().orEmpty()
        val palette = paletteOverride ?: preferences.themeMode.renderPalette()
        val fontSize = preferences.fontSizeSp.coerceIn(14, 30)
        val lineSpacing = preferences.lineSpacing.coerceIn(1.2f, 2.4f)
        val colorScheme = if (preferences.themeMode == ReaderThemeMode.DARK) "dark" else "light"
        val normalizedDocument = Jsoup.parse(rawHtml, baseUrl, Parser.htmlParser())
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
                val chapterFile = resolveInside(root, archivePath)
                if (!chapterFile.exists()) {
                    return@repeat
                }

                add(
                    EpubChapter(
                        title = prettyChapterTitle(manifestItem.href, index),
                        file = chapterFile,
                    ),
                )
            }
        }
    }

    private fun extractArchive(root: File, inputStream: java.io.InputStream) {
        ZipInputStream(inputStream).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val archivePath = normalizeArchivePath("", entry.name)
                if (archivePath.isBlank()) {
                    zip.closeEntry()
                    continue
                }

                val target = resolveInside(root, archivePath)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output ->
                        zip.copyTo(output)
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun parseXml(file: File): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        return file.inputStream().use { input ->
            factory.newDocumentBuilder().parse(input)
        }
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
