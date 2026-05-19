package app.areada.data

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

internal fun decodeLocalText(bytes: ByteArray): String {
    if (bytes.isEmpty()) {
        return ""
    }

    return when {
        bytes.startsWith(0xEF, 0xBB, 0xBF) ->
            bytes.decodeRange(offset = 3, charset = StandardCharsets.UTF_8)

        bytes.startsWith(0xFF, 0xFE) ->
            bytes.decodeRange(offset = 2, charset = StandardCharsets.UTF_16LE)

        bytes.startsWith(0xFE, 0xFF) ->
            bytes.decodeRange(offset = 2, charset = StandardCharsets.UTF_16BE)

        looksLikeUtf16Le(bytes) ->
            bytes.toString(StandardCharsets.UTF_16LE)

        looksLikeUtf16Be(bytes) ->
            bytes.toString(StandardCharsets.UTF_16BE)

        else -> decodeUtf8OrFallback(bytes)
    }
}

private fun ByteArray.startsWith(
    first: Int,
    second: Int,
): Boolean =
    size >= 2 &&
        (this[0].toInt() and 0xFF) == first &&
        (this[1].toInt() and 0xFF) == second

private fun ByteArray.startsWith(
    first: Int,
    second: Int,
    third: Int,
): Boolean =
    size >= 3 &&
        (this[0].toInt() and 0xFF) == first &&
        (this[1].toInt() and 0xFF) == second &&
        (this[2].toInt() and 0xFF) == third

private fun ByteArray.decodeRange(
    offset: Int,
    charset: java.nio.charset.Charset,
): String =
    copyOfRange(offset, size).toString(charset)

private fun decodeUtf8OrFallback(bytes: ByteArray): String =
    try {
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: CharacterCodingException) {
        bytes.toString(StandardCharsets.ISO_8859_1)
    }

private fun looksLikeUtf16Le(bytes: ByteArray): Boolean =
    looksLikeUtf16(bytes, zeroIndexParity = 1)

private fun looksLikeUtf16Be(bytes: ByteArray): Boolean =
    looksLikeUtf16(bytes, zeroIndexParity = 0)

private fun looksLikeUtf16(
    bytes: ByteArray,
    zeroIndexParity: Int,
): Boolean {
    val sampleSize = bytes.size.coerceAtMost(128)
    if (sampleSize < 8) {
        return false
    }
    var zeros = 0
    var checked = 0
    for (index in 0 until sampleSize) {
        if (index % 2 == zeroIndexParity) {
            checked += 1
            if (bytes[index].toInt() == 0) {
                zeros += 1
            }
        }
    }
    return checked > 0 && zeros >= checked / 2 && zeros >= 3
}
