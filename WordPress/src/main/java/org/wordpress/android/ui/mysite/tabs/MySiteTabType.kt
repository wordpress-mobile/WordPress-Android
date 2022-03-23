package org.wordpress.android.ui.mysite.tabs

import androidx.annotation.StringRes
import org.wordpress.android.R

enum class MySiteTabType(val label: String, @StringRes val stringResId: Int) {
    ALL("all", R.string.my_site_all_tab_title),
    DASHBOARD("dashboard", R.string.my_site_dashboard_tab_title),
    SITE_MENU("site_menu", R.string.my_site_menu_tab_title);

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
