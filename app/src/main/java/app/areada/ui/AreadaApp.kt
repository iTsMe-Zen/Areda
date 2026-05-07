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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ImportContacts
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import app.areada.data.ReaderThemeMode
import app.areada.data.ReadingProgress
import app.areada.data.RecentDocument
import app.areada.data.renderPalette
import app.areada.reader.EpubChapter
import app.areada.reader.EpubEngine
import app.areada.reader.PdfPageRenderer
import app.areada.reader.RenderedChapter
import app.areada.ui.theme.ReaderTheme
import java.io.ByteArrayInputStream
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun AreadaApp(viewModel: ReaderViewModel = viewModel()) {
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

    LaunchedEffect(viewModel, context) {
        viewModel.initialize(context)
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
                        onBack = viewModel::closeReader,
                        onPreferencesChange = { preferences ->
                            viewModel.updatePreferences(context, preferences)
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
                        onBack = viewModel::closeReader,
                        onPreferencesChange = { preferences ->
                            viewModel.updatePreferences(context, preferences)
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
                        onBack = viewModel::closeReader,
                        onPreferencesChange = { preferences ->
                            viewModel.updatePreferences(context, preferences)
                        },
                        onSaveText = { text ->
                            viewModel.saveTextDocument(context, screen.document, text)
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
    var actionTarget by remember {
        mutableStateOf<LibraryActionTarget?>(null)
    }
    var renameTarget by remember {
        mutableStateOf<LibraryActionTarget?>(null)
    }
    var deleteTarget by remember {
        mutableStateOf<LibraryActionTarget?>(null)
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

    actionTarget?.let { target ->
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
                        onSwipe = { onRemoveRecent(recent) },
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
private fun CompactChoiceDialog(
    question: String,
    onDismiss: () -> Unit,
    onYes: () -> Unit,
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = question,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
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
            .height(44.dp)
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
            Text(text = "Search folders and files")
        },
    )
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
            TextButton(onClick = onTogglePin) {
                Text(text = if (target.pinned) "Unpin from top" else "Pin to top")
            }
            TextButton(onClick = onRename) {
                Text(text = "Rename")
            }
            TextButton(onClick = onDelete) {
                Text(text = "Delete")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
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
    onBack: () -> Unit,
    onPreferencesChange: (ReaderPreferences) -> Unit,
    onSaveProgress: (chapterIndex: Int, chapterCount: Int, scrollFraction: Float) -> Unit,
) {
    var showSettings by rememberSaveable(screen.document.uriString) {
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
    var chapterIndex by rememberSaveable(screen.document.uriString, screen.initialChapterIndex) {
        mutableIntStateOf(screen.initialChapterIndex)
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
    val latestChapterIndex by rememberUpdatedState(chapterIndex)
    val latestScrollFraction by rememberUpdatedState(scrollFraction)

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
                chapterError = displayError(throwable, "Unable to render this chapter.")
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
    KeepReaderScreenAwake(enabled = preferences.keepScreenOn)
    LaunchedEffect(isFullMode) {
        fullControlsVisible = false
    }
    val showReaderChrome = !isFullMode || fullControlsVisible
    ReaderStatusBarHidden(hidden = !showReaderChrome)

    fun switchToChapter(nextIndex: Int) {
        if (nextIndex !in screen.book.chapters.indices || nextIndex == chapterIndex) {
            return
        }
        chapterIndex = nextIndex
        scrollFraction = 0f
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                chapterError != null -> ReaderMessage(message = chapterError ?: "")
                renderedChapter == null -> LoadingState(label = "Rendering chapter")
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
                        subtitle = "Chapter ${chapterIndex + 1} of ${screen.book.chapters.size}",
                        onBack = onBack,
                        onSettings = { showSettings = true },
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
                        centerLabel = renderedChapter?.title ?: "Loading",
                        leftEnabled = chapterIndex > 0,
                        rightEnabled = chapterIndex < screen.book.chapters.lastIndex,
                        onLeft = ::goToPreviousChapter,
                        onRight = ::goToNextChapter,
                    )
                }
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
    onBack: () -> Unit,
    onPreferencesChange: (ReaderPreferences) -> Unit,
    onSaveProgress: (pageIndex: Int, pageCount: Int, zoomScale: Float) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val rendererResult = remember(screen.document.uriString) {
        runCatching { PdfPageRenderer(context, screen.document.uri) }
    }
    val renderer = rendererResult.getOrNull()

    DisposableEffect(renderer) {
        onDispose {
            renderer?.close()
        }
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
                    message = rendererResult.exceptionOrNull()?.let { displayError(it, "Unable to open that PDF.") }
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
    var isFullMode by remember(screen.document.uriString) {
        mutableStateOf(true)
    }
    var fullControlsVisible by rememberSaveable(screen.document.uriString) {
        mutableStateOf(false)
    }
    var pageIndex by rememberSaveable(screen.document.uriString, screen.initialPageIndex) {
        mutableIntStateOf(screen.initialPageIndex.coerceIn(0, max(pageCount - 1, 0)))
    }
    var zoomScale by rememberSaveable(screen.document.uriString, screen.initialZoomScale) {
        mutableFloatStateOf(screen.initialZoomScale.coerceIn(1f, 5f))
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
                        onSwipePrevious = ::goToPreviousPage,
                        onSwipeNext = ::goToNextPage,
                    )
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
                        centerLabel = "${zoomScale.roundToInt()}x zoom",
                        leftEnabled = pageIndex > 0,
                        rightEnabled = pageIndex < pageCount - 1,
                        onLeft = ::goToPreviousPage,
                        onRight = ::goToNextPage,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextReaderScreen(
    screen: ReaderScreen.Text,
    preferences: ReaderPreferences,
    onBack: () -> Unit,
    onPreferencesChange: (ReaderPreferences) -> Unit,
    onSaveText: (String) -> Unit,
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
    var text by remember(screen.document.uriString) {
        mutableStateOf(screen.initialText)
    }
    var saveOnDispose by remember(screen.document.uriString) {
        mutableStateOf(true)
    }
    val latestText by rememberUpdatedState(text)
    val shouldSaveOnDispose by rememberUpdatedState(saveOnDispose)
    val renderPalette = rememberReaderRenderPalette(preferences.themeMode)
    val backgroundColor = Color(AndroidColor.parseColor(renderPalette.backgroundHex))
    val textColor = Color(AndroidColor.parseColor(renderPalette.textHex))

    fun saveAndLeave() {
        saveOnDispose = false
        onSaveText(latestText)
        onBack()
    }

    fun discardAndLeave() {
        saveOnDispose = false
        onDiscardText()
    }

    BackHandler {
        saveAndLeave()
    }

    DisposableEffect(screen.document.uriString) {
        onDispose {
            if (shouldSaveOnDispose) {
                onSaveText(latestText)
            }
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

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
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
                    TextButton(onClick = ::saveAndLeave) {
                        Text(text = "Save")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { nextText ->
                    text = nextText
                },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = textColor,
                    fontFamily = preferences.fontChoice.composeFontFamily(),
                    fontSize = preferences.fontSizeSp.sp,
                    lineHeight = (preferences.fontSizeSp * preferences.lineSpacing.coerceIn(1.2f, 2.4f)).sp,
                ),
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
    }
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
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.setSupportZoom(true)
                addJavascriptInterface(NoteBridge(openNote), "AreadaNote")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        restoreWebViewScroll(view, initialScrollFraction)
                        injectNoteHandler(view)
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
        onRelease = { webView ->
            webView.stopLoading()
            webView.setOnTouchListener(null)
            webView.setOnScrollChangeListener(null)
            webView.webViewClient = WebViewClient()
            runCatching { webView.removeJavascriptInterface("AreadaNote") }
            webView.destroy()
        },
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun ZoomablePage(
    bitmap: Bitmap,
    backgroundColor: Color,
    initialScale: Float,
    onScaleChange: (Float) -> Unit,
    onReaderTap: () -> Unit,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
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
                .pointerInput(bitmap.generationId, scale) {
                    if (scale <= 1.05f) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = {
                                totalDrag = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDrag += dragAmount
                            },
                            onDragEnd = {
                                when {
                                    totalDrag < -90f -> onSwipeNext()
                                    totalDrag > 90f -> onSwipePrevious()
                                }
                                totalDrag = 0f
                            },
                            onDragCancel = {
                                totalDrag = 0f
                            },
                        )
                    }
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingChip(
                    label = "Follow system",
                    selected = !preferences.keepScreenOn,
                    onClick = {
                        onPreferencesChange(preferences.copy(keepScreenOn = false))
                    },
                    modifier = Modifier.weight(1f),
                )
                SettingChip(
                    label = "Always ON",
                    selected = preferences.keepScreenOn,
                    onClick = {
                        onPreferencesChange(preferences.copy(keepScreenOn = true))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
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
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onLeft, enabled = leftEnabled) {
                    Text(text = leftLabel)
                }
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRight, enabled = rightEnabled) {
                    Text(text = rightLabel)
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
