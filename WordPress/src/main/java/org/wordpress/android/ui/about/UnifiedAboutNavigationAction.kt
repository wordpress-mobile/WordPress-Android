package org.wordpress.android.ui.about

sealed class UnifiedAboutNavigationAction {
    object Dismiss : UnifiedAboutNavigationAction()
    data class OpenBlog(val url: String) : UnifiedAboutNavigationAction()
}
