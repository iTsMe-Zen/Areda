package app.areada.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ImportContacts
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.areada.R
import app.areada.data.DocumentType
import app.areada.data.LibraryBookEntry
import app.areada.data.LibraryFolderEntry
import app.areada.data.LibraryFolderPickerEntry
import app.areada.data.LibraryPathSegment
import app.areada.data.LibraryRoot
import app.areada.data.LibrarySearchResult
import app.areada.data.LibrarySearchResultType
import app.areada.data.LibrarySortMode
import app.areada.data.ReaderFontChoice
import app.areada.data.ReaderPreferences
import app.areada.data.ReaderRenderPalette
import app.areada.data.ReadingBookmark
import app.areada.data.ReaderThemeMode
import app.areada.data.ReadingProgress
import app.areada.data.RecentDocument
import app.areada.data.renderPalette
import app.areada.data.epubBookmarkId
import app.areada.data.pdfBookmarkId
import app.areada.data.txtBookmarkId
import app.areada.reader.EpubChapter
import app.areada.reader.EpubEngine
import app.areada.reader.PdfPageRenderer
import app.areada.reader.RenderedChapter
import app.areada.ui.theme.ReaderTheme
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

interface VolumePageTurnHost {
    fun setVolumePageTurnHandler(handler: ((volumeUp: Boolean) -> Boolean)?)
}

