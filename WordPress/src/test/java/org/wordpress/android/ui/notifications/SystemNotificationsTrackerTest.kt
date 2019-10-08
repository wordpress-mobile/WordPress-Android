package org.wordpress.android.ui.notifications

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class SystemNotificationsTrackerTest {
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Mock lateinit var appPrefs: AppPrefsWrapper
    @Mock lateinit var notificationManager: NotificationManagerWrapper
    private lateinit var systemNotificationsTracker: SystemNotificationsTracker
    @Before
    fun setUp() {
        systemNotificationsTracker = SystemNotificationsTracker(analyticsTracker, appPrefs, notificationManager)
    }

    @Test
    fun `tracks NOTIFICATIONS_ENABLED when system notifications enabled`() {
        whenever(appPrefs.systemNotificationsEnabled).thenReturn(false)
        whenever(notificationManager.areNotificationsEnabled()).thenReturn(true)

        systemNotificationsTracker.checkSystemNotificationsState()

        verify(analyticsTracker).track(Stat.NOTIFICATIONS_ENABLED)
        verify(appPrefs).systemNotificationsEnabled = true
    }

    @Test
    fun `tracks NOTIFICATIONS_DISABLED when system notifications disabled`() {
        whenever(appPrefs.systemNotificationsEnabled).thenReturn(true)
        whenever(notificationManager.areNotificationsEnabled()).thenReturn(false)

        systemNotificationsTracker.checkSystemNotificationsState()

        verify(analyticsTracker).track(Stat.NOTIFICATIONS_DISABLED)
        verify(appPrefs).systemNotificationsEnabled = false
    }

    @Test
    fun `does not track when system notifications haven't changed`() {
        whenever(appPrefs.systemNotificationsEnabled).thenReturn(false)
        whenever(notificationManager.areNotificationsEnabled()).thenReturn(false)

        systemNotificationsTracker.checkSystemNotificationsState()

        verifyZeroInteractions(analyticsTracker)
    }
}
