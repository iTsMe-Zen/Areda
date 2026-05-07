package app.areada.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import java.util.Locale

object LibraryRepository {
    private const val MAX_SEARCH_RESULTS = 80
    private const val MAX_SEARCH_INDEX_ITEMS = 20_000
    private const val MAX_SEARCH_VISITED = 50_000
    private const val MAX_SEARCH_DEPTH = 18

    fun resolveRoot(
        context: Context,
        treeUri: Uri,
    ): LibraryRoot {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("Unable to read that folder.")
        val name = root.name?.ifBlank { null } ?: "Library"
        return LibraryRoot(
            treeUriString = treeUri.toString(),
            name = name,
        )
    }

    fun loadFolder(
        context: Context,
        root: LibraryRoot,
        relativePath: String,
    ): LibraryFolderState {
        val rootUri = Uri.parse(root.treeUriString)
        val rootFolder = DocumentFile.fromTreeUri(context, rootUri)
            ?: error("Unable to access the selected folder.")
        val currentFolder = resolveFolder(rootFolder, relativePath)
        val entries = runCatching { currentFolder.listFiles().toList() }
            .getOrElse { throw IllegalArgumentException("Unable to open that folder.") }

        val folders = entries
            .mapNotNull { folder ->
                runCatching {
                    if (!folder.isDirectory) {
                        return@runCatching null
                    }
                    val name = folder.name?.trim().orEmpty()
                    if (name.isBlank()) {
                        null
                    } else {
                        val folderPath = if (relativePath.isBlank()) name else "$relativePath/$name"
                        LibraryFolderEntry(
                            id = folderId(root, folderPath),
                            relativePath = folderPath,
                            name = name,
                        )
                    }
                }.getOrNull()
            }
            .sortedBy { it.name.lowercase(Locale.ROOT) }

        val books = entries
            .mapNotNull { file ->
                runCatching {
                    if (!file.isFile) {
                        return@runCatching null
                    }
                    val name = file.name?.trim().orEmpty()
                    if (name.isBlank()) {
                        return@runCatching null
                    }

                    val type = DocumentResolver.detectSupportedType(file.type, name) ?: return@runCatching null
                    LibraryBookEntry(
                        id = file.uri.toString(),
                        uriString = file.uri.toString(),
                        fileName = name,
                        title = name.substringBeforeLast('.', name).ifBlank { name },
                        type = type,
                    )
                }.getOrNull()
            }
            .sortedBy { it.title.lowercase(Locale.ROOT) }

        return LibraryFolderState(
            root = root,
            currentRelativePath = relativePath,
            pathSegments = buildPathSegments(root, relativePath),
            folders = folders,
            books = books,
        )
    }

    fun deleteFolder(
        context: Context,
        root: LibraryRoot,
        relativePath: String,
    ): Boolean {
        val rootFolder = DocumentFile.fromTreeUri(context, Uri.parse(root.treeUriString))
            ?: return false
        return resolveFolder(rootFolder, relativePath).delete()
    }

    fun deleteBook(
        context: Context,
        uriString: String,
    ): Boolean =
        DocumentFile.fromSingleUri(context, Uri.parse(uriString))?.delete() == true

    fun renameFolder(
        context: Context,
        root: LibraryRoot,
        relativePath: String,
        newName: String,
    ): Boolean {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) {
            return false
        }

