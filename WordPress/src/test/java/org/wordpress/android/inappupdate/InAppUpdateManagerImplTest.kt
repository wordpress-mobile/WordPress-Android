package org.wordpress.android.inappupdate

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.wordpress.android.inappupdate.IInAppUpdateManager.Companion.APP_UPDATE_FLEXIBLE_REQUEST_CODE
import org.wordpress.android.inappupdate.IInAppUpdateManager.Companion.APP_UPDATE_IMMEDIATE_REQUEST_CODE
import org.wordpress.android.inappupdate.InAppUpdateManagerImpl.Companion.IMMEDIATE_UPDATE_INTERVAL_IN_MILLIS
import org.wordpress.android.inappupdate.InAppUpdateManagerImpl.Companion.KEY_LAST_APP_UPDATE_CHECK_TIME
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.RemoteConfigWrapper

@RunWith(MockitoJUnitRunner::class)
class InAppUpdateManagerImplTest {
    @Mock
    lateinit var applicationContext: Context

    @Mock
    lateinit var appUpdateManager: AppUpdateManager

    @Mock
    lateinit var remoteConfigWrapper: RemoteConfigWrapper

    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var inAppUpdateAnalyticsTracker: InAppUpdateAnalyticsTracker

    @Mock
    lateinit var updateListener: InAppUpdateListener

    @Mock
    lateinit var activity: Activity

    @Mock
    lateinit var appUpdateInfo: AppUpdateInfo

    @Mock
    lateinit var sharedPreferences: SharedPreferences

    @Mock
    lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    lateinit var currentTimeProvider: () -> Long

    lateinit var inAppUpdateManager: InAppUpdateManagerImpl

