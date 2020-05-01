package org.wordpress.android.ui.prefs

import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.posts.PostListViewLayoutType
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color.DARK
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color.LIGHT
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType.COMMENTS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType.LIKES
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType.VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType.VISITORS
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

    var featureAnnouncementShownVersion: Int
        get() = AppPrefs.getFeatureAnnouncementShownVersion()
        set(version) = AppPrefs.setFeatureAnnouncementShownVersion(version)

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

    var systemNotificationsEnabled: Boolean
        get() = AppPrefs.getSystemNotificationsEnabled()
        set(value) = AppPrefs.setSystemNotificationsEnabled(value)

    var shouldShowPostSignupInterstitial: Boolean
        get() = AppPrefs.shouldShowPostSignupInterstitial()
        set(shouldShow) = AppPrefs.setShouldShowPostSignupInterstitial(shouldShow)

    fun getAppWidgetSiteId(appWidgetId: Int) = AppPrefs.getStatsWidgetSelectedSiteId(appWidgetId)
    fun setAppWidgetSiteId(siteId: Long, appWidgetId: Int) = AppPrefs.setStatsWidgetSelectedSiteId(siteId, appWidgetId)
    fun removeAppWidgetSiteId(appWidgetId: Int) = AppPrefs.removeStatsWidgetSelectedSiteId(appWidgetId)
    fun isGutenbergEditorEnabled() = AppPrefs.isGutenbergEditorEnabled()

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

    fun getAppWidgetDataType(appWidgetId: Int): DataType? {
        return when (AppPrefs.getStatsWidgetDataTypeId(appWidgetId)) {
            VIEWS_TYPE_ID -> VIEWS
            VISITORS_TYPE_ID -> VISITORS
            COMMENTS_TYPE_ID -> COMMENTS
            LIKES_TYPE_ID -> LIKES
            else -> null
        }
    }

    fun setAppWidgetDataType(dataType: DataType, appWidgetId: Int) {
        val dataTypeId = when (dataType) {
            VIEWS -> VIEWS_TYPE_ID
            VISITORS -> VISITORS_TYPE_ID
            COMMENTS -> COMMENTS_TYPE_ID
            LIKES -> LIKES_TYPE_ID
        }
        AppPrefs.setStatsWidgetDataTypeId(dataTypeId, appWidgetId)
    }

    fun removeAppWidgetDataTypeModeId(appWidgetId: Int) = AppPrefs.removeStatsWidgetDataTypeId(appWidgetId)

    fun hasAppWidgetData(appWidgetId: Int): Boolean {
        return AppPrefs.getStatsWidgetHasData(appWidgetId)
    }

    fun setAppWidgetHasData(hasData: Boolean, appWidgetId: Int) {
        AppPrefs.setStatsWidgetHasData(hasData, appWidgetId)
    }

    fun removeAppWidgetHasData(appWidgetId: Int) = AppPrefs.removeStatsWidgetHasData(appWidgetId)

    fun isMainFabTooltipDisabled() = AppPrefs.isMainFabTooltipDisabled()
    fun setMainFabTooltipDisabled(disable: Boolean) = AppPrefs.setMainFabTooltipDisabled(disable)

    fun getReaderSubfilter() = AppPrefs.getReaderSubfilter()
    fun setReaderSubfilter(json: String) = AppPrefs.setReaderSubfilter(json)

    fun getLastReaderKnownAccessTokenStatus() = AppPrefs.getLastReaderKnownAccessTokenStatus()
    fun setLastReaderKnownAccessTokenStatus(lastKnownAccessTokenStatus: Boolean) =
            AppPrefs.setLastReaderKnownAccessTokenStatus(lastKnownAccessTokenStatus)

    fun getLastReaderKnownUserId() = AppPrefs.getLastReaderKnownUserId()
    fun setLastReaderKnownUserId(userId: Long) = AppPrefs.setLastReaderKnownUserId(userId)

    companion object {
        private const val LIGHT_MODE_ID = 0
        private const val DARK_MODE_ID = 1

        private const val VIEWS_TYPE_ID = 0
        private const val VISITORS_TYPE_ID = 1
        private const val COMMENTS_TYPE_ID = 2
        private const val LIKES_TYPE_ID = 3
    }
}
