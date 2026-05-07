package app.areada.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReaderStateStore {
    private const val PREFERENCES_NAME = "areada_reader"
    private const val KEY_READER_PREFERENCES = "reader_preferences"
    private const val KEY_READING_PROGRESS = "reading_progress"
    private const val KEY_LIBRARY_ROOTS = "library_roots"
    private const val KEY_LIBRARY_SORT_MODE = "library_sort_mode"
    private const val KEY_PINNED_LIBRARY_ITEMS = "pinned_library_items"
    private const val KEY_LIBRARY_ADDED_AT = "library_added_at"

    fun loadPreferences(context: Context): ReaderPreferences {
        val payload = context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_READER_PREFERENCES, null)
            ?: return ReaderPreferences()

        return runCatching {
            val item = JSONObject(payload)
            ReaderPreferences(
                themeMode = ReaderThemeMode.entries.firstOrNull { it.name == item.optString("themeMode") }
                    ?: ReaderPreferences().themeMode,
                fontChoice = ReaderFontChoice.entries.firstOrNull { it.name == item.optString("fontChoice") }
                    ?: ReaderPreferences().fontChoice,
                fontSizeSp = item.optInt("fontSizeSp", ReaderPreferences().fontSizeSp).coerceIn(14, 30),
                lineSpacing = item.optDouble("lineSpacing", ReaderPreferences().lineSpacing.toDouble())
                    .toFloat()
                    .coerceIn(1.2f, 2.4f),
                keepScreenOn = item.optBoolean("keepScreenOn", ReaderPreferences().keepScreenOn),
            )
        }.getOrDefault(ReaderPreferences())
    }

    fun savePreferences(context: Context, preferences: ReaderPreferences) {
        val payload = JSONObject().apply {
            put("themeMode", preferences.themeMode.name)
            put("fontChoice", preferences.fontChoice.name)
            put("fontSizeSp", preferences.fontSizeSp.coerceIn(14, 30))
            put("lineSpacing", preferences.lineSpacing.coerceIn(1.2f, 2.4f).toDouble())
            put("keepScreenOn", preferences.keepScreenOn)
        }

        context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_READER_PREFERENCES, payload.toString())
            .apply()
    }

    fun loadProgress(context: Context): Map<String, ReadingProgress> {
        val payload = context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_READING_PROGRESS, null)
            ?: return emptyMap()

        return runCatching {
            buildMap {
                val items = JSONArray(payload)
                repeat(items.length()) { index ->
                    val item = items.optJSONObject(index) ?: return@repeat
                    val uriString = item.optString("uri")
                    if (uriString.isBlank()) {
                        return@repeat
                    }

                    val type = DocumentType.entries.firstOrNull { it.name == item.optString("type") } ?: return@repeat
                    put(
                        uriString,
                        ReadingProgress(
                            uriString = uriString,
                            type = type,
                            epubChapterIndex = item.optInt("epubChapterIndex").coerceAtLeast(0),
                            epubChapterCount = item.optInt("epubChapterCount").coerceAtLeast(0),
                            epubScrollFraction = item.optDouble("epubScrollFraction", 0.0).toFloat().coerceIn(0f, 1f),
                            pdfPageIndex = item.optInt("pdfPageIndex").coerceAtLeast(0),
                            pdfPageCount = item.optInt("pdfPageCount").coerceAtLeast(0),
                            pdfZoomScale = item.optDouble("pdfZoomScale", 1.0).toFloat().coerceIn(1f, 5f),
                            updatedAt = item.optLong("updatedAt", 0L),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun saveProgress(
        context: Context,
        progressByUri: Map<String, ReadingProgress>,
    ) {
        val payload = JSONArray()
        progressByUri.values
            .sortedByDescending { it.updatedAt }
            .forEach { progress ->
                payload.put(
                    JSONObject().apply {
                        put("uri", progress.uriString)
                        put("type", progress.type.name)
                        put("epubChapterIndex", progress.epubChapterIndex)
                        put("epubChapterCount", progress.epubChapterCount)
                        put("epubScrollFraction", progress.epubScrollFraction)
                        put("pdfPageIndex", progress.pdfPageIndex)
                        put("pdfPageCount", progress.pdfPageCount)
                        put("pdfZoomScale", progress.pdfZoomScale)
                        put("updatedAt", progress.updatedAt)
                    },
                )
            }

        context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_READING_PROGRESS, payload.toString())
            .apply()
    }

    fun loadLibraryRoots(context: Context): List<LibraryRoot> {
        val payload = context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LIBRARY_ROOTS, null)
            ?: return emptyList()

        return runCatching {
            buildList {
                val items = JSONArray(payload)
                repeat(items.length()) { index ->
                    val item = items.optJSONObject(index) ?: return@repeat
                    val treeUriString = item.optString("treeUri")
                    val name = item.optString("name")
                    if (treeUriString.isBlank() || name.isBlank()) {
                        return@repeat
                    }

                    add(
                        LibraryRoot(
                            treeUriString = treeUriString,
                            name = name,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveLibraryRoots(
        context: Context,
        roots: List<LibraryRoot>,
    ) {
        val payload = JSONArray()
        roots.forEach { root ->
            payload.put(
                JSONObject().apply {
                    put("treeUri", root.treeUriString)
                    put("name", root.name)
                },
            )
        }

        context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LIBRARY_ROOTS, payload.toString())
            .apply()
    }

    fun loadLibrarySortMode(context: Context): LibrarySortMode {
        val savedName = context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LIBRARY_SORT_MODE, null)
        return LibrarySortMode.entries.firstOrNull { it.name == savedName } ?: LibrarySortMode.NAME_ASC
    }

    fun saveLibrarySortMode(
        context: Context,
        sortMode: LibrarySortMode,
    ) {
        context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LIBRARY_SORT_MODE, sortMode.name)
            .apply()
    }

    fun loadPinnedLibraryItemIds(context: Context): Set<String> {
        val payload = context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PINNED_LIBRARY_ITEMS, null)
            ?: return emptySet()

        return runCatching {
            buildSet {
                val items = JSONArray(payload)
                repeat(items.length()) { index ->
                    val id = items.optString(index)
                    if (id.isNotBlank()) {
                        add(id)
                    }
                }
            }
        }.getOrDefault(emptySet())
    }

    fun savePinnedLibraryItemIds(
        context: Context,
        itemIds: Set<String>,
    ) {
        val payload = JSONArray()
        itemIds.sorted().forEach { id ->
            payload.put(id)
        }

        context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PINNED_LIBRARY_ITEMS, payload.toString())
            .apply()
    }

    fun loadLibraryAddedAt(context: Context): Map<String, Long> {
        val payload = context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LIBRARY_ADDED_AT, null)
            ?: return emptyMap()

        return runCatching {
            buildMap {
                val item = JSONObject(payload)
                item.keys().forEach { id ->
                    val timestamp = item.optLong(id, 0L)
                    if (id.isNotBlank() && timestamp > 0L) {
                        put(id, timestamp)
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun saveLibraryAddedAt(
        context: Context,
        addedAtById: Map<String, Long>,
    ) {
        val payload = JSONObject()
        addedAtById.forEach { (id, timestamp) ->
            if (id.isNotBlank() && timestamp > 0L) {
                payload.put(id, timestamp)
            }
        }

        context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LIBRARY_ADDED_AT, payload.toString())
            .apply()
    }
}
