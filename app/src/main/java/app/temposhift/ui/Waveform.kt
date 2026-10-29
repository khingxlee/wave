package app.temposhift.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Big, bold loudness strip that fills the width of the screen: thick rounded bars, the played part bright,
 * the rest softer, a playhead line, and the A-B loop shaded. Tap or drag to seek.
 * [loopA] / [loopB] are positions from 0..1 (or -1 when not set).
 */
@Composable
fun Waveform(
    bars: ByteArray?,
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    animated: Boolean = false,
    playing: Boolean = false,
    loopA: Float = -1f,
    loopB: Float = -1f
) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val seekNow by rememberUpdatedState(onSeek)
    val shown = dragging ?: progress
    // "animated" look: while playing, the bars around the playhead dance like a voice note
    val phase = if (animated && playing) {
        val transition = rememberInfiniteTransition(label = "waveDance")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
            label = "wavePhase"
        ).value
    } else 0f
    val grow by animateFloatAsState(
        targetValue = if (bars != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "waveGrow"
    )
    Canvas(
        modifier
            .pointerInput(Unit) {
                detectTapGestures { off -> seekNow((off.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { off -> dragging = (off.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { dragging?.let { seekNow(it) }; dragging = null },
                    onDragCancel = { dragging = null },
                    onHorizontalDrag = { change, _ -> dragging = (change.position.x / size.width).coerceIn(0f, 1f) }
                )
            }
    ) {
        if (size.width < 16f || size.height < 2f) return@Canvas
        val step = 8.dp.toPx()                       // one bar every 8 dp: fewer, thicker bars
        val n = (size.width / step).toInt().coerceAtLeast(8)
        val barW = step * 0.62f
        val mid = size.height / 2f
        val maxH = (size.height * 0.96f).coerceAtLeast(2f)
        val minH = minOf(10.dp.toPx(), maxH)

        // A-B loop area
        if (loopA >= 0f) {
            val x1 = loopA * size.width
            val x2 = if (loopB > loopA) loopB * size.width else x1
            drawRect(ACCENT.copy(alpha = 0.16f), Offset(x1, 0f), Size(maxOf(x2 - x1, 3.dp.toPx()), size.height))
            drawRect(ACCENT, Offset(x1, 0f), Size(2.dp.toPx(), size.height))
            if (loopB > loopA) drawRect(ACCENT, Offset(x2 - 2.dp.toPx(), 0f), Size(2.dp.toPx(), size.height))
        }

        val len = bars?.size ?: 0
        for (i in 0 until n) {
            // one display bar = the loudest of the stored bars that fall inside it
            val real = if (bars != null && len > 0) {
                val from = (i * len) / n
                val to = (((i + 1) * len) / n).coerceAtLeast(from + 1).coerceAtMost(len)
                var m = 0
                for (k in from until to) { val v = bars[k].toInt() and 0xFF; if (v > m) m = v }
                m / 255f
            } else 0.15f
            val shaped = Math.pow(real.toDouble(), 0.8).toFloat()
            val v = 0.15f + (shaped - 0.15f) * grow
            var h = (v * maxH).coerceIn(minH, maxH)
            if (animated && playing) {
                val d = kotlin.math.abs(i - shown * n)
                if (d < 10f) h = (h * (1f + 0.4f * (1f - d / 10f) * kotlin.math.sin(phase + i * 0.9f))).coerceIn(minH, maxH)
            }
            val played = (i + 0.5f) / n <= shown
            val color = if (played) ACCENT else TEXT.copy(alpha = 0.28f)
            val x = i * (size.width / n) + (size.width / n - barW) / 2f
            drawRoundRect(color, Offset(x, mid - h / 2f), Size(barW, h), CornerRadius(barW / 2f))
        }

        // playhead
        val px = (shown * size.width).coerceIn(0f, size.width)
        drawRect(TEXT, Offset(px - 1.dp.toPx(), 0f), Size(2.dp.toPx(), size.height))
    }
}
