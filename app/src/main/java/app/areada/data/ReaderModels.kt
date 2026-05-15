package app.areada.data

import android.net.Uri

enum class DocumentType {
    EPUB,
    PDF,
    TXT,
    FB2,
}

data class ReaderDocument(
    val uri: Uri,
    val uriString: String,
    val title: String,
    val type: DocumentType,
)

data class RecentDocument(
    val uriString: String,
    val title: String,
    val type: DocumentType,
    val lastOpenedAt: Long,
)
