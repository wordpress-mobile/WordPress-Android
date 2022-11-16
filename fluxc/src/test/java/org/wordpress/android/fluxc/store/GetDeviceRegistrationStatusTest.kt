package org.wordpress.android.fluxc.store

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.NotificationStore.Companion.WPCOM_PUSH_DEVICE_SERVER_ID
import org.wordpress.android.fluxc.utils.PreferenceUtils

@RunWith(MockitoJUnitRunner::class)
class GetDeviceRegistrationStatusTest {
    private val preferences: SharedPreferences = mock()
    private val preferencesWrapper: PreferenceUtils.PreferenceUtilsWrapper = mock {
        on { getFluxCPreferences() } doReturn preferences
    }

    private val sut = GetDeviceRegistrationStatus(preferencesWrapper)

    @Test
    fun `when device id is not empty, return registered status`() {
        // given
        whenever(preferences.getString(WPCOM_PUSH_DEVICE_SERVER_ID, null)).doReturn("not-empty-id")

        // when
        val result = sut.invoke()

        // then
        assertEquals(GetDeviceRegistrationStatus.Status.REGISTERED, result)
    }

    @Test
    fun `when device id is empty, return unregistered status`() {
        // given
        whenever(preferences.getString(WPCOM_PUSH_DEVICE_SERVER_ID, null)).doReturn("")

        // when
        val result = sut.invoke()

        // then
        assertEquals(GetDeviceRegistrationStatus.Status.UNREGISTERED, result)
    }

    @Test
    fun `when device id is null, return unregistered status`() {
        // given
        whenever(preferences.getString(WPCOM_PUSH_DEVICE_SERVER_ID, null)).doReturn(null)

        // when
        val result = sut.invoke()

        // then
        assertEquals(GetDeviceRegistrationStatus.Status.UNREGISTERED, result)
    }
}
