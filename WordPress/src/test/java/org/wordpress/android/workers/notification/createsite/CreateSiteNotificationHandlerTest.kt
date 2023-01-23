package org.wordpress.android.workers.notification.createsite

import android.content.SharedPreferences
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NotificationType.CREATE_SITE
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.viewmodel.ResourceProvider

@RunWith(MockitoJUnitRunner::class)
class CreateSiteNotificationHandlerTest {
    private lateinit var createSiteNotificationHandler: CreateSiteNotificationHandler

    private val sharedPrefs: SharedPreferences = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val accountStore: AccountStore = mock()
    private val siteStore: SiteStore = mock()
    private val notificationsTracker: SystemNotificationsTracker = mock()
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper = mock()

    @Before
    fun setUp() {
        val notificationsMainKey = "wp_pref_notifications_master"
        whenever(resourceProvider.getString(R.string.wp_pref_notifications_main)).thenReturn(notificationsMainKey)
        createSiteNotificationHandler = CreateSiteNotificationHandler(
            sharedPrefs,
            resourceProvider,
            accountStore,
            siteStore,
            notificationsTracker,
            jetpackFeatureRemovalPhaseHelper
        )
    }

    @Test
    fun `should not show notification when the notification settings is disabled`() {
        whenever(
            sharedPrefs.getBoolean(
                resourceProvider.getString(R.string.wp_pref_notifications_main),
                true
            )
        ).thenReturn(false)

        assertThat(createSiteNotificationHandler.shouldShowNotification()).isFalse
    }

    @Test
    fun `should not show notification when the user is logged out`() {
        whenever(
            sharedPrefs.getBoolean(
                resourceProvider.getString(R.string.wp_pref_notifications_main),
                true
            )
        ).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        assertThat(createSiteNotificationHandler.shouldShowNotification()).isFalse
    }

    @Test
    fun `should not show notification when the user has sites`() {
        val notificationsMainKey = "wp_pref_notifications_master"
        whenever(resourceProvider.getString(R.string.wp_pref_notifications_main)).thenReturn(notificationsMainKey)
        whenever(
            sharedPrefs.getBoolean(
                resourceProvider.getString(R.string.wp_pref_notifications_main),
                true
            )
        ).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(siteStore.hasSite()).thenReturn(true)

        assertThat(createSiteNotificationHandler.shouldShowNotification()).isFalse
    }

    @Test
    fun `should show notification when the notification settings is enabled`() {
        whenever(
            sharedPrefs.getBoolean(
                resourceProvider.getString(R.string.wp_pref_notifications_main),
                true
            )
        ).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldShowNotifications()).thenReturn(true)

        assertThat(createSiteNotificationHandler.shouldShowNotification()).isTrue
    }

    @Test
    fun `should show notification when the user is logged in and has no sites`() {
        whenever(
            sharedPrefs.getBoolean(
                resourceProvider.getString(R.string.wp_pref_notifications_main),
                true
            )
        ).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(siteStore.hasSite()).thenReturn(false)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldShowNotifications()).thenReturn(true)

        assertThat(createSiteNotificationHandler.shouldShowNotification()).isTrue
    }

    @Test
    fun `should not show notification when in jetpack removal phase 4`() {
        whenever(
            sharedPrefs.getBoolean(
                resourceProvider.getString(R.string.wp_pref_notifications_main),
                true
            )
        ).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(siteStore.hasSite()).thenReturn(true)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldShowNotifications()).thenReturn(false)

        assertThat(createSiteNotificationHandler.shouldShowNotification()).isFalse()
    }


    @Test
    fun `should track notification shown`() {
        createSiteNotificationHandler.onNotificationShown()

        verify(notificationsTracker).trackShownNotification(CREATE_SITE)
    }
}
