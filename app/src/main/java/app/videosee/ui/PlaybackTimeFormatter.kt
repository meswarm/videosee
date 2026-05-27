package app.videosee.ui

object PlaybackTimeFormatter {
    fun formatMillis(value: Long): String {
        val totalSeconds = (value.coerceAtLeast(0L)) / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }
}
