package app.areada.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSectionsTest {
    @Test
    fun plainNoteBecomesSingleNoteSectionWithoutRewritingText() {
        val parsed = parseNoteSections("First line\nSecond line")

        assertFalse(parsed.hasExplicitSections)
        assertEquals(listOf(NoteSection("Note", "First line\nSecond line")), parsed.sections)
    }

    @Test
    fun headingBasedNoteParsesSectionsAndPreservesContent() {
        val parsed = parseNoteSections(
            """
            # Description

            Text for description.

            # Skills

            Kotlin
            Compose
            """.trimIndent(),
        )

        assertTrue(parsed.hasExplicitSections)
        assertEquals("Description", parsed.sections[0].title)
        assertEquals("Text for description.", parsed.sections[0].content)
        assertEquals("Skills", parsed.sections[1].title)
        assertEquals("Kotlin\nCompose", parsed.sections[1].content)
    }

    @Test
    fun duplicateAndBlankHeadingNamesAreMadeSafe() {
        val parsed = parseNoteSections(
            """
            #
            First
            # Skills
            Second
            # Skills
            Third
            """.trimIndent(),
        )

        assertEquals(listOf("Section", "Skills", "Skills 2"), parsed.sections.map { it.title })
    }

    @Test
    fun serializerWritesReadableTxtHeadings() {
        val serialized = serializeNoteSections(
            listOf(
                NoteSection("Description", "Text for description."),
                NoteSection("Skills", "Kotlin\nCompose"),
            ),
        )

        assertEquals(
            """
            # Description

            Text for description.

            # Skills

            Kotlin
            Compose
            """.trimIndent(),
            serialized,
        )
    }

    @Test
    fun addAndRenamePreserveExistingSectionContent() {
        val sections = listOf(
            NoteSection("Description", "Keep me"),
            NoteSection("Skills", "Kotlin"),
        )
        val added = addNoteSection(sections)
        val renamed = renameNoteSection(added, 1, "Description")

        assertEquals("Keep me", renamed[0].content)
        assertEquals("Description 2", renamed[1].title)
        assertEquals("Kotlin", renamed[1].content)
        assertEquals("New section", renamed[2].title)
    }

    @Test
    fun resolvesLastOpenedSectionByTitleAndFallsBackSafely() {
        val sections = listOf(
            NoteSection("Description", "A"),
            NoteSection("Skills", "B"),
        )

        assertEquals(1, resolveNoteSectionIndex(sections, "Skills"))
        assertEquals(0, resolveNoteSectionIndex(sections, "Missing"))
        assertEquals(0, resolveNoteSectionIndex(sections, ""))
        assertEquals(0, resolveNoteSectionIndex(emptyList(), "Skills"))
    }
}
