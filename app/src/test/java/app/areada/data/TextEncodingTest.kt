package app.areada.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.StandardCharsets

class TextEncodingTest {
    @Test
    fun decodesUtf8WithBom() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            "Hello".toByteArray(StandardCharsets.UTF_8)

        assertEquals("Hello", decodeLocalText(bytes))
    }

    @Test
    fun decodesUtf16LittleEndianWithBom() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) +
            "Hello".toByteArray(StandardCharsets.UTF_16LE)

        assertEquals("Hello", decodeLocalText(bytes))
    }

    @Test
    fun fallsBackForSimpleLatin1Text() {
        val bytes = byteArrayOf('C'.code.toByte(), 'a'.code.toByte(), 'f'.code.toByte(), 0xE9.toByte())

        assertEquals("Café", decodeLocalText(bytes))
    }
}
