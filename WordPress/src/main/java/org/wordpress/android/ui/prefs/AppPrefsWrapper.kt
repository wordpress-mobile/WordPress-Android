package org.wordpress.android.ui.prefs

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.JetpackCapability
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.prefs.AppPrefs.PrefKey
import org.wordpress.android.ui.prefs.AppPrefs.getBoolean
import org.wordpress.android.ui.quickstart.QuickStartType
import org.wordpress.android.ui.reader.tracker.ReaderTab
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color.DARK
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color.LIGHT
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType.COMMENTS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType.LIKES
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType.VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType.VISITORS
import org.wordpress.android.usecase.social.JetpackSocialFlow
import org.wordpress.android.util.BuildConfigWrapper
import java.util.Date
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
class AppPrefsWrapper @Inject constructor(val buildConfigWrapper: BuildConfigWrapper) {
    var featureAnnouncementShownVersion: Int
        get() = AppPrefs.getFeatureAnnouncementShownVersion()
        set(version) = AppPrefs.setFeatureAnnouncementShownVersion(version)

    var lastFeatureAnnouncementAppVersionCode: Int
        get() = AppPrefs.getLastFeatureAnnouncementAppVersionCode()
        set(version) = AppPrefs.setLastFeatureAnnouncementAppVersionCode(version)

    var avatarVersion: Int
        get() = AppPrefs.getAvatarVersion()
        set(version) = AppPrefs.setAvatarVersion(version)

    var isAztecEditorEnabled: Boolean
        get() = AppPrefs.isAztecEditorEnabled()
        set(enabled) = AppPrefs.setAztecEditorEnabled(enabled)

    var postListAuthorSelection: AuthorFilterSelection
        get() = AppPrefs.getAuthorFilterSelection()
        set(value) = AppPrefs.setAuthorFilterSelection(value)

    var systemNotificationsEnabled: Boolean
        get() = AppPrefs.getSystemNotificationsEnabled()
        set(value) = AppPrefs.setSystemNotificationsEnabled(value)

    var shouldShowPostSignupInterstitial: Boolean
        get() = AppPrefs.shouldShowPostSignupInterstitial()
        set(shouldShow) = AppPrefs.setShouldShowPostSignupInterstitial(shouldShow)

    var readerTagsUpdatedTimestamp: Long
        get() = AppPrefs.getReaderTagsUpdatedTimestamp()
        set(timestamp) = AppPrefs.setReaderTagsUpdatedTimestamp(timestamp)

    var readerAnalyticsCountTagsTimestamp: Long
        get() = AppPrefs.getReaderAnalyticsCountTagsTimestamp()
        set(timestamp) = AppPrefs.setReaderAnalyticsCountTagsTimestamp(timestamp)

    var readerCssUpdatedTimestamp: Long
        get() = AppPrefs.getReaderCssUpdatedTimestamp()
        set(timestamp) = AppPrefs.setReaderCssUpdatedTimestamp(timestamp)

    var readerCardsPageHandle: String?
        get() = AppPrefs.getReaderCardsPageHandle()
        set(pageHandle) = AppPrefs.setReaderCardsPageHandle(pageHandle)

    var readerTopBarSelectedFeedItemId: String?
        get() = AppPrefs.getReaderTopBarSelectedFeedItemId()
        set(selectedFeedItemId) = AppPrefs.setReaderTopBarSelectedFeedItemId(selectedFeedItemId)

    var shouldScheduleCreateSiteNotification: Boolean
        get() = AppPrefs.shouldScheduleCreateSiteNotification()
        set(shouldSchedule) = AppPrefs.setShouldScheduleCreateSiteNotification(shouldSchedule)

    val wpJetpackIndividualPluginOverlayShownCount: Int
        get() = AppPrefs.getWPJetpackIndividualPluginOverlayShownCount()

    var wpJetpackIndividualPluginOverlayLastShownTimestamp: Long
        get() = AppPrefs.getWPJetpackIndividualPluginOverlayLastShownTimestamp()
        set(timestamp) = AppPrefs.setWPJetpackIndividualPluginOverlayLastShownTimestamp(timestamp)

