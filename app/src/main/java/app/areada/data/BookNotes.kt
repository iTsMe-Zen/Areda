package app.areada.data

data class BookNoteLink(
    val bookUriString: String,
    val noteUriString: String,
    val noteTitle: String,
)

fun hasBookNote(
    bookUriString: String,
    linksByBookUri: Map<String, BookNoteLink>,
): Boolean =
    bookUriString.isNotBlank() && linksByBookUri[bookUriString]?.noteUriString?.isNotBlank() == true
