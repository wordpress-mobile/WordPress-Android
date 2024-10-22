package org.wordpress.android.fluxc.store

import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.store.NotificationStore.Companion.WPCOM_PUSH_DEVICE_SERVER_ID
import org.wordpress.android.fluxc.utils.PreferenceUtils

class InvalidateDeviceRegistrationTest {
    private val preferencesEditor: SharedPreferences.Editor = mock {
        on { remove(any()) } doReturn mock
    }
    private val preferences: SharedPreferences = mock {
        on { edit() } doReturn preferencesEditor
    }
    private val preferencesWrapper: PreferenceUtils.PreferenceUtilsWrapper = mock {
        on { getFluxCPreferences() } doReturn preferences
    }

    lateinit var sut: InvalidateDeviceRegistration

    @Before
    fun setUp() {
        sut = InvalidateDeviceRegistration(preferencesWrapper)
    }

    @Test
    fun `remove device id when executed`() {
        // when
        sut.invoke()

        // then
        verify(preferencesEditor).remove(WPCOM_PUSH_DEVICE_SERVER_ID)
    }
}