    var notificationPermissionsWarningDismissed: Boolean
        get() = AppPrefs.getNotificationsPermissionsWarningDismissed()
        set(dismissed) = AppPrefs.setNotificationsPermissionWarningDismissed(dismissed)

    var readerReadingPreferencesJson: String?
        get() = AppPrefs.getReaderReadingPreferencesJson()
        set(json) = AppPrefs.setReaderReadingPreferencesJson(json)

    fun getAppWidgetSiteId(appWidgetId: Int) = AppPrefs.getStatsWidgetSelectedSiteId(appWidgetId)
    fun setAppWidgetSiteId(siteId: Long, appWidgetId: Int) = AppPrefs.setStatsWidgetSelectedSiteId(siteId, appWidgetId)
    fun removeAppWidgetSiteId(appWidgetId: Int) = AppPrefs.removeStatsWidgetSelectedSiteId(appWidgetId)
    fun isGutenbergEditorEnabled() = AppPrefs.isGutenbergEditorEnabled()
    fun getReaderCardsRefreshCounter() = AppPrefs.getReaderCardsRefreshCounter()
    fun incrementReaderCardsRefreshCounter() = AppPrefs.incrementReaderCardsRefreshCounter()

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

    fun getLastReaderKnownAccessTokenStatus() = AppPrefs.getLastReaderKnownAccessTokenStatus()
    fun setLastReaderKnownAccessTokenStatus(lastKnownAccessTokenStatus: Boolean) =
        AppPrefs.setLastReaderKnownAccessTokenStatus(lastKnownAccessTokenStatus)

    fun getLastReaderKnownUserId() = AppPrefs.getLastReaderKnownUserId()
    fun setLastReaderKnownUserId(userId: Long) = AppPrefs.setLastReaderKnownUserId(userId)

    fun getLastAppVersionCode() = AppPrefs.getLastAppVersionCode()

    fun setReaderTag(selectedTag: ReaderTag?) = AppPrefs.setReaderTag(selectedTag)
    fun getReaderTag(): ReaderTag? = AppPrefs.getReaderTag()

    fun setReaderActiveTab(selectedTab: ReaderTab?) = AppPrefs.setReaderActiveTab(selectedTab)
    fun getReaderActiveTab(): ReaderTab? = AppPrefs.getReaderActiveTab()

    fun shouldShowBookmarksSavedLocallyDialog(): Boolean = AppPrefs.shouldShowBookmarksSavedLocallyDialog()
    fun setBookmarksSavedLocallyDialogShown() = AppPrefs.setBookmarksSavedLocallyDialogShown()

    fun isPostListFabTooltipDisabled() = AppPrefs.isPostListFabTooltipDisabled()
    fun setPostListFabTooltipDisabled(disable: Boolean) = AppPrefs.setPostListFabTooltipDisabled(disable)

    fun hasManualFeatureConfig(featureKey: String): Boolean {
        return AppPrefs.hasManualFeatureConfig(featureKey)
    }

    fun setManualFeatureConfig(isEnabled: Boolean, featureKey: String) {
        AppPrefs.setManualFeatureConfig(isEnabled, featureKey)
    }

    fun getManualFeatureConfig(featureKey: String): Boolean {
        return AppPrefs.getManualFeatureConfig(featureKey)
    }

    fun incrementPublishedPostCount() {
        AppPrefs.incrementPublishedPostCount()
    }
    fun resetPublishedPostCount() {
        AppPrefs.resetPublishedPostCount()
    }

    fun getPublishedPostCount(): Int {
        return AppPrefs.getPublishedPostCount()
    }

    fun setBloggingRemindersShown(siteId: Int) {
        AppPrefs.setBloggingRemindersShown(siteId)
    }

    fun isBloggingRemindersShown(siteId: Int): Boolean {
        return AppPrefs.isBloggingRemindersShown(siteId)
    }

    fun setShouldShowWeeklyRoundupNotification(siteId: Long, shouldShow: Boolean) {
        AppPrefs.setShouldShowWeeklyRoundupNotification(siteId, shouldShow)
    }

