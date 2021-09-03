package org.wordpress.android.ui.reader.utils

enum class SiteVisibility {
    PRIVATE,
    PRIVATE_ATOMIC,
    PUBLIC
}

data class SiteAccessibilityInfo(val siteVisibility: SiteVisibility, val isPhotonCapable: Boolean)
