package org.wordpress.android.usecase.social

import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.config.JetpackSocialFeatureConfig

class GetIsSocialSharingLimitedUseCaseTest {
    private val jetpackSocialFeatureConfig: JetpackSocialFeatureConfig = mock()
    private val siteStore: SiteStore = mock()
    private val classToTest = GetIsSocialSharingLimitedUseCase(
        jetpackSocialFeatureConfig = jetpackSocialFeatureConfig,
        siteStore = siteStore,
    )

    @Test
    fun `Should return FALSE if Jetpack Social FF is disabled`() {
        whenever(jetpackSocialFeatureConfig.isEnabled()).thenReturn(false)
        val expected = false
        val actual = classToTest.execute(1L)
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return FALSE if get site is null`() {
        whenever(jetpackSocialFeatureConfig.isEnabled()).thenReturn(true)
        whenever(siteStore.getSiteBySiteId(any())).thenReturn(null)
        val expected = false
        val actual = classToTest.execute(1L)
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return FALSE if site is not self hosted`() {
        val siteModel = SiteModel().apply {
            siteId = 1L
            setIsJetpackInstalled(false)
        }
        whenever(jetpackSocialFeatureConfig.isEnabled()).thenReturn(true)
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
        whenever(jetpackSocialFeatureConfig.isEnabled()).thenReturn(true)
        whenever(siteStore.getSiteBySiteId(any())).thenReturn(siteModel)
        val expected = false
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
        whenever(jetpackSocialFeatureConfig.isEnabled()).thenReturn(true)
        whenever(siteStore.getSiteBySiteId(any())).thenReturn(siteModel)
        val expected = true
        val actual = classToTest.execute(1L)
        assertEquals(expected, actual)
    }
}
