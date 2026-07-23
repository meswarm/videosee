package app.videosee.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedOptionsTest {
    @Test
    fun fixed_speed_options_match_viewer_controls() {
        assertEquals(listOf(0.7f, 0.9f, 1.2f, 1.5f), PlaybackSpeedOptions.values)
    }
}