        val rootFolder = DocumentFile.fromTreeUri(context, Uri.parse(root.treeUriString))
            ?: return false
        return renameDocumentUri(context, resolveFolder(rootFolder, relativePath), cleanName) != null
    }

    fun renameBook(
        context: Context,
        root: LibraryRoot?,
        relativePath: String,
        book: LibraryBookEntry,
        newTitle: String,
    ): Boolean {
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isBlank()) {
            return false
        }

        val originalExtension = book.fileName.substringAfterLast('.', "")
        val targetName = if (
            originalExtension.isNotBlank() &&
            !cleanTitle.endsWith(".$originalExtension", ignoreCase = true)
        ) {
            "$cleanTitle.$originalExtension"
        } else {
            cleanTitle
        }

        findFileInFolder(
            context = context,
            root = root,
            relativePath = relativePath,
            uriString = book.uriString,
        )?.let { file ->
            if (renameDocumentUri(context, file, targetName) != null) {
                return true
            }
        }

        return DocumentFile.fromSingleUri(context, Uri.parse(book.uriString))?.let { file ->
            renameDocumentUri(context, file, targetName) != null
        } == true
    }

    fun renameTextDocument(
        context: Context,
        roots: List<LibraryRoot>,
        currentRoot: LibraryRoot?,
        currentRelativePath: String,
        document: ReaderDocument,
        newTitle: String,
    ): ReaderDocument? {
        val targetName = targetTextFileName(newTitle) ?: return null

        findFileInFolder(
            context = context,
            root = currentRoot,
            relativePath = currentRelativePath,
            uriString = document.uriString,
        )?.let { file ->
            renameTextFile(context, file, targetName)?.let { return it }
        }

        DocumentFile.fromSingleUri(context, document.uri)?.let { file ->
            renameTextFile(context, file, targetName)?.let { return it }
        }

        roots.forEach { root ->
            val rootFolder = DocumentFile.fromTreeUri(context, Uri.parse(root.treeUriString)) ?: return@forEach
            findFileByUri(rootFolder, document.uriString)?.let { file ->
                renameTextFile(context, file, targetName)?.let { return it }
            }
        }

        return null
    }

    fun createTextNote(
        context: Context,
        root: LibraryRoot,
        relativePath: String,
    ): LibraryBookEntry? {
        val rootFolder = DocumentFile.fromTreeUri(context, Uri.parse(root.treeUriString))
            ?: return null
        val targetFolder = resolveFolder(rootFolder, relativePath)
        val noteName = uniqueNoteName(targetFolder)
        val noteFile = targetFolder.createFile("text/plain", noteName)
            ?: return null
        context.contentResolver.openOutputStream(noteFile.uri, "wt")?.use { output ->
            output.write(ByteArray(0))
        } ?: return null

        val fileName = noteFile.name?.trim().orEmpty().ifBlank { noteName }
        return LibraryBookEntry(
            id = noteFile.uri.toString(),
            uriString = noteFile.uri.toString(),
            fileName = fileName,
            title = fileName.substringBeforeLast('.', fileName).ifBlank { fileName },
            type = DocumentType.TXT,
        )
    }

    fun readText(
        context: Context,
        uri: Uri,
    ): String =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
            }
        }.getOrDefault(null).orEmpty()

    fun saveText(
        context: Context,
        uri: Uri,
        text: String,
    ): Boolean =
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
            } != null
        }.getOrDefault(false)

    fun searchLibrary(
        context: Context,
        roots: List<LibraryRoot>,
        query: String,
    ): List<LibrarySearchResult> {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        if (cleanQuery.isBlank() || roots.isEmpty()) {
            return emptyList()
        }

        val results = ArrayList<LibrarySearchResult>(MAX_SEARCH_RESULTS)
        var visited = 0
        roots.forEach { root ->
            if (results.size >= MAX_SEARCH_RESULTS || visited >= MAX_SEARCH_VISITED) {
                return@forEach
            }

            val rootFolder = DocumentFile.fromTreeUri(context, Uri.parse(root.treeUriString))
                ?: return@forEach
            if (root.name.lowercase(Locale.ROOT).contains(cleanQuery)) {
                results += LibrarySearchResult(
                    id = folderId(root, ""),
                    rootUriString = root.treeUriString,
                    rootName = root.name,
                    relativePath = "",
                    title = root.name,
                    type = LibrarySearchResultType.FOLDER,
                )
            }
            searchFolder(
                root = root,
                folder = rootFolder,
                relativePath = "",
                query = cleanQuery,
                depth = 0,
                results = results,
                visitedProvider = { visited },
                onVisited = { visited++ },
            )
        }

        return results
    }

    fun buildSearchIndex(
        context: Context,
        roots: List<LibraryRoot>,
    ): List<LibrarySearchIndexEntry> {
        if (roots.isEmpty()) {
            return emptyList()
        }

        val index = ArrayList<LibrarySearchIndexEntry>(512)
        var visited = 0
        roots.forEach { root ->
            if (index.size >= MAX_SEARCH_INDEX_ITEMS || visited >= MAX_SEARCH_VISITED) {
                return@forEach
            }

            val rootFolder = DocumentFile.fromTreeUri(context, Uri.parse(root.treeUriString))
                ?: return@forEach
            index += rootSearchIndexEntry(root)

            val pending = ArrayDeque<SearchFolderWork>()
            pending.add(SearchFolderWork(rootFolder, "", 0))
            while (
                pending.isNotEmpty() &&
                index.size < MAX_SEARCH_INDEX_ITEMS &&
                visited < MAX_SEARCH_VISITED
            ) {
                val work = pending.removeFirst()
                if (work.depth > MAX_SEARCH_DEPTH) {
                    continue
                }

                val children = runCatching { work.folder.listFiles().toList() }.getOrDefault(emptyList())
                for (child in children) {
                    if (index.size >= MAX_SEARCH_INDEX_ITEMS || visited >= MAX_SEARCH_VISITED) {
                        break
                    }
                    visited++

                    val name = runCatching { child.name?.trim().orEmpty() }.getOrDefault("")
                    if (name.isBlank()) {
                        continue
                    }
                    val childRelativePath = if (work.relativePath.isBlank()) name else "${work.relativePath}/$name"

                    if (runCatching { child.isDirectory }.getOrDefault(false)) {
                        index += folderSearchIndexEntry(root, childRelativePath, name)
                        pending.add(SearchFolderWork(child, childRelativePath, work.depth + 1))
                        continue
                    }

                    val documentType = supportedTypeFromName(name) ?: continue
                    index += bookSearchIndexEntry(
                        root = root,
                        relativePath = childRelativePath,
                        name = name,
                        documentType = documentType,
                        uriString = child.uri.toString(),
                    )
                }
            }
        }

        return index
    }

    private fun resolveFolder(
        root: DocumentFile,
        relativePath: String,
    ): DocumentFile {
        if (relativePath.isBlank()) {
            return root
        }

        return relativePath.split('/')
            .filter { it.isNotBlank() }
            .fold(root) { current, segment ->
                val children = runCatching { current.listFiles() }
                    .getOrElse { throw IllegalArgumentException("Unable to open that folder.") }
                children.firstOrNull { file ->
                    runCatching { file.isDirectory && file.name == segment }.getOrDefault(false)
                } ?: throw IllegalArgumentException("That folder is no longer available.")
            }
    }

    private fun buildPathSegments(
        root: LibraryRoot,
        relativePath: String,
    ): List<LibraryPathSegment> {
        val segments = mutableListOf(
            LibraryPathSegment(
                relativePath = "",
                name = root.name,
            ),
        )

        if (relativePath.isBlank()) {
            return segments
        }

        val names = relativePath.split('/').filter { it.isNotBlank() }
        var runningPath = ""
        names.forEach { name ->
            runningPath = if (runningPath.isBlank()) name else "$runningPath/$name"
            segments += LibraryPathSegment(
                relativePath = runningPath,
                name = name,
            )
        }

        return segments
    }

    fun folderId(
        root: LibraryRoot,
        relativePath: String,
    ): String = "${root.treeUriString}::$relativePath"

    private data class SearchFolderWork(
        val folder: DocumentFile,
        val relativePath: String,
        val depth: Int,
    )

    private fun rootSearchIndexEntry(root: LibraryRoot): LibrarySearchIndexEntry =
        folderSearchIndexEntry(
            root = root,
            relativePath = "",
            name = root.name,
        )

    private fun folderSearchIndexEntry(
        root: LibraryRoot,
        relativePath: String,
        name: String,
    ): LibrarySearchIndexEntry {
        val result = LibrarySearchResult(
            id = folderId(root, relativePath),
            rootUriString = root.treeUriString,
            rootName = root.name,
            relativePath = relativePath,
            title = name,
            type = LibrarySearchResultType.FOLDER,
        )
        return LibrarySearchIndexEntry(
            result = result,
            searchText = searchText(root.name, relativePath, name),
        )
    }

    private fun bookSearchIndexEntry(
        root: LibraryRoot,
        relativePath: String,
        name: String,
        documentType: DocumentType,
        uriString: String,
    ): LibrarySearchIndexEntry {
        val title = name.substringBeforeLast('.', name).ifBlank { name }
        val result = LibrarySearchResult(
            id = uriString,
            rootUriString = root.treeUriString,
            rootName = root.name,
            relativePath = relativePath,
            title = title,
            type = LibrarySearchResultType.BOOK,
            documentType = documentType,
            uriString = uriString,
        )
        return LibrarySearchIndexEntry(
            result = result,
            searchText = searchText(root.name, relativePath, "$title $name"),
        )
    }

    private fun searchText(
        rootName: String,
        relativePath: String,
        title: String,
    ): String = "$rootName $relativePath $title".lowercase(Locale.ROOT)

    private fun supportedTypeFromName(name: String): DocumentType? {
        val lowerName = name.lowercase(Locale.ROOT)
        return when {
            lowerName.endsWith(".epub") -> DocumentType.EPUB
            lowerName.endsWith(".pdf") -> DocumentType.PDF
            lowerName.endsWith(".txt") -> DocumentType.TXT
            else -> null
        }
    }

    private fun searchFolder(
        root: LibraryRoot,
        folder: DocumentFile,
        relativePath: String,
        query: String,
        depth: Int,
        results: MutableList<LibrarySearchResult>,
        visitedProvider: () -> Int,
        onVisited: () -> Unit,
    ) {
        if (
            depth > MAX_SEARCH_DEPTH ||
            results.size >= MAX_SEARCH_RESULTS ||
            visitedProvider() >= MAX_SEARCH_VISITED
        ) {
            return
        }

        val children = runCatching { folder.listFiles().toList() }.getOrDefault(emptyList())
        children.forEach { child ->
            if (results.size >= MAX_SEARCH_RESULTS || visitedProvider() >= MAX_SEARCH_VISITED) {
                return
            }
            onVisited()

            val name = runCatching { child.name?.trim().orEmpty() }.getOrDefault("")
            if (name.isBlank()) {
                return@forEach
            }
            val childRelativePath = if (relativePath.isBlank()) name else "$relativePath/$name"
            val lowerName = name.lowercase(Locale.ROOT)

            if (runCatching { child.isDirectory }.getOrDefault(false)) {
                if (lowerName.contains(query)) {
                    results += LibrarySearchResult(
                        id = folderId(root, childRelativePath),
                        rootUriString = root.treeUriString,
                        rootName = root.name,
                        relativePath = childRelativePath,
                        title = name,
                        type = LibrarySearchResultType.FOLDER,
                    )
                }
                searchFolder(
                    root = root,
                    folder = child,
                    relativePath = childRelativePath,
                    query = query,
                    depth = depth + 1,
                    results = results,
                    visitedProvider = visitedProvider,
                    onVisited = onVisited,
                )
                return@forEach
            }

            if (!runCatching { child.isFile }.getOrDefault(false)) {
                return@forEach
            }

            val documentType = DocumentResolver.detectSupportedType(child.type, name) ?: return@forEach
            if (!lowerName.contains(query)) {
                return@forEach
            }
            results += LibrarySearchResult(
                id = child.uri.toString(),
                rootUriString = root.treeUriString,
                rootName = root.name,
                relativePath = childRelativePath,
                title = name.substringBeforeLast('.', name).ifBlank { name },
                type = LibrarySearchResultType.BOOK,
                documentType = documentType,
                uriString = child.uri.toString(),
            )
        }
    }

    private fun uniqueNoteName(folder: DocumentFile): String {
        val existingNames = runCatching {
            folder.listFiles()
                .mapNotNull { file -> file.name?.lowercase(Locale.ROOT) }
                .toSet()
        }.getOrDefault(emptySet())

        repeat(1000) { index ->
            val name = if (index == 0) "Note.txt" else "Note ${index + 1}.txt"
            if (name.lowercase(Locale.ROOT) !in existingNames) {
                return name
            }
        }

        return "Note ${System.currentTimeMillis()}.txt"
    }

    private fun targetTextFileName(rawTitle: String): String? {
        val cleanTitle = rawTitle
            .trim()
            .map { character ->
                if (character in invalidFileNameCharacters) ' ' else character
            }
            .joinToString("")
            .trim()
        if (cleanTitle.isBlank()) {
            return null
        }

        return if (cleanTitle.endsWith(".txt", ignoreCase = true)) {
            cleanTitle
        } else {
            "$cleanTitle.txt"
        }
    }

    private val invalidFileNameCharacters = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')

    private fun findFileInFolder(
        context: Context,
        root: LibraryRoot?,
        relativePath: String,
        uriString: String,
    ): DocumentFile? {
        root ?: return null
        val rootFolder = DocumentFile.fromTreeUri(context, Uri.parse(root.treeUriString))
            ?: return null
        val targetFolder = runCatching { resolveFolder(rootFolder, relativePath) }.getOrNull()
            ?: return null
        return runCatching {
            targetFolder.listFiles().firstOrNull { file ->
                runCatching {
                    file.isFile && file.uri.toString() == uriString
                }.getOrDefault(false)
            }
        }.getOrNull()
    }

    private fun findFileByUri(
        folder: DocumentFile,
        uriString: String,
        depth: Int = 0,
    ): DocumentFile? {
        if (depth > MAX_SEARCH_DEPTH) {
            return null
        }

        val children = runCatching { folder.listFiles().toList() }.getOrDefault(emptyList())
        children.forEach { child ->
            val found = runCatching {
                when {
                    child.uri.toString() == uriString && child.isFile -> child
                    child.isDirectory -> findFileByUri(child, uriString, depth + 1)
                    else -> null
                }
            }.getOrNull()
            if (found != null) {
                return found
            }
        }
        return null
    }

    private fun renameTextFile(
        context: Context,
        file: DocumentFile,
        targetName: String,
    ): ReaderDocument? {
        val currentName = file.name?.trim().orEmpty()
        val currentType = DocumentResolver.detectSupportedType(file.type, currentName)
        if (currentType != null && currentType != DocumentType.TXT) {
            return null
        }

        val renamedUri = if (currentName.equals(targetName, ignoreCase = false)) {
            file.uri
        } else {
            renameDocumentUri(context, file, targetName) ?: return null
        }
        val renamedName = queryDisplayName(context, renamedUri).orEmpty()
            .ifBlank { file.name?.trim().orEmpty() }
            .ifBlank { targetName }
        return ReaderDocument(
            uri = renamedUri,
            uriString = renamedUri.toString(),
            title = renamedName.substringBeforeLast('.', renamedName).ifBlank { renamedName },
            type = DocumentType.TXT,
        )
    }

    private fun renameDocumentUri(
        context: Context,
        file: DocumentFile,
        targetName: String,
    ): Uri? {
        val renamedUri = runCatching {
            DocumentsContract.renameDocument(context.contentResolver, file.uri, targetName)
        }.getOrNull()
        if (renamedUri != null) {
            return renamedUri
        }

        return if (runCatching { file.renameTo(targetName) }.getOrDefault(false)) {
            file.uri
        } else {
            null
        }
    }

    private fun queryDisplayName(
        context: Context,
        uri: Uri,
    ): String? =
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use null
            }
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        }
}
