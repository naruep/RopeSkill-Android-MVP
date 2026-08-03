package com.ropeskill.app

class TrackingLossPauseController(
    private val pauseAfterMillis: Long = DEFAULT_PAUSE_AFTER_MILLIS,
) {
    private var trackingLostAtMillis: Long? = null

    fun update(
        trackingLost: Boolean,
        timestampMillis: Long,
    ): Boolean {
        if (!trackingLost) {
            reset()
            return false
        }

        val lostAtMillis = trackingLostAtMillis
        if (lostAtMillis == null) {
            trackingLostAtMillis = timestampMillis
            return false
        }
        return timestampMillis - lostAtMillis >= pauseAfterMillis
    }

    fun reset() {
        trackingLostAtMillis = null
    }

    private companion object {
        const val DEFAULT_PAUSE_AFTER_MILLIS = 1_000L
    }
}
