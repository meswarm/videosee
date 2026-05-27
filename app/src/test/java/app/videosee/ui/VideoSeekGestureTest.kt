package app.videosee.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoSeekGestureTest {
    @Test
    fun left_swipe_seeks_backward_five_seconds() {
        assertEquals(-5_000L, VideoSeekGesture.seekOffsetMillis(-96.1f))
    }

    @Test
    fun right_swipe_seeks_forward_five_seconds() {
        assertEquals(5_000L, VideoSeekGesture.seekOffsetMillis(96.1f))
    }

    @Test
    fun seek_feedback_text_matches_direction() {
        assertEquals("后退5s", VideoSeekGesture.feedbackText(-5_000L))
        assertEquals("前进5s", VideoSeekGesture.feedbackText(5_000L))
    }

    @Test
    fun small_horizontal_drag_does_not_seek() {
        assertNull(VideoSeekGesture.seekOffsetMillis(12f))
    }
}
