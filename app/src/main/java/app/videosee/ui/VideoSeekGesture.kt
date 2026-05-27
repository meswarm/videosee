package app.videosee.ui

object VideoSeekGesture {
    private const val THRESHOLD_PX = 96f
    const val SEEK_STEP_MILLIS = 5_000L

    fun seekOffsetMillis(totalDragX: Float): Long? = when {
        totalDragX > THRESHOLD_PX -> SEEK_STEP_MILLIS
        totalDragX < -THRESHOLD_PX -> -SEEK_STEP_MILLIS
        else -> null
    }

    fun feedbackText(seekOffsetMillis: Long): String = if (seekOffsetMillis < 0L) {
        "后退5s"
    } else {
        "前进5s"
    }
}
