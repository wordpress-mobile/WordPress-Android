package org.wordpress.android.workers

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.workers.LocalNotification.Type.CREATE_SITE

@RunWith(MockitoJUnitRunner::class)
class CreateSiteNotificationSchedulerTest {
    private lateinit var createSiteNotificationScheduler: CreateSiteNotificationScheduler

    private val localNotificationScheduler: LocalNotificationScheduler = mock()
    private val createSiteNotificationHandler: CreateSiteNotificationHandler = mock()
    private val appsPrefs: AppPrefsWrapper = mock()

    @Before
    fun setUp() {
        createSiteNotificationScheduler = CreateSiteNotificationScheduler(
                localNotificationScheduler,
                createSiteNotificationHandler,
                appsPrefs
        )
    }

    @Test
    fun `notification isn't scheduled when shouldShowNotification is false`() {
        whenever(createSiteNotificationHandler.shouldShowNotification()).thenReturn(false)

        createSiteNotificationScheduler.scheduleCreateSiteNotificationIfNeeded()

        verifyZeroInteractions(localNotificationScheduler)
        verifyZeroInteractions(appsPrefs)
    }

    @Test
    fun `notification isn't scheduled when shouldScheduleCreateSiteNotification is false`() {
        whenever(createSiteNotificationHandler.shouldShowNotification()).thenReturn(true)
        whenever(appsPrefs.shouldScheduleCreateSiteNotification).thenReturn(false)

        createSiteNotificationScheduler.scheduleCreateSiteNotificationIfNeeded()

        verifyZeroInteractions(localNotificationScheduler)
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
}
