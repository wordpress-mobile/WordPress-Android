package org.wordpress.android.ui.sitecreation.misc

enum class SiteCreationSource(val label: String) {
    DEEP_LINK("deep_link"),
    LOGIN_EPILOGUE("login_epilogue"),
    MY_SITE("my_site"),
    MY_SITE_NO_SITES("my_sites_no_sites"),
    NOTIFICATION("notification"),
    SIGNUP_EPILOGUE("signup_epilogue"),
    UNSPECIFIED("unspecified");

    override fun toString() = label

    companion object {
        @JvmStatic
        fun fromString(label: String?) = when {
            DEEP_LINK.label == label -> DEEP_LINK
            LOGIN_EPILOGUE.label == label -> LOGIN_EPILOGUE
            MY_SITE.label == label -> MY_SITE
            MY_SITE_NO_SITES.label == label -> MY_SITE_NO_SITES
            NOTIFICATION.label == label -> NOTIFICATION
            SIGNUP_EPILOGUE.label == label -> SIGNUP_EPILOGUE
            else -> UNSPECIFIED
        }
    }
}
