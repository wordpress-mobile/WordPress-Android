package org.wordpress.android.ui.prefs

import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.posts.PostListViewLayoutType
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

    fun getAppWidgetColorModeId(appWidgetId: Int) = AppPrefs.getStatsWidgetColorModeId(appWidgetId)
    fun setAppWidgetColorModeId(colorModeId: Int, appWidgetId: Int) =
            AppPrefs.setStatsWidgetColorModeId(colorModeId, appWidgetId)

    fun removeAppWidgetColorModeId(appWidgetId: Int) = AppPrefs.removeStatsWidgetColorModeId(appWidgetId)
}
