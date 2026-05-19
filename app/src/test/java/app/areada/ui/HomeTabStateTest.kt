package app.areada.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeTabStateTest {
    @Test
    fun homeTabFromNameRestoresValidPersistedTab() {
        assertEquals(HomeTab.Reading, homeTabFromName("Reading"))
        assertEquals(HomeTab.Bookmarks, homeTabFromName("Bookmarks"))
    }

    @Test
    fun homeTabFromNameFallsBackToBooksForInvalidValue() {
        assertEquals(HomeTab.Collection, homeTabFromName(""))
        assertEquals(HomeTab.Collection, homeTabFromName("Missing"))
    }
}
