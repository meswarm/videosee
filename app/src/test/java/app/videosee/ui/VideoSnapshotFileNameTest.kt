package app.videosee.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoSnapshotFileNameTest {
    @Test
    fun appends_timestamp_to_video_file_name() {
        assertEquals(
            "sample.mp4_20260529_002301_123.jpg",
            VideoSnapshotFileName.create("sample.mp4", 1_779_985_381_123L),
        )
    }

    @Test
    fun replaces_path_unsafe_characters() {
        assertEquals(
            "a_b_c.mp4_20260529_002301_123.jpg",
            VideoSnapshotFileName.create("a/b:c.mp4", 1_779_985_381_123L),
        )
    }
}
