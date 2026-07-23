package app.videosee.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoSeekGestureTest {
    @Test
    fun left_swipe_seeks_by_its_distance() {
        assertEquals(-2_400L, VideoSeekGesture.seekOffsetMillis(-96f, 600f, 60_000L))
    }

    @Test
    fun right_swipe_seeks_by_its_distance() {
        assertEquals(22_500L, VideoSeekGesture.seekOffsetMillis(300f, 600f, 300_000L))
    }

    @Test
    fun seek_feedback_text_matches_direction() {
        assertEquals("9.6s", VideoSeekGesture.feedbackText(-9_600L))
        assertEquals("37.5s", VideoSeekGesture.feedbackText(37_500L))
    }

    @Test
    fun small_horizontal_drag_does_not_seek() {
        assertNull(VideoSeekGesture.seekOffsetMillis(12f, 600f, 60_000L))
    }
}