    fun shouldShowWeeklyRoundupNotification(siteId: Long): Boolean {
        return AppPrefs.shouldShowWeeklyRoundupNotification(siteId)
    }

    fun setSiteJetpackCapabilities(remoteSiteId: Long, capabilities: List<JetpackCapability>) =
        AppPrefs.setSiteJetpackCapabilities(remoteSiteId, capabilities)

    fun getSiteJetpackCapabilities(remoteSiteId: Long): List<JetpackCapability> =
        AppPrefs.getSiteJetpackCapabilities(remoteSiteId)

    fun setMainPageIndex(index: Int) = AppPrefs.setMainPageIndex(index)

    fun getSelectedSite() = AppPrefs.getSelectedSite()

    fun setSelectedSite(siteLocalId: Int) = AppPrefs.setSelectedSite(siteLocalId)

    fun isQuickStartNoticeRequired() = AppPrefs.isQuickStartNoticeRequired()

    fun setQuickStartNoticeRequired(shown: Boolean) = AppPrefs.setQuickStartNoticeRequired(shown)

    fun setLastSkippedQuickStartTask(task: QuickStartTask) = AppPrefs.setLastSkippedQuickStartTask(task)

    fun getLastSelectedQuickStartTypeForSite(siteLocalId: Long): QuickStartType =
        AppPrefs.getLastSelectedQuickStartTypeForSite(siteLocalId)

    fun setLastSelectedQuickStartTypeForSite(quickStartType: QuickStartType, siteLocalId: Long) =
        AppPrefs.setLastSelectedQuickStartTypeForSite(quickStartType, siteLocalId)

    fun getMySiteInitialScreen(isJetpackApp: Boolean): String = AppPrefs.getMySiteInitialScreen(isJetpackApp)

    fun setSkippedPromptDay(date: Date?, siteId: Int) = AppPrefs.setSkippedPromptDay(date, siteId)

    fun getSkippedPromptDay(siteId: Int): Date? = AppPrefs.getSkippedPromptDay(siteId)

    fun getIsFirstBloggingPromptsOnboarding(): Boolean = AppPrefs.getIsFirstBloggingPromptsOnboarding()

    fun saveFirstBloggingPromptsOnboarding(isFirstTime: Boolean) {
        AppPrefs.saveFirstBloggingPromptsOnboarding(isFirstTime)
    }

    fun getIsFirstTrySharedLoginJetpack(): Boolean = AppPrefs.getIsFirstTrySharedLoginJetpack()

    fun saveIsFirstTrySharedLoginJetpack(isFirstTry: Boolean) = AppPrefs.saveIsFirstTrySharedLoginJetpack(isFirstTry)

    fun getIsFirstTryUserFlagsJetpack(): Boolean = AppPrefs.getIsFirstTryUserFlagsJetpack()

    fun saveIsFirstTryUserFlagsJetpack(isFirstTry: Boolean) = AppPrefs.saveIsFirstTryUserFlagsJetpack(isFirstTry)

    fun getIsFirstTryBloggingRemindersSyncJetpack(): Boolean = AppPrefs.getIsFirstTryBloggingRemindersSyncJetpack()

    fun saveIsFirstTryBloggingRemindersSyncJetpack(isFirstTry: Boolean) =
        AppPrefs.saveIsFirstTryBloggingRemindersSyncJetpack(isFirstTry)

    fun getIsFirstTryReaderSavedPostsJetpack(): Boolean = AppPrefs.getIsFirstTryReaderSavedPostsJetpack()

    fun saveIsFirstTryReaderSavedPostsJetpack(isFirstTry: Boolean) =
        AppPrefs.saveIsFirstTryReaderSavedPostsJetpack(isFirstTry)

    fun setJetpackMigrationCompleted(isCompleted: Boolean) = AppPrefs.setIsJetpackMigrationCompleted(isCompleted)

    fun isJetpackMigrationCompleted() = AppPrefs.getIsJetpackMigrationCompleted()

    fun setJetpackMigrationInProgress(isInProgress: Boolean) = AppPrefs.setIsJetpackMigrationInProgress(isInProgress)

    fun isJetpackMigrationInProgress() = AppPrefs.getIsJetpackMigrationInProgress()

