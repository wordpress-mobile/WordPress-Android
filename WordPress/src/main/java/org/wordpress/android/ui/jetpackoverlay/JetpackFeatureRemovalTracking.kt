package org.wordpress.android.ui.jetpackoverlay

/**
 * Identifies the final Jetpack feature-removal phase in analytics and in stored preference keys.
 *
 * The removal used to be a staged rollout driven by `jp_removal_*` remote flags, and both the Tracks
 * `phase` property and several SharedPrefs keys embed the phase name. The rollout finished, so the
 * flags are gone, but the literal is kept so events stay comparable and so users who dismissed a
 * card do not see it reappear.
 */
const val JETPACK_REMOVAL_TRACKING_NAME = "self_hosted"

/**
 * The value deep-link overlay events report for the Tracks `phase` property. These events used a
 * coarser two-bucket view of the same rollout than [JETPACK_REMOVAL_TRACKING_NAME] does: "one"
 * while the Jetpack-powered features were still present, "two" once they were removed. Kept
 * verbatim so historic events stay comparable.
 */
const val JETPACK_DEEPLINK_TRACKING_NAME = "two"
