package org.wordpress.android.ui.prefs.privacy.banner.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.privacy.GeoRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ShouldAskPrivacyConsentTest: BaseUnitTest() {
    private val appPrefs: AppPrefsWrapper = mock()
    private val geoRepository: GeoRepository = mock()
    private val accountStore: AccountStore = mock()
    private val selectedSiteRepository: SelectedSiteRepository = mock()

    // SUT
    val shouldAskPrivacyConsent = ShouldAskPrivacyConsent(appPrefs, geoRepository, accountStore, selectedSiteRepository)

    @Test
    fun `it returns false when logged out`() = test {
        // Given
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        // When
        val result = shouldAskPrivacyConsent()

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `it only shows the banner once`() = test {
        // Given
        whenever(appPrefs.savedPrivacyBannerSettings).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        // When
        val result = shouldAskPrivacyConsent()

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `it does not show the banner for non-GDPR countries`() = test {
        // Given
        whenever(appPrefs.savedPrivacyBannerSettings).thenReturn(false)
        whenever(geoRepository.isGdprComplianceRequired()).thenReturn(false)
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        // When
        val result = shouldAskPrivacyConsent()

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `it does show the banner for GDPR countries`() = test {
        // Given
        whenever(appPrefs.savedPrivacyBannerSettings).thenReturn(false)
        whenever(geoRepository.isGdprComplianceRequired()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        // When
        val result = shouldAskPrivacyConsent()

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `it does show the banner for self-hosted sites`() = test {
        val site: SiteModel = mock()

        // Given
        whenever(appPrefs.savedPrivacyBannerSettings).thenReturn(false)
        whenever(geoRepository.isGdprComplianceRequired()).thenReturn(true)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(site.origin).thenReturn(SiteModel.ORIGIN_WPAPI)

        // When
        val result = shouldAskPrivacyConsent()

        // Then
        assertThat(result).isTrue()
    }
}