    fun setJetpackMigrationEligible(isEligible: Boolean) = AppPrefs.setIsJetpackMigrationEligible(isEligible)

    fun isJetpackMigrationEligible() = AppPrefs.getIsJetpackMigrationEligible()

    fun getOpenWebLinksWithJetpackOverlayLastShownTimestamp(): Long =
        AppPrefs.getOpenWebLinksWithJetpackOverlayLastShownTimestamp()

    fun setOpenWebLinksWithJetpackOverlayLastShownTimestamp(lastShown: Long) =
        AppPrefs.setOpenWebLinksWithJetpackOverlayLastShownTimestamp(lastShown)

    fun getIsOpenWebLinksWithJetpack(): Boolean = AppPrefs.getIsOpenWebLinksWithJetpack()

    fun setIsOpenWebLinksWithJetpack(isOpenWebLinksWithJetpack: Boolean) =
        AppPrefs.setIsOpenWebLinksWithJetpack(isOpenWebLinksWithJetpack)

    fun getShouldHideJetpackFeatureCard(jetpackFeatureRemovalPhase: JetpackFeatureRemovalPhase): Boolean =
        AppPrefs.getShouldHideJetpackFeatureCard(jetpackFeatureRemovalPhase)

    fun setShouldHideJetpackFeatureCard(jetpackFeatureRemovalPhase: JetpackFeatureRemovalPhase, isHidden: Boolean) =
        AppPrefs.setShouldHideJetpackFeatureCard(jetpackFeatureRemovalPhase, isHidden)

    fun getJetpackFeatureCardLastShownTimestamp(jetpackFeatureRemovalPhase: JetpackFeatureRemovalPhase): Long =
        AppPrefs.getJetpackFeatureCardLastShownTimestamp(jetpackFeatureRemovalPhase)

    fun setJetpackFeatureCardLastShownTimestamp(
        jetpackFeatureRemovalPhase: JetpackFeatureRemovalPhase,
        lastShownTimestamp: Long
    ) {
        AppPrefs.setJetpackFeatureCardLastShownTimestamp(jetpackFeatureRemovalPhase, lastShownTimestamp)
    }

    fun getSwitchToJetpackMenuCardLastShownTimestamp(): Long = AppPrefs.getSwitchToJetpackMenuCardLastShownTimestamp()

    fun setSwitchToJetpackMenuCardLastShownTimestamp(lastShownTimestamp: Long) {
        AppPrefs.setSwitchToJetpackMenuCardLastShownTimestamp(lastShownTimestamp)
    }

    fun getShouldHideSwitchToJetpackMenuCard(jetpackFeatureRemovalPhase: JetpackFeatureRemovalPhase): Boolean =
        AppPrefs.getShouldHideSwitchToJetpackMenuCard(jetpackFeatureRemovalPhase)

    fun setShouldHideSwitchToJetpackMenuCard(
        jetpackFeatureRemovalPhase: JetpackFeatureRemovalPhase,
        isHidden: Boolean
    ) = AppPrefs.setShouldHideSwitchToJetpackMenuCard(jetpackFeatureRemovalPhase, isHidden)

    fun getShouldHideJetpackInstallFullPluginCard(siteId: Int): Boolean =
        AppPrefs.getShouldHideJetpackInstallFullPluginCard(siteId)

    fun setShouldHideJetpackInstallFullPluginCard(
        siteId: Int,
        isHidden: Boolean
    ) = AppPrefs.setShouldHideJetpackInstallFullPluginCard(siteId, isHidden)

    fun getShouldShowJetpackInstallOnboarding(siteId: Int): Boolean =
        AppPrefs.getShouldShowJetpackFullPluginInstallOnboarding(siteId)

    fun setShouldShowJetpackInstallOnboarding(
        siteId: Int,
        isShown: Boolean
    ) = AppPrefs.setShouldShowJetpackFullPluginInstallOnboarding(siteId, isShown)

    fun hideBlazeCard(siteId: Long): Boolean =
        AppPrefs.getShouldHidePromoteWithBlazeCard(siteId)

