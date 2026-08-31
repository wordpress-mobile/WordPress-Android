package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.wordpress.android.fluxc.model.SiteModel

class WpAppNotifierHandlerTest {
    private lateinit var wpAppNotifierHandler: WpAppNotifierHandler
    private lateinit var mockListener1: WpAppNotifierHandler.NotifierListener
    private lateinit var mockListener2: WpAppNotifierHandler.NotifierListener
    private lateinit var testSite: SiteModel

    private val testSiteUrl = "https://example.com"

    @Before
    fun setUp() {
        wpAppNotifierHandler = WpAppNotifierHandler()
        mockListener1 = mock()
        mockListener2 = mock()
        testSite = SiteModel().apply {
            url = testSiteUrl
            siteId = 123L
        }
    }

    @Test
    fun `notifyRequestedWithInvalidAuthentication calls all active listeners`() {
        // Given
        wpAppNotifierHandler.addListener(mockListener1)
        wpAppNotifierHandler.addListener(mockListener2)

        // When
        wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(testSite)

        // Then
        verify(mockListener1, times(1)).onRequestedWithInvalidAuthentication(testSiteUrl)
        verify(mockListener2, times(1)).onRequestedWithInvalidAuthentication(testSiteUrl)
    }

    @Test
    fun `notifyRequestedWithInvalidAuthentication with no listeners does nothing`() {
        // When
        wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(testSite)

        // Then - no exceptions should be thrown and no interactions should occur
        verifyNoInteractions(mockListener1)
        verifyNoInteractions(mockListener2)
    }

    @Test
    fun `notifyRequestedWithInvalidAuthentication passes correct site URL to listeners`() {
        // Given
        val customSiteUrl = "https://custom-site.example.org"
        val customSite = SiteModel().apply {
            url = customSiteUrl
            siteId = 456L
        }
        wpAppNotifierHandler.addListener(mockListener1)

        // When
        wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(customSite)

        // Then
        verify(mockListener1, times(1)).onRequestedWithInvalidAuthentication(customSiteUrl)
    }

    @Test
    fun `removeListener prevents notification of removed listener`() {
        // Given
        wpAppNotifierHandler.addListener(mockListener1)
        wpAppNotifierHandler.addListener(mockListener2)

        // When - remove one listener
        wpAppNotifierHandler.removeListener(mockListener1)
        wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(testSite)

        // Then - only remaining listener should be called
        verify(mockListener1, never()).onRequestedWithInvalidAuthentication(testSiteUrl)
        verify(mockListener2, times(1)).onRequestedWithInvalidAuthentication(testSiteUrl)
    }

    @Test
    fun `removeListener with no existing listeners does nothing`() {
        // When
        wpAppNotifierHandler.removeListener(mockListener1)
        wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(testSite)

        // Then - no interactions should occur
        verifyNoInteractions(mockListener1)
    }

    @Test
    fun `removeListener with non-existent listener does not affect other listeners`() {
        // Given
        wpAppNotifierHandler.addListener(mockListener1)

        // When - remove different listener
        wpAppNotifierHandler.removeListener(mockListener2)
        wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(testSite)

        // Then - original listener should still be called
        verify(mockListener1, times(1)).onRequestedWithInvalidAuthentication(testSiteUrl)
    }

    @Test
    fun `adding same listener multiple times only calls it once per notification`() {
        // Given
        wpAppNotifierHandler.addListener(mockListener1)
        wpAppNotifierHandler.addListener(mockListener1) // Add same listener again

        // When
        wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(testSite)

        // Then - should only be called once (overwrites previous entry)
        verify(mockListener1, times(1)).onRequestedWithInvalidAuthentication(testSiteUrl)
    }
}
