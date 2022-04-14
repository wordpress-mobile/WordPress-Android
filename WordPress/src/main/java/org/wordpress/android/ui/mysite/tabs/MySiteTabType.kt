package org.wordpress.android.ui.mysite.tabs

import androidx.annotation.StringRes
import org.wordpress.android.R

enum class MySiteTabType(val trackingLabel: String, @StringRes val stringResId: Int) {
    ALL("all", R.string.my_site_all_tab_title),
    DASHBOARD("dashboard", R.string.my_site_dashboard_tab_title),
    SITE_MENU("site_menu", R.string.my_site_menu_tab_title);

    override fun toString() = trackingLabel

    companion object {
        @JvmStatic
        fun fromString(label: String) = when {
            ALL.trackingLabel == label -> ALL
            DASHBOARD.trackingLabel == label -> DASHBOARD
            SITE_MENU.trackingLabel == label -> SITE_MENU
            else -> SITE_MENU
        }
    }
}
