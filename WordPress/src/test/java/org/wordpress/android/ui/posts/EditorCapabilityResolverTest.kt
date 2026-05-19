package org.wordpress.android.ui.posts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.datasets.SiteSettingsProvider
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.SiteSettingsModel
import org.wordpress.android.repositories.EditorSettingsRepository
import org.wordpress.android.util.config.GutenbergKitPluginsFeature

@RunWith(MockitoJUnitRunner::class)
class EditorCapabilityResolverTest {
    @Mock
    lateinit var gutenbergKitFeatureChecker: GutenbergKitFeatureChecker

    @Mock
    lateinit var gutenbergKitPluginsFeature: GutenbergKitPluginsFeature

    @Mock
    lateinit var editorSettingsRepository: EditorSettingsRepository

    @Mock
    lateinit var siteSettingsProvider: SiteSettingsProvider

    private val site = SiteModel()

    private lateinit var resolver: EditorCapabilityResolver

    @Before
    fun setUp() {
        resolver = EditorCapabilityResolver(
            gutenbergKitFeatureChecker,
            gutenbergKitPluginsFeature,
            editorSettingsRepository,
            siteSettingsProvider,
        )
        // Defaults that let resolution reach `Available` unless
        // a test overrides them.
        whenever(gutenbergKitFeatureChecker.isGutenbergKitEnabled()).thenReturn(true)
        whenever(gutenbergKitPluginsFeature.isEnabled()).thenReturn(true)
        whenever(editorSettingsRepository.getSupportsEditorAssetsForSite(any())).thenReturn(true)
        whenever(editorSettingsRepository.getSupportsEditorSettingsForSite(any())).thenReturn(true)
        whenever(editorSettingsRepository.getThemeSupportsBlockStyles(any())).thenReturn(true)
    }

    // ===== Third-party blocks =====

    @Test
    fun `third-party blocks hidden when GutenbergKit disabled`() {
        whenever(gutenbergKitFeatureChecker.isGutenbergKitEnabled()).thenReturn(false)

        val result = resolver.resolveThirdPartyBlocks(site)

        assertThat(result).isEqualTo(EditorCapabilityState.Hidden)
    }

    @Test
    fun `third-party blocks hidden when plugins feature disabled`() {
        whenever(gutenbergKitPluginsFeature.isEnabled()).thenReturn(false)

        val result = resolver.resolveThirdPartyBlocks(site)

        assertThat(result).isEqualTo(EditorCapabilityState.Hidden)
    }

    @Test
    fun `third-party blocks hidden when both feature flags disabled`() {
        whenever(gutenbergKitFeatureChecker.isGutenbergKitEnabled()).thenReturn(false)
        // lenient(): the resolver short-circuits on the GutenbergKit flag, so the plugins
        // stub is never read — strict mocking would treat that as a smell.
        lenient().`when`(gutenbergKitPluginsFeature.isEnabled()).thenReturn(false)

        val result = resolver.resolveThirdPartyBlocks(site)

        assertThat(result).isEqualTo(EditorCapabilityState.Hidden)
    }

    @Test
    fun `third-party blocks unsupported when site capability missing`() {
        whenever(editorSettingsRepository.getSupportsEditorAssetsForSite(any())).thenReturn(false)

        val result = resolver.resolveThirdPartyBlocks(site)

        assertThat(result).isEqualTo(
            EditorCapabilityState.Unsupported(EditorCapabilityState.UnsupportedReason.CapabilityMissing)
        )
    }

    @Test
    fun `third-party blocks available reflects user preference when set`() {
        val settings = SiteSettingsModel().apply { useThirdPartyBlocks = true }
        whenever(siteSettingsProvider.getSettings(any())).thenReturn(settings)

        val result = resolver.resolveThirdPartyBlocks(site)

        assertThat(result).isEqualTo(EditorCapabilityState.Available(userEnabled = true))
    }

    @Test
    fun `third-party blocks default off when user preference absent`() {
        whenever(siteSettingsProvider.getSettings(any())).thenReturn(null)

        val result = resolver.resolveThirdPartyBlocks(site)

        assertThat(result).isEqualTo(EditorCapabilityState.Available(userEnabled = false))
    }

    @Test
    fun `third-party blocks shouldApplyInEditor follows Available state`() {
        whenever(siteSettingsProvider.getSettings(any())).thenReturn(
            SiteSettingsModel().apply { useThirdPartyBlocks = true }
        )

        assertThat(resolver.resolveThirdPartyBlocks(site).shouldApplyInEditor).isTrue
    }

    @Test
    fun `third-party blocks shouldApplyInEditor is false when hidden`() {
        whenever(gutenbergKitPluginsFeature.isEnabled()).thenReturn(false)

        assertThat(resolver.resolveThirdPartyBlocks(site).shouldApplyInEditor).isFalse
    }

    @Test
    fun `third-party blocks shouldApplyInEditor is false when unsupported`() {
        whenever(editorSettingsRepository.getSupportsEditorAssetsForSite(any())).thenReturn(false)

        assertThat(resolver.resolveThirdPartyBlocks(site).shouldApplyInEditor).isFalse
    }

    // ===== Theme styles =====

    @Test
    fun `theme styles hidden when GutenbergKit disabled`() {
        whenever(gutenbergKitFeatureChecker.isGutenbergKitEnabled()).thenReturn(false)

        val result = resolver.resolveThemeStyles(site)

        assertThat(result).isEqualTo(EditorCapabilityState.Hidden)
    }

