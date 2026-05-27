package app.videosee.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeIntentTest {
    @Test
    fun maps_downward_drag_to_previous_media() {
        assertEquals(SwipeIntent.Previous, SwipeIntent.fromVerticalDrag(96.1f))
    }

    @Test
    fun maps_upward_drag_to_next_media() {
        assertEquals(SwipeIntent.Next, SwipeIntent.fromVerticalDrag(-96.1f))
    }

    @Test
    fun ignores_small_vertical_drag() {
        assertNull(SwipeIntent.fromVerticalDrag(12f))
    }
}