@Composable
fun AreadaApp(
    externalOpenUri: Uri? = null,
    onExternalOpenHandled: () -> Unit = {},
    viewModel: ReaderViewModel = viewModel(),
) {
    val localContext = LocalContext.current
    val context = localContext.applicationContext
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitPrompt by rememberSaveable {
        mutableStateOf(false)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { pickedUri ->
            viewModel.addLibraryRoot(context, pickedUri)
        }
    }

    LaunchedEffect(viewModel, context, externalOpenUri) {
        viewModel.initialize(context, externalOpenUri)
        if (externalOpenUri != null) {
            onExternalOpenHandled()
        }
    }

    BackHandler {
        if (showExitPrompt) {
            showExitPrompt = false
            return@BackHandler
        }

        when (uiState.currentScreen) {
            ReaderScreen.Home -> {
                val handled = viewModel.goBackInLibrary(context)
                if (!handled) {
                    showExitPrompt = true
                }
            }

            else -> viewModel.closeReader()
        }
    }

    ReaderTheme(mode = uiState.preferences.themeMode) {
        if (showExitPrompt) {
            ExitPromptDialog(
                onDismiss = { showExitPrompt = false },
                onExit = {
                    showExitPrompt = false
                    localContext.findActivity()?.finish()
                },
            )
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (val screen = uiState.currentScreen) {
                    ReaderScreen.Home -> HomeScreen(
                        roots = uiState.libraryRoots,
                        folderPickerEntries = uiState.libraryFolderPickerEntries,
                        selectedRootUriString = uiState.selectedRootUriString,
                        currentRelativePath = uiState.currentRelativePath,
                        folders = uiState.currentFolders,
                        books = uiState.currentBooks,
                        searchQuery = uiState.searchQuery,
                        searchResults = uiState.searchResults,
                        isSearching = uiState.isSearching,
                        recents = uiState.recents,
                        bookmarks = uiState.bookmarks,
                        preferences = uiState.preferences,
                        sortMode = uiState.librarySortMode,
                        progressByUri = uiState.progressByUri,
                        onChooseFolder = { folderPicker.launch(null) },
                        onRefresh = { viewModel.refreshCurrentFolder(context, showLoading = true) },
                        onSelectRoot = { root -> viewModel.selectLibraryRoot(context, root) },
                        onRemoveRoot = { root -> viewModel.removeLibraryRoot(context, root) },
                        onOpenPickerEntry = { entry -> viewModel.openLibraryPickerEntry(context, entry) },
                        onCreateTextNote = { viewModel.createTextNote(context) },
                        onSearchQueryChange = { query -> viewModel.updateSearchQuery(context, query) },
                        onOpenSearchResult = { result -> viewModel.openSearchResult(context, result) },
                        onOpenFolder = { relativePath -> viewModel.openLibraryFolder(context, relativePath) },
                        onOpenBook = { book -> viewModel.openLibraryBook(context, book) },
                        onOpenRecent = { recent -> viewModel.reopenRecent(context, recent) },
                        onOpenBookmark = { bookmark -> viewModel.openBookmark(context, bookmark) },
                        onRemoveBookmark = { bookmark -> viewModel.removeBookmark(context, bookmark) },
                        onRemoveRecent = { recent -> viewModel.removeRecent(context, recent) },
                        onSortModeChange = { sortMode -> viewModel.updateLibrarySortMode(context, sortMode) },
                        onDeleteFolder = { folder -> viewModel.deleteLibraryFolder(context, folder) },
                        onDeleteBook = { book -> viewModel.deleteLibraryBook(context, book) },
                        onRenameFolder = { folder, name -> viewModel.renameLibraryFolder(context, folder, name) },
                        onRenameBook = { book, name -> viewModel.renameLibraryBook(context, book, name) },
                        onTogglePinFolder = { folder -> viewModel.togglePinFolder(context, folder) },
                        onTogglePinBook = { book -> viewModel.togglePinBook(context, book) },
                        onPreferencesChange = { preferences ->
                            viewModel.updatePreferences(context, preferences)
                        },
                    )

                    is ReaderScreen.Epub -> EpubReaderScreen(
                        screen = screen,
                        preferences = uiState.preferences,
                        bookmarks = uiState.bookmarks,
                        onBack = viewModel::closeReader,
                        onPreferencesChange = { preferences ->
                            viewModel.updatePreferences(context, preferences)
                        },
                        onToggleBookmark = { chapterIndex, chapterCount, scrollFraction, chapterTitle ->
                            viewModel.toggleEpubBookmark(
                                context = context,
                                document = screen.document,
                                chapterIndex = chapterIndex,
                                chapterCount = chapterCount,
                                scrollFraction = scrollFraction,
                                chapterTitle = chapterTitle,
                            )
                        },
                        onSaveProgress = { chapterIndex, chapterCount, scrollFraction ->
                            viewModel.saveEpubProgress(
                                context = context,
                                document = screen.document,
                                chapterIndex = chapterIndex,
                                chapterCount = chapterCount,
                                scrollFraction = scrollFraction,
                            )
                        },
                    )

                    is ReaderScreen.Pdf -> PdfReaderScreen(
                        screen = screen,
                        preferences = uiState.preferences,
                        bookmarks = uiState.bookmarks,
                        onBack = viewModel::closeReader,
                        onPreferencesChange = { preferences ->
                            viewModel.updatePreferences(context, preferences)
                        },
                        onToggleBookmark = { pageIndex, pageCount ->
                            viewModel.togglePdfBookmark(
                                context = context,
                                document = screen.document,
                                pageIndex = pageIndex,
                                pageCount = pageCount,
                            )
                        },
                        onSaveProgress = { pageIndex, pageCount, zoomScale ->
                            viewModel.savePdfProgress(
                                context = context,
                                document = screen.document,
                                pageIndex = pageIndex,
                                pageCount = pageCount,
                                zoomScale = zoomScale,
                            )
                        },
                    )

                    is ReaderScreen.Text -> TextReaderScreen(
                        screen = screen,
                        preferences = uiState.preferences,
                        bookmarks = uiState.bookmarks,
                        onBack = viewModel::closeReader,
                        onPreferencesChange = { preferences ->
                            viewModel.updatePreferences(context, preferences)
                        },
                        onSaveText = { text ->
                            viewModel.saveTextDocument(context, screen.document, text)
                        },
                        onSaveProgress = { scrollFraction ->
                            viewModel.saveTextProgress(context, screen.document, scrollFraction)
                        },
                        onToggleBookmark = { scrollFraction ->
                            viewModel.toggleTextBookmark(context, screen.document, scrollFraction)
                        },
                        onDiscardText = {
                            viewModel.discardTextDocument(context, screen.document, screen.deleteOnDiscard)
                        },
                        onRenameText = { name, text ->
                            viewModel.renameTextDocument(context, screen.document, name, text)
                        },
                    )
                }

                uiState.errorMessage?.let { message ->
                    ErrorBanner(
                        message = message,
                        onDismiss = viewModel::dismissError,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                if (uiState.isLoading) {
                    LoadingOverlay()
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    roots: List<LibraryRoot>,
    folderPickerEntries: List<LibraryFolderPickerEntry>,
    selectedRootUriString: String?,
    currentRelativePath: String,
    folders: List<LibraryFolderEntry>,
    books: List<LibraryBookEntry>,
    searchQuery: String,
    searchResults: List<LibrarySearchResult>,
    isSearching: Boolean,
    recents: List<RecentDocument>,
    bookmarks: List<ReadingBookmark>,
    preferences: ReaderPreferences,
    sortMode: LibrarySortMode,
    progressByUri: Map<String, ReadingProgress>,
    onChooseFolder: () -> Unit,
    onRefresh: () -> Unit,
    onSelectRoot: (LibraryRoot) -> Unit,
    onRemoveRoot: (LibraryRoot) -> Unit,
    onOpenPickerEntry: (LibraryFolderPickerEntry) -> Unit,
    onCreateTextNote: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenSearchResult: (LibrarySearchResult) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenBook: (LibraryBookEntry) -> Unit,
    onOpenRecent: (RecentDocument) -> Unit,
    onOpenBookmark: (ReadingBookmark) -> Unit,
    onRemoveBookmark: (ReadingBookmark) -> Unit,
    onRemoveRecent: (RecentDocument) -> Unit,
    onSortModeChange: (LibrarySortMode) -> Unit,
    onDeleteFolder: (LibraryFolderEntry) -> Unit,
    onDeleteBook: (LibraryBookEntry) -> Unit,
    onRenameFolder: (LibraryFolderEntry, String) -> Unit,
    onRenameBook: (LibraryBookEntry, String) -> Unit,
    onTogglePinFolder: (LibraryFolderEntry) -> Unit,
    onTogglePinBook: (LibraryBookEntry) -> Unit,
    onPreferencesChange: (ReaderPreferences) -> Unit,
) {
    var showSettings by rememberSaveable {
        mutableStateOf(false)
    }
    var showManageFolders by rememberSaveable {
        mutableStateOf(false)
    }
    var showFolderPicker by rememberSaveable {
        mutableStateOf(false)
    }
    var showBookmarks by rememberSaveable {
        mutableStateOf(false)
    }
    var actionTarget by remember {
        mutableStateOf<LibraryActionTarget?>(null)
    }
    var renameTarget by remember {
        mutableStateOf<LibraryActionTarget?>(null)
    }
    var deleteTarget by remember {
        mutableStateOf<LibraryActionTarget?>(null)
    }
    var bookmarkRemovalTarget by remember {
        mutableStateOf<ReadingBookmark?>(null)
    }
    var recentRemovalTarget by remember {
        mutableStateOf<RecentDocument?>(null)
    }
    var renameText by rememberSaveable {
        mutableStateOf("")
    }

    BackHandler(enabled = showFolderPicker) {
        showFolderPicker = false
    }

    if (showSettings) {
        ReaderSettingsSheet(
            preferences = preferences,
            showPdfNote = false,
            showReadingControls = false,
            onDismiss = { showSettings = false },
            onPreferencesChange = onPreferencesChange,
        )
    }

    if (showManageFolders) {
        ManageFoldersSheet(
            roots = roots,
            selectedRootUriString = selectedRootUriString,
            onDismiss = { showManageFolders = false },
            onRemoveRoot = onRemoveRoot,
        )
    }

    val currentActionTarget = actionTarget?.let { target ->
        when (target) {
            is LibraryActionTarget.Folder -> folders
                .firstOrNull { folder -> folder.id == target.folder.id }
                ?.let { folder -> LibraryActionTarget.Folder(folder) }
                ?: target

            is LibraryActionTarget.Book -> books
                .firstOrNull { book -> book.id == target.book.id }
                ?.let { book -> LibraryActionTarget.Book(book) }
                ?: target
        }
    }

    currentActionTarget?.let { target ->
        LibraryActionSheet(
            target = target,
            onDismiss = { actionTarget = null },
            onDelete = {
                deleteTarget = target
                actionTarget = null
            },
            onRename = {
                renameTarget = target
                renameText = target.displayName
                actionTarget = null
            },
            onTogglePin = {
                when (target) {
                    is LibraryActionTarget.Folder -> onTogglePinFolder(target.folder)
                    is LibraryActionTarget.Book -> onTogglePinBook(target.book)
                }
                actionTarget = null
            },
        )
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            onDismiss = { deleteTarget = null },
            onConfirm = {
                when (target) {
                    is LibraryActionTarget.Folder -> onDeleteFolder(target.folder)
                    is LibraryActionTarget.Book -> onDeleteBook(target.book)
                }
                deleteTarget = null
            },
        )
    }

    bookmarkRemovalTarget?.let { bookmark ->
        CompactChoiceDialog(
            question = "Remove?",
            onDismiss = { bookmarkRemovalTarget = null },
            onYes = {
                onRemoveBookmark(bookmark)
                bookmarkRemovalTarget = null
            },
        )
    }

    recentRemovalTarget?.let { recent ->
        CompactChoiceDialog(
            question = "Remove?",
            onDismiss = { recentRemovalTarget = null },
            onYes = {
                onRemoveRecent(recent)
                recentRemovalTarget = null
            },
        )
    }

    renameTarget?.let { target ->
        RenameDialog(
            name = renameText,
            onNameChange = { renameText = it },
            onDismiss = { renameTarget = null },
            onConfirm = {
                when (target) {
                    is LibraryActionTarget.Folder -> onRenameFolder(target.folder, renameText)
                    is LibraryActionTarget.Book -> onRenameBook(target.book, renameText)
                }
                renameTarget = null
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            item {
                val headerActionColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = "Areada",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (roots.isNotEmpty()) {
                            HeaderIconButton(onClick = onRefresh) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Refresh library",
                                    modifier = Modifier.size(24.dp),
                                    tint = headerActionColor,
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (selectedRootUriString != null) {
                            Text(
                                text = "Add Note",
                                modifier = Modifier
                                    .offset(y = 5.dp)
                                    .clickable(onClick = onCreateTextNote)
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = headerActionColor,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        HeaderIconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(24.dp),
                                tint = headerActionColor,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onChooseFolder,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CreateNewFolder,
                                contentDescription = "Choose folder",
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Choose folder",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                    FolderPickerDropdown(
                        entries = folderPickerEntries,
                        selectedRootUriString = selectedRootUriString,
                        currentRelativePath = currentRelativePath,
                        expanded = showFolderPicker,
                        onToggleExpanded = {
                            showFolderPicker = !showFolderPicker
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                    )
                }
                if (showFolderPicker && folderPickerEntries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FolderPickerInlinePanel(
                        entries = folderPickerEntries,
                        selectedRootUriString = selectedRootUriString,
                        currentRelativePath = currentRelativePath,
                        onSelectEntry = { entry ->
                            showFolderPicker = false
                            onOpenPickerEntry(entry)
                        },
                    )
                }
                if (roots.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = "Manage folders",
                            modifier = Modifier
                                .clickable { showManageFolders = true }
                                .padding(vertical = 5.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                BookmarksSection(
                    bookmarks = bookmarks,
                    expanded = showBookmarks,
                    onToggleExpanded = { showBookmarks = !showBookmarks },
                    onOpenBookmark = onOpenBookmark,
                    onRemoveBookmark = { bookmark -> bookmarkRemovalTarget = bookmark },
                )
                Spacer(modifier = Modifier.height(12.dp))
                SearchBar(
                    query = searchQuery,
                    isSearching = isSearching,
                    onQueryChange = onSearchQueryChange,
                )
                if (searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SearchResults(
                        results = searchResults,
                        isSearching = isSearching,
                        onOpenResult = onOpenSearchResult,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                SectionHeader(title = "Reading")
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (recents.isEmpty()) {
                item {
                    InfoCard(message = "No recent files yet.")
                }
            } else {
                items(
                    items = recents.take(3),
                    key = { recent -> "recent:${recent.uriString}" },
                ) { recent ->
                    SwipeActionBox(
                        actionLabel = "Remove",
                        onSwipe = { recentRemovalTarget = recent },
                    ) {
                        BookRow(
                            title = recent.title,
                            type = recent.type,
                            progressLabel = buildResumeLabel(progressByUri[recent.uriString], recent.type),
                            pinned = false,
                            onClick = { onOpenRecent(recent) },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (selectedRootUriString != null && roots.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionHeader(title = "Collection")
                        Text(
                            text = sortMode.label,
                            modifier = Modifier
                                .clickable { onSortModeChange(sortMode.next()) }
                                .padding(vertical = 5.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (folders.isNotEmpty()) {
                    items(
                        items = folders,
                        key = { folder -> "folder:${folder.id}" },
                    ) { folder ->
                        SwipeActionBox(
                            actionLabel = "Actions",
                            onSwipe = { actionTarget = LibraryActionTarget.Folder(folder) },
                        ) {
                            FolderRow(
                                name = folder.name,
                                pinned = folder.pinned,
                                onClick = { onOpenFolder(folder.relativePath) },
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (books.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionHeader(title = "Books")
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    items(
                        items = books,
                        key = { book -> "book:${book.id}" },
                    ) { book ->
                        SwipeActionBox(
                            actionLabel = "Actions",
                            onSwipe = { actionTarget = LibraryActionTarget.Book(book) },
                        ) {
                            BookRow(
                                title = book.title,
                                type = book.type,
                                progressLabel = buildResumeLabel(progressByUri[book.uriString], book.type),
                                pinned = book.pinned,
                                onClick = { onOpenBook(book) },
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else if (folders.isEmpty()) {
                    item {
                        InfoCard(message = "No EPUB, PDF, or TXT files here.")
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .offset(y = 5.dp)
            .size(40.dp),
        content = content,
    )
}

@Composable
private fun KeepReaderScreenAwake(enabled: Boolean) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, enabled) {
        val window = activity?.window
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            if (enabled) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

@Composable
private fun VolumePageTurnEffect(
    enabled: Boolean,
    inverted: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val host = LocalContext.current.findActivity() as? VolumePageTurnHost
    val latestPrevious by rememberUpdatedState(onPrevious)
    val latestNext by rememberUpdatedState(onNext)

    DisposableEffect(host, enabled, inverted) {
        if (enabled && host != null) {
            host.setVolumePageTurnHandler { volumeUp ->
                val goNext = if (inverted) volumeUp else !volumeUp
                if (goNext) {
                    latestNext()
                } else {
                    latestPrevious()
                }
                true
            }
        } else {
            host?.setVolumePageTurnHandler(null)
        }

        onDispose {
            if (enabled) {
                host?.setVolumePageTurnHandler(null)
            }
        }
    }
}

@Composable
private fun ReaderStatusBarHidden(hidden: Boolean) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, hidden) {
        val window = activity?.window
        val decorView = window?.decorView
        if (window == null || decorView == null) {
            return@DisposableEffect onDispose {}
        }

        @Suppress("DEPRECATION")
        val previousSystemUiVisibility = decorView.systemUiVisibility

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (hidden) {
                window.insetsController?.hide(WindowInsets.Type.statusBars())
            } else {
                window.insetsController?.show(WindowInsets.Type.statusBars())
            }
        } else {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = if (hidden) {
                previousSystemUiVisibility or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            } else {
                previousSystemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN.inv() and
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY.inv()
            }
        }

        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.show(WindowInsets.Type.statusBars())
            } else {
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = previousSystemUiVisibility
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
private fun rememberReaderRenderPalette(mode: ReaderThemeMode): ReaderRenderPalette =
    if (mode == ReaderThemeMode.ANDROID) {
        val colors = MaterialTheme.colorScheme
        ReaderRenderPalette(
            backgroundHex = colors.background.toCssHex(),
            surfaceHex = colors.surface.toCssHex(),
            textHex = colors.onBackground.toCssHex(),
            mutedHex = colors.outline.toCssHex(),
            accentHex = colors.primary.toCssHex(),
        )
    } else {
        mode.renderPalette()
    }

private fun Color.toCssHex(): String =
    "#%06X".format(0xFFFFFF and toArgb())

private fun ReaderFontChoice.composeFontFamily(): FontFamily =
    when (this) {
        ReaderFontChoice.SERIF -> FontFamily.Serif
        ReaderFontChoice.SANS -> FontFamily.SansSerif
        ReaderFontChoice.MONO -> FontFamily.Monospace
    }

private fun LibrarySearchResult.searchSubtitle(): String {
    val parentPath = relativePath.substringBeforeLast('/', "")
    val location = if (parentPath.isBlank()) rootName else "$rootName / $parentPath"
    return when (type) {
        LibrarySearchResultType.FOLDER -> "Folder in $location"
        LibrarySearchResultType.BOOK -> "${documentType?.name ?: "FILE"} in $location"
    }
}

private fun EpubChapter.matchesLocalHref(urlString: String): Boolean {
    val target = urlString.substringBefore('#')
    if (target.isBlank()) {
        return false
    }
    val chapterUrl = file.toURI().toString().substringBefore('#')
    if (target == chapterUrl) {
        return true
    }

    return runCatching {
        java.io.File(URI(target)).canonicalFile == file.canonicalFile
    }.getOrDefault(false)
}

private fun displayError(
    throwable: Throwable,
    fallback: String,
): String {
    val message = throwable.message.orEmpty()
    return when {
        message.contains("Invalid or unsupported EPUB", ignoreCase = true) -> "Invalid or unsupported EPUB file."
        message.contains("readable chapters", ignoreCase = true) -> "This EPUB does not contain readable chapters."
        message.contains("/data/", ignoreCase = true) ||
            message.contains("ENOENT", ignoreCase = true) ||
            message.contains("No such file", ignoreCase = true) ||
            message.contains("cache/", ignoreCase = true) -> fallback
        message.isBlank() -> fallback
        else -> message
    }
}

@Composable
private fun ExitPromptDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit,
) {
    CompactChoiceDialog(
        question = "Quit?",
        onDismiss = onDismiss,
        onYes = onExit,
    )
}

@Composable
private fun ConfirmDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    CompactChoiceDialog(
        question = "Delete permanently?",
        onDismiss = onDismiss,
        onYes = onConfirm,
    )
}

@Composable
private fun GoToPositionDialog(
    label: String,
    currentIndex: Int,
    total: Int,
    title: String = "Go to:",
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val safeTotal = total.coerceAtLeast(1)
    var input by rememberSaveable(label, currentIndex, safeTotal) {
        mutableStateOf((currentIndex + 1).coerceIn(1, safeTotal).toString())
    }
    val targetNumber = input.trim().toIntOrNull()
    val targetIsValid = targetNumber != null && targetNumber in 1..safeTotal

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "$label ${currentIndex + 1} / $safeTotal",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { value ->
                        input = value.filter { it.isDigit() }.take(6)
                    },
                    singleLine = true,
                    isError = input.isNotBlank() && !targetIsValid,
                    placeholder = {
                        Text(text = "1-$safeTotal")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PromptChoiceButton(
                        label = "OK",
                        highlighted = true,
                        enabled = targetIsValid,
                        onClick = {
                            val selectedNumber = targetNumber ?: return@PromptChoiceButton
                            onConfirm((selectedNumber - 1).coerceIn(0, safeTotal - 1))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    PromptChoiceButton(
                        label = "Cancel",
                        highlighted = false,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactChoiceDialog(
    question: String,
    onDismiss: () -> Unit,
    onYes: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.82f),
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = question,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PromptChoiceButton(
                        label = "Yes",
                        highlighted = true,
                        onClick = onYes,
                        modifier = Modifier.weight(1f),
                    )
                    PromptChoiceButton(
                        label = "No",
                        highlighted = false,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PromptChoiceButton(
    label: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (highlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
    }
    val effectiveBackground = if (enabled) {
        backgroundColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    }
    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RectangleShape,
        color = effectiveBackground,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
            )
        }
    }
}

private sealed interface LibraryActionTarget {
    val displayName: String
    val pinned: Boolean

    data class Folder(
        val folder: LibraryFolderEntry,
    ) : LibraryActionTarget {
        override val displayName: String = folder.name
        override val pinned: Boolean = folder.pinned
    }

    data class Book(
        val book: LibraryBookEntry,
    ) : LibraryActionTarget {
        override val displayName: String = book.title
        override val pinned: Boolean = book.pinned
    }
}

private fun LibrarySortMode.next(): LibrarySortMode {
    val entries = LibrarySortMode.entries
    val nextIndex = (entries.indexOf(this) + 1) % entries.size
    return entries[nextIndex]
}

@Composable
private fun FolderPickerDropdown(
    entries: List<LibraryFolderPickerEntry>,
    selectedRootUriString: String?,
    currentRelativePath: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedEntry = entries.firstOrNull { entry ->
        entry.rootUriString == selectedRootUriString
    } ?: entries.firstOrNull()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(enabled = entries.isNotEmpty()) {
                onToggleExpanded()
            },
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedEntry?.name ?: "Folders",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = if (expanded) "Close folder menu" else "Open folder menu",
                modifier = Modifier.graphicsLayer {
                    rotationZ = if (expanded) 180f else 0f
                },
            )
        }
    }
}

@Composable
private fun FolderPickerInlinePanel(
    entries: List<LibraryFolderPickerEntry>,
    selectedRootUriString: String?,
    currentRelativePath: String,
    onSelectEntry: (LibraryFolderPickerEntry) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .padding(vertical = 6.dp),
        ) {
            items(
                items = entries,
                key = { entry -> "${entry.rootUriString}::${entry.relativePath}" },
            ) { entry ->
                val selected = entry.rootUriString == selectedRootUriString
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectEntry(entry)
                        }
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            } else {
                                Color.Transparent
                            },
                            RectangleShape,
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = entry.name,
                        maxLines = 1,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
        },
        placeholder = {
            Text(text = "Search folders, files, notes")
        },
    )
}

@Composable
private fun BookmarksSection(
    bookmarks: List<ReadingBookmark>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onOpenBookmark: (ReadingBookmark) -> Unit,
    onRemoveBookmark: (ReadingBookmark) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(title = "Bookmarks")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = bookmarks.size.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = if (expanded) 180f else 0f
                    },
                )
            }
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(6.dp))
            if (bookmarks.isEmpty()) {
                InfoCard(message = "No bookmarks yet.")
            } else {
                bookmarks.take(20).forEach { bookmark ->
                    BookmarkRow(
                        bookmark = bookmark,
                        onClick = { onOpenBookmark(bookmark) },
                        onRemove = { onRemoveBookmark(bookmark) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: ReadingBookmark,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    SwipeActionBox(
        actionLabel = "Remove",
        onSwipe = onRemove,
    ) {
        BookRow(
            title = bookmark.title,
            type = bookmark.type,
            progressLabel = bookmark.positionLabel,
            pinned = false,
            onClick = onClick,
        )
    }
}
@Composable
private fun SearchResults(
    results: List<LibrarySearchResult>,
    isSearching: Boolean,
    onOpenResult: (LibrarySearchResult) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            isSearching -> InfoCard(message = "Searching selected folders...")
            results.isEmpty() -> InfoCard(message = "No matches.")
            else -> {
                results.take(40).forEach { result ->
                    SearchResultRow(
                        result = result,
                        onClick = { onOpenResult(result) },
                    )
                }
                if (results.size > 40) {
                    Text(
                        text = "Showing first 40 results.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    result: LibrarySearchResult,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (result.type) {
                    LibrarySearchResultType.FOLDER -> Icons.Outlined.Folder
                    LibrarySearchResultType.BOOK -> when (result.documentType) {
                        DocumentType.EPUB -> Icons.Outlined.ImportContacts
                        DocumentType.PDF -> Icons.Outlined.PictureAsPdf
                        DocumentType.TXT -> Icons.Outlined.Description
                        null -> Icons.Outlined.LibraryBooks
                    }
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    text = result.searchSubtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageFoldersSheet(
    roots: List<LibraryRoot>,
    selectedRootUriString: String?,
    onDismiss: () -> Unit,
    onRemoveRoot: (LibraryRoot) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Manage folders",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (roots.isEmpty()) {
                InfoCard(message = "No folders selected.")
            } else {
                roots.forEachIndexed { index, root ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = root.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            if (root.treeUriString == selectedRootUriString) {
                                Text(
                                    text = "Current",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        TextButton(onClick = { onRemoveRoot(root) }) {
                            Text(text = "Remove")
                        }
                    }
                    if (index < roots.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeActionBox(
    actionLabel: String,
    onSwipe: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onSwipe()
            }
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RectangleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = actionLabel,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        content = {
            content()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryActionSheet(
    target: LibraryActionTarget,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = target.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActionSheetItem(
                label = if (target.pinned) "Unpin" else "Pin",
                onClick = onTogglePin,
            )
            ActionSheetItem(
                label = "Rename",
                onClick = onRename,
            )
            ActionSheetItem(
                label = "Delete",
                onClick = onDelete,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ActionSheetItem(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun RenameDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Text(
                    text = "Rename",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PromptChoiceButton(
                        label = "Save",
                        highlighted = true,
                        onClick = onConfirm,
                        enabled = name.trim().isNotBlank(),
                        modifier = Modifier.weight(1f),
                    )
                    PromptChoiceButton(
                        label = "Cancel",
                        highlighted = false,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun EpubReaderScreen(
    screen: ReaderScreen.Epub,
    preferences: ReaderPreferences,
    bookmarks: List<ReadingBookmark>,
    onBack: () -> Unit,
    onPreferencesChange: (ReaderPreferences) -> Unit,
    onToggleBookmark: (chapterIndex: Int, chapterCount: Int, scrollFraction: Float, chapterTitle: String) -> Unit,
    onSaveProgress: (chapterIndex: Int, chapterCount: Int, scrollFraction: Float) -> Unit,
) {
    var showSettings by rememberSaveable(screen.document.uriString) {
        mutableStateOf(false)
    }
    var showToc by rememberSaveable(screen.document.uriString) {
        mutableStateOf(false)
    }
    var isFullMode by remember(screen.document.uriString) {
        mutableStateOf(true)
    }
    var fullControlsVisible by rememberSaveable(screen.document.uriString) {
        mutableStateOf(false)
    }
    var noteText by rememberSaveable(screen.document.uriString) {
        mutableStateOf<String?>(null)
    }
    var showGoToChapter by rememberSaveable(screen.document.uriString) {
        mutableStateOf(false)
    }
    var showChapterSearch by rememberSaveable(screen.document.uriString) {
        mutableStateOf(false)
    }
    var chapterSearchQuery by rememberSaveable(screen.document.uriString) {
        mutableStateOf("")
    }
    var chapterSearchCurrent by rememberSaveable(screen.document.uriString) {
        mutableIntStateOf(0)
    }
    var chapterSearchCount by rememberSaveable(screen.document.uriString) {
        mutableIntStateOf(0)
    }
    var chapterSearchRequest by rememberSaveable(screen.document.uriString) {
        mutableIntStateOf(0)
    }
    var chapterSearchBackwards by rememberSaveable(screen.document.uriString) {
        mutableStateOf(false)
    }
    var chapterIndex by rememberSaveable(screen.document.uriString, screen.initialChapterIndex) {
        mutableIntStateOf(screen.initialChapterIndex.coerceIn(0, screen.book.chapters.lastIndex.coerceAtLeast(0)))
    }
    var scrollFraction by rememberSaveable(
        screen.document.uriString,
        screen.initialChapterIndex,
        screen.initialScrollFraction,
    ) {
        mutableFloatStateOf(screen.initialScrollFraction)
    }
    var renderedChapter by remember(screen.document.uriString, chapterIndex) {
        mutableStateOf<RenderedChapter?>(null)
    }
    var chapterError by remember(screen.document.uriString, chapterIndex) {
        mutableStateOf<String?>(null)
    }
    val renderPalette = rememberReaderRenderPalette(preferences.themeMode)
    val sectionLabel = "Section ${chapterIndex + 1} / ${screen.book.chapters.size}"
    val topSubtitle = renderedChapter
        ?.title
        ?.ifBlank { null }
        ?: screen.book.chapters.getOrNull(chapterIndex)?.title?.ifBlank { null }
        ?: sectionLabel
    val tocEntries = remember(screen.book.chapters) {
        screen.book.chapters.mapIndexed { index, chapter ->
            ReaderTocEntry(
                index = index,
                label = chapter.title.ifBlank { "Section ${index + 1}" },
            )
        }
    }
    val currentBookmarkId = epubBookmarkId(screen.document.uriString, chapterIndex, scrollFraction)
    val currentBookmarked = bookmarks.any { it.id == currentBookmarkId }
    val latestChapterIndex by rememberUpdatedState(chapterIndex)
    val latestScrollFraction by rememberUpdatedState(scrollFraction)

    fun switchToChapter(nextIndex: Int) {
        if (nextIndex !in screen.book.chapters.indices || nextIndex == chapterIndex) {
            return
        }
        chapterIndex = nextIndex
        scrollFraction = 0f
        chapterSearchQuery = ""
        chapterSearchCurrent = 0
        chapterSearchCount = 0
        onSaveProgress(nextIndex, screen.book.chapters.size, 0f)
    }

    fun goToPreviousChapter() {
        if (chapterIndex <= 0) {
            return
        }
        switchToChapter(chapterIndex - 1)
    }

    fun goToNextChapter() {
        if (chapterIndex >= screen.book.chapters.lastIndex) {
            return
        }
        switchToChapter(chapterIndex + 1)
    }

    fun openLocalChapterLink(urlString: String): Boolean {
        val nextIndex = screen.book.chapters.indexOfFirst { chapter ->
            chapter.matchesLocalHref(urlString)
        }
        if (nextIndex < 0) {
            return false
        }
        switchToChapter(nextIndex)
        return true
    }

    DisposableEffect(screen.document.uriString) {
        onDispose {
            onSaveProgress(latestChapterIndex, screen.book.chapters.size, latestScrollFraction)
        }
    }

    LaunchedEffect(screen.document.uriString, chapterIndex, preferences, renderPalette) {
        renderedChapter = null
        chapterError = null
        runCatching {
            EpubEngine.render(
                book = screen.book,
                chapterIndex = chapterIndex,
                preferences = preferences,
                paletteOverride = renderPalette,
            )
        }
            .onSuccess { chapter ->
                renderedChapter = chapter
            }
            .onFailure { throwable ->
                chapterError = displayError(throwable, "Unable to render this section.")
            }
    }

    if (showSettings) {
        ReaderSettingsSheet(
            preferences = preferences,
            showPdfNote = false,
            onDismiss = { showSettings = false },
            onPreferencesChange = onPreferencesChange,
        )
    }
    if (showGoToChapter) {
        GoToPositionDialog(
            label = "Section",
            currentIndex = chapterIndex,
            total = screen.book.chapters.size,
            title = "Go to section",
            onDismiss = { showGoToChapter = false },
            onConfirm = { nextIndex ->
                switchToChapter(nextIndex)
                showGoToChapter = false
            },
        )
    }
    BackHandler(enabled = showToc) {
        showToc = false
    }
    BackHandler(enabled = showChapterSearch) {
        showChapterSearch = false
        chapterSearchQuery = ""
    }
    KeepReaderScreenAwake(enabled = preferences.keepScreenOn)
    VolumePageTurnEffect(
        enabled = preferences.volumeButtonsTurnPages && !showSettings && !showGoToChapter && !showChapterSearch,
        inverted = preferences.invertVolumeButtons,
        onPrevious = ::goToPreviousChapter,
        onNext = ::goToNextChapter,
    )
    LaunchedEffect(isFullMode) {
        fullControlsVisible = false
    }
    val showReaderChrome = !isFullMode || fullControlsVisible
    ReaderStatusBarHidden(hidden = !showReaderChrome)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                chapterError != null -> ReaderMessage(message = chapterError ?: "")
                renderedChapter == null -> LoadingState(label = "Rendering section")
                else -> {
                    val chapter = renderedChapter ?: return@Box
                    key(
                        screen.document.uriString,
                        chapterIndex,
                        preferences,
                        chapter.html.hashCode(),
                    ) {
                        val renderedIndex = chapterIndex
                        EpubWebView(
                            chapter = chapter,
                            currentChapterFileUrl = screen.book.chapters[chapterIndex].file.toURI().toString(),
                            preferences = preferences,
                            renderPalette = renderPalette,
                            initialScrollFraction = scrollFraction,
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                            onScrollProgressChange = { progress ->
                                if (chapterIndex == renderedIndex) {
                                    scrollFraction = progress
                                }
                            },
                            onReaderTap = {
                                if (isFullMode) {
                                    fullControlsVisible = !fullControlsVisible
                                }
                            },
                            onSwipePrevious = ::goToPreviousChapter,
                            onSwipeNext = ::goToNextChapter,
                            onOpenLocalHref = ::openLocalChapterLink,
                            onNoteOpen = { note ->
                                noteText = note
                            },
                            searchQuery = chapterSearchQuery,
                            searchRequest = chapterSearchRequest,
                            searchBackwards = chapterSearchBackwards,
                            onSearchResult = { current, count ->
                                chapterSearchCurrent = current
                                chapterSearchCount = count
                            },
                        )
                    }
                }
            }

            if (showReaderChrome) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                ) {
                    ReaderTopBar(
                        title = screen.document.title,
                        subtitle = topSubtitle,
                        onBack = onBack,
                        onSettings = { showSettings = true },
                        onSearch = { showChapterSearch = !showChapterSearch },
                        onTableOfContents = { showToc = !showToc },
                        onBookmarkToggle = {
                            onToggleBookmark(
                                chapterIndex,
                                screen.book.chapters.size,
                                scrollFraction,
                                renderedChapter?.title ?: screen.book.chapters[chapterIndex].title,
                            )
                        },
                        isBookmarked = currentBookmarked,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    ReaderFooter(
                        leftLabel = "Prev",
                        rightLabel = "Next",
                        centerLabel = sectionLabel,
                        leftEnabled = chapterIndex > 0,
                        rightEnabled = chapterIndex < screen.book.chapters.lastIndex,
                        onLeft = ::goToPreviousChapter,
                        onCenter = { showGoToChapter = true },
                        onRight = ::goToNextChapter,
                    )
                }
            }

            if (showReaderChrome && showToc) {
                ReaderTocOverlay(
                    title = "Table of contents",
                    entries = tocEntries,
                    currentIndex = chapterIndex,
                    onDismiss = { showToc = false },
                    onSelect = { nextIndex ->
                        switchToChapter(nextIndex)
                        showToc = false
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }

            if (showReaderChrome && showChapterSearch) {
                ReaderChapterSearchOverlay(
                    query = chapterSearchQuery,
                    current = chapterSearchCurrent,
                    count = chapterSearchCount,
                    onQueryChange = { query ->
                        chapterSearchQuery = query.take(80)
                        chapterSearchCurrent = 0
                        chapterSearchCount = 0
                    },
                    onPrevious = {
                        chapterSearchBackwards = true
                        chapterSearchRequest += 1
                    },
                    onNext = {
                        chapterSearchBackwards = false
                        chapterSearchRequest += 1
                    },
                    onDismiss = {
                        showChapterSearch = false
                        chapterSearchQuery = ""
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }

            noteText?.let { note ->
                NotePopup(
                    note = note,
                    onClose = { noteText = null },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 28.dp),
                )
            }
        }
    }
}

@Composable
private fun PdfReaderScreen(
    screen: ReaderScreen.Pdf,
    preferences: ReaderPreferences,
    bookmarks: List<ReadingBookmark>,
    onBack: () -> Unit,
    onPreferencesChange: (ReaderPreferences) -> Unit,
    onToggleBookmark: (pageIndex: Int, pageCount: Int) -> Unit,
    onSaveProgress: (pageIndex: Int, pageCount: Int, zoomScale: Float) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    var rendererResult by remember(screen.document.uriString) {
        mutableStateOf<Result<PdfPageRenderer>?>(null)
    }
    LaunchedEffect(screen.document.uriString) {
        rendererResult = null
        rendererResult = withContext(Dispatchers.IO) {
            runCatching { PdfPageRenderer(context, screen.document.uri) }
        }
    }
    val renderer = rendererResult?.getOrNull()

    DisposableEffect(renderer) {
        onDispose {
            renderer?.close()
        }
    }

    if (rendererResult == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            LoadingState(label = "Opening PDF")
        }
        return
    }

    if (renderer == null) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                ReaderTopBar(
                    title = screen.document.title,
                    subtitle = "PDF",
                    onBack = onBack,
                    onSettings = null,
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                ReaderMessage(
                    message = rendererResult?.exceptionOrNull()?.let { displayError(it, "Unable to open that PDF.") }
                        ?: "Unable to open that PDF.",
                )
            }
        }
        return
    }
    val pageCount = renderer.pageCount

    var showSettings by rememberSaveable(screen.document.uriString) {
        mutableStateOf(false)
    }
    var showToc by rememberSaveable(screen.document.uriString) {
        mutableStateOf(false)
    }
    var isFullMode by remember(screen.document.uriString) {
        mutableStateOf(true)
    }
    var fullControlsVisible by rememberSaveable(screen.document.uriString) {
        mutableStateOf(false)
    }
    var showGoToPage by rememberSaveable(screen.document.uriString) {
        mutableStateOf(false)
    }
    var pageIndex by rememberSaveable(screen.document.uriString, screen.initialPageIndex) {
        mutableIntStateOf(screen.initialPageIndex.coerceIn(0, max(pageCount - 1, 0)))
    }
    var zoomScale by rememberSaveable(screen.document.uriString, screen.initialZoomScale) {
        mutableFloatStateOf(screen.initialZoomScale.coerceIn(1f, 5f))
    }
    val currentBookmarked = bookmarks.any { it.id == pdfBookmarkId(screen.document.uriString, pageIndex) }
    val tocEntries = remember(pageCount) {
        List(pageCount.coerceAtLeast(0)) { index ->
            ReaderTocEntry(
                index = index,
                label = "Page ${index + 1}",
            )
        }
    }

    DisposableEffect(screen.document.uriString, pageIndex) {
        onDispose {
            onSaveProgress(pageIndex, pageCount, zoomScale)
        }
    }

    if (showSettings) {
        ReaderSettingsSheet(
            preferences = preferences,
            showPdfNote = true,
            onDismiss = { showSettings = false },
            onPreferencesChange = onPreferencesChange,
        )
    }
    if (showGoToPage) {
        GoToPositionDialog(
            label = "Page",
            currentIndex = pageIndex,
            total = pageCount,
            onDismiss = { showGoToPage = false },
            onConfirm = { nextIndex ->
                onSaveProgress(pageIndex, pageCount, zoomScale)
                pageIndex = nextIndex
                showGoToPage = false
            },
        )
    }
    BackHandler(enabled = showToc) {
        showToc = false
    }
    KeepReaderScreenAwake(enabled = preferences.keepScreenOn)
    LaunchedEffect(isFullMode) {
        fullControlsVisible = false
    }
    val showReaderChrome = !isFullMode || fullControlsVisible

    fun goToPreviousPage() {
        if (pageIndex <= 0) {
            return
        }
        onSaveProgress(pageIndex, pageCount, zoomScale)
        pageIndex -= 1
    }

    fun goToNextPage() {
        if (pageIndex >= pageCount - 1) {
            return
        }
        onSaveProgress(pageIndex, pageCount, zoomScale)
        pageIndex += 1
    }

    VolumePageTurnEffect(
        enabled = preferences.volumeButtonsTurnPages && !showSettings && !showGoToPage && !showToc,
        inverted = preferences.invertVolumeButtons,
        onPrevious = ::goToPreviousPage,
        onNext = ::goToNextPage,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { _ ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            val density = LocalDensity.current
            val horizontalPaddingPx = with(density) { 32.dp.roundToPx() }
            val widthPx = (constraints.maxWidth - horizontalPaddingPx).coerceAtLeast(1)
            var bitmap by remember(screen.document.uriString, pageIndex, widthPx) {
                mutableStateOf<Bitmap?>(null)
            }
            var renderError by remember(screen.document.uriString, pageIndex, widthPx) {
                mutableStateOf<String?>(null)
            }

            LaunchedEffect(screen.document.uriString, pageIndex, widthPx) {
                bitmap = null
                renderError = null

                runCatching {
                    withContext(Dispatchers.IO) {
                        renderer.renderPage(pageIndex, widthPx)
                    }
                }
                    .onSuccess { rendered ->
                        bitmap = rendered
                    }
                    .onFailure { throwable ->
                        renderError = displayError(throwable, "Unable to render this page.")
                    }
            }

            when {
                renderError != null -> ReaderMessage(message = renderError ?: "")
                bitmap == null -> LoadingState(label = "Rendering page")
                else -> {
                    val pageBitmap = bitmap ?: return@BoxWithConstraints
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pageIndex, zoomScale, showSettings, showGoToPage, showToc) {
                                if (!showSettings && !showGoToPage && !showToc && zoomScale <= 1.05f) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(
                                            requireUnconsumed = false,
                                            pass = PointerEventPass.Initial,
                                        )
                                        val pointerId = down.id
                                        var totalHorizontalDrag = 0f
                                        var totalVerticalDrag = 0f
                                        var multiTouch = false

                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            if (event.changes.count { change -> change.pressed } > 1) {
                                                multiTouch = true
                                            }
                                            val change = event.changes.firstOrNull { it.id == pointerId }
                                                ?: event.changes.firstOrNull()
                                                ?: break
                                            val movement = change.position - change.previousPosition
                                            totalHorizontalDrag += movement.x
                                            totalVerticalDrag += movement.y
                                            if (change.changedToUpIgnoreConsumed()) {
                                                break
                                            }
                                        }

                                        val horizontalDistance = abs(totalHorizontalDrag)
                                        val verticalDistance = abs(totalVerticalDrag)
                                        if (
                                            !multiTouch &&
                                            horizontalDistance > 72f &&
                                            horizontalDistance > verticalDistance * 1.25f
                                        ) {
                                            if (totalHorizontalDrag < 0f) {
                                                goToNextPage()
                                            } else {
                                                goToPreviousPage()
                                            }
                                        }
                                    }
                                }
                            },
                    ) {
                        ZoomablePage(
                            bitmap = pageBitmap,
                            backgroundColor = MaterialTheme.colorScheme.background,
                            initialScale = zoomScale,
                            onScaleChange = { scale ->
                                zoomScale = scale
                            },
                            onReaderTap = {
                                if (isFullMode) {
                                    fullControlsVisible = !fullControlsVisible
                                }
                            },
                        )
                    }
                }
            }
            if (showReaderChrome) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                ) {
                    ReaderTopBar(
                        title = screen.document.title,
                        subtitle = "Page ${pageIndex + 1} of $pageCount",
                        onBack = onBack,
                        onSettings = { showSettings = true },
                        onTableOfContents = { showToc = !showToc },
                        onBookmarkToggle = { onToggleBookmark(pageIndex, pageCount) },
                        isBookmarked = currentBookmarked,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    ReaderFooter(
                        leftLabel = "Prev",
                        rightLabel = "Next",
                        centerLabel = "Pg ${pageIndex + 1}",
                        leftEnabled = pageIndex > 0,
                        rightEnabled = pageIndex < pageCount - 1,
                        onLeft = ::goToPreviousPage,
                        onCenter = { showGoToPage = true },
                        onRight = ::goToNextPage,
                    )
                }
            }
            if (showReaderChrome && showToc) {
                ReaderTocOverlay(
                    title = "Table of contents",
                    entries = tocEntries,
                    currentIndex = pageIndex,
                    onDismiss = { showToc = false },
                    onSelect = { nextIndex ->
                        onSaveProgress(pageIndex, pageCount, zoomScale)
                        pageIndex = nextIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                        showToc = false
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextReaderScreen(
    screen: ReaderScreen.Text,
    preferences: ReaderPreferences,
    bookmarks: List<ReadingBookmark>,
    onBack: () -> Unit,
    onPreferencesChange: (ReaderPreferences) -> Unit,
    onSaveText: (String) -> Unit,
    onSaveProgress: (Float) -> Unit,
    onToggleBookmark: (Float) -> Unit,
    onDiscardText: () -> Unit,
    onRenameText: (String, String) -> Unit,
) {
    var showSettings by rememberSaveable(screen.document.uriString) {
        mutableStateOf(false)
    }
    var showRename by rememberSaveable(screen.document.uriString) {
        mutableStateOf(false)
    }
    var renameText by rememberSaveable(screen.document.uriString) {
        mutableStateOf(screen.document.title)
    }
    var noteValue by remember(screen.document.uriString) {
        mutableStateOf(
            TextFieldValue(
                text = screen.initialText,
                selection = TextRange(screen.initialText.length),
            ),
        )
    }
    val undoHistory = remember(screen.document.uriString) {
        mutableStateListOf<NoteHistoryEntry>()
    }
    val redoHistory = remember(screen.document.uriString) {
        mutableStateListOf<NoteHistoryEntry>()
    }
    var noteHistoryOrder by remember(screen.document.uriString) {
        mutableStateOf(0L)
    }
    var lastUndoCheckpointAt by remember(screen.document.uriString) {
        mutableStateOf(0L)
    }
    var lastUndoCheckpointLength by remember(screen.document.uriString) {
        mutableIntStateOf(screen.initialText.length)
    }
    var showNoteSearch by rememberSaveable(screen.document.uriString) {
        mutableStateOf(false)
    }
    var noteSearchQuery by rememberSaveable(screen.document.uriString) {
        mutableStateOf("")
    }
    var noteSearchIndex by rememberSaveable(screen.document.uriString) {
        mutableIntStateOf(0)
    }
    val text = noteValue.text
    var saveOnDispose by remember(screen.document.uriString) {
        mutableStateOf(true)
    }
    var noteEditedSinceOpen by remember(screen.document.uriString) {
        mutableStateOf(false)
    }
    val scrollState = rememberScrollState()
    var editorHeightPx by remember(screen.document.uriString) {
        mutableIntStateOf(0)
    }
    val noteTextLayoutRef = remember(screen.document.uriString) {
        NoteTextLayoutRef()
    }
    var lastAutosaveAt by remember(screen.document.uriString) {
        mutableStateOf(0L)
    }
    var lastAutosavedText by remember(screen.document.uriString) {
        mutableStateOf(screen.initialText)
    }
    val cursorKeepVisiblePaddingPx = with(LocalDensity.current) {
        72.dp.roundToPx()
    }
    val currentScrollFraction = if (scrollState.maxValue > 0) {
        (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val latestText by rememberUpdatedState(text)
    val latestScrollFraction by rememberUpdatedState(currentScrollFraction)
    val shouldSaveOnDispose by rememberUpdatedState(saveOnDispose)
    val currentBookmarked = bookmarks.any {
        it.id == txtBookmarkId(screen.document.uriString, currentScrollFraction)
    }
    val renderPalette = rememberReaderRenderPalette(preferences.themeMode)
    val backgroundColor = Color(AndroidColor.parseColor(renderPalette.backgroundHex))
    val textColor = Color(AndroidColor.parseColor(renderPalette.textHex))
    val timestampBackgroundColor = Color(AndroidColor.parseColor(renderPalette.mutedHex)).copy(
        alpha = if (preferences.themeMode == ReaderThemeMode.DARK) 0.55f else 0.35f,
    )
    val searchMatchBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val currentSearchMatchBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
    val noteSearchMatches = remember(text, noteSearchQuery) {
        findTextMatches(text, noteSearchQuery)
    }
    val currentSearchIndex = if (noteSearchMatches.isEmpty()) {
        -1
    } else {
        noteSearchIndex.coerceIn(0, noteSearchMatches.lastIndex)
    }
    val currentSearchRange = noteSearchMatches.getOrNull(currentSearchIndex)
    val enableInlineNoteStyling = text.length <= NoteInlineStyleMaxChars
    val noteVisualTransformation: VisualTransformation = remember(
        timestampBackgroundColor,
        searchMatchBackgroundColor,
        currentSearchMatchBackgroundColor,
        noteSearchMatches,
        currentSearchRange,
        enableInlineNoteStyling,
    ) {
        if (enableInlineNoteStyling) {
            NoteTextVisualTransformation(
                timestampChipColor = timestampBackgroundColor,
                searchMatchColor = searchMatchBackgroundColor,
                currentSearchMatchColor = currentSearchMatchBackgroundColor,
                searchMatches = noteSearchMatches,
                currentSearchRange = currentSearchRange,
            )
        } else {
            VisualTransformation.None
        }
    }

    fun addNoteHistoryState(
        history: MutableList<NoteHistoryEntry>,
        value: TextFieldValue,
    ) {
        if (history.lastOrNull()?.value?.text == value.text) {
            return
        }
        if (estimateNoteHistoryBytes(value) > NoteHistorySnapshotMaxBytes) {
            return
        }
        noteHistoryOrder += 1L
        history += NoteHistoryEntry(
            value = value,
            order = noteHistoryOrder,
        )
        trimNoteHistories(undoHistory, redoHistory)
    }

    fun shouldRecordUndoCheckpoint(
        previousValue: TextFieldValue,
        nextValue: TextFieldValue,
        forceCheckpoint: Boolean,
    ): Boolean {
        if (forceCheckpoint || undoHistory.isEmpty()) {
            return true
        }
        if (estimateNoteHistoryBytes(nextValue) > NoteHistorySnapshotMaxBytes) {
            return false
        }
        val now = SystemClock.uptimeMillis()
        val lengthDelta = abs(nextValue.text.length - lastUndoCheckpointLength)
        val insertedText = when {
            nextValue.text.length > previousValue.text.length -> {
                val start = minOf(previousValue.selection.start, previousValue.selection.end)
                    .coerceIn(0, nextValue.text.length)
                val insertedLength = nextValue.text.length - previousValue.text.length
                nextValue.text.substring(start, (start + insertedLength).coerceAtMost(nextValue.text.length))
            }
            else -> ""
        }
        return now - lastUndoCheckpointAt >= NoteUndoTypingCoalesceMs ||
            lengthDelta >= NoteUndoCharacterBatch ||
            '\n' in insertedText
    }

    fun updateNoteValue(
        nextValue: TextFieldValue,
        recordUndo: Boolean = true,
        forceUndoCheckpoint: Boolean = false,
    ) {
        if (recordUndo && nextValue.text != noteValue.text) {
            if (shouldRecordUndoCheckpoint(noteValue, nextValue, forceUndoCheckpoint)) {
                addNoteHistoryState(undoHistory, noteValue)
                lastUndoCheckpointAt = SystemClock.uptimeMillis()
                lastUndoCheckpointLength = noteValue.text.length
            }
            redoHistory.clear()
        }
        if (nextValue.text != noteValue.text) {
            noteEditedSinceOpen = true
            val now = SystemClock.uptimeMillis()
            if (
                nextValue.text != lastAutosavedText &&
                now - lastAutosaveAt >= NoteAutosaveMinIntervalMs &&
                abs(nextValue.text.length - lastAutosavedText.length) >= NoteAutosaveCharacterBatch
            ) {
                lastAutosaveAt = now
                lastAutosavedText = nextValue.text
                onSaveText(nextValue.text)
            }
        }
        noteValue = nextValue
    }

    fun undoNoteChange() {
        val previousEntry = undoHistory.lastOrNull() ?: return
        undoHistory.removeAt(undoHistory.lastIndex)
        addNoteHistoryState(redoHistory, noteValue)
        noteValue = previousEntry.value
    }

    fun redoNoteChange() {
        val nextEntry = redoHistory.lastOrNull() ?: return
        redoHistory.removeAt(redoHistory.lastIndex)
        addNoteHistoryState(undoHistory, noteValue)
        noteValue = nextEntry.value
    }

    fun saveDraftIfChanged(textToSave: String = latestText) {
        if (textToSave == lastAutosavedText) {
            return
        }
        lastAutosaveAt = SystemClock.uptimeMillis()
        lastAutosavedText = textToSave
        onSaveText(textToSave)
    }

    fun selectNoteSearchMatch(index: Int) {
        if (noteSearchMatches.isEmpty()) {
            return
        }
        val safeIndex = ((index % noteSearchMatches.size) + noteSearchMatches.size) % noteSearchMatches.size
        val range = noteSearchMatches[safeIndex]
        noteSearchIndex = safeIndex
        updateNoteValue(
            nextValue = noteValue.copy(selection = TextRange(range.first, range.last + 1)),
            recordUndo = false,
        )
    }

    fun closeNoteSearch() {
        showNoteSearch = false
        noteSearchQuery = ""
        noteSearchIndex = 0
    }

    fun saveAndLeave() {
        saveOnDispose = false
        saveDraftIfChanged(latestText)
        onSaveProgress(currentScrollFraction)
        onBack()
    }

    fun discardAndLeave() {
        saveOnDispose = false
        onDiscardText()
    }

    fun insertTimestamp() {
        val timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd | hh:mm a :", Locale.US),
        )
        val selectionStart = minOf(noteValue.selection.start, noteValue.selection.end)
            .coerceIn(0, noteValue.text.length)
        val selectionEnd = maxOf(noteValue.selection.start, noteValue.selection.end)
            .coerceIn(0, noteValue.text.length)
        val prefix = if (selectionStart > 0 && noteValue.text.getOrNull(selectionStart - 1) != '\n') "\n" else ""
        val suffix = if (selectionEnd < noteValue.text.length && noteValue.text.getOrNull(selectionEnd) != '\n') "\n" else ""
        val insertion = "$prefix$timestamp $suffix"
        val nextText = buildString {
            append(noteValue.text.substring(0, selectionStart))
            append(insertion)
            append(noteValue.text.substring(selectionEnd))
        }
        val cursor = selectionStart + insertion.length
        updateNoteValue(
            nextValue = TextFieldValue(
                text = nextText,
                selection = TextRange(cursor),
            ),
            forceUndoCheckpoint = true,
        )
    }

    BackHandler {
        saveAndLeave()
    }

    DisposableEffect(screen.document.uriString) {
        onDispose {
            if (shouldSaveOnDispose) {
                saveDraftIfChanged(latestText)
                onSaveProgress(latestScrollFraction)
            }
        }
    }

    LaunchedEffect(screen.document.uriString, screen.initialScrollFraction, scrollState.maxValue) {
        if (screen.initialScrollFraction > 0f && scrollState.maxValue > 0) {
            scrollState.scrollTo((scrollState.maxValue * screen.initialScrollFraction.coerceIn(0f, 1f)).roundToInt())
        }
    }
    LaunchedEffect(showNoteSearch, noteSearchQuery, noteSearchMatches.size) {
        noteSearchIndex = if (showNoteSearch && noteSearchQuery.isNotBlank() && noteSearchMatches.isNotEmpty()) {
            noteSearchIndex.coerceIn(0, noteSearchMatches.lastIndex)
        } else {
            0
        }
    }
    LaunchedEffect(text, noteEditedSinceOpen) {
        if (noteEditedSinceOpen && text != lastAutosavedText) {
            delay(NoteAutosaveDebounceMs)
            saveDraftIfChanged(text)
        }
    }
    LaunchedEffect(
        noteValue.text.length,
        noteValue.selection.start,
        noteValue.selection.end,
        editorHeightPx,
        scrollState.maxValue,
        noteEditedSinceOpen,
    ) {
        val layout = noteTextLayoutRef.value ?: return@LaunchedEffect
        if (!noteEditedSinceOpen || editorHeightPx <= 0 || scrollState.maxValue <= 0) {
            return@LaunchedEffect
        }

        val cursor = maxOf(noteValue.selection.start, noteValue.selection.end)
            .coerceIn(0, noteValue.text.length)
        val cursorRect = layout.getCursorRect(cursor)
        val visibleTop = scrollState.value + cursorKeepVisiblePaddingPx
        val visibleBottom = scrollState.value + editorHeightPx - cursorKeepVisiblePaddingPx
        val nextScroll = when {
            cursorRect.bottom > visibleBottom -> {
                (cursorRect.bottom - editorHeightPx + cursorKeepVisiblePaddingPx)
                    .roundToInt()
                    .coerceIn(0, scrollState.maxValue)
            }
            cursorRect.top < visibleTop -> {
                (cursorRect.top - cursorKeepVisiblePaddingPx)
                    .roundToInt()
                    .coerceIn(0, scrollState.maxValue)
            }
            else -> null
        }

        if (nextScroll != null && nextScroll != scrollState.value) {
            scrollState.scrollTo(nextScroll)
        }
    }

    if (showSettings) {
        ReaderSettingsSheet(
            preferences = preferences,
            showPdfNote = false,
            onDismiss = { showSettings = false },
            onPreferencesChange = onPreferencesChange,
        )
    }

    if (showRename) {
        RenameDialog(
            name = renameText,
            onNameChange = { renameText = it },
            onDismiss = { showRename = false },
            onConfirm = {
                onRenameText(renameText, latestText)
                showRename = false
            },
        )
    }
    KeepReaderScreenAwake(enabled = preferences.keepScreenOn)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = backgroundColor,
                titleContentColor = textColor,
            ),
            navigationIcon = {
                TextButton(onClick = ::discardAndLeave) {
                    Text(text = "Discard")
                }
            },
            title = {
                Column {
                    Text(
                        text = screen.document.title,
                        modifier = Modifier.clickable {
                            renameText = screen.document.title
                            showRename = true
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "TXT",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                IconButton(onClick = { showNoteSearch = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search note",
                        tint = textColor,
                    )
                }
                TextButton(onClick = ::saveAndLeave) {
                    Text(text = "Save")
                }
                IconButton(onClick = { onToggleBookmark(currentScrollFraction) }) {
                    Icon(
                        imageVector = if (currentBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (currentBookmarked) "Remove bookmark" else "Add bookmark",
                        tint = if (currentBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                    )
                }
            },
        )
        if (showNoteSearch) {
            NoteEditorSearchBar(
                query = noteSearchQuery,
                matchLabel = when {
                    noteSearchQuery.isBlank() -> ""
                    noteSearchMatches.isEmpty() -> "0 / 0"
                    else -> "${currentSearchIndex + 1} / ${noteSearchMatches.size}"
                },
                hasMatches = noteSearchMatches.isNotEmpty(),
                backgroundColor = backgroundColor,
                textColor = textColor,
                onQueryChange = { query ->
                    noteSearchQuery = query
                    noteSearchIndex = 0
                },
                onPrevious = {
                    selectNoteSearchMatch(if (currentSearchIndex < 0) 0 else currentSearchIndex - 1)
                },
                onNext = {
                    selectNoteSearchMatch(if (currentSearchIndex < 0) 0 else currentSearchIndex + 1)
                },
                onClose = ::closeNoteSearch,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            BasicTextField(
                value = noteValue,
                onValueChange = { nextValue ->
                    updateNoteValue(nextValue)
                },
                onTextLayout = { layoutResult ->
                    noteTextLayoutRef.value = layoutResult
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size -> editorHeightPx = size.height }
                    .verticalScroll(scrollState),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = textColor,
                    fontFamily = preferences.fontChoice.composeFontFamily(),
                    fontSize = preferences.fontSizeSp.sp,
                    lineHeight = (preferences.fontSizeSp * preferences.lineSpacing.coerceIn(1.2f, 2.4f)).sp,
                ),
                visualTransformation = noteVisualTransformation,
            )
            if (text.isBlank()) {
                Text(
                    text = "Write note...",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = textColor.copy(alpha = 0.45f),
                        fontFamily = preferences.fontChoice.composeFontFamily(),
                        fontSize = preferences.fontSizeSp.sp,
                        lineHeight = (preferences.fontSizeSp * preferences.lineSpacing.coerceIn(1.2f, 2.4f)).sp,
                    ),
                )
            }
        }
        NoteEditorHelperBar(
            backgroundColor = backgroundColor,
            iconColor = textColor,
            canUndo = undoHistory.isNotEmpty(),
            canRedo = redoHistory.isNotEmpty(),
            onUndo = ::undoNoteChange,
            onRedo = ::redoNoteChange,
            onInsertTimestamp = ::insertTimestamp,
        )
    }
}

@Composable
private fun NoteEditorSearchBar(
    query: String,
    matchLabel: String,
    hasMatches: Boolean,
    backgroundColor: Color,
    textColor: Color,
    onQueryChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                placeholder = {
                    Text(text = "Search note")
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            )
            if (matchLabel.isNotBlank()) {
                Text(
                    text = matchLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.68f),
                )
            }
            IconButton(
                enabled = hasMatches,
                onClick = onPrevious,
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = "Previous match",
                    tint = if (hasMatches) textColor else textColor.copy(alpha = 0.28f),
                )
            }
            IconButton(
                enabled = hasMatches,
                onClick = onNext,
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Next match",
                    tint = if (hasMatches) textColor else textColor.copy(alpha = 0.28f),
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close search",
                    tint = textColor,
                )
            }
        }
    }
}

@Composable
private fun NoteEditorHelperBar(
    backgroundColor: Color,
    iconColor: Color,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onInsertTimestamp: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    enabled = canUndo,
                    onClick = onUndo,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) iconColor else iconColor.copy(alpha = 0.28f),
                    )
                }
                IconButton(
                    enabled = canRedo,
                    onClick = onRedo,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) iconColor else iconColor.copy(alpha = 0.28f),
                    )
                }
            }
            IconButton(onClick = onInsertTimestamp) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = "Insert timestamp",
                    tint = iconColor,
                )
            }
        }
    }
}

private const val NoteHistoryMaxDepth = 20
private const val NoteHistoryMemoryLimitBytes = 512 * 1024
private const val NoteHistorySnapshotMaxBytes = 128 * 1024
private const val NoteUndoTypingCoalesceMs = 1_200L
private const val NoteUndoCharacterBatch = 512
private const val NoteAutosaveMinIntervalMs = 1_200L
private const val NoteAutosaveDebounceMs = 700L
private const val NoteAutosaveCharacterBatch = 256
private const val NoteTimestampMaxPrefixLength = 48
private const val NoteInlineStyleMaxChars = 8_000

private class NoteTextLayoutRef {
    var value: TextLayoutResult? = null
}

private data class NoteHistoryEntry(
    val value: TextFieldValue,
    val order: Long,
)

private fun estimateNoteHistoryBytes(value: TextFieldValue): Int {
    return value.text.length * 2 + 64
}

private fun trimNoteHistories(
    undoHistory: MutableList<NoteHistoryEntry>,
    redoHistory: MutableList<NoteHistoryEntry>,
) {
    while (undoHistory.size > NoteHistoryMaxDepth) {
        undoHistory.removeAt(0)
    }
    while (redoHistory.size > NoteHistoryMaxDepth) {
        redoHistory.removeAt(0)
    }

    while (noteHistoryBytes(undoHistory) + noteHistoryBytes(redoHistory) > NoteHistoryMemoryLimitBytes) {
        val oldestUndo = undoHistory.firstOrNull()
        val oldestRedo = redoHistory.firstOrNull()
        when {
            oldestUndo == null && oldestRedo == null -> return
            oldestRedo == null -> undoHistory.removeAt(0)
            oldestUndo == null -> redoHistory.removeAt(0)
            oldestUndo.order <= oldestRedo.order -> undoHistory.removeAt(0)
            else -> redoHistory.removeAt(0)
        }
    }
}

private fun noteHistoryBytes(history: List<NoteHistoryEntry>): Int {
    return history.sumOf { entry -> estimateNoteHistoryBytes(entry.value) }
}

private class NoteTextVisualTransformation(
    private val timestampChipColor: Color,
    private val searchMatchColor: Color,
    private val currentSearchMatchColor: Color,
    private val searchMatches: List<IntRange>,
    private val currentSearchRange: IntRange?,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder()
        builder.append(text.text)
        text.spanStyles.forEach { style ->
            builder.addStyle(style.item, style.start, style.end)
        }
        text.paragraphStyles.forEach { style ->
            builder.addStyle(style.item, style.start, style.end)
        }
        findTimestampPrefixRanges(text.text).forEach { range ->
            builder.addStyle(
                style = SpanStyle(background = timestampChipColor),
                start = range.first,
                end = range.last + 1,
            )
        }
        searchMatches
            .filter { range -> range.first >= 0 && range.last < text.text.length }
            .forEach { range ->
                builder.addStyle(
                    style = SpanStyle(background = searchMatchColor),
                    start = range.first,
                    end = range.last + 1,
                )
            }
        currentSearchRange
            ?.takeIf { range -> range.first >= 0 && range.last < text.text.length }
            ?.let { range ->
                builder.addStyle(
                    style = SpanStyle(background = currentSearchMatchColor),
                    start = range.first,
                    end = range.last + 1,
                )
            }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

private fun findTimestampPrefixRanges(text: String): List<IntRange> {
    if ('|' !in text || text.length < 12) {
        return emptyList()
    }

    val ranges = mutableListOf<IntRange>()
    var lineStart = 0
    while (lineStart <= text.length) {
        val lineEnd = text.indexOf('\n', lineStart).let { index ->
            if (index < 0) text.length else index
        }
        timestampPrefixLength(text, lineStart, lineEnd)?.let { length ->
            if (length > 0) {
                ranges += lineStart until (lineStart + length)
            }
        }
        if (lineEnd >= text.length) {
            break
        }
        lineStart = lineEnd + 1
    }
    return ranges
}

private fun timestampPrefixLength(
    text: String,
    lineStart: Int,
    lineEnd: Int,
): Int? {
    if (lineEnd - lineStart < 12) {
        return null
    }
    fun charAt(offset: Int): Char = text[lineStart + offset]
    fun isDigitAt(offset: Int): Boolean = charAt(offset).isDigit()
    val dateLooksRight = isDigitAt(0) &&
        isDigitAt(1) &&
        isDigitAt(2) &&
        isDigitAt(3) &&
        charAt(4) == '-' &&
        isDigitAt(5) &&
        isDigitAt(6) &&
        charAt(7) == '-' &&
        isDigitAt(8) &&
        isDigitAt(9)
    if (!dateLooksRight) {
        return null
    }

    var cursor = lineStart + 10
    while (cursor < lineEnd && text[cursor].isWhitespace() && text[cursor] != '\n') {
        cursor += 1
    }
    if (cursor >= lineEnd || text[cursor] != '|') {
        return null
    }

    val maxEnd = minOf(lineEnd, lineStart + NoteTimestampMaxPrefixLength)
    val afterPipe = (cursor + 1).coerceAtMost(maxEnd)
    val colonSpace = text.indexOf(": ", startIndex = afterPipe).takeIf { index ->
        index >= afterPipe && index + 1 < maxEnd
    }
    if (colonSpace != null) {
        return (colonSpace + 2 - lineStart).coerceAtMost(NoteTimestampMaxPrefixLength)
    }

    val lastColon = text.lastIndexOf(':', startIndex = maxEnd - 1).takeIf { index ->
        index >= afterPipe
    }
    if (lastColon != null) {
        return (lastColon + 1 - lineStart).coerceAtMost(NoteTimestampMaxPrefixLength)
    }

    val naturalStop = generateSequence(afterPipe) { index ->
        (index + 1).takeIf { it < maxEnd }
    }.firstOrNull { index ->
        text[index].isWhitespace() && index > afterPipe && text.getOrNull(index - 1)?.isLetter() == true
    }
    return ((naturalStop ?: maxEnd) - lineStart)
        .coerceIn(0, NoteTimestampMaxPrefixLength)
}

private fun findTextMatches(
    text: String,
    query: String,
): List<IntRange> {
    if (query.isBlank() || text.isEmpty()) {
        return emptyList()
    }
    val matches = mutableListOf<IntRange>()
    var startIndex = 0
    while (startIndex <= text.length - query.length) {
        val nextIndex = text.indexOf(query, startIndex, ignoreCase = true)
        if (nextIndex < 0) {
            break
        }
        matches += nextIndex..(nextIndex + query.length - 1)
        startIndex = nextIndex + query.length.coerceAtLeast(1)
    }
    return matches
}

@Composable
private fun EpubWebView(
    chapter: RenderedChapter,
    currentChapterFileUrl: String,
    preferences: ReaderPreferences,
    renderPalette: ReaderRenderPalette,
    initialScrollFraction: Float,
    modifier: Modifier = Modifier,
    onScrollProgressChange: (Float) -> Unit,
    onReaderTap: () -> Unit,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onOpenLocalHref: (String) -> Boolean,
    onNoteOpen: (String) -> Unit,
    searchQuery: String,
    searchRequest: Int,
    searchBackwards: Boolean,
    onSearchResult: (current: Int, count: Int) -> Unit,
) {
    val backgroundColor = AndroidColor.parseColor(renderPalette.backgroundHex)
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                var lastProgress = -1f
                var lastProgressAt = 0L
                var lastNoteOpenAt = 0L
                val openNote: (String) -> Unit = { note ->
                    lastNoteOpenAt = SystemClock.uptimeMillis()
                    onNoteOpen(note)
                }
                setBackgroundColor(backgroundColor)
                settings.javaScriptEnabled = true
                settings.loadsImagesAutomatically = true
                settings.blockNetworkLoads = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.setSupportZoom(true)
                addJavascriptInterface(NoteBridge(openNote), "AreadaNote")
                setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
                    if (isDoneCounting) {
                        onSearchResult(
                            if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0,
                            numberOfMatches,
                        )
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        restoreWebViewScroll(view, initialScrollFraction)
                        injectNoteHandler(view)
                        applyEpubChapterSearch(
                            webView = view,
                            query = searchQuery,
                            request = searchRequest,
                            backwards = searchBackwards,
                            onSearchResult = onSearchResult,
                        )
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val targetUrl = request?.url ?: return false
                        val scheme = targetUrl.scheme?.lowercase()
                        if (scheme == "http" || scheme == "https") {
                            return true
                        }

                        val fragment = targetUrl.fragment
                        val targetUrlString = targetUrl.toString()
                        val baseTarget = targetUrlString.substringBefore('#')
                        val currentBase = currentChapterFileUrl.substringBefore('#')

                        if (baseTarget != currentBase && onOpenLocalHref(targetUrlString)) {
                            return true
                        }

                        if (!fragment.isNullOrBlank() && view != null) {
                            openChapterNote(view, fragment, openNote)
                            return true
                        }

                        return true
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val scheme = request?.url?.scheme?.lowercase()
                        return if (scheme == "http" || scheme == "https") {
                            WebResourceResponse(
                                "text/plain",
                                "utf-8",
                                ByteArrayInputStream(ByteArray(0)),
                            )
                        } else {
                            super.shouldInterceptRequest(view, request)
                        }
                    }
                }

                setOnScrollChangeListener { _, _, scrollY, _, _ ->
                    val fullHeight = contentHeight * scale
                    val maxScroll = (fullHeight - height).coerceAtLeast(1f)
                    val progress = (scrollY / maxScroll).coerceIn(0f, 1f)
                    val now = SystemClock.uptimeMillis()
                    if (abs(progress - lastProgress) >= 0.015f || now - lastProgressAt >= 500L) {
                        lastProgress = progress
                        lastProgressAt = now
                        onScrollProgressChange(progress)
                    }
                }

                val gestureDetector = GestureDetector(
                    context,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                            if (SystemClock.uptimeMillis() - lastNoteOpenAt < 700L) {
                                return true
                            }
                            onReaderTap()
                            return false
                        }

                        override fun onDoubleTap(event: MotionEvent): Boolean {
                            resetWebViewZoom(this@apply)
                            return true
                        }

                        override fun onFling(
                            downEvent: MotionEvent?,
                            upEvent: MotionEvent,
                            velocityX: Float,
                            velocityY: Float,
                        ): Boolean {
                            val startEvent = downEvent ?: return false
                            val deltaX = upEvent.x - startEvent.x
                            val deltaY = upEvent.y - startEvent.y
                            val horizontal = abs(deltaX) > 90f &&
                                abs(deltaX) > abs(deltaY) * 1.25f &&
                                abs(velocityX) > 350f
                            if (!horizontal) {
                                return false
                            }

                            if (deltaX < 0f) {
                                onSwipeNext()
                            } else {
                                onSwipePrevious()
                            }
                            return true
                        }
                    },
                )

                setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                }

                loadDataWithBaseURL(
                    chapter.baseUrl,
                    chapter.html,
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        },
        update = { webView ->
            applyEpubChapterSearch(
                webView = webView,
                query = searchQuery,
                request = searchRequest,
                backwards = searchBackwards,
                onSearchResult = onSearchResult,
            )
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.setFindListener(null)
            webView.setOnTouchListener(null)
            webView.setOnScrollChangeListener(null)
            webView.webViewClient = WebViewClient()
            runCatching { webView.removeJavascriptInterface("AreadaNote") }
            webView.destroy()
        },
        modifier = modifier.fillMaxSize(),
    )
}

private data class EpubSearchViewState(
    val query: String,
    val request: Int,
)

private fun applyEpubChapterSearch(
    webView: WebView,
    query: String,
    request: Int,
    backwards: Boolean,
    onSearchResult: (current: Int, count: Int) -> Unit,
) {
    val cleanQuery = query.trim()
    val previous = webView.getTag(R.id.tag_epub_search_state) as? EpubSearchViewState

    if (cleanQuery.isBlank()) {
        if (previous?.query?.isNotBlank() == true) {
            webView.clearMatches()
            onSearchResult(0, 0)
        }
        webView.setTag(R.id.tag_epub_search_state, EpubSearchViewState("", request))
        return
    }

    if (previous?.query != cleanQuery) {
        webView.findAllAsync(cleanQuery)
    } else if (previous.request != request) {
        webView.findNext(backwards)
    }

    webView.setTag(R.id.tag_epub_search_state, EpubSearchViewState(cleanQuery, request))
}

@Composable
private fun ZoomablePage(
    bitmap: Bitmap,
    backgroundColor: Color,
    initialScale: Float,
    onScaleChange: (Float) -> Unit,
    onReaderTap: () -> Unit,
) {
    var scale by rememberSaveable(bitmap.generationId, initialScale) {
        mutableFloatStateOf(initialScale.coerceIn(1f, 5f))
    }
    var offset by remember(bitmap.generationId) {
        mutableStateOf(Offset.Zero)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        val containerWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val containerHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val baseWidth = containerWidth
        val baseHeight = baseWidth * bitmap.height.toFloat() / bitmap.width.toFloat()

        fun clampOffset(
            nextScale: Float,
            proposed: Offset,
        ): Offset {
            val maxX = ((baseWidth * nextScale) - containerWidth).coerceAtLeast(0f) / 2f
            val maxY = ((baseHeight * nextScale) - containerHeight).coerceAtLeast(0f) / 2f
            return Offset(
                x = proposed.x.coerceIn(-maxX, maxX),
                y = proposed.y.coerceIn(-maxY, maxY),
            )
        }

        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
            scale = nextScale
            offset = clampOffset(nextScale, offset + panChange)
            onScaleChange(nextScale)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmap.generationId) {
                    detectTapGestures(
                        onTap = {
                            onReaderTap()
                        },
                        onDoubleTap = {
                            scale = 1f
                            offset = Offset.Zero
                            onScaleChange(scale)
                        },
                    )
                }
                .transformable(state = transformableState),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF page",
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    preferences: ReaderPreferences,
    showPdfNote: Boolean,
    showReadingControls: Boolean = true,
    onDismiss: () -> Unit,
    onPreferencesChange: (ReaderPreferences) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var fontSizeDraft by rememberSaveable {
        mutableFloatStateOf(preferences.fontSizeSp.toFloat())
    }
    var lineSpacingDraft by rememberSaveable {
        mutableFloatStateOf(preferences.lineSpacing)
    }
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.5f).coerceAtLeast(260.dp)

    LaunchedEffect(preferences.fontSizeSp) {
        fontSizeDraft = preferences.fontSizeSp.toFloat()
    }
    LaunchedEffect(preferences.lineSpacing) {
        lineSpacingDraft = preferences.lineSpacing
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReaderThemeMode.entries.chunked(3).forEach { rowModes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowModes.forEach { mode ->
                            SettingChip(
                                label = mode.label,
                                selected = preferences.themeMode == mode,
                                onClick = {
                                    onPreferencesChange(preferences.copy(themeMode = mode))
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - rowModes.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Font",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReaderFontChoice.entries.forEach { choice ->
                    SettingChip(
                        label = choice.label,
                        selected = preferences.fontChoice == choice,
                        onClick = {
                            onPreferencesChange(preferences.copy(fontChoice = choice))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (showReadingControls) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Font size",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "${fontSizeDraft.roundToInt().coerceIn(14, 30)}sp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Slider(
                    value = fontSizeDraft,
                    onValueChange = { value ->
                        fontSizeDraft = value
                    },
                    onValueChangeFinished = {
                        onPreferencesChange(preferences.copy(fontSizeSp = fontSizeDraft.roundToInt().coerceIn(14, 30)))
                    },
                    valueRange = 14f..30f,
                    steps = 7,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Line spacing",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "${(lineSpacingDraft * 10f).roundToInt() / 10f}x",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Slider(
                    value = lineSpacingDraft,
                    onValueChange = { value ->
                        lineSpacingDraft = value
                    },
                    onValueChangeFinished = {
                        onPreferencesChange(preferences.copy(lineSpacing = lineSpacingDraft.coerceIn(1.2f, 2.4f)))
                    },
                    valueRange = 1.2f..2.4f,
                    steps = 5,
                )
                if (showPdfNote) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Font settings apply to EPUB and TXT. PDF keeps the document's embedded typography.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Display",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(10.dp))
                SettingToggleRow(
                    label = "Keep screen on",
                    checked = preferences.keepScreenOn,
                    onCheckedChange = { checked ->
                        onPreferencesChange(preferences.copy(keepScreenOn = checked))
                    },
                )
                SettingToggleRow(
                    label = "Volume buttons turn pages",
                    checked = preferences.volumeButtonsTurnPages,
                    onCheckedChange = { checked ->
                        onPreferencesChange(preferences.copy(volumeButtonsTurnPages = checked))
                    },
                )
                SettingToggleRow(
                    label = "Invert volume buttons",
                    checked = preferences.invertVolumeButtons,
                    enabled = preferences.volumeButtonsTurnPages,
                    onCheckedChange = { checked ->
                        onPreferencesChange(preferences.copy(invertVolumeButtons = checked))
                    },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private data class ReaderTocEntry(
    val index: Int,
    val label: String,
)

@Composable
private fun ReaderChapterSearchOverlay(
    query: String,
    current: Int,
    count: Int,
    onQueryChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 92.dp, start = 16.dp, end = 16.dp),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                    ) {
                        if (query.isBlank()) {
                            Text(
                                text = "Search chapter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Text(
                text = if (query.isBlank()) "0 / 0" else "$current / $count",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onPrevious, enabled = count > 0) {
                Text(text = "Prev")
            }
            TextButton(onClick = onNext, enabled = count > 0) {
                Text(text = "Next")
            }
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        }
    }
}

@Composable
private fun ReaderTocOverlay(
    title: String,
    entries: List<ReaderTocEntry>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 112.dp)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(max = 240.dp)
                .clickable { },
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                if (entries.isEmpty()) {
                    Text(
                        text = "No entries available.",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 220.dp),
                    ) {
                        items(
                            items = entries,
                            key = { entry -> entry.index },
                        ) { entry ->
                            val selected = entry.index == currentIndex
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(entry.index) },
                                shape = RectangleShape,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ) {
                                Text(
                                    text = entry.label,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onSettings: (() -> Unit)?,
    onSearch: (() -> Unit)? = null,
    onTableOfContents: (() -> Unit)? = null,
    onBookmarkToggle: (() -> Unit)? = null,
    isBookmarked: Boolean = false,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        navigationIcon = {
            TextButton(onClick = onBack) {
                Text(text = "Library")
            }
        },
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            onSearch?.let { openSearch ->
                IconButton(onClick = openSearch) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search current chapter",
                    )
                }
            }
            onTableOfContents?.let { openToc ->
                IconButton(onClick = openToc) {
                    Icon(
                        imageVector = Icons.Outlined.FormatListBulleted,
                        contentDescription = "Table of contents",
                    )
                }
            }
            onBookmarkToggle?.let { toggleBookmark ->
                IconButton(onClick = toggleBookmark) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            onSettings?.let { openSettings ->
                IconButton(onClick = openSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                    )
                }
            }
        },
    )
}

@Composable
private fun ReaderFooter(
    leftLabel: String,
    rightLabel: String,
    centerLabel: String,
    leftEnabled: Boolean,
    rightEnabled: Boolean,
    onLeft: () -> Unit,
    onCenter: (() -> Unit)? = null,
    onRight: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    TextButton(onClick = onLeft, enabled = leftEnabled) {
                        Text(text = leftLabel)
                    }
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (onCenter == null) {
                        Text(
                            text = centerLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        TextButton(onClick = onCenter) {
                            Text(
                                text = centerLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    TextButton(onClick = onRight, enabled = rightEnabled) {
                        Text(text = rightLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun FolderRow(
    name: String,
    pinned: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RectangleShape,
        color = if (pinned) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = "Folder",
                tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (pinned) FontWeight.SemiBold else FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BookRow(
    title: String,
    type: DocumentType,
    progressLabel: String?,
    pinned: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RectangleShape,
        color = if (pinned) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (type) {
                    DocumentType.EPUB -> Icons.Outlined.ImportContacts
                    DocumentType.PDF -> Icons.Outlined.PictureAsPdf
                    DocumentType.TXT -> Icons.Outlined.Description
                },
                contentDescription = type.name,
                tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = type.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                progressLabel?.let { label ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PathRow(
    segments: List<LibraryPathSegment>,
    onOpenPath: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        segments.forEachIndexed { index, segment ->
            TextButton(onClick = { onOpenPath(segment.relativePath) }) {
                Text(text = segment.name)
            }
            if (index < segments.lastIndex) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NotePopup(
    note: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Note",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onClose) {
                    Text(text = "Close")
                }
            }
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun InfoCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RectangleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val contentAlpha = if (enabled) 1f else 0.45f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
        )
        Text(
            text = if (checked) "ON" else "OFF",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (checked && enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            },
        )
    }
}

@Composable
private fun ReaderMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
            ) {
                Text(text = "Dismiss")
            }
        }
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Loading",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun LoadingState(label: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun buildResumeLabel(
    progress: ReadingProgress?,
    type: DocumentType,
): String? {
    if (progress == null) {
        return null
    }

    return when (type) {
        DocumentType.EPUB -> {
            val total = progress.epubChapterCount.takeIf { it > 0 } ?: return "Resume chapter ${progress.epubChapterIndex + 1}"
            "Resume chapter ${progress.epubChapterIndex + 1} of $total"
        }

        DocumentType.PDF -> {
            val total = progress.pdfPageCount.takeIf { it > 0 } ?: return "Resume page ${progress.pdfPageIndex + 1}"
            "Resume page ${progress.pdfPageIndex + 1} of $total"
        }

        DocumentType.TXT -> null
    }
}

private fun restoreWebViewScroll(
    webView: WebView,
    fraction: Float,
) {
    if (fraction <= 0f) {
        return
    }

    val restore = {
        val fullHeight = (webView.contentHeight * webView.scale).toInt()
        val maxScroll = (fullHeight - webView.height).coerceAtLeast(0)
        webView.scrollTo(0, (maxScroll * fraction).roundToInt())
    }

    webView.post(restore)
    webView.postDelayed(restore, 150L)
}

private fun resetWebViewZoom(webView: WebView) {
    val currentScale = webView.scale.takeIf { it > 0f } ?: 1f
    val zoomFactor = (1f / currentScale).coerceIn(0.2f, 5f)
    if (abs(currentScale - 1f) > 0.02f) {
        webView.zoomBy(zoomFactor)
    }
}

private class NoteBridge(
    private val onNoteOpen: (String) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun open(note: String?) {
        val cleanNote = note?.trim().orEmpty()
        if (cleanNote.isBlank()) {
            return
        }

        mainHandler.post {
            onNoteOpen(cleanNote)
        }
    }
}

private fun injectNoteHandler(webView: WebView) {
    webView.evaluateJavascript(
        """
        (function() {
          if (window.__areadaNoteHandlerInstalled) return;
          window.__areadaNoteHandlerInstalled = true;

          function findAnchor(start) {
            var node = start;
            while (node && node.tagName !== 'A') {
              node = node.parentElement;
            }
            return node;
          }

          function noteFor(targetId, fallback) {
            if (!targetId) return fallback || '';
            var node = document.getElementById(targetId) ||
              document.querySelector('[name="' + targetId.replace(/"/g, '\\"') + '"]');
            if (!node) return fallback || '';
            return (node.innerText || node.textContent || fallback || '').trim();
          }

          document.addEventListener('click', function(event) {
            var anchor = findAnchor(event.target);
            if (!anchor) return;

            var href = anchor.getAttribute('href') || '';
            var hash = '';
            try {
              hash = new URL(href, window.location.href).hash || '';
            } catch (error) {
              var hashIndex = href.indexOf('#');
              if (hashIndex >= 0) hash = href.substring(hashIndex);
            }
            if (!hash || hash.length < 2) return;

            var targetId = decodeURIComponent(hash.substring(1));
            var fallback = (anchor.getAttribute('title') || anchor.innerText || anchor.textContent || '').trim();
            var note = noteFor(targetId, fallback);
            if (note && window.AreadaNote) {
              event.preventDefault();
              window.AreadaNote.open(note);
            }
          }, true);
        })();
        """.trimIndent(),
        null,
    )
}

private fun openChapterNote(
    webView: WebView,
    fragment: String,
    onNoteOpen: (String) -> Unit,
) {
    val quotedFragment = JSONObject.quote(fragment)
    val script = """
        (function() {
          var id = $quotedFragment;
          var node = document.getElementById(id) || document.querySelector('[name="' + id.replace(/"/g, '\\"') + '"]');
          return node ? node.innerText : '';
        })();
    """.trimIndent()

    webView.evaluateJavascript(script) { result ->
        val note = decodeJavascriptString(result).trim()
        if (note.isNotBlank()) {
            onNoteOpen(note)
        }
    }
}

private fun decodeJavascriptString(value: String?): String {
    if (value.isNullOrBlank() || value == "null") {
        return ""
    }

    return runCatching {
        JSONArray("[$value]").optString(0)
    }.getOrDefault("")
}
