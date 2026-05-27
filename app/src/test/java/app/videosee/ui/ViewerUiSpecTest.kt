package app.videosee.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerUiSpecTest {
    @Test
    fun video_controls_are_hidden_by_default() {
        assertFalse(ViewerUiSpec.CONTROLS_VISIBLE_BY_DEFAULT)
    }

    @Test
    fun folder_switch_animation_is_brief() {
        assertTrue(ViewerUiSpec.FOLDER_SWITCH_DURATION_MILLIS in 120..260)
    }

    @Test
    fun app_interaction_animations_are_fast_enough_for_browsing() {
        assertTrue(ViewerUiSpec.SCREEN_TRANSITION_DURATION_MILLIS in 120..260)
        assertTrue(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS in 120..260)
        assertTrue(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS in 100..220)
        assertTrue(ViewerUiSpec.SELECTION_TRANSITION_DURATION_MILLIS in 100..220)
    }
}
