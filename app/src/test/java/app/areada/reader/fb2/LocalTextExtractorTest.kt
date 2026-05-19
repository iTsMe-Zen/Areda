package app.areada.reader.fb2

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LocalTextExtractorTest {
    @Test
    fun extractsTitleAndParagraphsFromFb2() {
        val input = """
            <FictionBook>
                <description>
                    <title-info>
                        <book-title> Tiny Book </book-title>
                        <author>
                            <first-name>Ada</first-name>
                            <last-name>Lovelace</last-name>
                        </author>
                    </title-info>
                </description>
                <body>
                    <section>
                        <title><p>Chapter One</p></title>
                        <p>Hello   world.</p>
                        <empty-line/>
                        <p>Second paragraph.</p>
                    </section>
                </body>
            </FictionBook>
        """.trimIndent().byteInputStream()

        val document = LocalTextExtractor.extractFb2Document(input)

        assertEquals(
            "Tiny Book\n\nChapter One\n\nHello world.\n\nSecond paragraph.",
            document.text,
        )
        assertEquals("Tiny Book", document.title)
        assertEquals("Ada Lovelace", document.author)
    }

    @Test
    fun extractsFb2EntryFromZip() {
        val zipped = ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("book.fb2"))
                zip.write(
                    """
                    <FictionBook>
                        <body>
                            <section>
                                <p>Zipped text.</p>
                            </section>
                        </body>
                    </FictionBook>
                    """.trimIndent().toByteArray(),
                )
                zip.closeEntry()
            }
            output.toByteArray()
        }

        val text = LocalTextExtractor.extractFb2Text(zipped.inputStream())

        assertEquals("Zipped text.", text)
    }

    @Test
    fun returnsEmptyWhenZipHasNoFb2Entry() {
        val zipped = ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("notes.txt"))
                zip.write("not an fb2 document".toByteArray())
                zip.closeEntry()
            }
            output.toByteArray()
        }

        val text = LocalTextExtractor.extractFb2Text(zipped.inputStream())

        assertEquals("", text)
    }
}
