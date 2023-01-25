package org.wordpress.android.ui

import android.app.Activity
import android.content.pm.PackageManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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


    private fun mockAppPreinstalled(isInstalled: Boolean): Activity {
        val activity = mock<Activity>()
        val packageManager = mock<PackageManager>()
        whenever(activity.packageManager).thenReturn(packageManager)

        if (isInstalled) {
            whenever(packageManager.getLaunchIntentForPackage(any())).thenReturn(mock())
        }

        return activity
    }
}
