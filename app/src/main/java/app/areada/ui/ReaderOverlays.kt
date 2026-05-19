package app.areada.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.areada.R
import kotlin.math.max
import kotlin.math.roundToInt

internal data class ReaderTocEntry(
    val index: Int,
    val label: String,
)

@Composable
internal fun ReaderChapterSearchOverlay(
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
                                text = stringResource(R.string.search_chapter),
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
                Text(text = stringResource(R.string.previous))
            }
            TextButton(onClick = onNext, enabled = count > 0) {
                Text(text = stringResource(R.string.next))
            }
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        }
    }
}

@Composable
internal fun ReaderTocOverlay(
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
                        text = stringResource(R.string.no_entries_available),
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
internal fun ReaderTopBar(
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
                Text(text = stringResource(R.string.library))
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
                        contentDescription = stringResource(R.string.search_current_chapter),
                    )
                }
            }
            onTableOfContents?.let { openToc ->
                IconButton(onClick = openToc) {
                    Icon(
                        imageVector = Icons.Outlined.FormatListBulleted,
                        contentDescription = stringResource(R.string.table_of_contents),
                    )
                }
            }
            onBookmarkToggle?.let { toggleBookmark ->
                IconButton(onClick = toggleBookmark) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isBookmarked) {
                            stringResource(R.string.remove_bookmark)
                        } else {
                            stringResource(R.string.add_bookmark)
                        },
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            onSettings?.let { openSettings ->
                IconButton(onClick = openSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings),
                    )
                }
            }
        },
    )
}

@Composable
internal fun ReaderFooter(
    leftLabel: String,
    rightLabel: String,
    centerLabel: String,
    leftEnabled: Boolean,
    rightEnabled: Boolean,
    onLeft: () -> Unit,
    onCenter: (() -> Unit)? = null,
    onRight: () -> Unit,
    progressFraction: Float? = null,
    progressPercentFraction: Float? = null,
    progressKey: Any? = null,
    onProgressScrubbed: ((Float) -> Unit)? = null,
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
            if (progressFraction != null && onProgressScrubbed != null) {
                ReaderFooterProgressTrack(
                    progressFraction = progressFraction,
                    progressKey = progressKey,
                    onProgressScrubbed = onProgressScrubbed,
                )
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
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
                        ReaderFooterCenterLabel(
                            centerLabel = centerLabel,
                            progressPercentFraction = progressPercentFraction,
                        )
                    } else {
                        TextButton(onClick = onCenter) {
                            ReaderFooterCenterLabel(
                                centerLabel = centerLabel,
                                progressPercentFraction = progressPercentFraction,
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
private fun ReaderFooterCenterLabel(
    centerLabel: String,
    progressPercentFraction: Float?,
) {
    val cleanProgressPercent = progressPercentFraction
        ?.takeIf { value -> value.isFinite() }
        ?.coerceIn(0f, 1f)
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = centerLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        if (cleanProgressPercent != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${(cleanProgressPercent * 100f).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.66f),
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun ReaderFooterProgressTrack(
    progressFraction: Float,
    progressKey: Any?,
    onProgressScrubbed: (Float) -> Unit,
) {
    val latestOnProgressScrubbed by rememberUpdatedState(onProgressScrubbed)
    val releaseHandler = remember(progressKey) {
        Handler(Looper.getMainLooper())
    }
    var dragProgress by remember(progressKey) {
        mutableStateOf<Float?>(null)
    }

    DisposableEffect(progressKey) {
        onDispose {
            releaseHandler.removeCallbacksAndMessages(null)
        }
    }

    val shownProgress = (dragProgress ?: progressFraction).coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)
    val dotColor = MaterialTheme.colorScheme.primary
    val handleSize = 10.dp

    fun updateProgress(progress: Float) {
        val cleanProgress = progress.coerceIn(0f, 1f)
        releaseHandler.removeCallbacksAndMessages(null)
        dragProgress = cleanProgress
        latestOnProgressScrubbed(cleanProgress)
    }

    fun finishScrub() {
        val finalProgress = dragProgress
        if (finalProgress != null) {
            latestOnProgressScrubbed(finalProgress)
        }

        releaseHandler.removeCallbacksAndMessages(null)
        releaseHandler.postDelayed(
            {
                dragProgress = null
            },
            220L,
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(progressKey) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (size.width > 0) {
                            updateProgress(offset.x / size.width.toFloat())
                        }
                    },
                    onDrag = { change, _ ->
                        if (size.width > 0) {
                            updateProgress(change.position.x / size.width.toFloat())
                        }
                        change.consume()
                    },
                    onDragCancel = {
                        releaseHandler.removeCallbacksAndMessages(null)
                        dragProgress = null
                    },
                    onDragEnd = {
                        finishScrub()
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val travel = if (maxWidth > handleSize) maxWidth - handleSize else 0.dp
        val labelWidth = 40.dp
        val labelTravel = if (maxWidth > labelWidth) maxWidth - labelWidth else 0.dp
        Text(
            text = "${(shownProgress * 100f).roundToInt()}%",
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = labelTravel * shownProgress)
                .width(labelWidth),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 5.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(trackColor),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = travel * shownProgress)
                .size(handleSize)
                .background(dotColor, CircleShape),
        )
    }
}

@Composable
internal fun EpubSectionScrollThumb(
    progressFraction: Float,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 80),
        label = "epubSectionScrollThumb",
    )
    val backgroundIsDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val thumbColor = if (backgroundIsDark) {
        Color.White.copy(alpha = 0.48f)
    } else {
        Color.Black.copy(alpha = 0.40f)
    }
    val thumbHeight = 56.dp

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        if (maxHeight <= thumbHeight) {
            return@BoxWithConstraints
        }
        val travel = maxHeight - thumbHeight
        Box(
            modifier = Modifier
                .offset(y = travel * animatedProgress)
                .width(4.dp)
                .height(thumbHeight)
                .background(thumbColor, CircleShape),
        )
    }
}

