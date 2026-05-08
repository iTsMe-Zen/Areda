package app.areada.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object DocumentResolver {
    fun resolve(context: Context, uri: Uri): ReaderDocument {
        val contentResolver = context.contentResolver
        val displayName = queryDisplayName(contentResolver, uri) ?: uri.lastPathSegment ?: "Untitled"
        val documentType = detectSupportedType(null, displayName)
            ?: detectSupportedType(contentResolver.getType(uri), displayName)
            ?: error("Only EPUB, PDF, and TXT files are supported.")
        val title = displayName.substringBeforeLast('.', displayName).ifBlank { displayName }

        return ReaderDocument(
            uri = uri,
            uriString = uri.toString(),
            title = title,
            type = documentType,
        )
    }

    fun detectSupportedType(mimeType: String?, name: String): DocumentType? {
        val lowerName = name.lowercase()
        return when {
            mimeType == "application/epub+zip" || lowerName.endsWith(".epub") -> DocumentType.EPUB
            mimeType == "application/pdf" || lowerName.endsWith(".pdf") -> DocumentType.PDF
            mimeType == "text/plain" || lowerName.endsWith(".txt") -> DocumentType.TXT
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
