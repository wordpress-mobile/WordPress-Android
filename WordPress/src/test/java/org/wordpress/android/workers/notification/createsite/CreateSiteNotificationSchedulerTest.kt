package org.wordpress.android.workers.notification.createsite

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.analytics.AnalyticsTracker.Stat.CREATE_SITE_NOTIFICATION_SCHEDULED
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.workers.notification.local.LocalNotification
import org.wordpress.android.workers.notification.local.LocalNotification.Type.CREATE_SITE
import org.wordpress.android.workers.notification.local.LocalNotificationScheduler

@RunWith(MockitoJUnitRunner::class)
class CreateSiteNotificationSchedulerTest {
    private lateinit var createSiteNotificationScheduler: CreateSiteNotificationScheduler

    private val localNotificationScheduler: LocalNotificationScheduler = mock()
    private val createSiteNotificationHandler: CreateSiteNotificationHandler = mock()
    private val appsPrefs: AppPrefsWrapper = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    @Before
    fun setUp() {
        createSiteNotificationScheduler = CreateSiteNotificationScheduler(
            localNotificationScheduler,
            createSiteNotificationHandler,
            appsPrefs,
            analyticsTracker
        )
    }

    @Test
    fun `notification isn't scheduled when shouldShowNotification is false`() {
        whenever(createSiteNotificationHandler.shouldShowNotification()).thenReturn(false)

        createSiteNotificationScheduler.scheduleCreateSiteNotificationIfNeeded()

        verifyNoInteractions(localNotificationScheduler)
        verifyNoInteractions(appsPrefs)
    }

    @Test
    fun `notification isn't scheduled when shouldScheduleCreateSiteNotification is false`() {
        whenever(createSiteNotificationHandler.shouldShowNotification()).thenReturn(true)
        whenever(appsPrefs.shouldScheduleCreateSiteNotification).thenReturn(false)

        createSiteNotificationScheduler.scheduleCreateSiteNotificationIfNeeded()

        verifyNoInteractions(localNotificationScheduler)
        verify(appsPrefs, never()).shouldScheduleCreateSiteNotification = any()
    }

    @Test
    fun `notifications are scheduled when shouldShowNotification and shouldScheduleCreateSiteNotification are true`() {
        whenever(createSiteNotificationHandler.shouldShowNotification()).thenReturn(true)
        whenever(appsPrefs.shouldScheduleCreateSiteNotification).thenReturn(true)

        createSiteNotificationScheduler.scheduleCreateSiteNotificationIfNeeded()

        argumentCaptor<LocalNotification>().apply {
            verify(localNotificationScheduler).scheduleOneTimeNotification(capture(), capture())

            assertThat(firstValue.delay).isEqualTo(1)
            assertThat(secondValue.delay).isEqualTo(8)
            assertThat(allValues).allMatch { it.type == CREATE_SITE }
        }
        verify(appsPrefs).shouldScheduleCreateSiteNotification = false
    }

    @Test
    fun `cancel calls LocalNotificationScheduler with correct type`() {
        createSiteNotificationScheduler.cancelCreateSiteNotification()
        verify(localNotificationScheduler).cancelScheduledNotification(argThat { this == CREATE_SITE })
    }

    @Test
    fun `notification scheduled event is tracked after notifications are scheduled`() {
        whenever(createSiteNotificationHandler.shouldShowNotification()).thenReturn(true)
        whenever(appsPrefs.shouldScheduleCreateSiteNotification).thenReturn(true)

        createSiteNotificationScheduler.scheduleCreateSiteNotificationIfNeeded()

        verify(analyticsTracker).track(CREATE_SITE_NOTIFICATION_SCHEDULED)
    }
}
