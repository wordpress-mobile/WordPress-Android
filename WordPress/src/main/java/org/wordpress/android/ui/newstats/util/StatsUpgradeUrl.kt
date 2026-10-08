package org.wordpress.android.ui.newstats.util

import org.wordpress.android.fluxc.model.SiteModel

private const val WPCOM_PRICING_URL = "https://wordpress.com/pricing/"
private const val JETPACK_PRICING_URL = "https://cloud.jetpack.com/pricing"

/**
 * The plans page to open when the user taps "Explore Plans" on a stat that is
 * gated behind a paid plan. Sites hosted on WordPress.com (including Atomic)
 * upgrade through WordPress.com, self-hosted Jetpack sites through Jetpack.
 */
fun SiteModel.statsUpgradeUrl(): String =
    if (isWPCom || isWPComAtomic) WPCOM_PRICING_URL else JETPACK_PRICING_URL
