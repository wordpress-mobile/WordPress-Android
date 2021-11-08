package org.wordpress.android.ui.about

/**
 * Helper class for converting values between Android and JBox2D.
 */
object Metrics {
    /**
     * This value defines how many pixels correspond to 1 meter.
     */
    private const val SCREEN_TO_WORLD_RATIO = 2000.0f

    /**
     * Coverts pixels to meters.
     */
    fun pixelsToMeters(value: Float): Float {
        return value / SCREEN_TO_WORLD_RATIO
    }

    /**
     * Coverts meters to pixels.
     */
    fun metersToPixels(value: Float): Float {
        return value * SCREEN_TO_WORLD_RATIO
    }
}
