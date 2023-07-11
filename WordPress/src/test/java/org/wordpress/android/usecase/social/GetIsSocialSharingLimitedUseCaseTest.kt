package org.wordpress.android.usecase.social

import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore

class GetIsSocialSharingLimitedUseCaseTest {
    private val siteStore: SiteStore = mock()
    private val classToTest = GetIsSocialSharingLimitedUseCase(
        siteStore = siteStore,
    )

    @Test
    fun `Should return FALSE if site is not self hosted`() {
        val siteModel = SiteModel().apply {
            siteId = 1L
            setIsJetpackInstalled(false)
        }
        whenever(siteStore.getSiteBySiteId(any())).thenReturn(siteModel)
        val expected = false
        val actual = classToTest.execute(1L)
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return FALSE if site is null`() {
        val siteModel = null
        whenever(siteStore.getSiteBySiteId(any())).thenReturn(siteModel)
        val expected = false
        val actual = classToTest.execute(1L)
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return FALSE if site has social-shares-1000 feature active`() {
        val siteModel = SiteModel().apply {
            siteId = 1L
            setIsJetpackInstalled(true)
            planActiveFeatures = "social-shares-1000"
        }
        whenever(siteStore.getSiteBySiteId(any())).thenReturn(siteModel)
        val expected = false
        val actual = classToTest.execute(1L)
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return TRUE if active features list is null AND is self hosted`() {
        val siteModel = SiteModel().apply {
            siteId = 1L
            setIsJetpackInstalled(true)
            planActiveFeatures = null
        }
        whenever(siteStore.getSiteBySiteId(any())).thenReturn(siteModel)
        val expected = true
        val actual = classToTest.execute(1L)
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return TRUE if site is self hosted AND does not have social-shares-1000 feature active`() {
        val siteModel = SiteModel().apply {
            siteId = 1L
            setIsJetpackInstalled(true)
            planActiveFeatures = ""
        }
        whenever(siteStore.getSiteBySiteId(any())).thenReturn(siteModel)
        val expected = true
        val actual = classToTest.execute(1L)
        assertEquals(expected, actual)
    }
}
