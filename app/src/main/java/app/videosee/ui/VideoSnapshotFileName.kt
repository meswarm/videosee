package app.videosee.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object VideoSnapshotFileName {
    fun create(videoFileName: String, timestampMillis: Long): String {
        val safeName = videoFileName.replace(Regex("""[\\/:*?"<>|]"""), "_")
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }
        return "${safeName}_${formatter.format(Date(timestampMillis))}.jpg"
    }
}