    fun setShouldHideBlazeCard(siteId: Long, isHidden: Boolean) =
        AppPrefs.setShouldHidePromoteWithBlazeCard(siteId, isHidden)

    fun getShouldHideDashboardPlansCard(siteId: Long): Boolean =
        AppPrefs.getShouldHideDashboardPlansCard(siteId)

    fun setShouldHideDashboardPlansCard(siteId: Long, isHidden: Boolean) =
        AppPrefs.setShouldHideDashboardPlansCard(siteId, isHidden)

    fun incrementWPJetpackIndividualPluginOverlayShownCount() =
        AppPrefs.incrementWPJetpackIndividualPluginOverlayShownCount()

    fun getShouldHideBlazeOverlay(): Boolean =
        AppPrefs.getShouldHideBlazeOverlay()

    fun setShouldHideBlazeOverlay(isHidden: Boolean) =
        AppPrefs.setShouldHideBlazeOverlay(isHidden)

    fun getShouldShowJetpackSocialNoConnections(remoteSiteId: Long, flow: JetpackSocialFlow): Boolean =
        AppPrefs.getShouldShowJetpackSocialNoConnections(remoteSiteId, flow)

    fun setShouldShowJetpackSocialNoConnections(show: Boolean, remoteSiteId: Long, flow: JetpackSocialFlow) =
        AppPrefs.setShouldShowJetpackSocialNoConnections(show, remoteSiteId, flow)

    fun setShouldHideActivityDashboardCard(
        siteId: Long,
        isHidden: Boolean
    ) = AppPrefs.setShouldHideActivityDashboardCard(siteId, isHidden)

    fun getShouldHideActivityDashboardCard(siteId: Long): Boolean =
        AppPrefs.getShouldHideActivityDashboardCard(siteId)

    fun setShouldHidePagesDashboardCard(
        siteId: Long,
        isHidden: Boolean
    ) = AppPrefs.setShouldHidePagesDashboardCard(siteId, isHidden)

    fun getShouldHidePagesDashboardCard(siteId: Long): Boolean =
        AppPrefs.getShouldHidePagesDashboardCard(siteId)

    fun setShouldHideTodaysStatsDashboardCard(
        siteId: Long,
        isHidden: Boolean
    ) = AppPrefs.setShouldHideTodaysStatsDashboardCard(siteId, isHidden)

    fun getShouldHideTodaysStatsDashboardCard(siteId: Long): Boolean =
        AppPrefs.getShouldHideTodaysStatsDashboardCard(siteId)

    fun setShouldHidePostDashboardCard(
        siteId: Long,
        postCardType: String,
        isHidden: Boolean
    ) = AppPrefs.setShouldHidePostDashboardCard(siteId, postCardType, isHidden)

    fun getShouldHidePostDashboardCard(siteId: Long, postCardType: String,): Boolean =
        AppPrefs.getShouldHidePostDashboardCard(siteId, postCardType)

    fun setShouldHideNextStepsDashboardCard(siteId: Long, isHidden: Boolean) =
        AppPrefs.setShouldHideNextStepsDashboardCard(siteId, isHidden)

    fun getShouldHideNextStepsDashboardCard(siteId: Long): Boolean =
        AppPrefs.getShouldHideNextStepsDashboardCard(siteId)

    fun setShouldHideGetToKnowTheAppDashboardCard(siteId: Long, isHidden: Boolean) =
        AppPrefs.setShouldHideGetToKnowTheAppDashboardCard(siteId, isHidden)

    fun getShouldHideGetToKnowTheAppDashboardCard(siteId: Long): Boolean =
        AppPrefs.getShouldHideGetToKnowTheAppDashboardCard(siteId)

    fun setShouldShowSiteItemAsQuickLink(siteItem: String, siteId: Long, shouldShow: Boolean) =
        AppPrefs.setShouldShowSiteItemAsQuickLink(siteItem, siteId, shouldShow)

    fun getShouldShowSiteItemAsQuickLink(siteItem: String, siteId: Long): Boolean =
        AppPrefs.getShouldShowSiteItemAsQuickLink(siteItem, siteId)

