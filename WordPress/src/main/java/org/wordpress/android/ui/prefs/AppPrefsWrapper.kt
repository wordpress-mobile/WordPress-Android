package org.wordpress.android.ui.prefs

import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.posts.PostListViewLayoutType
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidgetConfigureViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidgetConfigureViewModel.Color.DARK
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidgetConfigureViewModel.Color.LIGHT
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable wrapper around AppPrefs.
 *
 * AppPrefs interface is consisted of static methods, which make the client code difficult to test/mock. Main purpose of
 * this wrapper is to make testing easier.
 *
 */
@Singleton
class AppPrefsWrapper @Inject constructor() {
    var newsCardDismissedVersion: Int
        get() = AppPrefs.getNewsCardDismissedVersion()
        set(version) = AppPrefs.setNewsCardDismissedVersion(version)

    var newsCardShownVersion: Int
        get() = AppPrefs.getNewsCardShownVersion()
        set(version) = AppPrefs.setNewsCardShownVersion(version)

    var avatarVersion: Int
        get() = AppPrefs.getAvatarVersion()
        set(version) = AppPrefs.setAvatarVersion(version)

    var isAztecEditorEnabled: Boolean
        get() = AppPrefs.isAztecEditorEnabled()
        set(enabled) = AppPrefs.setAztecEditorEnabled(enabled)

    var postListAuthorSelection: AuthorFilterSelection
        get() = AppPrefs.getAuthorFilterSelection()
        set(value) = AppPrefs.setAuthorFilterSelection(value)

    var postListViewLayoutType: PostListViewLayoutType
        get() = AppPrefs.getPostsListViewLayoutType()
        set(value) = AppPrefs.setPostsListViewLayoutType(value)

    fun getAppWidgetSiteId(appWidgetId: Int) = AppPrefs.getStatsWidgetSelectedSiteId(appWidgetId)
    fun setAppWidgetSiteId(siteId: Long, appWidgetId: Int) = AppPrefs.setStatsWidgetSelectedSiteId(siteId, appWidgetId)
    fun removeAppWidgetSiteId(appWidgetId: Int) = AppPrefs.removeStatsWidgetSelectedSiteId(appWidgetId)

    fun getAppWidgetColor(appWidgetId: Int): Color? {
        return when (AppPrefs.getStatsWidgetColorModeId(appWidgetId)) {
            LIGHT_MODE_ID -> LIGHT
            DARK_MODE_ID -> DARK
            else -> null
        }
    }

    fun setAppWidgetColor(colorMode: Color, appWidgetId: Int) {
        val colorModeId = when (colorMode) {
            LIGHT -> LIGHT_MODE_ID
            DARK -> DARK_MODE_ID
        }
        AppPrefs.setStatsWidgetColorModeId(colorModeId, appWidgetId)
    }

    fun removeAppWidgetColorModeId(appWidgetId: Int) = AppPrefs.removeStatsWidgetColorModeId(appWidgetId)

    companion object {
        private const val LIGHT_MODE_ID = 0
        private const val DARK_MODE_ID = 1
    }
}