    @Before
    fun setUp() {
        currentTimeProvider = {1715866314746L} // Thu May 16 2024 13:31:54 UTC

        // Mock SharedPreferences behavior
        `when`(applicationContext.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        `when`(sharedPreferences.getInt(anyString(), anyInt())).thenReturn(-1)
        `when`(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
        `when`(sharedPreferencesEditor.putInt(anyString(), anyInt())).thenReturn(sharedPreferencesEditor)
        `when`(sharedPreferencesEditor.putLong(anyString(), anyLong())).thenReturn(sharedPreferencesEditor)

        inAppUpdateManager = InAppUpdateManagerImpl(
            applicationContext,
            TestScope(),
            appUpdateManager,
            remoteConfigWrapper,
            buildConfigWrapper,
            inAppUpdateAnalyticsTracker,
            currentTimeProvider
        )
    }

    @Test
    fun `checkForAppUpdate when update is not available does not trigger update`() {
        // Arrange
        val task = mockAppUpdateInfoTask(appUpdateInfo)
        `when`(appUpdateManager.appUpdateInfo).thenReturn(task)
        `when`(appUpdateInfo.updateAvailability()).thenReturn(UpdateAvailability.UPDATE_NOT_AVAILABLE)

        // Act
        inAppUpdateManager.checkForAppUpdate(activity, updateListener)

        // Assert
        verify(appUpdateManager.appUpdateInfo).addOnSuccessListener(any())
        verify(appUpdateManager, times(0)).startUpdateFlowForResult(
            any<AppUpdateInfo>(),
            any<Activity>(),
            any<AppUpdateOptions>(),
            anyInt()
        )
    }

    @Test
    fun `checkForAppUpdate when update is downloaded calls update listener`() {
        // Arrange
        val task = mockAppUpdateInfoTask(appUpdateInfo)
        `when`(appUpdateManager.appUpdateInfo).thenReturn(task)
        `when`(appUpdateInfo.updateAvailability()).thenReturn(UpdateAvailability.UPDATE_AVAILABLE)
        `when`(appUpdateInfo.installStatus()).thenReturn(InstallStatus.DOWNLOADED)

        // Act
        inAppUpdateManager.checkForAppUpdate(activity, updateListener)

        // Assert
        verify(updateListener).onAppUpdateDownloaded()
    }

    @Test
    fun `checkForAppUpdate requests immediate update when necessary`() {
        // Arrange
        val task = mockAppUpdateInfoTask(appUpdateInfo)
        `when`(appUpdateManager.appUpdateInfo).thenReturn(task)
        `when`(appUpdateInfo.updateAvailability()).thenReturn(UpdateAvailability.UPDATE_AVAILABLE)
        `when`(appUpdateInfo.installStatus()).thenReturn(InstallStatus.UNKNOWN)
        `when`(buildConfigWrapper.getAppVersionCode()).thenReturn(100) // current version
        `when`(remoteConfigWrapper.getInAppUpdateBlockingVersion()).thenReturn(200) // blocking version
        val lastCheckTimestamp = currentTimeProvider.invoke() - IMMEDIATE_UPDATE_INTERVAL_IN_MILLIS
        `when`(sharedPreferences.getLong( eq(KEY_LAST_APP_UPDATE_CHECK_TIME), anyLong())).thenReturn(lastCheckTimestamp)

        // Act
        inAppUpdateManager.checkForAppUpdate(activity, updateListener)

        // Assert
        verify(appUpdateManager).startUpdateFlowForResult(
            any<AppUpdateInfo>(),
            any<Activity>(),
            any<AppUpdateOptions>(),
            eq(APP_UPDATE_IMMEDIATE_REQUEST_CODE)
        )
    }

    @Test
    fun `checkForAppUpdate requests flexible update when necessary`() {
        // Arrange
        val task = mockAppUpdateInfoTask(appUpdateInfo)
        `when`(appUpdateManager.appUpdateInfo).thenReturn(task)
        `when`(appUpdateInfo.updateAvailability()).thenReturn(UpdateAvailability.UPDATE_AVAILABLE)
        `when`(appUpdateInfo.installStatus()).thenReturn(InstallStatus.UNKNOWN)
        `when`(buildConfigWrapper.getAppVersionCode()).thenReturn(100) // current version
        `when`(remoteConfigWrapper.getInAppUpdateBlockingVersion()).thenReturn(50) // blocking version
        `when`(remoteConfigWrapper.getInAppUpdateFlexibleIntervalInDays()).thenReturn(1)
        val lastCheckTimestamp = currentTimeProvider.invoke() - 1000*60*60*24
        `when`(sharedPreferences.getLong( eq(KEY_LAST_APP_UPDATE_CHECK_TIME), anyLong())).thenReturn(lastCheckTimestamp)

        // Act
        inAppUpdateManager.checkForAppUpdate(activity, updateListener)

        // Assert
        verify(appUpdateManager).startUpdateFlowForResult(
            any<AppUpdateInfo>(),
            any<Activity>(),
            any<AppUpdateOptions>(),
            eq(APP_UPDATE_FLEXIBLE_REQUEST_CODE)
        )
    }

    @Test
    fun `checkForAppUpdate handles developer triggered update in progress`() {
        // Arrange
        val task = mockAppUpdateInfoTask(appUpdateInfo)
        `when`(appUpdateManager.appUpdateInfo).thenReturn(task)
        `when`(appUpdateInfo.updateAvailability()).thenReturn(UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
        `when`(appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)).thenReturn(true)
        `when`(buildConfigWrapper.getAppVersionCode()).thenReturn(100)
        `when`(remoteConfigWrapper.getInAppUpdateBlockingVersion()).thenReturn(200)

        // Act
        inAppUpdateManager.checkForAppUpdate(activity, updateListener)

        // Assert
        verify(appUpdateManager).startUpdateFlowForResult(
            eq(appUpdateInfo),
            eq(activity),
            any<AppUpdateOptions>(),
            eq(APP_UPDATE_IMMEDIATE_REQUEST_CODE)
        )
    }

    @Test
    fun `checkForAppUpdate handles failure correctly`() {
        // Arrange
        val task = mockAppUpdateInfoTaskWithFailure()
        `when`(appUpdateManager.appUpdateInfo).thenReturn(task)

        // Act
        inAppUpdateManager.checkForAppUpdate(activity, updateListener)

        // Assert
        verify(appUpdateManager.appUpdateInfo).addOnFailureListener(any())
    }

    // Helper method to mock Task<AppUpdateInfo> with success
    @Suppress("UNCHECKED_CAST")
    private fun mockAppUpdateInfoTask(appUpdateInfo: AppUpdateInfo): Task<AppUpdateInfo> {
        val task = mock(Task::class.java) as Task<AppUpdateInfo>
        `when`(task.addOnSuccessListener(any())).thenAnswer { invocation ->
            (invocation.arguments[0] as OnSuccessListener<AppUpdateInfo>).onSuccess(appUpdateInfo)
            task
        }
        `when`(task.addOnFailureListener(any())).thenReturn(task)
        return task
    }

    // Helper method to mock Task<AppUpdateInfo> with failure
    @Suppress("UNCHECKED_CAST")
    private fun mockAppUpdateInfoTaskWithFailure(): Task<AppUpdateInfo> {
        val task = mock(Task::class.java) as Task<AppUpdateInfo>
        `when`(task.addOnFailureListener(any())).thenAnswer { invocation ->
            (invocation.arguments[0] as OnFailureListener).onFailure(Exception("Update check failed"))
            task
        }
        `when`(task.addOnSuccessListener(any())).thenReturn(task)
        return task
    }
}
