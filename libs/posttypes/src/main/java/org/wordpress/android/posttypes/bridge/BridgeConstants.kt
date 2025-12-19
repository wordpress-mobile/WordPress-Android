package org.wordpress.android.posttypes.bridge

/**
 * Constants used for bridging with the main WordPress app module.
 *
 * ## Purpose
 * This file contains constants that mirror values from the main app module to maintain
 * compatibility while keeping this module isolated from FluxC and other legacy dependencies.
 *
 * ## Migration Notes
 * When merging this module back into the main app:
 * - Replace [EXTRA_SITE] usages with `WordPress.SITE`
 * - Remove this file entirely
 *
 * @see org.wordpress.android.posttypes.bridge package documentation
 */
object BridgeConstants {
    /**
     * Intent extra key for passing site data between activities.
     * Mirrors `WordPress.SITE` from the main app module.
     */
    const val EXTRA_SITE = "SITE"
}
