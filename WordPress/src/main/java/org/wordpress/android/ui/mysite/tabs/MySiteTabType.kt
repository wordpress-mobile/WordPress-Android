package org.wordpress.android.ui.mysite.tabs

import androidx.annotation.StringRes
import org.wordpress.android.R

enum class MySiteTabType(val label: String, @StringRes val stringResId: Int, val trackingLabel: String) {
    ALL("all", R.string.my_site_all_tab_title, "nonexistent"),
    DASHBOARD("home", R.string.my_site_dashboard_tab_title, "dashboard"),
    SITE_MENU("menu", R.string.my_site_menu_tab_title, "site_menu");

    override fun toString() = label

    companion object {
        @JvmStatic
        fun fromString(label: String) = when {
            ALL.label == label -> ALL
            DASHBOARD.label == label -> DASHBOARD
            SITE_MENU.label == label -> SITE_MENU
            else -> SITE_MENU
        }
    }
}
