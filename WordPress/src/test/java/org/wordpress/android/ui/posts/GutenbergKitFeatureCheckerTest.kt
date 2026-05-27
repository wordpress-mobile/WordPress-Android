package org.wordpress.android.ui.posts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.config.GutenbergKitFeature

@RunWith(MockitoJUnitRunner::class)
class GutenbergKitFeatureCheckerTest {
    @Mock
    private lateinit var experimentalFeatures: ExperimentalFeatures

    @Mock
    private lateinit var gutenbergKitFeature: GutenbergKitFeature

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    private lateinit var featureChecker: GutenbergKitFeatureChecker

    @Before
    fun setUp() {
        featureChecker = GutenbergKitFeatureChecker(experimentalFeatures, gutenbergKitFeature, appPrefsWrapper)
    }

    private fun setupFeatureFlags(
        experimentalBlockEditor: Boolean = false,
        gutenbergKitEnabled: Boolean = false,
    ) {
        whenever(experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_BLOCK_EDITOR))
            .thenReturn(experimentalBlockEditor)
        whenever(gutenbergKitFeature.isEnabled()).thenReturn(gutenbergKitEnabled)
    }

    // ===== Feature State Tests =====

    @Test
    fun `getFeatureState returns correct individual flag values when all flags are false`() {
        setupFeatureFlags(experimentalBlockEditor = false, gutenbergKitEnabled = false)

        val featureState = featureChecker.getFeatureState()

        assertThat(featureState.isExperimentalBlockEditorEnabled).isFalse()
        assertThat(featureState.isGutenbergKitFeatureEnabled).isFalse()
        assertThat(featureState.isGutenbergKitEnabled).isFalse()
    }

    @Test
    fun `getFeatureState returns correct values for mixed flag states`() {
        setupFeatureFlags(experimentalBlockEditor = true, gutenbergKitEnabled = false)

        val featureState = featureChecker.getFeatureState()

        assertThat(featureState.isExperimentalBlockEditorEnabled).isTrue()
        assertThat(featureState.isGutenbergKitFeatureEnabled).isFalse()
        assertThat(featureState.isGutenbergKitEnabled).isTrue()
    }

    // ===== Editor-routing Logic Tests =====

    @Test
    fun `isGutenbergKitEnabled returns true when experimental block editor is enabled`() {
        setupFeatureFlags(experimentalBlockEditor = true, gutenbergKitEnabled = false)

        assertThat(featureChecker.isGutenbergKitEnabled()).isTrue()
    }

    @Test
    fun `remote feature flag alone does not enable GutenbergKit for editor routing`() {
        // The remote `gutenberg_kit` flag only gates the announcement and Site Settings toggle
        // visibility. Editor routing requires either the experimental flag or a per-site opt-in.
        setupFeatureFlags(experimentalBlockEditor = false, gutenbergKitEnabled = true)

        assertThat(featureChecker.isGutenbergKitEnabled()).isFalse()
    }

    @Test
    fun `isGutenbergKitEnabled returns true when experimental flag is on regardless of remote flag`() {
        setupFeatureFlags(experimentalBlockEditor = true, gutenbergKitEnabled = true)

        assertThat(featureChecker.isGutenbergKitEnabled()).isTrue()
    }

    @Test
    fun `isGutenbergKitEnabled returns false when all flags are disabled`() {
        setupFeatureFlags(experimentalBlockEditor = false, gutenbergKitEnabled = false)

        assertThat(featureChecker.isGutenbergKitEnabled()).isFalse()
    }

    // ===== Per-site Override Tests =====

    @Test
    fun `per-site opt-in enables GutenbergKit when no other flag is set`() {
        setupFeatureFlags(experimentalBlockEditor = false, gutenbergKitEnabled = false)
        val site = SiteModel().apply { url = "https://example.com" }
        whenever(appPrefsWrapper.getGutenbergKitSiteOverride("https://example.com")).thenReturn(true)

        assertThat(featureChecker.isGutenbergKitEnabled(site)).isTrue()
    }

    @Test
    fun `remote feature flag on with no override does not enable for a site`() {
        // Editor routing only: announcement visibility is checked via
        // `isGutenbergKitRemoteFeatureEnabled()` separately.
        setupFeatureFlags(experimentalBlockEditor = false, gutenbergKitEnabled = true)
        val site = SiteModel().apply { url = "https://example.com" }
        whenever(appPrefsWrapper.getGutenbergKitSiteOverride("https://example.com")).thenReturn(null)

        assertThat(featureChecker.isGutenbergKitEnabled(site)).isFalse()
    }

    @Test
    fun `per-site opt-out wins over experimental flag`() {
        setupFeatureFlags(experimentalBlockEditor = true, gutenbergKitEnabled = false)
        val site = SiteModel().apply { url = "https://example.com" }
        whenever(appPrefsWrapper.getGutenbergKitSiteOverride("https://example.com")).thenReturn(false)

        assertThat(featureChecker.isGutenbergKitEnabled(site)).isFalse()
    }

    @Test
    fun `per-site opt-in wins when remote and experimental flags are off`() {
        setupFeatureFlags(experimentalBlockEditor = false, gutenbergKitEnabled = false)
        val site = SiteModel().apply { url = "https://example.com" }
        whenever(appPrefsWrapper.getGutenbergKitSiteOverride("https://example.com")).thenReturn(true)

        assertThat(featureChecker.isGutenbergKitEnabled(site)).isTrue()
    }

    @Test
    fun `per-site opt-in wins when remote flag is on`() {
        setupFeatureFlags(experimentalBlockEditor = false, gutenbergKitEnabled = true)
        val site = SiteModel().apply { url = "https://example.com" }
        whenever(appPrefsWrapper.getGutenbergKitSiteOverride("https://example.com")).thenReturn(true)

        assertThat(featureChecker.isGutenbergKitEnabled(site)).isTrue()
    }

    @Test
    fun `editor routing is enabled only by experimental flag or per-site opt-in`() {
        // The remote `gutenberg_kit` flag is intentionally NOT an editor-routing input — it only
        // gates announcement visibility. Editor routing requires experimental OR per-site opt-in.
        data class Case(
            val experimental: Boolean,
            val gutenbergKitRemote: Boolean,
            val siteOverride: Boolean?,
            val expected: Boolean,
        )
        val cases = listOf(
            Case(experimental = true, gutenbergKitRemote = false, siteOverride = null, expected = true),
            Case(experimental = true, gutenbergKitRemote = true, siteOverride = null, expected = true),
            Case(experimental = false, gutenbergKitRemote = true, siteOverride = null, expected = false),
            Case(experimental = false, gutenbergKitRemote = false, siteOverride = true, expected = true),
            Case(experimental = false, gutenbergKitRemote = true, siteOverride = true, expected = true),
            Case(experimental = true, gutenbergKitRemote = true, siteOverride = false, expected = false),
            Case(experimental = false, gutenbergKitRemote = false, siteOverride = null, expected = false),
        )

        cases.forEach { case ->
            setupFeatureFlags(
                experimentalBlockEditor = case.experimental,
                gutenbergKitEnabled = case.gutenbergKitRemote,
            )
            val site = SiteModel().apply { url = "https://example.com" }
            whenever(appPrefsWrapper.getGutenbergKitSiteOverride("https://example.com"))
                .thenReturn(case.siteOverride)

            assertThat(featureChecker.isGutenbergKitEnabled(site))
                .withFailMessage("Case $case")
                .isEqualTo(case.expected)
        }
    }
}
