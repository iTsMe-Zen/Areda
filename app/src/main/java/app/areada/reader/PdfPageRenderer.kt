package app.areada.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import java.io.Closeable
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val PdfRenderScale = 2.25f
private const val PdfMaxRenderWidthPx = 2800
private const val PdfMaxBitmapPixels = 10_000_000

class PdfPageRenderer(
    context: Context,
    uri: Uri,
) : Closeable {
    private val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        ?: error("Unable to read that PDF.")
    private val renderer = PdfRenderer(fileDescriptor)
    private var closed = false

    val pageCount: Int
        get() = synchronized(this) {
            if (closed) 0 else renderer.pageCount
        }

    fun renderPage(pageIndex: Int, widthPx: Int): Bitmap {
        synchronized(this) {
            check(!closed) { "PDF renderer is closed." }
            require(pageIndex in 0 until renderer.pageCount) { "Page out of range." }

            val safeWidth = max(widthPx, 1)
            val page = renderer.openPage(pageIndex)
            try {
                val aspectRatio = page.height.toFloat() / page.width.toFloat()
                val targetWidth = (safeWidth * PdfRenderScale)
                    .roundToInt()
                    .coerceAtLeast(safeWidth)
                    .coerceAtMost(PdfMaxRenderWidthPx)
                val pixelCappedWidth = sqrt(PdfMaxBitmapPixels / aspectRatio)
                    .roundToInt()
                    .coerceAtLeast(1)
                val renderWidth = targetWidth.coerceAtMost(pixelCappedWidth)
                val heightPx = max((renderWidth * aspectRatio).roundToInt(), 1)
                val bitmap = Bitmap.createBitmap(renderWidth, heightPx, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(
                    bitmap,
                    Rect(0, 0, renderWidth, heightPx),
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                )
                return bitmap
            } finally {
                page.close()
            }
        }
    }

    override fun close() {
        synchronized(this) {
            if (closed) {
                return
            }
            closed = true
            runCatching { renderer.close() }
            runCatching { fileDescriptor.close() }
        }
    }
}
