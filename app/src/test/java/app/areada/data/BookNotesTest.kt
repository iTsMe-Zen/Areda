package app.areada.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookNotesTest {
    @Test
    fun detectsExistingBookNoteByStableBookUri() {
        val links = mapOf(
            "book://one" to BookNoteLink(
                bookUriString = "book://one",
                noteUriString = "note://one",
                noteTitle = "Book Note",
            ),
        )

        assertTrue(hasBookNote("book://one", links))
        assertFalse(hasBookNote("book://two", links))
        assertFalse(hasBookNote("", links))
    }
}
