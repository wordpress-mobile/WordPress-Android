package org.wordpress.android.ui.mysite.tabs

enum class MySiteTabType(val label: String) {
    ALL("all"),
    DASHBOARD("dashboard"),
    SITE_MENU("site_menu");

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
