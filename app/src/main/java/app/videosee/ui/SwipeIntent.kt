package app.videosee.ui

enum class SwipeIntent {
    Previous,
    Next;

    companion object {
        private const val THRESHOLD = 96f

        fun fromVerticalDrag(totalDrag: Float): SwipeIntent? = when {
            totalDrag > THRESHOLD -> Previous
            totalDrag < -THRESHOLD -> Next
            else -> null
        }
    }
}