    @Test
    fun `theme styles available even when plugins feature disabled`() {
        // lenient(): the assertion is precisely that resolveThemeStyles ignores this flag,
        // so the stub goes unused — strict mocking would treat that as a smell.
        lenient().`when`(gutenbergKitPluginsFeature.isEnabled()).thenReturn(false)

        val result = resolver.resolveThemeStyles(site)

        assertThat(result).isInstanceOf(EditorCapabilityState.Available::class.java)
    }

    @Test
    fun `theme styles unsupported when site capability missing`() {
        whenever(editorSettingsRepository.getSupportsEditorSettingsForSite(any())).thenReturn(false)

        val result = resolver.resolveThemeStyles(site)

        assertThat(result).isEqualTo(
            EditorCapabilityState.Unsupported(EditorCapabilityState.UnsupportedReason.CapabilityMissing)
        )
    }

    @Test
    fun `theme styles advisory when theme is not a block theme`() {
        whenever(editorSettingsRepository.getThemeSupportsBlockStyles(any())).thenReturn(false)

        val result = resolver.resolveThemeStyles(site)

        assertThat(result).isEqualTo(
            EditorCapabilityState.Available(
                userEnabled = true,
                advisory = EditorCapabilityState.AdvisoryReason.ThemeNotBlockTheme,
            )
        )
    }

    @Test
    fun `theme styles reflects user preference when set`() {
        val settings = SiteSettingsModel().apply { useThemeStyles = false }
        whenever(siteSettingsProvider.getSettings(any())).thenReturn(settings)

        val result = resolver.resolveThemeStyles(site)

        assertThat(result).isEqualTo(EditorCapabilityState.Available(userEnabled = false))
    }

    @Test
    fun `theme styles default on when user preference absent`() {
        whenever(siteSettingsProvider.getSettings(any())).thenReturn(null)

        val result = resolver.resolveThemeStyles(site)

        assertThat(result).isEqualTo(EditorCapabilityState.Available(userEnabled = true))
    }

    @Test
    fun `theme styles shouldApplyInEditor honours user toggle even with advisory`() {
        whenever(editorSettingsRepository.getThemeSupportsBlockStyles(any())).thenReturn(false)
        whenever(siteSettingsProvider.getSettings(any())).thenReturn(
            SiteSettingsModel().apply { useThemeStyles = true }
        )

        assertThat(resolver.resolveThemeStyles(site).shouldApplyInEditor).isTrue
    }

    @Test
    fun `theme styles shouldApplyInEditor is false when user disabled`() {
        whenever(siteSettingsProvider.getSettings(any())).thenReturn(
            SiteSettingsModel().apply { useThemeStyles = false }
        )

        assertThat(resolver.resolveThemeStyles(site).shouldApplyInEditor).isFalse
    }

    // ===== EditorCapabilityState accessors (for Java callers) =====

    @Test
    fun `Hidden reports isHidden true and other flags false`() {
        val state: EditorCapabilityState = EditorCapabilityState.Hidden

        assertThat(state.isHidden).isTrue
        assertThat(state.isUnsupported).isFalse
        assertThat(state.isAvailable).isFalse
    }

    @Test
    fun `Unsupported reports isUnsupported true and other flags false`() {
        val state: EditorCapabilityState =
            EditorCapabilityState.Unsupported(EditorCapabilityState.UnsupportedReason.CapabilityMissing)

        assertThat(state.isUnsupported).isTrue
        assertThat(state.isHidden).isFalse
        assertThat(state.isAvailable).isFalse
    }

    @Test
    fun `Available reports isAvailable true and other flags false`() {
        val state: EditorCapabilityState = EditorCapabilityState.Available(userEnabled = true)

        assertThat(state.isAvailable).isTrue
        assertThat(state.isHidden).isFalse
        assertThat(state.isUnsupported).isFalse
    }

    @Test
    fun `asAvailable returns the Available instance when available`() {
        val available = EditorCapabilityState.Available(
            userEnabled = true,
            advisory = EditorCapabilityState.AdvisoryReason.ThemeNotBlockTheme,
        )
        val state: EditorCapabilityState = available

        assertThat(state.asAvailable).isSameAs(available)
    }

    @Test
    fun `asAvailable returns null for Hidden`() {
        val state: EditorCapabilityState = EditorCapabilityState.Hidden

        assertThat(state.asAvailable).isNull()
    }

    @Test
    fun `asAvailable returns null for Unsupported`() {
        val state: EditorCapabilityState =
            EditorCapabilityState.Unsupported(EditorCapabilityState.UnsupportedReason.CapabilityMissing)

        assertThat(state.asAvailable).isNull()
    }

    @Test
    fun `advisory is null for Hidden`() {
        val state: EditorCapabilityState = EditorCapabilityState.Hidden

        assertThat(state.advisory).isNull()
    }

    @Test
    fun `advisory is null for Unsupported`() {
        val state: EditorCapabilityState =
            EditorCapabilityState.Unsupported(EditorCapabilityState.UnsupportedReason.CapabilityMissing)

        assertThat(state.advisory).isNull()
    }

    @Test
    fun `advisory is null for Available without advisory`() {
        val state: EditorCapabilityState = EditorCapabilityState.Available(userEnabled = true)

        assertThat(state.advisory).isNull()
    }

    @Test
    fun `advisory returns reason for Available with advisory`() {
        val state: EditorCapabilityState = EditorCapabilityState.Available(
            userEnabled = true,
            advisory = EditorCapabilityState.AdvisoryReason.ThemeNotBlockTheme,
        )

        assertThat(state.advisory).isEqualTo(EditorCapabilityState.AdvisoryReason.ThemeNotBlockTheme)
    }
}
