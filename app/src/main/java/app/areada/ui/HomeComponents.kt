package app.areada.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.ImportContacts
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.areada.R
import app.areada.data.BookStatus
import app.areada.data.DocumentType
import app.areada.data.LibraryBookEntry
import app.areada.data.LibraryFileFilter
import app.areada.data.LibraryFolderEntry
import app.areada.data.LibraryFolderPickerEntry
import app.areada.data.LibraryRoot
import app.areada.data.LibrarySearchResult
import app.areada.data.LibrarySearchResultType
import app.areada.data.LibrarySortMode
import app.areada.data.ReadingBookmark
import app.areada.data.ReadingProgress
import app.areada.data.RecentDocument
import app.areada.data.readingProgressPercent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

internal sealed interface LibraryActionTarget {
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

internal enum class HomeTab(val label: String) {
    Collection("Books"),
    Reading("Reading"),
    Bookmarks("Bookmarks"),
}

internal fun homeTabFromName(name: String): HomeTab =
    HomeTab.entries.firstOrNull { tab -> tab.name == name } ?: HomeTab.Collection

@Composable
private fun HomeTab.displayLabel(): String =
    when (this) {
        HomeTab.Collection -> stringResource(R.string.books)
        HomeTab.Reading -> stringResource(R.string.reading)
        HomeTab.Bookmarks -> stringResource(R.string.bookmarks)
    }

@Composable
private fun LibraryFileFilter.displayLabel(): String =
    when (this) {
        LibraryFileFilter.ALL -> stringResource(R.string.all)
        else -> label
    }

@Composable
private fun LibrarySortMode.displayLabel(): String =
    when (this) {
        LibrarySortMode.NAME_ASC -> stringResource(R.string.sort_name_az)
        LibrarySortMode.NAME_DESC -> stringResource(R.string.sort_name_za)
        LibrarySortMode.DATE_ADDED_ASC -> stringResource(R.string.sort_oldest_added)
        LibrarySortMode.DATE_ADDED_DESC -> stringResource(R.string.sort_newest_added)
        LibrarySortMode.RECENTLY_OPENED -> stringResource(R.string.sort_recently_opened)
        LibrarySortMode.READING_PROGRESS -> stringResource(R.string.sort_reading_progress)
        LibrarySortMode.FILE_TYPE -> stringResource(R.string.sort_file_type)
    }

@Composable
internal fun HomeTabRow(
    selectedTab: HomeTab,
    readingCount: Int,
    bookmarkCount: Int,
    onSelectTab: (HomeTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeTab.entries.forEach { tab ->
            val count = when (tab) {
                HomeTab.Collection -> null
                HomeTab.Reading -> readingCount
                HomeTab.Bookmarks -> bookmarkCount
            }
            HomeTabChip(
                label = tab.displayLabel(),
                count = count,
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HomeTabChip(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    }
    val labelText = buildAnnotatedString {
        append(label)
        count?.let { value ->
            append(" ")
            pushStyle(
                SpanStyle(
                    color = textColor.copy(alpha = if (selected) 0.58f else 0.52f),
                    fontWeight = FontWeight.Normal,
                ),
            )
            append("($value)")
            pop()
        }
    }
    Column(
        modifier = modifier
            .height(34.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = labelText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(2.dp)
                .background(
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RectangleShape,
                ),
        )
    }
}

internal fun List<LibraryBookEntry>.filterBooksByLibraryFileFilter(filter: LibraryFileFilter): List<LibraryBookEntry> =
    filter.documentType?.let { type -> this.filter { book -> book.type == type } } ?: this

internal fun List<ReadingBookmark>.filterBookmarksByLibraryFileFilter(filter: LibraryFileFilter): List<ReadingBookmark> =
    filter.documentType?.let { type -> this.filter { bookmark -> bookmark.type == type } } ?: this

internal fun List<RecentDocument>.filterRecentsByLibraryFileFilter(filter: LibraryFileFilter): List<RecentDocument> =
    filter.documentType?.let { type -> this.filter { recent -> recent.type == type } } ?: this

internal fun List<LibraryFolderEntry>.filterFoldersByLibraryFileFilter(
    filter: LibraryFileFilter,
    folderDocumentTypesById: Map<String, Set<DocumentType>>,
): List<LibraryFolderEntry> {
    val documentType = filter.documentType ?: return this
    if (folderDocumentTypesById.isEmpty()) {
        return this
    }
    return filter { folder -> folderDocumentTypesById[folder.id]?.contains(documentType) == true }
}

internal fun List<LibrarySearchResult>.filterSearchResultsByLibraryFileFilter(
    filter: LibraryFileFilter,
    folderDocumentTypesById: Map<String, Set<DocumentType>>,
): List<LibrarySearchResult> {
    val documentType = filter.documentType ?: return this
    if (folderDocumentTypesById.isEmpty()) {
        return filter { result ->
            result.type == LibrarySearchResultType.FOLDER || result.documentType == documentType
        }
    }
    return filter { result ->
        when (result.type) {
            LibrarySearchResultType.BOOK -> result.documentType == documentType
            LibrarySearchResultType.FOLDER -> folderDocumentTypesById[result.id]?.contains(documentType) == true
        }
    }
}

@Composable
internal fun LibraryFileFilter.emptyLibraryMessage(): String =
    when (this) {
        LibraryFileFilter.ALL -> stringResource(R.string.empty_library_all)
        LibraryFileFilter.EPUB -> stringResource(R.string.empty_library_epub)
        LibraryFileFilter.PDF -> stringResource(R.string.empty_library_pdf)
        LibraryFileFilter.TXT -> stringResource(R.string.empty_library_txt)
        LibraryFileFilter.FB2 -> stringResource(R.string.empty_library_fb2)
        LibraryFileFilter.ZIP -> stringResource(R.string.empty_library_zip)
    }

internal fun LibraryFileFilter.icon() =
    when (this) {
        LibraryFileFilter.ALL -> Icons.Outlined.LibraryBooks
        LibraryFileFilter.EPUB -> Icons.Outlined.ImportContacts
        LibraryFileFilter.PDF -> Icons.Outlined.PictureAsPdf
        LibraryFileFilter.TXT -> Icons.Outlined.Description
        LibraryFileFilter.FB2 -> Icons.Outlined.LibraryBooks
        LibraryFileFilter.ZIP -> Icons.Outlined.LibraryBooks
    }

@Composable
internal fun LibrarySortButton(
    sortMode: LibrarySortMode,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = sortMode.displayLabel(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            imageVector = Icons.Outlined.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = if (expanded) 180f else 0f },
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun LibrarySortInlinePanel(
    selectedSortMode: LibrarySortMode,
    onSelectSortMode: (LibrarySortMode) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            LibrarySortMode.entries.forEach { sortMode ->
                val selected = sortMode == selectedSortMode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSortMode(sortMode) }
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
                            RectangleShape,
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = sortMode.displayLabel(),
                        modifier = Modifier.weight(1f),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    if (selected) {
                        Text(
                            text = stringResource(R.string.selected),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LibraryFilterButton(
    filter: LibraryFileFilter,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.FilterList,
            contentDescription = stringResource(R.string.filter_files),
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = filter.displayLabel(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            imageVector = Icons.Outlined.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = if (expanded) 180f else 0f },
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun LibraryFilterInlinePanel(
    selectedFilter: LibraryFileFilter,
    onSelectFilter: (LibraryFileFilter) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            LibraryFileFilter.entries.forEach { filter ->
                val selected = filter == selectedFilter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFilter(filter) }
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
                            RectangleShape,
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = filter.icon(),
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = filter.displayLabel(),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
internal fun FolderPickerDropdown(
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
            .clickable {
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
                text = selectedEntry?.name ?: stringResource(R.string.folders),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = if (expanded) {
                    stringResource(R.string.close_folder_menu)
                } else {
                    stringResource(R.string.open_folder_menu)
                },
                modifier = Modifier.graphicsLayer {
                    rotationZ = if (expanded) 180f else 0f
                },
            )
        }
    }
}

@Composable
internal fun FolderPickerInlinePanel(
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
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SearchBar(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .height(40.dp)
            .onFocusChanged { state -> onFocusChanged(state.isFocused) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RectangleShape,
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.46f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = mutedColor,
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.bodyMedium,
                                color = mutedColor.copy(alpha = 0.78f),
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                    if (isSearching) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    if (query.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.clear_search),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onQueryChange("") },
                            tint = mutedColor,
                        )
                    }
                }
            }
        },
    )
}

@Composable
internal fun BookmarksSection(
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
            SectionHeader(title = stringResource(R.string.bookmarks))
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
                EmptyStateCard(
                    title = stringResource(R.string.no_bookmarks_title),
                    body = stringResource(R.string.no_bookmarks_body),
                )
            } else {
                bookmarks.take(20).forEach { bookmark ->
                    BookmarkRow(
                        bookmark = bookmark,
                        pinned = false,
                        hasNote = false,
                        onClick = { onOpenBookmark(bookmark) },
                        onActions = { onRemoveBookmark(bookmark) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
internal fun ReadingSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionHeader(title = stringResource(R.string.reading))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = count.toString(),
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
}

@Composable
internal fun BookmarkRow(
    bookmark: ReadingBookmark,
    pinned: Boolean,
    hasNote: Boolean,
    onClick: () -> Unit,
    onActions: () -> Unit,
) {
    SwipeActionBox(
        actionLabel = stringResource(R.string.actions),
        onSwipe = onActions,
    ) {
        BookRow(
            title = bookmark.title,
            type = bookmark.type,
            progressLabel = bookmark.positionLabel,
            pinned = pinned,
            hasNote = hasNote,
            onClick = onClick,
        )
    }
}
@Composable
internal fun SearchResults(
    results: List<LibrarySearchResult>,
    isSearching: Boolean,
    onOpenResult: (LibrarySearchResult) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            isSearching -> InfoCard(message = stringResource(R.string.searching_selected_folders))
            results.isEmpty() -> EmptyStateCard(
                title = stringResource(R.string.no_matching_books_title),
                body = stringResource(R.string.no_matching_books_body),
            )
            else -> {
                results.take(40).forEach { result ->
                    SearchResultRow(
                        result = result,
                        onClick = { onOpenResult(result) },
                    )
                }
                if (results.size > 40) {
                    Text(
                        text = stringResource(R.string.showing_first_40_results),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SearchResultRow(
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
                        DocumentType.FB2 -> Icons.Outlined.LibraryBooks
                        DocumentType.ZIP -> Icons.Outlined.LibraryBooks
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
internal fun ManageFoldersSheet(
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
                text = stringResource(R.string.manage_folders),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (roots.isEmpty()) {
                InfoCard(message = stringResource(R.string.no_folders_selected))
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
                                text = stringResource(R.string.current),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        TextButton(onClick = { onRemoveRoot(root) }) {
                            Text(text = stringResource(R.string.remove))
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
internal fun SwipeActionBox(
    actionLabel: String,
    onSwipe: () -> Unit,
    modifier: Modifier = Modifier,
    actionContainerColor: Color = MaterialTheme.colorScheme.primary,
    actionContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.38f },
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onSwipe()
            }
            false
        },
    )

    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RectangleShape,
                color = actionContainerColor,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = actionLabel,
                        color = actionContentColor,
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
internal fun DocumentListActionSheet(
    title: String,
    pinned: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActionSheetItem(
                label = if (pinned) stringResource(R.string.unpin) else stringResource(R.string.pin),
                onClick = onTogglePin,
            )
            if (canMoveUp) {
                ActionSheetItem(label = stringResource(R.string.move_up), onClick = onMoveUp)
            }
            if (canMoveDown) {
                ActionSheetItem(label = stringResource(R.string.move_down), onClick = onMoveDown)
            }
            ActionSheetItem(label = stringResource(R.string.remove), onClick = onRemove)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryActionSheet(
    target: LibraryActionTarget,
    bookStatus: BookStatus? = null,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onShowInfo: (() -> Unit)? = null,
    onMarkBookStatus: ((BookStatus) -> Unit)? = null,
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
            if (target is LibraryActionTarget.Book && onShowInfo != null) {
                ActionSheetItem(
                    label = stringResource(R.string.info),
                    onClick = onShowInfo,
                )
            }
            if (target is LibraryActionTarget.Book && onMarkBookStatus != null) {
                val nextStatus = if (bookStatus == BookStatus.Finished) {
                    BookStatus.Reading
                } else {
                    BookStatus.Finished
                }
                ActionSheetItem(
                    label = if (nextStatus == BookStatus.Finished) {
                        stringResource(R.string.mark_as_finished)
                    } else {
                        stringResource(R.string.mark_as_reading)
                    },
                    onClick = { onMarkBookStatus(nextStatus) },
                )
            }
            ActionSheetItem(
                label = if (target.pinned) stringResource(R.string.unpin) else stringResource(R.string.pin),
                onClick = onTogglePin,
            )
            ActionSheetItem(
                label = stringResource(R.string.rename),
                onClick = onRename,
            )
            ActionSheetItem(
                label = stringResource(R.string.delete),
                onClick = onDelete,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookInfoSheet(
    book: LibraryBookEntry,
    progress: ReadingProgress?,
    status: BookStatus,
    recent: RecentDocument?,
    onDismiss: () -> Unit,
    onMarkStatus: (BookStatus) -> Unit,
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
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(14.dp))
            BookInfoRow(label = stringResource(R.string.format), value = book.type.name)
            BookInfoRow(label = stringResource(R.string.status), value = status.displayLabel())
            BookInfoRow(label = stringResource(R.string.progress), value = bookInfoProgressLabel(progress))
            recent?.lastOpenedAt
                ?.takeIf { timestamp -> timestamp > 0L }
                ?.let { timestamp ->
                    BookInfoRow(label = stringResource(R.string.last_opened), value = formatBookInfoTimestamp(timestamp))
                }
            book.addedAt
                .takeIf { timestamp -> timestamp > 0L }
                ?.let { timestamp ->
                    BookInfoRow(label = stringResource(R.string.added), value = formatBookInfoTimestamp(timestamp))
                }
            BookInfoRow(label = stringResource(R.string.file), value = book.fileName)
            if (book.pinned) {
                BookInfoRow(label = stringResource(R.string.pinned), value = stringResource(R.string.yes))
            }
            Spacer(modifier = Modifier.height(10.dp))
            ActionSheetItem(
                label = if (status == BookStatus.Finished) {
                    stringResource(R.string.mark_as_reading)
                } else {
                    stringResource(R.string.mark_as_finished)
                },
                onClick = {
                    onMarkStatus(
                        if (status == BookStatus.Finished) {
                            BookStatus.Reading
                        } else {
                            BookStatus.Finished
                        },
                    )
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BookInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(92.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BookStatus.displayLabel(): String =
    when (this) {
        BookStatus.Unread -> stringResource(R.string.not_started)
        BookStatus.Reading -> stringResource(R.string.reading_status)
        BookStatus.Finished -> stringResource(R.string.finished)
    }

@Composable
private fun bookInfoProgressLabel(progress: ReadingProgress?): String {
    val percent = readingProgressPercent(progress) ?: return stringResource(R.string.not_started)
    return "$percent%"
}

private fun formatBookInfoTimestamp(timestamp: Long): String =
    runCatching {
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(timestamp))
    }.getOrDefault("Unknown")

@Composable
internal fun ActionSheetItem(
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
internal fun RenameDialog(
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
                    text = stringResource(R.string.rename),
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
                        label = stringResource(R.string.save),
                        highlighted = true,
                        onClick = onConfirm,
                        enabled = name.trim().isNotBlank(),
                        modifier = Modifier.weight(1f),
                    )
                    PromptChoiceButton(
                        label = stringResource(R.string.cancel),
                        highlighted = false,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

