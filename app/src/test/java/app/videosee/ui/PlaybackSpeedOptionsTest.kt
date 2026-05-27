package app.videosee.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedOptionsTest {
    @Test
    fun fixed_speed_options_match_viewer_controls() {
        assertEquals(listOf(0.25f, 0.5f, 0.75f, 0.9f), PlaybackSpeedOptions.values)
    }
}
