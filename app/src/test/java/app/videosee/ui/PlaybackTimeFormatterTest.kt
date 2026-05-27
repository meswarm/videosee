package app.videosee.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackTimeFormatterTest {
    @Test
    fun formats_minutes_and_seconds() {
        assertEquals("1:05", PlaybackTimeFormatter.formatMillis(65_000))
    }

    @Test
    fun clamps_negative_values_to_zero() {
        assertEquals("0:00", PlaybackTimeFormatter.formatMillis(-1_000))
    }
}
