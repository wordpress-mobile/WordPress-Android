package org.wordpress.android.ui.posts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.config.GutenbergKitFeature

@RunWith(MockitoJUnitRunner::class)
class GutenbergKitFeatureCheckerTest {
    @Mock
    private lateinit var experimentalFeatures: ExperimentalFeatures

    @Mock
    private lateinit var gutenbergKitFeature: GutenbergKitFeature

    private lateinit var featureChecker: GutenbergKitFeatureChecker

    @Before
    fun setUp() {
        featureChecker = GutenbergKitFeatureChecker(experimentalFeatures, gutenbergKitFeature)
    }

    // Helper method to setup mock behavior
    private fun setupFeatureFlags(
        experimentalBlockEditor: Boolean = false,
        gutenbergKitEnabled: Boolean = false,
        disableExperimentalBlockEditor: Boolean = false
    ) {
        whenever(experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_BLOCK_EDITOR))
            .thenReturn(experimentalBlockEditor)
        whenever(gutenbergKitFeature.isEnabled()).thenReturn(gutenbergKitEnabled)
        whenever(experimentalFeatures.isEnabled(Feature.DISABLE_EXPERIMENTAL_BLOCK_EDITOR))
            .thenReturn(disableExperimentalBlockEditor)
    }

    // ===== Feature State Tests =====

    @Test
    fun `getFeatureState returns correct individual flag values when all flags are false`() {
        setupFeatureFlags(
            experimentalBlockEditor = false,
            gutenbergKitEnabled = false,
            disableExperimentalBlockEditor = false
        )

        val featureState = featureChecker.getFeatureState()

        assertThat(featureState.isExperimentalBlockEditorEnabled).isFalse()
        assertThat(featureState.isGutenbergKitFeatureEnabled).isFalse()
        assertThat(featureState.isDisableExperimentalBlockEditorEnabled).isFalse()
        assertThat(featureState.isGutenbergKitEnabled).isFalse()
    }

    @Test
    fun `getFeatureState returns correct individual flag values when all flags are true`() {
        setupFeatureFlags(
            experimentalBlockEditor = true,
            gutenbergKitEnabled = true,
            disableExperimentalBlockEditor = true
        )

        val featureState = featureChecker.getFeatureState()

        assertThat(featureState.isExperimentalBlockEditorEnabled).isTrue()
        assertThat(featureState.isGutenbergKitFeatureEnabled).isTrue()
        assertThat(featureState.isDisableExperimentalBlockEditorEnabled).isTrue()
        // Should be false because disable flag overrides
        assertThat(featureState.isGutenbergKitEnabled).isFalse()
    }

    @Test
    fun `getFeatureState returns correct values for mixed flag states`() {
        setupFeatureFlags(
            experimentalBlockEditor = true,
            gutenbergKitEnabled = false,
            disableExperimentalBlockEditor = false
        )

        val featureState = featureChecker.getFeatureState()

        assertThat(featureState.isExperimentalBlockEditorEnabled).isTrue()
        assertThat(featureState.isGutenbergKitFeatureEnabled).isFalse()
        assertThat(featureState.isDisableExperimentalBlockEditorEnabled).isFalse()
        assertThat(featureState.isGutenbergKitEnabled).isTrue()
    }

    // ===== GutenbergKit Enabled Logic Tests =====

    @Test
    fun `isGutenbergKitEnabled returns true when experimental block editor is enabled`() {
        setupFeatureFlags(
            experimentalBlockEditor = true,
            gutenbergKitEnabled = false,
            disableExperimentalBlockEditor = false
        )

        val result = featureChecker.isGutenbergKitEnabled()

        assertThat(result).isTrue()
    }

    @Test
    fun `isGutenbergKitEnabled returns true when GutenbergKit feature is enabled`() {
        setupFeatureFlags(
            experimentalBlockEditor = false,
            gutenbergKitEnabled = true,
            disableExperimentalBlockEditor = false
        )

        val result = featureChecker.isGutenbergKitEnabled()

        assertThat(result).isTrue()
    }

    @Test
    fun `isGutenbergKitEnabled returns true when both experimental and GutenbergKit features are enabled`() {
        setupFeatureFlags(
            experimentalBlockEditor = true,
            gutenbergKitEnabled = true,
            disableExperimentalBlockEditor = false
        )

        val result = featureChecker.isGutenbergKitEnabled()

        assertThat(result).isTrue()
    }

    @Test
    fun `isGutenbergKitEnabled returns false when all flags are disabled`() {
        setupFeatureFlags(
            experimentalBlockEditor = false,
            gutenbergKitEnabled = false,
            disableExperimentalBlockEditor = false
        )

        val result = featureChecker.isGutenbergKitEnabled()

        assertThat(result).isFalse()
    }

    @Test
    fun `isGutenbergKitEnabled returns false when disable flag is enabled regardless of other flags`() {
        setupFeatureFlags(
            experimentalBlockEditor = true,
            gutenbergKitEnabled = true,
            disableExperimentalBlockEditor = true
        )

        val result = featureChecker.isGutenbergKitEnabled()

        assertThat(result).isFalse()
    }

    @Test
    fun `isGutenbergKitEnabled returns false when only disable flag is enabled`() {
        setupFeatureFlags(
            experimentalBlockEditor = false,
            gutenbergKitEnabled = false,
            disableExperimentalBlockEditor = true
        )

        val result = featureChecker.isGutenbergKitEnabled()

        assertThat(result).isFalse()
    }

    // ===== Edge Cases and Consistency Tests =====

    @Test
    fun `isGutenbergKitEnabled matches getFeatureState isGutenbergKitEnabled for all combinations`() {
        val testCases = listOf(
            Triple(false, false, false),
            Triple(false, false, true),
            Triple(false, true, false),
            Triple(false, true, true),
            Triple(true, false, false),
            Triple(true, false, true),
            Triple(true, true, false),
            Triple(true, true, true)
        )

        testCases.forEach { (experimental, gutenbergKit, disable) ->
            setupFeatureFlags(
                experimentalBlockEditor = experimental,
                gutenbergKitEnabled = gutenbergKit,
                disableExperimentalBlockEditor = disable
            )

            val directResult = featureChecker.isGutenbergKitEnabled()
            val stateResult = featureChecker.getFeatureState().isGutenbergKitEnabled

            assertThat(directResult)
                .withFailMessage(
                    "Results should match for flags: experimental=$experimental, " +
                            "gutenbergKit=$gutenbergKit, disable=$disable"
                )
                .isEqualTo(stateResult)
        }
    }

    @Test
    fun `disable flag always overrides other settings`() {
        val testCases = listOf(
            Triple(false, false, true),  // Only disable flag
            Triple(false, true, true),   // GutenbergKit + disable
            Triple(true, false, true),   // Experimental + disable
            Triple(true, true, true)     // All flags on
        )

        testCases.forEach { (experimental, gutenbergKit, disable) ->
            setupFeatureFlags(
                experimentalBlockEditor = experimental,
                gutenbergKitEnabled = gutenbergKit,
                disableExperimentalBlockEditor = disable
            )

            val result = featureChecker.isGutenbergKitEnabled()

            assertThat(result)
                .withFailMessage(
                    "Should be false when disable flag is true " +
                            "(experimental=$experimental, gutenbergKit=$gutenbergKit)"
                )
                .isFalse()
        }
    }

    @Test
    fun `feature is enabled when at least one enabling flag is true and disable flag is false`() {
        val enabledTestCases = listOf(
            Triple(true, false, false),   // Only experimental
            Triple(false, true, false),   // Only GutenbergKit
            Triple(true, true, false)     // Both enabled
        )

        enabledTestCases.forEach { (experimental, gutenbergKit, disable) ->
            setupFeatureFlags(
                experimentalBlockEditor = experimental,
                gutenbergKitEnabled = gutenbergKit,
                disableExperimentalBlockEditor = disable
            )

            val result = featureChecker.isGutenbergKitEnabled()

            assertThat(result)
                .withFailMessage(
                    "Should be true when at least one enabling flag is true " +
                            "(experimental=$experimental, gutenbergKit=$gutenbergKit)"
                )
                .isTrue()
        }
    }
}
