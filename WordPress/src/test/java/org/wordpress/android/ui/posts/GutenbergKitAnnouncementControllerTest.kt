package org.wordpress.android.ui.posts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.datasets.SiteSettingsProvider
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RunWith(MockitoJUnitRunner::class)
class GutenbergKitAnnouncementControllerTest {
    @Mock private lateinit var featureChecker: GutenbergKitFeatureChecker
    @Mock private lateinit var siteSettingsProvider: SiteSettingsProvider
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper

    private val now = Instant.parse("2026-05-25T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("UTC"))
    private val site = SiteModel().apply { url = SITE_URL }

    private lateinit var controller: GutenbergKitAnnouncementController

    @Before
    fun setUp() {
        controller = GutenbergKitAnnouncementController(
            featureChecker, siteSettingsProvider, appPrefsWrapper, clock
        )
        // Defaults that let shouldShowAnnouncement reach `true` unless a test overrides them.
        whenever(featureChecker.isGutenbergKitRemoteFeatureEnabled()).thenReturn(true)
        whenever(siteSettingsProvider.isBlockEditorDefault(site)).thenReturn(true)
        whenever(appPrefsWrapper.getGutenbergKitSiteOverride(SITE_URL)).thenReturn(null)
        whenever(appPrefsWrapper.getGutenbergKitAnnouncementDeferredUntil(SITE_URL)).thenReturn(0L)
    }

    @Test
    fun `shouldShowAnnouncement is true when all gates pass`() {
        assertThat(controller.shouldShowAnnouncement(site)).isTrue()
    }

    @Test
    fun `shouldShowAnnouncement is false when site URL is empty`() {
        val emptyUrlSite = SiteModel().apply { url = "" }
        assertThat(controller.shouldShowAnnouncement(emptyUrlSite)).isFalse()
    }

    @Test
    fun `shouldShowAnnouncement is false when site URL is null`() {
        val nullUrlSite = SiteModel()
        assertThat(controller.shouldShowAnnouncement(nullUrlSite)).isFalse()
    }

    @Test
    fun `shouldShowAnnouncement is false when remote flag is off`() {
        whenever(featureChecker.isGutenbergKitRemoteFeatureEnabled()).thenReturn(false)
        assertThat(controller.shouldShowAnnouncement(site)).isFalse()
    }

    @Test
    fun `shouldShowAnnouncement is false when site does not default to block editor`() {
        whenever(siteSettingsProvider.isBlockEditorDefault(site)).thenReturn(false)
        assertThat(controller.shouldShowAnnouncement(site)).isFalse()
    }

    @Test
    fun `shouldShowAnnouncement is false when site has already opted in`() {
        whenever(appPrefsWrapper.getGutenbergKitSiteOverride(SITE_URL)).thenReturn(true)
        assertThat(controller.shouldShowAnnouncement(site)).isFalse()
    }

    @Test
    fun `shouldShowAnnouncement is false when site has already opted out`() {
        whenever(appPrefsWrapper.getGutenbergKitSiteOverride(SITE_URL)).thenReturn(false)
        assertThat(controller.shouldShowAnnouncement(site)).isFalse()
    }

    @Test
    fun `shouldShowAnnouncement is false while deferral is still in the future`() {
        whenever(appPrefsWrapper.getGutenbergKitAnnouncementDeferredUntil(SITE_URL))
            .thenReturn(now.toEpochMilli() + 1)
        assertThat(controller.shouldShowAnnouncement(site)).isFalse()
    }

    @Test
    fun `shouldShowAnnouncement is true once deferral has expired`() {
        whenever(appPrefsWrapper.getGutenbergKitAnnouncementDeferredUntil(SITE_URL))
            .thenReturn(now.toEpochMilli() - 1)
        assertThat(controller.shouldShowAnnouncement(site)).isTrue()
    }

    @Test
    fun `shouldShowAnnouncement is true when deferral equals current clock`() {
        whenever(appPrefsWrapper.getGutenbergKitAnnouncementDeferredUntil(SITE_URL))
            .thenReturn(now.toEpochMilli())
        assertThat(controller.shouldShowAnnouncement(site)).isTrue()
    }

    @Test
    fun `onActivate writes a positive per-site override and clears any deferral`() {
        controller.onActivate(site)
        verify(appPrefsWrapper).setGutenbergKitSiteOverride(SITE_URL, true)
        verify(appPrefsWrapper).setGutenbergKitAnnouncementDeferredUntil(SITE_URL, 0L)
    }

    @Test
    fun `setOverride with false writes opt-out and clears any deferral`() {
        controller.setOverride(site, false)
        verify(appPrefsWrapper).setGutenbergKitSiteOverride(SITE_URL, false)
        verify(appPrefsWrapper).setGutenbergKitAnnouncementDeferredUntil(SITE_URL, 0L)
    }

    @Test
    fun `onMaybeLater defers for one week from now and does not write an override`() {
        controller.onMaybeLater(site)
        val expected = now.toEpochMilli() + GutenbergKitAnnouncementController.DEFER_DURATION_MILLIS
        verify(appPrefsWrapper).setGutenbergKitAnnouncementDeferredUntil(SITE_URL, expected)
        verify(appPrefsWrapper, never()).setGutenbergKitSiteOverride(eq(SITE_URL), any())
    }

    companion object {
        private const val SITE_URL = "https://example.com"
    }
}
