package app.areada.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import app.areada.R

object DocumentResolver {
    fun resolve(context: Context, uri: Uri): ReaderDocument {
        val contentResolver = context.contentResolver
        val displayName = runCatching { queryDisplayName(contentResolver, uri) }.getOrNull()
            ?: uri.lastPathSegment
            ?: "Untitled"
        val documentType = detectSupportedType(null, displayName)
            ?: detectSupportedType(runCatching { contentResolver.getType(uri) }.getOrNull(), displayName)
            ?: error(context.getString(R.string.unsupported_file_type))
        val title = displayName.substringBeforeLast('.', displayName).ifBlank { displayName }

        return ReaderDocument(
            uri = uri,
            uriString = uri.toString(),
            title = title,
            type = documentType,
        )
    }

    fun detectSupportedType(mimeType: String?, name: String): DocumentType? {
        val normalizedMime = mimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
        val lowerName = name.lowercase()
        return when {
            normalizedMime == "application/epub+zip" ||
                normalizedMime == "application/x-epub" ||
                normalizedMime == "application/epub" ||
                lowerName.endsWith(".epub") -> DocumentType.EPUB
            normalizedMime == "application/pdf" || lowerName.endsWith(".pdf") -> DocumentType.PDF
            normalizedMime == "text/plain" || lowerName.endsWith(".txt") -> DocumentType.TXT
            normalizedMime == "application/x-fictionbook+xml" ||
                normalizedMime == "application/fb2+xml" ||
                lowerName.endsWith(".fb2") ||
                lowerName.endsWith(".fb2.zip") ||
                lowerName.endsWith(".fbz") -> DocumentType.FB2
            normalizedMime == "application/zip" ||
                normalizedMime == "application/x-zip-compressed" ||
                lowerName.endsWith(".zip") -> DocumentType.ZIP
            else -> null
        }
    }

    private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    return cursor.getString(index)
                }
            }
        }

        return null
    }
}
