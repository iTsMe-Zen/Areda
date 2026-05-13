package app.areada.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.areada.data.AreadaCacheManager
import app.areada.data.DocumentResolver
import app.areada.data.DocumentType
import app.areada.data.LibraryBookEntry
import app.areada.data.LibraryFolderEntry
import app.areada.data.LibraryFolderPickerEntry
import app.areada.data.LibraryPathSegment
import app.areada.data.LibraryRepository
import app.areada.data.LibraryRoot
import app.areada.data.LibrarySearchIndexEntry
import app.areada.data.LibrarySearchResult
import app.areada.data.LibrarySearchResultType
import app.areada.data.LibrarySortMode
import app.areada.data.NaturalSort
import app.areada.data.ReaderPreferences
import app.areada.data.ReaderDocument
import app.areada.data.ReaderStateStore
import app.areada.data.ReadingBookmark
import app.areada.data.ReadingProgress
import app.areada.data.RecentDocument
import app.areada.data.RecentDocumentStore
import app.areada.data.epubBookmarkId
import app.areada.data.pdfBookmarkId
import app.areada.data.txtBookmarkId
import app.areada.reader.EpubBook
import app.areada.reader.EpubEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class ReaderUiState(
    val isLoading: Boolean = false,
    val recents: List<RecentDocument> = emptyList(),
    val bookmarks: List<ReadingBookmark> = emptyList(),
    val preferences: ReaderPreferences = ReaderPreferences(),
    val progressByUri: Map<String, ReadingProgress> = emptyMap(),
    val libraryRoots: List<LibraryRoot> = emptyList(),
    val libraryFolderPickerEntries: List<LibraryFolderPickerEntry> = emptyList(),
    val selectedRootUriString: String? = null,
    val currentRelativePath: String = "",
    val currentPathSegments: List<LibraryPathSegment> = emptyList(),
    val currentFolders: List<LibraryFolderEntry> = emptyList(),
    val currentBooks: List<LibraryBookEntry> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<LibrarySearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val librarySortMode: LibrarySortMode = LibrarySortMode.NAME_ASC,
    val pinnedLibraryItemIds: Set<String> = emptySet(),
    val libraryAddedAtById: Map<String, Long> = emptyMap(),
    val currentScreen: ReaderScreen = ReaderScreen.Home,
    val errorMessage: String? = null,
)

sealed interface ReaderScreen {
    data object Home : ReaderScreen

    data class Epub(
        val document: ReaderDocument,
        val book: EpubBook,
        val initialChapterIndex: Int,
        val initialScrollFraction: Float,
    ) : ReaderScreen

    data class Pdf(
        val document: ReaderDocument,
        val initialPageIndex: Int,
        val initialZoomScale: Float,
    ) : ReaderScreen

    data class Text(
        val document: ReaderDocument,
        val initialText: String,
        val initialScrollFraction: Float = 0f,
        val deleteOnDiscard: Boolean = false,
    ) : ReaderScreen
}

class ReaderViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState = _uiState.asStateFlow()

    private var initialized = false
    private val epubBookCache = linkedMapOf<String, EpubBook>()
    private var libraryLoadJob: Job? = null
    private var documentOpenJob: Job? = null
    private var searchJob: Job? = null
    private var searchIndexJob: Job? = null
    private var cacheCleanupJob: Job? = null
    private var searchIndex: List<LibrarySearchIndexEntry> = emptyList()
    private var searchIndexSignature: String = ""
    private var requestedSearchIndexSignature: String = ""
    private var searchIndexGeneration: Int = 0
    private var libraryAddedAtCache: Map<String, Long> = emptyMap()
    private var applicationContext: Context? = null
    private val textSaveMutex = Mutex()
    private val textSaveSequenceByUri = mutableMapOf<String, Long>()
    private val folderStateCacheLock = Any()
    private val folderStateCache = linkedMapOf<FolderCacheKey, CachedFolderState>()
    private val folderCacheFreshMillis = 60_000L

    private data class FolderCacheKey(
        val rootUriString: String,
        val relativePath: String,
    )

    private data class CachedFolderState(
        val folderState: app.areada.data.LibraryFolderState,
        val loadedAtMillis: Long,
    )

    fun initialize(
        context: Context,
        externalOpenUri: Uri? = null,
    ) {
        if (initialized) {
            externalOpenUri?.let { uri -> openExternalDocument(context, uri) }
            return
        }
        initialized = true

        val appContext = context.applicationContext
        applicationContext = appContext
        viewModelScope.launch(Dispatchers.IO) {
            val recents = RecentDocumentStore.load(appContext)
                .sortedByDescending { it.lastOpenedAt }
                .take(3)
            val preferences = ReaderStateStore.loadPreferences(appContext)
            val progressByUri = ReaderStateStore.loadProgress(appContext)
            val bookmarks = ReaderStateStore.loadBookmarks(appContext)
            val libraryRoots = ReaderStateStore.loadLibraryRoots(appContext)
            val sortMode = ReaderStateStore.loadLibrarySortMode(appContext)
            val pinnedIds = ReaderStateStore.loadPinnedLibraryItemIds(appContext)
            val addedAtById = ReaderStateStore.loadLibraryAddedAt(appContext)
            libraryAddedAtCache = addedAtById
            val pickerEntries = rootPickerEntries(libraryRoots)

            if (libraryRoots.isEmpty()) {
                clearSearchIndex()
                _uiState.update { state ->
                    state.copy(
                        recents = recents,
                        bookmarks = bookmarks,
                        preferences = preferences,
                        progressByUri = progressByUri,
                        libraryRoots = emptyList(),
                        libraryFolderPickerEntries = emptyList(),
                        librarySortMode = sortMode,
                        pinnedLibraryItemIds = pinnedIds,
                        libraryAddedAtById = addedAtById,
                    )
                }
                if (externalOpenUri == null) {
                    scheduleCacheCleanup(appContext)
                }
                externalOpenUri?.let { uri -> openExternalDocument(appContext, uri) }
                return@launch
            }

            val initialRoot = libraryRoots.first()
            runCatching {
                prepareFolderState(
                    context = appContext,
                    folderState = LibraryRepository.loadFolder(
                        context = appContext,
                        root = initialRoot,
                        relativePath = "",
                    ),
                    sortMode = sortMode,
                    pinnedIds = pinnedIds,
                    addedAtById = addedAtById,
                )
            }.onSuccess { folderState ->
                cacheFolderState(initialRoot, folderState)
                _uiState.update { state ->
                    state.copy(
                        recents = recents,
                        bookmarks = bookmarks,
                        preferences = preferences,
                        progressByUri = progressByUri,
                        libraryRoots = libraryRoots,
                        libraryFolderPickerEntries = buildFolderPickerEntries(
                            roots = libraryRoots,
                            selectedRoot = initialRoot,
                            pathSegments = folderState.pathSegments,
                            folders = folderState.folders,
                        ),
                        selectedRootUriString = initialRoot.treeUriString,
                        currentRelativePath = folderState.currentRelativePath,
                        currentPathSegments = folderState.pathSegments,
                        currentFolders = folderState.folders,
                        currentBooks = folderState.books,
                        librarySortMode = sortMode,
                        pinnedLibraryItemIds = pinnedIds,
                        libraryAddedAtById = libraryAddedAtCache,
                    )
                }
                rebuildSearchIndex(
                    context = appContext,
                    roots = libraryRoots,
                    delayMillis = 250L,
                )
                if (externalOpenUri == null) {
                    scheduleCacheCleanup(appContext)
                }
                externalOpenUri?.let { uri -> openExternalDocument(appContext, uri) }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        recents = recents,
                        bookmarks = bookmarks,
                        preferences = preferences,
                        progressByUri = progressByUri,
                        libraryRoots = libraryRoots,
                        libraryFolderPickerEntries = pickerEntries,
                        selectedRootUriString = initialRoot.treeUriString,
                        librarySortMode = sortMode,
                        pinnedLibraryItemIds = pinnedIds,
                        libraryAddedAtById = addedAtById,
                        errorMessage = "One of the saved folders is no longer available.",
                    )
                }
                rebuildSearchIndex(
                    context = appContext,
                    roots = libraryRoots,
                    delayMillis = 250L,
                )
                if (externalOpenUri == null) {
                    scheduleCacheCleanup(appContext)
                }
                externalOpenUri?.let { uri -> openExternalDocument(appContext, uri) }
            }
        }
    }

    fun addLibraryRoot(
        context: Context,
        treeUri: Uri,
    ) {
        val appContext = context.applicationContext
        libraryLoadJob?.cancel()
        libraryLoadJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoading = true, errorMessage = null)
            }

            try {
                persistTreePermission(appContext, treeUri)
                val resolvedRoot = withContext(Dispatchers.IO) {
                    LibraryRepository.resolveRoot(appContext, treeUri)
                }
                val existingRoots = _uiState.value.libraryRoots
                val updatedRoots = buildList {
                    add(resolvedRoot)
                    addAll(existingRoots.filterNot { it.treeUriString == resolvedRoot.treeUriString })
                }
                withContext(Dispatchers.IO) {
                    ReaderStateStore.saveLibraryRoots(appContext, updatedRoots)
                }
                val folderState = withContext(Dispatchers.IO) {
                    prepareFolderState(
                        context = appContext,
                        folderState = LibraryRepository.loadFolder(
                            context = appContext,
                            root = resolvedRoot,
                            relativePath = "",
                        ),
                        sortMode = _uiState.value.librarySortMode,
                        pinnedIds = _uiState.value.pinnedLibraryItemIds,
                        addedAtById = _uiState.value.libraryAddedAtById,
                    )
                }
                cacheFolderState(resolvedRoot, folderState)

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        libraryRoots = updatedRoots,
                        libraryFolderPickerEntries = buildFolderPickerEntries(
                            roots = updatedRoots,
                            selectedRoot = resolvedRoot,
                            pathSegments = folderState.pathSegments,
                            folders = folderState.folders,
                        ),
                        selectedRootUriString = resolvedRoot.treeUriString,
                        currentRelativePath = folderState.currentRelativePath,
                        currentPathSegments = folderState.pathSegments,
                        currentFolders = folderState.folders,
                        currentBooks = folderState.books,
                        libraryAddedAtById = libraryAddedAtCache,
                        errorMessage = null,
                    )
                }
                markSearchIndexDirty(appContext, updatedRoots)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    return@launch
                }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = displayError(throwable, "Unable to open that folder."),
                    )
                }
            }
        }
    }

    fun selectLibraryRoot(
        context: Context,
        root: LibraryRoot,
    ) {
        loadLibraryFolder(
            context = context,
            root = root,
            relativePath = "",
        )
    }

    fun openLibraryFolder(
        context: Context,
        relativePath: String,
    ) {
        val state = _uiState.value
        val root = state.libraryRoots.firstOrNull { it.treeUriString == state.selectedRootUriString }
            ?: return
        if (state.currentRelativePath == relativePath) {
            return
        }
        loadLibraryFolder(context, root, relativePath)
    }

    fun openLibraryPickerEntry(
        context: Context,
        entry: LibraryFolderPickerEntry,
    ) {
        if (
            entry.rootUriString == _uiState.value.selectedRootUriString &&
            entry.relativePath == _uiState.value.currentRelativePath
        ) {
            return
        }

        val root = _uiState.value.libraryRoots.firstOrNull { it.treeUriString == entry.rootUriString }
        if (root == null) {
            _uiState.update { state ->
                state.copy(errorMessage = "That folder is no longer available.")
            }
            return
        }
        loadLibraryFolder(context, root, entry.relativePath)
    }

    fun refreshCurrentFolder(
        context: Context,
        showLoading: Boolean = false,
    ) {
        val state = _uiState.value
        if (
            !initialized ||
            state.isLoading ||
            state.currentScreen !is ReaderScreen.Home ||
            state.libraryRoots.isEmpty()
        ) {
            return
        }

        val root = state.libraryRoots.firstOrNull { it.treeUriString == state.selectedRootUriString }
            ?: return
        if (showLoading) {
            clearFolderStateCache()
            LibraryRepository.clearFolderNavigationCache(root.treeUriString)
        }
        loadLibraryFolder(
            context = context,
            root = root,
            relativePath = state.currentRelativePath,
            showLoading = showLoading,
            useCache = !showLoading,
        )
        markSearchIndexDirty(context.applicationContext, state.libraryRoots)
    }

    fun updateSearchQuery(
        context: Context,
        query: String,
    ) {
        val appContext = context.applicationContext
        val cleanQuery = query.take(80)
        searchJob?.cancel()

        if (cleanQuery.isBlank()) {
            _uiState.update { state ->
                state.copy(
                    searchQuery = "",
                    searchResults = emptyList(),
                    isSearching = false,
                )
            }
            return
        }

        val roots = _uiState.value.libraryRoots
        val freshIndex = searchIndexSignature == roots.signature()
        if (!freshIndex) {
            val indexJobActive = searchIndexJob?.isActive == true
            if (!indexJobActive || searchIndex.isEmpty()) {
                rebuildSearchIndex(
                    context = appContext,
                    roots = roots,
                    force = indexJobActive,
                )
            }
        }

        _uiState.update { state ->
            state.copy(
                searchQuery = cleanQuery,
                isSearching = !freshIndex && searchIndex.isEmpty(),
            )
        }

        searchJob = viewModelScope.launch(Dispatchers.Default) {
            delay(35L)
            val results = filterSearchIndex(cleanQuery)
            _uiState.update { state ->
                if (state.searchQuery != cleanQuery) {
                    state
                } else {
                    state.copy(
                        searchResults = results,
                        isSearching = searchIndexJob?.isActive == true && results.isEmpty(),
                    )
                }
            }
        }
    }

    private fun filterSearchIndex(query: String): List<LibrarySearchResult> {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        if (cleanQuery.isBlank()) {
            return emptyList()
        }
        return searchIndex.asSequence()
            .filter { entry -> entry.searchText.contains(cleanQuery) }
            .take(80)
            .map { entry -> entry.result }
            .toList()
    }

    private fun rebuildSearchIndex(
        context: Context,
        roots: List<LibraryRoot>,
        force: Boolean = false,
        delayMillis: Long = 0L,
    ) {
        val signature = roots.signature()

        if (roots.isEmpty()) {
            clearSearchIndex()
            _uiState.update { state ->
                state.copy(searchResults = emptyList(), isSearching = false)
            }
            return
        }

        if (!force && searchIndexSignature == signature && searchIndex.isNotEmpty()) {
            val currentQuery = _uiState.value.searchQuery
            val currentResults = filterSearchIndex(currentQuery)
            _uiState.update { state ->
                if (state.searchQuery.isBlank()) {
                    state.copy(isSearching = false)
                } else {
                    state.copy(
                        searchResults = currentResults,
                        isSearching = false,
                    )
                }
            }
            return
        }

        if (!force && requestedSearchIndexSignature == signature && searchIndexJob?.isActive == true) {
            return
        }

        val generation = ++searchIndexGeneration
        requestedSearchIndexSignature = signature
        searchIndexJob?.cancel()

        _uiState.update { state ->
            if (state.searchQuery.isBlank()) {
                state
            } else {
                state.copy(isSearching = true)
            }
        }

        val appContext = context.applicationContext
        searchIndexJob = viewModelScope.launch(Dispatchers.IO) {
            if (delayMillis > 0L && _uiState.value.searchQuery.isBlank()) {
                delay(delayMillis)
            }
            val rebuiltIndex = LibraryRepository.buildSearchIndex(appContext, roots)
            if (searchIndexGeneration != generation || requestedSearchIndexSignature != signature) {
                return@launch
            }

            searchIndex = rebuiltIndex
            searchIndexSignature = signature
            val currentQuery = _uiState.value.searchQuery
            val currentResults = withContext(Dispatchers.Default) {
                filterSearchIndex(currentQuery)
            }
            _uiState.update { state ->
                if (state.searchQuery.isBlank()) {
                    state.copy(isSearching = false)
                } else {
                    state.copy(
                        searchResults = currentResults,
                        isSearching = false,
                    )
                }
            }
        }
    }

    private fun clearSearchIndex() {
        searchIndexJob?.cancel()
        searchIndexGeneration++
        searchIndex = emptyList()
        searchIndexSignature = ""
        requestedSearchIndexSignature = ""
    }

    private fun pauseSearchIndexingForReaderOpen() {
        searchJob?.cancel()
        if (searchIndexJob?.isActive == true) {
            searchIndexJob?.cancel()
            searchIndexGeneration++
            requestedSearchIndexSignature = ""
        }
    }

    private fun markSearchIndexDirty(
        context: Context,
        roots: List<LibraryRoot>,
    ) {
        rebuildSearchIndex(
            context = context.applicationContext,
            roots = roots,
            force = true,
            delayMillis = if (_uiState.value.searchQuery.isBlank()) 350L else 0L,
        )
    }

    fun openSearchResult(
        context: Context,
        result: LibrarySearchResult,
    ) {
        _uiState.update { state ->
            state.copy(
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false,
            )
        }
        when (result.type) {
            LibrarySearchResultType.FOLDER -> {
                val root = _uiState.value.libraryRoots.firstOrNull { root ->
                    root.treeUriString == result.rootUriString
                } ?: return
                loadLibraryFolder(context, root, result.relativePath)
            }

            LibrarySearchResultType.BOOK -> {
                val uriString = result.uriString ?: return
                val uri = Uri.parse(uriString)
                val document = result.documentType?.let { type ->
                    ReaderDocument(
                        uri = uri,
                        uriString = uriString,
                        title = result.title,
                        type = type,
                    )
                }
                openDocument(context, uri, preResolvedDocument = document)
            }
        }
    }

    fun openLibraryBook(
        context: Context,
        book: LibraryBookEntry,
    ) {
        val uri = Uri.parse(book.uriString)
        openDocument(
            context = context,
            uri = uri,
            preResolvedDocument = ReaderDocument(
                uri = uri,
                uriString = book.uriString,
                title = book.title,
                type = book.type,
            ),
        )
    }

    fun openDocument(
        context: Context,
        uri: Uri,
        fromRecent: Boolean = false,
        preResolvedDocument: ReaderDocument? = null,
        initialProgressOverride: ReadingProgress? = null,
    ) {
        val appContext = context.applicationContext

        documentOpenJob?.cancel()
        pauseSearchIndexingForReaderOpen()
        documentOpenJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoading = true, errorMessage = null)
            }

            try {
                val document = preResolvedDocument ?: withContext(Dispatchers.IO) {
                    DocumentResolver.resolve(appContext, uri)
                }
                val savedProgress = initialProgressOverride ?: _uiState.value.progressByUri[document.uriString]

                val screen = when (document.type) {
                    DocumentType.EPUB -> {
                        cancelCacheCleanup()
                        val book = withContext(Dispatchers.IO) {
                            epubBookCache[document.uriString]
                                ?.takeIf { cachedBook -> EpubEngine.isCacheUsable(cachedBook) }
                                ?: EpubEngine
                                    .parse(appContext, document.uri, document.title)
                                    .also { parsedBook ->
                                        epubBookCache[document.uriString] = parsedBook
                                        trimEpubCache()
                                    }
                        }
                        ReaderScreen.Epub(
                            document = document,
                            book = book,
                            initialChapterIndex = savedProgress?.epubChapterIndex?.coerceIn(0, book.chapters.lastIndex) ?: 0,
                            initialScrollFraction = savedProgress?.epubScrollFraction?.coerceIn(0f, 1f) ?: 0f,
                        )
                    }

                    DocumentType.PDF -> ReaderScreen.Pdf(
                        document = document,
                        initialPageIndex = savedProgress?.pdfPageIndex?.coerceAtLeast(0) ?: 0,
                        initialZoomScale = savedProgress?.pdfZoomScale?.coerceIn(1f, 5f) ?: 1f,
                    )

                    DocumentType.TXT -> ReaderScreen.Text(
                        document = document,
                        initialText = withContext(Dispatchers.IO) {
                            LibraryRepository.readText(appContext, document.uri)
                        },
                        initialScrollFraction = savedProgress?.txtScrollFraction?.coerceIn(0f, 1f) ?: 0f,
                    )
                }

                val updatedRecents = withContext(Dispatchers.IO) {
                    buildUpdatedRecents(_uiState.value.recents, document).also { recents ->
                        RecentDocumentStore.save(appContext, recents)
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        recents = updatedRecents,
                        currentScreen = screen,
                        errorMessage = null,
                    )
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    return@launch
                }

                if (fromRecent) {
                    val sanitizedRecents = _uiState.value.recents.filterNot { it.uriString == uri.toString() }
                    val sanitizedProgress = _uiState.value.progressByUri - uri.toString()
                    withContext(Dispatchers.IO) {
                        RecentDocumentStore.save(appContext, sanitizedRecents)
                        ReaderStateStore.saveProgress(appContext, sanitizedProgress)
                    }
                    _uiState.update { state ->
                        state.copy(
                            recents = sanitizedRecents,
                            progressByUri = sanitizedProgress,
                        )
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = displayError(throwable, "Unable to open that file."),
                    )
                }
            }
        }
    }

    fun openExternalDocument(
        context: Context,
        uri: Uri,
    ) {
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "content" && scheme != "file") {
            _uiState.update { state ->
                state.copy(errorMessage = "Unable to open that file.")
            }
            return
        }

        openDocument(context = context, uri = uri)
    }

    fun reopenRecent(
        context: Context,
        recent: RecentDocument,
    ) {
        val uri = Uri.parse(recent.uriString)
        openDocument(
            context = context,
            uri = uri,
            fromRecent = true,
            preResolvedDocument = ReaderDocument(
                uri = uri,
                uriString = recent.uriString,
                title = recent.title,
                type = recent.type,
            ),
        )
    }

    fun removeBookmark(
        context: Context,
        bookmark: ReadingBookmark,
    ) {
        val appContext = context.applicationContext
        val updatedBookmarks = _uiState.value.bookmarks.filterNot { it.id == bookmark.id }
        _uiState.update { state ->
            state.copy(bookmarks = updatedBookmarks)
        }
        viewModelScope.launch(Dispatchers.IO) {
            ReaderStateStore.saveBookmarks(appContext, updatedBookmarks)
        }
    }

    fun openBookmark(
        context: Context,
        bookmark: ReadingBookmark,
    ) {
        val uri = Uri.parse(bookmark.uriString)
        openDocument(
            context = context,
            uri = uri,
            preResolvedDocument = ReaderDocument(
                uri = uri,
                uriString = bookmark.uriString,
                title = bookmark.title,
                type = bookmark.type,
            ),
            initialProgressOverride = bookmark.toProgress(),
        )
    }

    fun toggleEpubBookmark(
        context: Context,
        document: ReaderDocument,
        chapterIndex: Int,
        chapterCount: Int,
        scrollFraction: Float,
        chapterTitle: String,
    ) {
        val safeIndex = chapterIndex.coerceAtLeast(0)
        val safeCount = chapterCount.coerceAtLeast(0)
        val safeScroll = scrollFraction.coerceIn(0f, 1f)
        val id = epubBookmarkId(document.uriString, safeIndex, safeScroll)
        toggleBookmark(context, id) { now ->
            ReadingBookmark(
                id = id,
                uriString = document.uriString,
                title = document.title,
                type = document.type,
                positionLabel = if (safeCount > 0) {
                    "Section ${safeIndex + 1} of $safeCount"
                } else {
                    "Section ${safeIndex + 1}"
                },
                epubChapterIndex = safeIndex,
                epubChapterCount = safeCount,
                epubChapterTitle = chapterTitle,
                epubScrollFraction = safeScroll,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    fun togglePdfBookmark(
        context: Context,
        document: ReaderDocument,
        pageIndex: Int,
        pageCount: Int,
    ) {
        val safeIndex = pageIndex.coerceAtLeast(0)
        val safeCount = pageCount.coerceAtLeast(0)
        val id = pdfBookmarkId(document.uriString, safeIndex)
        toggleBookmark(context, id) { now ->
            ReadingBookmark(
                id = id,
                uriString = document.uriString,
                title = document.title,
                type = document.type,
                positionLabel = if (safeCount > 0) {
                    "Page ${safeIndex + 1} of $safeCount"
                } else {
                    "Page ${safeIndex + 1}"
                },
                pdfPageIndex = safeIndex,
                pdfPageCount = safeCount,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    fun toggleTextBookmark(
        context: Context,
        document: ReaderDocument,
        scrollFraction: Float,
    ) {
        val safeScroll = scrollFraction.coerceIn(0f, 1f)
        val id = txtBookmarkId(document.uriString, safeScroll)
        toggleBookmark(context, id) { now ->
            ReadingBookmark(
                id = id,
                uriString = document.uriString,
                title = document.title,
                type = document.type,
                positionLabel = "TXT ${(safeScroll * 100f).toInt().coerceIn(0, 100)}%",
                txtScrollFraction = safeScroll,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    fun removeRecent(
        context: Context,
        recent: RecentDocument,
    ) {
        val appContext = context.applicationContext
        val updatedRecents = _uiState.value.recents.filterNot { it.uriString == recent.uriString }
        val updatedProgress = _uiState.value.progressByUri - recent.uriString
        _uiState.update { state ->
            state.copy(
                recents = updatedRecents,
                progressByUri = updatedProgress,
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            RecentDocumentStore.save(appContext, updatedRecents)
            ReaderStateStore.saveProgress(appContext, updatedProgress)
        }
    }

    fun removeLibraryRoot(
        context: Context,
        root: LibraryRoot,
    ) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            val previousState = _uiState.value
            val updatedRoots = previousState.libraryRoots
                .filterNot { it.treeUriString == root.treeUriString }
            val removedPrefix = "${root.treeUriString}::"
            val updatedPinnedIds = previousState.pinnedLibraryItemIds
                .filterNot { id -> id == root.treeUriString || id.startsWith(removedPrefix) || id.startsWith(root.treeUriString) }
                .toSet()
            val updatedAddedAt = previousState.libraryAddedAtById
                .filterKeys { id -> id != root.treeUriString && !id.startsWith(removedPrefix) && !id.startsWith(root.treeUriString) }

            _uiState.update { state ->
                state.copy(isLoading = true, errorMessage = null)
            }

            withContext(Dispatchers.IO) {
                releaseTreePermission(appContext, Uri.parse(root.treeUriString))
                ReaderStateStore.saveLibraryRoots(appContext, updatedRoots)
                ReaderStateStore.savePinnedLibraryItemIds(appContext, updatedPinnedIds)
                ReaderStateStore.saveLibraryAddedAt(appContext, updatedAddedAt)
            }
            removeCachedRoot(root.treeUriString)
            LibraryRepository.clearFolderNavigationCache(root.treeUriString)

            if (updatedRoots.isEmpty()) {
                clearSearchIndex()
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        libraryRoots = emptyList(),
                        libraryFolderPickerEntries = emptyList(),
                        selectedRootUriString = null,
                        currentRelativePath = "",
                        currentPathSegments = emptyList(),
                        currentFolders = emptyList(),
                        currentBooks = emptyList(),
                        pinnedLibraryItemIds = updatedPinnedIds,
                        libraryAddedAtById = updatedAddedAt,
                        errorMessage = null,
                    )
                }
                return@launch
            }

            val nextRoot = updatedRoots.firstOrNull { it.treeUriString == previousState.selectedRootUriString }
                ?: updatedRoots.first()
            val nextRelativePath = if (nextRoot.treeUriString == previousState.selectedRootUriString) {
                previousState.currentRelativePath
            } else {
                ""
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    prepareFolderState(
                        context = appContext,
                        folderState = LibraryRepository.loadFolder(
                            context = appContext,
                            root = nextRoot,
                            relativePath = nextRelativePath,
                        ),
                        sortMode = previousState.librarySortMode,
                        pinnedIds = updatedPinnedIds,
                        addedAtById = updatedAddedAt,
                    )
                }
            }.onSuccess { folderState ->
                cacheFolderState(nextRoot, folderState)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        libraryRoots = updatedRoots,
                        libraryFolderPickerEntries = buildFolderPickerEntries(
                            roots = updatedRoots,
                            selectedRoot = nextRoot,
                            pathSegments = folderState.pathSegments,
                            folders = folderState.folders,
                        ),
                        selectedRootUriString = nextRoot.treeUriString,
                        currentRelativePath = folderState.currentRelativePath,
                        currentPathSegments = folderState.pathSegments,
                        currentFolders = folderState.folders,
                        currentBooks = folderState.books,
                        pinnedLibraryItemIds = updatedPinnedIds,
                        libraryAddedAtById = libraryAddedAtCache,
                        errorMessage = null,
                    )
                }
                markSearchIndexDirty(appContext, updatedRoots)
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        libraryRoots = updatedRoots,
                        libraryFolderPickerEntries = rootPickerEntries(updatedRoots),
                        selectedRootUriString = nextRoot.treeUriString,
                        currentRelativePath = "",
                        currentPathSegments = emptyList(),
                        currentFolders = emptyList(),
                        currentBooks = emptyList(),
                        pinnedLibraryItemIds = updatedPinnedIds,
                        libraryAddedAtById = updatedAddedAt,
                        errorMessage = displayError(throwable, "Unable to open that folder."),
                    )
                }
                markSearchIndexDirty(appContext, updatedRoots)
            }
        }
    }

    fun createTextNote(context: Context) {
        val appContext = context.applicationContext
        val root = currentRoot() ?: return
        val relativePath = _uiState.value.currentRelativePath

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(errorMessage = null)
            }

            runCatching {
                val note = withContext(Dispatchers.IO) {
                    LibraryRepository.createTextNote(appContext, root, relativePath)
                        ?: error("Unable to create a note in this folder.")
                }
                val document = ReaderDocument(
                    uri = Uri.parse(note.uriString),
                    uriString = note.uriString,
                    title = note.title,
                    type = DocumentType.TXT,
                )
                val stateBeforeUpdate = _uiState.value
                val now = System.currentTimeMillis()
                val decoratedNote = note.copy(
                    addedAt = stateBeforeUpdate.libraryAddedAtById[note.id] ?: now,
                    pinned = note.id in stateBeforeUpdate.pinnedLibraryItemIds,
                )
                val updatedAddedAt = stateBeforeUpdate.libraryAddedAtById + (note.id to decoratedNote.addedAt)
                libraryAddedAtCache = updatedAddedAt
                val updatedBooks = sortBooks(
                    books = stateBeforeUpdate.currentBooks.filterNot { book -> book.id == note.id } + decoratedNote,
                    sortMode = stateBeforeUpdate.librarySortMode,
                )
                val updatedRecents = buildUpdatedRecents(stateBeforeUpdate.recents, document)
                if (
                    stateBeforeUpdate.selectedRootUriString == root.treeUriString &&
                    stateBeforeUpdate.currentRelativePath == relativePath
                ) {
                    cacheFolderState(
                        root = root,
                        folderState = app.areada.data.LibraryFolderState(
                            root = root,
                            currentRelativePath = relativePath,
                            pathSegments = stateBeforeUpdate.currentPathSegments,
                            folders = stateBeforeUpdate.currentFolders,
                            books = updatedBooks,
                        ),
                    )
                }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        recents = updatedRecents,
                        currentBooks = if (
                            state.selectedRootUriString == root.treeUriString &&
                            state.currentRelativePath == relativePath
                        ) {
                            updatedBooks
                        } else {
                            state.currentBooks
                        },
                        libraryAddedAtById = updatedAddedAt,
                        currentScreen = ReaderScreen.Text(
                            document = document,
                            initialText = "",
                            initialScrollFraction = 0f,
                            deleteOnDiscard = true,
                        ),
                        errorMessage = null,
                    )
                }
                viewModelScope.launch(Dispatchers.IO) {
                    RecentDocumentStore.save(appContext, updatedRecents)
                    ReaderStateStore.saveLibraryAddedAt(appContext, updatedAddedAt)
                }
                markSearchIndexDirty(appContext, _uiState.value.libraryRoots)
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = displayError(throwable, "Unable to create that note."),
                    )
                }
            }
        }
    }

    fun closeReader() {
        documentOpenJob?.cancel()
        _uiState.update { state ->
            state.copy(currentScreen = ReaderScreen.Home)
        }
        applicationContext?.let { context -> scheduleCacheCleanup(context) }
    }

    fun dismissError() {
        _uiState.update { state ->
            state.copy(errorMessage = null)
        }
    }

    fun updatePreferences(
        context: Context,
        preferences: ReaderPreferences,
    ) {
        val appContext = context.applicationContext
        val sanitized = preferences.copy(
            fontSizeSp = preferences.fontSizeSp.coerceIn(14, 30),
            lineSpacing = preferences.lineSpacing.coerceIn(1.2f, 2.4f),
        )
        _uiState.update { state ->
            state.copy(preferences = sanitized)
        }

        viewModelScope.launch(Dispatchers.IO) {
            ReaderStateStore.savePreferences(appContext, sanitized)
        }
    }

    fun updateLibrarySortMode(
        context: Context,
        sortMode: LibrarySortMode,
    ) {
        val appContext = context.applicationContext
        clearFolderStateCache()
        _uiState.update { state ->
            state.copy(librarySortMode = sortMode)
        }

        viewModelScope.launch(Dispatchers.IO) {
            ReaderStateStore.saveLibrarySortMode(appContext, sortMode)
        }
        reloadCurrentLibraryFolder(context)
    }

    fun togglePinFolder(
        context: Context,
        folder: LibraryFolderEntry,
    ) {
        togglePinnedItem(context, folder.id)
    }

    fun togglePinBook(
        context: Context,
        book: LibraryBookEntry,
    ) {
        togglePinnedItem(context, book.id)
    }

    fun deleteLibraryFolder(
        context: Context,
        folder: LibraryFolderEntry,
    ) {
        val appContext = context.applicationContext
        val root = currentRoot() ?: return
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val deleted = withContext(Dispatchers.IO) {
                    LibraryRepository.deleteFolder(appContext, root, folder.relativePath)
                }
                if (!deleted) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Unable to delete that folder.")
                    }
                    return@launch
                }
                LibraryRepository.clearFolderNavigationCache(root.treeUriString)
                clearFolderStateCache()
                reloadCurrentLibraryFolder(appContext)
                markSearchIndexDirty(appContext, _uiState.value.libraryRoots)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = displayError(throwable, "Unable to delete that folder."),
                    )
                }
            }
        }
    }

    fun deleteLibraryBook(
        context: Context,
        book: LibraryBookEntry,
    ) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val deleted = withContext(Dispatchers.IO) {
                    LibraryRepository.deleteBook(appContext, book.uriString)
                }
                if (!deleted) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Unable to delete that book.")
                    }
                    return@launch
                }

                val updatedRecents = _uiState.value.recents.filterNot { it.uriString == book.uriString }
                val updatedProgress = _uiState.value.progressByUri - book.uriString
                val updatedBookmarks = _uiState.value.bookmarks.filterNot { it.uriString == book.uriString }
                withContext(Dispatchers.IO) {
                    RecentDocumentStore.save(appContext, updatedRecents)
                    ReaderStateStore.saveProgress(appContext, updatedProgress)
                    ReaderStateStore.saveBookmarks(appContext, updatedBookmarks)
                }
                _uiState.update {
                    it.copy(
                        recents = updatedRecents,
                        progressByUri = updatedProgress,
                        bookmarks = updatedBookmarks,
                    )
                }
                clearFolderStateCache()
                reloadCurrentLibraryFolder(appContext)
                markSearchIndexDirty(appContext, _uiState.value.libraryRoots)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = displayError(throwable, "Unable to delete that book."),
                    )
                }
            }
        }
    }

    fun renameLibraryFolder(
        context: Context,
        folder: LibraryFolderEntry,
        newName: String,
    ) {
        val appContext = context.applicationContext
        val root = currentRoot() ?: return
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val renamed = withContext(Dispatchers.IO) {
                    LibraryRepository.renameFolder(appContext, root, folder.relativePath, newName)
                }
                if (!renamed) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Unable to rename that folder.")
                    }
                    return@launch
                }
                LibraryRepository.clearFolderNavigationCache(root.treeUriString)
                clearFolderStateCache()
                reloadCurrentLibraryFolder(appContext)
                markSearchIndexDirty(appContext, _uiState.value.libraryRoots)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = displayError(throwable, "Unable to rename that folder."),
                    )
                }
            }
        }
    }

    fun renameLibraryBook(
        context: Context,
        book: LibraryBookEntry,
        newTitle: String,
    ) {
        val appContext = context.applicationContext
        val root = currentRoot()
        val relativePath = _uiState.value.currentRelativePath
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val renamed = withContext(Dispatchers.IO) {
                    LibraryRepository.renameBook(appContext, root, relativePath, book, newTitle)
                }
                if (!renamed) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Unable to rename that book.")
                    }
                    return@launch
                }
                clearFolderStateCache()
                reloadCurrentLibraryFolder(appContext)
                markSearchIndexDirty(appContext, _uiState.value.libraryRoots)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = displayError(throwable, "Unable to rename that book."),
                    )
                }
            }
        }
    }

    fun saveEpubProgress(
        context: Context,
        document: ReaderDocument,
        chapterIndex: Int,
        chapterCount: Int,
        scrollFraction: Float,
    ) {
        saveProgress(
            context = context,
            progress = ReadingProgress(
                uriString = document.uriString,
                type = document.type,
                epubChapterIndex = chapterIndex.coerceAtLeast(0),
                epubChapterCount = chapterCount.coerceAtLeast(0),
                epubScrollFraction = scrollFraction.coerceIn(0f, 1f),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    fun savePdfProgress(
        context: Context,
        document: ReaderDocument,
        pageIndex: Int,
        pageCount: Int,
        zoomScale: Float,
    ) {
        saveProgress(
            context = context,
            progress = ReadingProgress(
                uriString = document.uriString,
                type = document.type,
                pdfPageIndex = pageIndex.coerceAtLeast(0),
                pdfPageCount = pageCount.coerceAtLeast(0),
                pdfZoomScale = zoomScale.coerceIn(1f, 5f),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    fun saveTextProgress(
        context: Context,
        document: ReaderDocument,
        scrollFraction: Float,
    ) {
        saveProgress(
            context = context,
            progress = ReadingProgress(
                uriString = document.uriString,
                type = document.type,
                txtScrollFraction = scrollFraction.coerceIn(0f, 1f),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    fun saveTextDocument(
        context: Context,
        document: ReaderDocument,
        text: String,
    ) {
        val appContext = context.applicationContext
        val uriString = document.uriString
        val saveSequence = synchronized(textSaveSequenceByUri) {
            val nextSequence = (textSaveSequenceByUri[uriString] ?: 0L) + 1L
            textSaveSequenceByUri[uriString] = nextSequence
            nextSequence
        }
        viewModelScope.launch(Dispatchers.IO) {
            val saved = textSaveMutex.withLock {
                val isLatestQueuedSave = synchronized(textSaveSequenceByUri) {
                    textSaveSequenceByUri[uriString] == saveSequence
                }
                if (!isLatestQueuedSave) {
                    return@withLock true
                }
                LibraryRepository.saveText(appContext, document.uri, text)
            }
            if (!saved) {
                _uiState.update { state ->
                    val currentScreen = state.currentScreen
                    if (
                        currentScreen is ReaderScreen.Text &&
                        currentScreen.document.uriString == document.uriString
                    ) {
                        state.copy(errorMessage = "Unable to save that note.")
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun discardTextDocument(
        context: Context,
        document: ReaderDocument,
        deleteFile: Boolean,
    ) {
        if (!deleteFile) {
            closeReader()
            return
        }

        val appContext = context.applicationContext
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    LibraryRepository.deleteBook(appContext, document.uriString)
                }
                val updatedRecents = _uiState.value.recents.filterNot { it.uriString == document.uriString }
                val updatedProgress = _uiState.value.progressByUri - document.uriString
                val updatedBookmarks = _uiState.value.bookmarks.filterNot { it.uriString == document.uriString }
                withContext(Dispatchers.IO) {
                    RecentDocumentStore.save(appContext, updatedRecents)
                    ReaderStateStore.saveProgress(appContext, updatedProgress)
                    ReaderStateStore.saveBookmarks(appContext, updatedBookmarks)
                }
                _uiState.update { state ->
                    state.copy(
                        recents = updatedRecents,
                        progressByUri = updatedProgress,
                        bookmarks = updatedBookmarks,
                        currentScreen = ReaderScreen.Home,
                    )
                }
                clearFolderStateCache()
                reloadCurrentLibraryFolder(appContext)
                markSearchIndexDirty(appContext, _uiState.value.libraryRoots)
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        currentScreen = ReaderScreen.Home,
                        errorMessage = displayError(throwable, "Unable to discard that note."),
                    )
                }
            }
        }
    }

    fun renameTextDocument(
        context: Context,
        document: ReaderDocument,
        newTitle: String,
        currentText: String,
    ) {
        val appContext = context.applicationContext
        val stateBeforeRename = _uiState.value
        val currentRoot = stateBeforeRename.libraryRoots.firstOrNull { root ->
            root.treeUriString == stateBeforeRename.selectedRootUriString
        }
        val currentRelativePath = stateBeforeRename.currentRelativePath
        val roots = stateBeforeRename.libraryRoots
        viewModelScope.launch {
            runCatching {
                val renamedDocument = withContext(Dispatchers.IO) {
                    LibraryRepository.renameTextDocument(
                        context = appContext,
                        roots = roots,
                        currentRoot = currentRoot,
                        currentRelativePath = currentRelativePath,
                        document = document,
                        newTitle = newTitle,
                    )
                        ?: error("Unable to rename that note.")
                }
                withContext(Dispatchers.IO) {
                    LibraryRepository.saveText(appContext, renamedDocument.uri, currentText)
                }
                val currentScreen = _uiState.value.currentScreen
                val updatedBookmarks = _uiState.value.bookmarks.map { bookmark ->
                    if (bookmark.uriString == document.uriString) {
                        bookmark.withDocument(renamedDocument)
                    } else {
                        bookmark
                    }
                }
                val updatedRecents = _uiState.value.recents.map { recent ->
                    if (recent.uriString == document.uriString) {
                        recent.copy(
                            uriString = renamedDocument.uriString,
                            title = renamedDocument.title,
                        )
                    } else {
                        recent
                    }
                }
                withContext(Dispatchers.IO) {
                    RecentDocumentStore.save(appContext, updatedRecents)
                    ReaderStateStore.saveBookmarks(appContext, updatedBookmarks)
                }
                _uiState.update { state ->
                    state.copy(
                        recents = updatedRecents,
                        bookmarks = updatedBookmarks,
                        currentScreen = if (
                            currentScreen is ReaderScreen.Text &&
                            currentScreen.document.uriString == document.uriString
                        ) {
                            currentScreen.copy(
                                document = renamedDocument,
                                initialText = currentText,
                            )
                        } else {
                            state.currentScreen
                        },
                    )
                }
                clearFolderStateCache()
                reloadCurrentLibraryFolder(appContext)
                markSearchIndexDirty(appContext, _uiState.value.libraryRoots)
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(errorMessage = displayError(throwable, "Unable to rename that note."))
                }
            }
        }
    }

    fun goBackInLibrary(context: Context): Boolean {
        val state = _uiState.value
        val root = currentRoot() ?: return false
        if (state.currentRelativePath.isBlank()) {
            return false
        }

        val parentPath = state.currentRelativePath.substringBeforeLast('/', "")
        loadLibraryFolder(context, root, parentPath)
        return true
    }

    private fun loadLibraryFolder(
        context: Context,
        root: LibraryRoot,
        relativePath: String,
        showLoading: Boolean = true,
        useCache: Boolean = true,
    ) {
        val appContext = context.applicationContext
        val cachedFolderState = if (useCache) {
            cachedFolderState(root, relativePath)
        } else {
            null
        }
        libraryLoadJob?.cancel()
        val cachedFolder = cachedFolderState?.folderState
        val shouldRefreshCachedFolder = cachedFolderState?.let { cached ->
            System.currentTimeMillis() - cached.loadedAtMillis > folderCacheFreshMillis
        } == true
        if (cachedFolder != null) {
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    selectedRootUriString = root.treeUriString,
                    currentRelativePath = cachedFolder.currentRelativePath,
                    currentPathSegments = cachedFolder.pathSegments,
                    currentFolders = cachedFolder.folders,
                    currentBooks = cachedFolder.books,
                    libraryFolderPickerEntries = buildFolderPickerEntries(
                        roots = state.libraryRoots,
                        selectedRoot = root,
                        pathSegments = cachedFolder.pathSegments,
                        folders = cachedFolder.folders,
                    ),
                    libraryAddedAtById = libraryAddedAtCache,
                    errorMessage = null,
                )
            }
            if (!shouldRefreshCachedFolder) {
                return
            }
        }

        libraryLoadJob = viewModelScope.launch {
            if (showLoading && cachedFolder == null) {
                _uiState.update { state ->
                    state.copy(isLoading = true, errorMessage = null)
                }
            } else if (cachedFolder == null) {
                _uiState.update { state ->
                    state.copy(errorMessage = null)
                }
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    prepareFolderState(
                        context = appContext,
                        folderState = LibraryRepository.loadFolder(
                            context = appContext,
                            root = root,
                            relativePath = relativePath,
                        ),
                        sortMode = _uiState.value.librarySortMode,
                        pinnedIds = _uiState.value.pinnedLibraryItemIds,
                        addedAtById = _uiState.value.libraryAddedAtById,
                    )
                }
            }.onSuccess { folderState ->
                cacheFolderState(root, folderState)
                _uiState.update { state ->
                    if (
                        (cachedFolder != null || !showLoading) &&
                        state.selectedRootUriString == root.treeUriString &&
                        state.currentRelativePath == folderState.currentRelativePath &&
                        sameFolders(state.currentFolders, folderState.folders) &&
                        sameBooks(state.currentBooks, folderState.books)
                    ) {
                        return@update state.copy(isLoading = false)
                    }

                    state.copy(
                        isLoading = false,
                        selectedRootUriString = root.treeUriString,
                        currentRelativePath = folderState.currentRelativePath,
                        currentPathSegments = folderState.pathSegments,
                        currentFolders = folderState.folders,
                        currentBooks = folderState.books,
                        libraryFolderPickerEntries = buildFolderPickerEntries(
                            roots = state.libraryRoots,
                            selectedRoot = root,
                            pathSegments = folderState.pathSegments,
                            folders = folderState.folders,
                        ),
                        libraryAddedAtById = libraryAddedAtCache,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) {
                    return@onFailure
                }
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = displayError(throwable, "Unable to open that folder."),
                    )
                }
            }
        }
    }

    private fun persistTreePermission(
        context: Context,
        treeUri: Uri,
    ) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    private fun releaseTreePermission(
        context: Context,
        treeUri: Uri,
    ) {
        val permission = context.contentResolver.persistedUriPermissions
            .firstOrNull { persisted -> persisted.uri == treeUri }
            ?: return
        var flags = 0
        if (permission.isReadPermission) {
            flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        if (permission.isWritePermission) {
            flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        if (flags != 0) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(treeUri, flags)
            }.onFailure {
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }
            }
        }
    }

    private fun buildUpdatedRecents(
        recents: List<RecentDocument>,
        document: ReaderDocument,
    ): List<RecentDocument> {
        val updated = RecentDocument(
            uriString = document.uriString,
            title = document.title,
            type = document.type,
            lastOpenedAt = System.currentTimeMillis(),
        )

        return buildList {
            add(updated)
            addAll(recents.filterNot { it.uriString == document.uriString })
        }.take(3)
    }

    private fun toggleBookmark(
        context: Context,
        id: String,
        createBookmark: (Long) -> ReadingBookmark,
    ) {
        val appContext = context.applicationContext
        val currentBookmarks = _uiState.value.bookmarks
        val existing = currentBookmarks.firstOrNull { it.id == id }
        val updatedBookmarks = if (existing == null) {
            val now = System.currentTimeMillis()
            listOf(createBookmark(now)) + currentBookmarks
        } else {
            currentBookmarks.filterNot { it.id == id }
        }.sortedByDescending { it.updatedAt }

        _uiState.update { state ->
            state.copy(bookmarks = updatedBookmarks)
        }
        viewModelScope.launch(Dispatchers.IO) {
            ReaderStateStore.saveBookmarks(appContext, updatedBookmarks)
        }
    }

    private fun ReadingBookmark.toProgress(): ReadingProgress =
        ReadingProgress(
            uriString = uriString,
            type = type,
            epubChapterIndex = epubChapterIndex,
            epubChapterCount = epubChapterCount,
            epubScrollFraction = epubScrollFraction,
            pdfPageIndex = pdfPageIndex,
            pdfPageCount = pdfPageCount,
            txtScrollFraction = txtScrollFraction,
            updatedAt = updatedAt,
        )

    private fun ReadingBookmark.withDocument(document: ReaderDocument): ReadingBookmark {
        val newId = when (type) {
            DocumentType.EPUB -> epubBookmarkId(document.uriString, epubChapterIndex, epubScrollFraction)
            DocumentType.PDF -> pdfBookmarkId(document.uriString, pdfPageIndex)
            DocumentType.TXT -> txtBookmarkId(document.uriString, txtScrollFraction)
        }
        return copy(
            id = newId,
            uriString = document.uriString,
            title = document.title,
            updatedAt = System.currentTimeMillis(),
        )
    }
    private fun saveProgress(
        context: Context,
        progress: ReadingProgress,
    ) {
        val appContext = context.applicationContext
        val completed = isCompleted(progress)
        val updatedProgress = if (completed) {
            _uiState.value.progressByUri - progress.uriString
        } else {
            _uiState.value.progressByUri + (progress.uriString to progress)
        }
        val updatedRecents = if (completed) {
            _uiState.value.recents.filterNot { it.uriString == progress.uriString }
        } else {
            _uiState.value.recents
        }
        _uiState.update { state ->
            state.copy(
                recents = updatedRecents,
                progressByUri = updatedProgress,
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (completed) {
                RecentDocumentStore.save(appContext, updatedRecents)
            }
            ReaderStateStore.saveProgress(appContext, updatedProgress)
        }
    }

    private fun currentRoot(): LibraryRoot? =
        _uiState.value.libraryRoots.firstOrNull { it.treeUriString == _uiState.value.selectedRootUriString }

    private fun protectedCacheRootsFor(screen: ReaderScreen): Set<File> =
        when (screen) {
            is ReaderScreen.Epub -> setOf(screen.book.extractedRoot)
            else -> emptySet()
        }

    private fun scheduleCacheCleanup(
        context: Context,
        protectedRoots: Set<File> = activeReaderCacheRoots(),
    ) {
        val appContext = context.applicationContext
        cacheCleanupJob?.cancel()
        cacheCleanupJob = viewModelScope.launch(Dispatchers.IO) {
            AreadaCacheManager.cleanup(appContext, protectedRoots)
        }
    }

    private fun cancelCacheCleanup() {
        cacheCleanupJob?.cancel()
        cacheCleanupJob = null
    }

    private fun activeReaderCacheRoots(): Set<File> =
        protectedCacheRootsFor(_uiState.value.currentScreen)

    private fun reloadCurrentLibraryFolder(context: Context) {
        val root = currentRoot() ?: return
        loadLibraryFolder(
            context = context,
            root = root,
            relativePath = _uiState.value.currentRelativePath,
            useCache = false,
        )
    }

    private fun togglePinnedItem(
        context: Context,
        itemId: String,
    ) {
        val appContext = context.applicationContext
        val updatedPinnedIds = if (itemId in _uiState.value.pinnedLibraryItemIds) {
            _uiState.value.pinnedLibraryItemIds - itemId
        } else {
            _uiState.value.pinnedLibraryItemIds + itemId
        }

        _uiState.update { state ->
            state.copy(pinnedLibraryItemIds = updatedPinnedIds)
        }

        viewModelScope.launch(Dispatchers.IO) {
            ReaderStateStore.savePinnedLibraryItemIds(appContext, updatedPinnedIds)
        }
        clearFolderStateCache()
        reloadCurrentLibraryFolder(context)
    }

    private fun cachedFolderState(
        root: LibraryRoot,
        relativePath: String,
    ): CachedFolderState? =
        synchronized(folderStateCacheLock) {
            val key = FolderCacheKey(root.treeUriString, relativePath)
            val cached = folderStateCache.remove(key)
            if (cached != null) {
                folderStateCache[key] = cached
            }
            cached
        }

    private fun cacheFolderState(
        root: LibraryRoot,
        folderState: app.areada.data.LibraryFolderState,
    ) {
        synchronized(folderStateCacheLock) {
            folderStateCache[FolderCacheKey(root.treeUriString, folderState.currentRelativePath)] = CachedFolderState(
                folderState = folderState,
                loadedAtMillis = System.currentTimeMillis(),
            )
            while (folderStateCache.size > 48 && folderStateCache.isNotEmpty()) {
                val oldestKey = folderStateCache.keys.first()
                folderStateCache.remove(oldestKey)
            }
        }
    }

    private fun removeCachedRoot(rootUriString: String) {
        synchronized(folderStateCacheLock) {
            val keysToRemove = folderStateCache.keys
                .filter { key -> key.rootUriString == rootUriString }
            keysToRemove.forEach { key -> folderStateCache.remove(key) }
        }
    }

    private fun clearFolderStateCache() {
        synchronized(folderStateCacheLock) {
            folderStateCache.clear()
        }
    }

    private fun sameFolders(
        left: List<LibraryFolderEntry>,
        right: List<LibraryFolderEntry>,
    ): Boolean =
        left.size == right.size && left.zip(right).all { (leftFolder, rightFolder) ->
            leftFolder.id == rightFolder.id &&
                leftFolder.name == rightFolder.name &&
                leftFolder.pinned == rightFolder.pinned
        }

    private fun sameBooks(
        left: List<LibraryBookEntry>,
        right: List<LibraryBookEntry>,
    ): Boolean =
        left.size == right.size && left.zip(right).all { (leftBook, rightBook) ->
            leftBook.id == rightBook.id &&
                leftBook.title == rightBook.title &&
                leftBook.fileName == rightBook.fileName &&
                leftBook.type == rightBook.type &&
                leftBook.pinned == rightBook.pinned
        }

    private fun List<LibraryRoot>.signature(): String =
        joinToString(separator = "|") { root -> root.treeUriString }

    private fun rootPickerEntries(roots: List<LibraryRoot>): List<LibraryFolderPickerEntry> =
        roots.map { root ->
            LibraryFolderPickerEntry(
                rootUriString = root.treeUriString,
                relativePath = "",
                name = root.name,
                depth = 0,
            )
        }

    private fun buildFolderPickerEntries(
        roots: List<LibraryRoot>,
        selectedRoot: LibraryRoot?,
        pathSegments: List<LibraryPathSegment>,
        folders: List<LibraryFolderEntry>,
    ): List<LibraryFolderPickerEntry> = rootPickerEntries(roots)

    private fun trimEpubCache() {
        while (epubBookCache.size > 4) {
            val oldestKey = epubBookCache.keys.firstOrNull() ?: return
            epubBookCache.remove(oldestKey)
        }
    }

    private fun prepareFolderState(
        context: Context,
        folderState: app.areada.data.LibraryFolderState,
        sortMode: LibrarySortMode,
        pinnedIds: Set<String>,
        addedAtById: Map<String, Long>,
    ): app.areada.data.LibraryFolderState {
        val now = System.currentTimeMillis()
        val updatedAddedAt = addedAtById.toMutableMap()
        val folders = folderState.folders.map { folder ->
            val addedAt = updatedAddedAt.getOrPut(folder.id) { now }
            folder.copy(
                addedAt = addedAt,
                pinned = folder.id in pinnedIds,
            )
        }
        val books = folderState.books.map { book ->
            val addedAt = updatedAddedAt.getOrPut(book.id) { now }
            book.copy(
                addedAt = addedAt,
                pinned = book.id in pinnedIds,
            )
        }

        val savedAddedAt = updatedAddedAt.toMap()
        libraryAddedAtCache = savedAddedAt
        if (savedAddedAt != addedAtById) {
            ReaderStateStore.saveLibraryAddedAt(context, savedAddedAt)
        }

        return folderState.copy(
            folders = sortFolders(folders, sortMode),
            books = sortBooks(books, sortMode),
        )
    }

    private fun sortFolders(
        folders: List<LibraryFolderEntry>,
        sortMode: LibrarySortMode,
    ): List<LibraryFolderEntry> =
        sortLibraryEntries(
            items = folders,
            sortMode = sortMode,
            title = { it.name },
            addedAt = { it.addedAt },
            pinned = { it.pinned },
        )

    private fun sortBooks(
        books: List<LibraryBookEntry>,
        sortMode: LibrarySortMode,
    ): List<LibraryBookEntry> =
        sortLibraryEntries(
            items = books,
            sortMode = sortMode,
            title = { it.title },
            addedAt = { it.addedAt },
            pinned = { it.pinned },
        )

    private fun <T> sortLibraryEntries(
        items: List<T>,
        sortMode: LibrarySortMode,
        title: (T) -> String,
        addedAt: (T) -> Long,
        pinned: (T) -> Boolean,
    ): List<T> {
        val sorted = when (sortMode) {
            LibrarySortMode.NAME_ASC -> items.sortedWith(NaturalSort.comparator(title))
            LibrarySortMode.NAME_DESC -> items.sortedWith(NaturalSort.comparator(title).reversed())
            LibrarySortMode.DATE_ADDED_ASC -> items.sortedBy { addedAt(it) }
            LibrarySortMode.DATE_ADDED_DESC -> items.sortedByDescending { addedAt(it) }
        }

        return sorted.sortedByDescending { pinned(it) }
    }

    private fun isCompleted(progress: ReadingProgress): Boolean =
        when (progress.type) {
            DocumentType.EPUB -> progress.epubChapterCount > 0 &&
                progress.epubChapterIndex >= progress.epubChapterCount - 1 &&
                progress.epubScrollFraction >= 0.98f

            DocumentType.PDF -> progress.pdfPageCount > 0 &&
                progress.pdfPageIndex >= progress.pdfPageCount - 1

            DocumentType.TXT -> false
        }

    private fun displayError(
        throwable: Throwable,
        fallback: String,
    ): String {
        val message = throwable.message.orEmpty()
        return when {
            message.contains("Invalid or unsupported EPUB", ignoreCase = true) -> "Invalid or unsupported EPUB file."
            message.contains("readable chapters", ignoreCase = true) -> "This EPUB does not contain readable chapters."
            message.contains("Unable to read", ignoreCase = true) -> "Unable to read that EPUB."
            message.contains("/data/", ignoreCase = true) ||
                message.contains("ENOENT", ignoreCase = true) ||
                message.contains("No such file", ignoreCase = true) ||
                message.contains("cache/", ignoreCase = true) -> fallback
            message.isBlank() -> fallback
            else -> message
        }
    }
}
