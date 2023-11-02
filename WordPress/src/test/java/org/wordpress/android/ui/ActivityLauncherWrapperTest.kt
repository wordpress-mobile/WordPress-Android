package org.wordpress.android.ui

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class ActivityLauncherWrapperTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    private lateinit var classToTest: ActivityLauncherWrapper

    @Before
    fun setUp() {
        classToTest = ActivityLauncherWrapper()
    }

    @Test
    fun `openPlayStoreLink, when target app is not preinstalled, should allow navigation back to the initial app`() {
        val activity = mockAppPreinstalled(false)

        classToTest.openPlayStoreLink(activity, "packageName")

        verify(activity, never()).finishAffinity()
    }

    @Test
    fun `openPlayStoreLink, when target app is preinstalled, should not allow navigation back to the initial app`() {
        val activity = mockAppPreinstalled(true)

        classToTest.openPlayStoreLink(activity, "packageName")

        verify(activity).finishAffinity()
    }

    @Test
    fun `when a campaign is passed the store url includes a referrer`() {
        val expectedStoreUrl = "https://play.google.com/store/apps/details?id=com.jetpack.android" +
                "&referrer=utm_source%3Dorg.wordpress.android%26utm_campaign%3Dtest_campaign"
        val actualStoreUrl = classToTest.getPlayStoreUrl(SOURCE_PACKAGE, TARGET_PACKAGE, TEST_CAMPAIGN)
        assertThat(actualStoreUrl).isEqualTo(expectedStoreUrl)
    }

    @Test
    fun `when a campaign is not passed the store url does not include a referrer`() {
        val expectedStoreUrl = "https://play.google.com/store/apps/details?id=com.jetpack.android"
        val actualStoreUrl = classToTest.getPlayStoreUrl(SOURCE_PACKAGE, TARGET_PACKAGE)
        assertThat(actualStoreUrl).isEqualTo(expectedStoreUrl)
    }

    private fun mockAppPreinstalled(isInstalled: Boolean): Activity {
        val activity = mock<Activity>()
        val application = mock<Application>()
        val packageManager = mock<PackageManager>()
        whenever(activity.packageManager).thenReturn(packageManager)
        whenever(activity.application).thenReturn(application)
        whenever(application.packageName).thenReturn("org.wordpress.android")

        if (isInstalled) {
            whenever(packageManager.getLaunchIntentForPackage(any())).thenReturn(mock())
        }

        return activity
    }

    companion object {
        const val SOURCE_PACKAGE = "org.wordpress.android"
        const val TARGET_PACKAGE = "com.jetpack.android"
        const val TEST_CAMPAIGN = "test_campaign"
    }
}
