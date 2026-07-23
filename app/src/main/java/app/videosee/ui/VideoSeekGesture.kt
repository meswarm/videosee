package app.videosee.ui

import kotlin.math.abs
import kotlin.math.roundToLong

object VideoSeekGesture {
    private const val THRESHOLD_PX = 24f
    private const val MIN_SEEK_RANGE_MILLIS = 15_000L
    private const val MAX_SEEK_RANGE_MILLIS = 300_000L
    private const val UNKNOWN_DURATION_SEEK_RANGE_MILLIS = 60_000L

    fun seekOffsetMillis(
        totalDragX: Float,
        viewportWidthPx: Float,
        durationMillis: Long,
    ): Long? {
        if (abs(totalDragX) < THRESHOLD_PX || viewportWidthPx <= 0f) return null
        val seekRange = durationMillis
            .takeIf { it > 0L }
            ?.let { (it * 0.15f).roundToLong().coerceIn(MIN_SEEK_RANGE_MILLIS, MAX_SEEK_RANGE_MILLIS) }
            ?: UNKNOWN_DURATION_SEEK_RANGE_MILLIS
        return (totalDragX / viewportWidthPx * seekRange)
            .roundToLong()
            .takeIf { it != 0L }
    }

    fun feedbackText(seekOffsetMillis: Long): String {
        val seconds = abs(seekOffsetMillis) / 1_000f
        val durationText = if (seconds % 1f == 0f) "${seconds.toInt()}s" else "${"%.1f".format(seconds)}s"
        return durationText
    }
}
