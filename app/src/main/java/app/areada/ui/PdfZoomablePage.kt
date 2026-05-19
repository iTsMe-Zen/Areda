package app.areada.ui

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.areada.R
import app.areada.reader.pdf.PdfLinkLayer
import app.areada.reader.pdf.PdfLinkTarget

@Composable
internal fun ZoomablePage(
    bitmap: Bitmap,
    pageKey: Int,
    resetToken: Int,
    backgroundColor: Color,
    initialScale: Float,
    onScaleChange: (Float) -> Unit,
    onReaderTap: () -> Unit,
    linkLayer: PdfLinkLayer?,
    onPdfLink: (PdfLinkTarget) -> Unit,
) {
    var scale by rememberSaveable(pageKey, resetToken) {
        mutableFloatStateOf(initialScale.coerceIn(1f, 5f))
    }
    var offset by remember(pageKey, resetToken) {
        mutableStateOf(Offset.Zero)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        val containerWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val containerHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val baseWidth = containerWidth
        val baseHeight = baseWidth * bitmap.height.toFloat() / bitmap.width.toFloat()

        fun clampOffset(
            nextScale: Float,
            proposed: Offset,
        ): Offset {
            val maxX = ((baseWidth * nextScale) - containerWidth).coerceAtLeast(0f) / 2f
            val maxY = ((baseHeight * nextScale) - containerHeight).coerceAtLeast(0f) / 2f
            return Offset(
                x = proposed.x.coerceIn(-maxX, maxX),
                y = proposed.y.coerceIn(-maxY, maxY),
            )
        }

        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
            scale = nextScale
            offset = clampOffset(nextScale, offset + panChange)
            onScaleChange(nextScale)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pageKey, linkLayer, scale, offset) {
                    detectTapGestures(
                        onTap = { position ->
                            val link = linkLayer?.let { layer ->
                                viewPointToPdfPoint(
                                    position = position,
                                    containerWidth = containerWidth,
                                    containerHeight = containerHeight,
                                    baseWidth = baseWidth,
                                    baseHeight = baseHeight,
                                    scale = scale,
                                    offset = offset,
                                    pageWidth = layer.pageWidth.toFloat(),
                                    pageHeight = layer.pageHeight.toFloat(),
                                )?.let { pagePoint ->
                                    findPdfLinkAt(layer, pagePoint)
                                }
                            }
                            if (link != null) {
                                onPdfLink(link)
                            } else {
                                onReaderTap()
                            }
                        },
                        onDoubleTap = {
                            scale = 1f
                            offset = Offset.Zero
                            onScaleChange(scale)
                        },
                    )
                }
                .transformable(state = transformableState),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.pdf_page_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                ),
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

internal fun findPdfLinkAt(
    layer: PdfLinkLayer,
    pagePoint: Offset,
): PdfLinkTarget? {
    val candidatePoints = listOf(
        pagePoint,
        Offset(pagePoint.x, layer.pageHeight - pagePoint.y),
    ).distinct()
    val hitSlop = (layer.pageWidth * 0.012f).coerceIn(4f, 18f)
    return candidatePoints.firstNotNullOfOrNull { point ->
        layer.links.firstOrNull { link ->
            link.bounds.any { rect -> rect.normalized().expandedBy(hitSlop).contains(point.x, point.y) }
        }
    }
}

internal fun trimPdfBitmapCache(
    cache: MutableMap<PdfPageBitmapKey, Bitmap>,
    centerKey: PdfPageBitmapKey,
) {
    val keepRange = (centerKey.pageIndex - 2)..(centerKey.pageIndex + 2)
    cache.keys
        .filterNot { key ->
            key.pageIndex in keepRange &&
                (key.pageIndex == centerKey.pageIndex || key.renderScaleBucket == PdfBaseRenderScaleBucket)
        }
        .forEach { key ->
            cache.remove(key)?.let { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
}

internal fun viewPointToPdfPoint(
    position: Offset,
    containerWidth: Float,
    containerHeight: Float,
    baseWidth: Float,
    baseHeight: Float,
    scale: Float,
    offset: Offset,
    pageWidth: Float,
    pageHeight: Float,
): Offset? {
    val imageRect = transformedPageRect(
        containerWidth = containerWidth,
        containerHeight = containerHeight,
        baseWidth = baseWidth,
        baseHeight = baseHeight,
        scale = scale,
        offset = offset,
    )
    if (!imageRect.contains(position.x, position.y)) {
        return null
    }
    val x = ((position.x - imageRect.left) / imageRect.width()).coerceIn(0f, 1f) * pageWidth
    val y = ((position.y - imageRect.top) / imageRect.height()).coerceIn(0f, 1f) * pageHeight
    return Offset(x, y)
}

internal fun transformedPageRect(
    containerWidth: Float,
    containerHeight: Float,
    baseWidth: Float,
    baseHeight: Float,
    scale: Float,
    offset: Offset,
): RectF {
    val scaledWidth = baseWidth * scale
    val scaledHeight = baseHeight * scale
    val left = (containerWidth - scaledWidth) / 2f + offset.x
    val top = (containerHeight - scaledHeight) / 2f + offset.y
    return RectF(left, top, left + scaledWidth, top + scaledHeight)
}

internal fun RectF.expandedBy(amount: Float): RectF =
    RectF(left - amount, top - amount, right + amount, bottom + amount)

internal fun RectF.normalized(): RectF =
    RectF(
        minOf(left, right),
        minOf(top, bottom),
        maxOf(left, right),
        maxOf(top, bottom),
    )


