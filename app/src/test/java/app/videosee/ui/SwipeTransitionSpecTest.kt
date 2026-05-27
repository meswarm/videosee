package app.videosee.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeTransitionSpecTest {
    @Test
    fun settle_animation_duration_is_smooth_but_not_slow() {
        assertTrue(SwipeTransitionSpec.SETTLE_DURATION_MILLIS in 200..300)
    }
}