    fun setShouldShowDefaultQuickLink(siteItem: String, siteId: Long, shouldShow: Boolean) =
        AppPrefs.setShouldShowDefaultQuickLink(siteItem, siteId, shouldShow)

    fun getShouldShowDefaultQuickLink(siteItem: String, siteId: Long): Boolean =
        AppPrefs.getShouldShowDefaultQuickLink(siteItem, siteId)

    fun setShouldHideBloganuaryNudgeCard(siteId: Long, isHidden: Boolean) =
        AppPrefs.setShouldHideBloganuaryNudgeCard(siteId, isHidden)

    fun getShouldHideBloganuaryNudgeCard(siteId: Long): Boolean =
        AppPrefs.getShouldHideBloganuaryNudgeCard(siteId)

    fun setShouldHideSotw2023NudgeCard(isHidden: Boolean): Unit =
        AppPrefs.setShouldHideSotw2023NudgeCard(isHidden)

    fun getShouldHideSotw2023NudgeCard(): Boolean =
        AppPrefs.getShouldHideSotw2023NudgeCard()

    fun setShouldHideDynamicCard(id: String, isHidden: Boolean): Unit =
        AppPrefs.setShouldHideDynamicCard(id, isHidden)

    fun getShouldHideDynamicCard(id: String, ): Boolean =
        AppPrefs.getShouldHideDynamicCard(id)

    fun shouldUpdateBookmarkPostsPseudoIds(tag: ReaderTag?): Boolean = AppPrefs.shouldUpdateBookmarkPostsPseudoIds(tag)

    fun setBookmarkPostsPseudoIdsUpdated() = AppPrefs.setBookmarkPostsPseudoIdsUpdated()

    fun shouldShowReaderAnnouncementCard(): Boolean = AppPrefs.getShouldShowReaderAnnouncementCard()

    fun setShouldShowReaderAnnouncementCard(shouldShow: Boolean) =
        AppPrefs.setShouldShowReaderAnnouncementCard(shouldShow)
    
    fun getAllPrefs(): Map<String, Any?> = AppPrefs.getAllPrefs()

    fun getDebugBooleanPref(key: String, default: Boolean = false) =
        buildConfigWrapper.isDebug() && AppPrefs.getRawBoolean({ key }, default)

    fun setString(prefKey: PrefKey, value: String) {
        AppPrefs.setString(prefKey, value)
    }

    fun setLong(prefKey: PrefKey, value: Long) {
        AppPrefs.putLong(prefKey, value)
    }

    fun setInt(prefKey: PrefKey, value: Int) {
        AppPrefs.putInt(prefKey, value)
    }

    fun putBoolean(prefKey: PrefKey, value: Boolean) {
        AppPrefs.putBoolean(prefKey, value)
    }

    fun setStringSet(prefKey: PrefKey, set: Set<String>?) {
        AppPrefs.putStringSet(prefKey, set)
    }

    var savedPrivacyBannerSettings: Boolean
        get() = getBoolean(AppPrefs.DeletablePrefKey.HAS_SAVED_PRIVACY_SETTINGS, false)
        set(value) = AppPrefs.setBoolean(AppPrefs.DeletablePrefKey.HAS_SAVED_PRIVACY_SETTINGS, value)

    var pinnedSiteLocalIds: MutableSet<Int>
        get() = Gson().fromJson(AppPrefs.getPinnedSiteLocalIds(), Array<Int>::class.java).toMutableSet()
        set(value) = AppPrefs.setPinnedSiteLocalIds(Gson().toJson(value))

    fun getRecentSiteLocalIds(): MutableSet<Int> = AppPrefs.getRecentlyPickedSiteIds().toMutableSet()
    fun addRecentSiteLocalId(siteLocalId: Int) {
        AppPrefs.addRecentlyPickedSiteId(siteLocalId)
    }

    companion object {
        private const val LIGHT_MODE_ID = 0
        private const val DARK_MODE_ID = 1

        private const val VIEWS_TYPE_ID = 0
        private const val VISITORS_TYPE_ID = 1
        private const val COMMENTS_TYPE_ID = 2
        private const val LIKES_TYPE_ID = 3
    }
}
