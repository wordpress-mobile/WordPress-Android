package org.wordpress.android.ui.mysite.tabs

enum class MySiteTabType(val label: String) {
    DASHBOARD("dashboard"),
    EVERYTHING("everything"),
    SITE_MENU("site_menu");

    override fun toString() = label

    companion object {
        @JvmStatic
        fun fromString(label: String) = when {
            DASHBOARD.label == label -> DASHBOARD
            EVERYTHING.label == label -> EVERYTHING
            SITE_MENU.label == label -> SITE_MENU
            else -> SITE_MENU
        }
    }
}



