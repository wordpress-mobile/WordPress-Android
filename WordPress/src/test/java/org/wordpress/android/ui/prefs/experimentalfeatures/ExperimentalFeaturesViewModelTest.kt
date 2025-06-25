package org.wordpress.android.ui.prefs.experimentalfeatures

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.BuildConfig
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.config.GutenbergKitFeature

@ExperimentalCoroutinesApi
class ExperimentalFeaturesViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var experimentalFeatures: ExperimentalFeatures

    @Mock
    private lateinit var gutenbergKitFeature: GutenbergKitFeature

    @Mock
    private lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    private lateinit var viewModel: ExperimentalFeaturesViewModel

    @Before
    fun setUp() {
        whenever(experimentalFeatures.isEnabled(any())).thenReturn(false)
        whenever(gutenbergKitFeature.isEnabled()).thenReturn(false)
    }

    @Test
    fun `init shows disable block editor when gutenberg kit is enabled`() = test {
        whenever(gutenbergKitFeature.isEnabled()).thenReturn(true)

        createViewModel()

        val states = viewModel.switchStates.value

        assertThat(states).containsKey(Feature.DISABLE_EXPERIMENTAL_BLOCK_EDITOR)
        assertThat(states).doesNotContainKey(Feature.EXPERIMENTAL_BLOCK_EDITOR)
    }

    @Test
    fun `init shows experimental block editor when gutenberg kit is disabled`() = test {
        whenever(gutenbergKitFeature.isEnabled()).thenReturn(false)

        createViewModel()

        val states = viewModel.switchStates.value

        assertThat(states).containsKey(Feature.EXPERIMENTAL_BLOCK_EDITOR)
        assertThat(states).doesNotContainKey(Feature.DISABLE_EXPERIMENTAL_BLOCK_EDITOR)
    }

    @Test
    fun `init loads enabled state from experimental features`() = test {
        whenever(experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_BLOCK_EDITOR)).thenReturn(true)
        whenever(experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_BLOCK_EDITOR_THEME_STYLES)).thenReturn(false)

        createViewModel()

        val states = viewModel.switchStates.value

        assertThat(states[Feature.EXPERIMENTAL_BLOCK_EDITOR]).isTrue()
        assertThat(states[Feature.EXPERIMENTAL_BLOCK_EDITOR_THEME_STYLES]).isFalse()
    }

    @Test
    fun `onFeatureToggled updates state and persists to experimental features`() = test {
        createViewModel()

        viewModel.onFeatureToggled(Feature.EXPERIMENTAL_BLOCK_EDITOR, true)

        val states = viewModel.switchStates.value
        assertThat(states[Feature.EXPERIMENTAL_BLOCK_EDITOR]).isTrue()
        verify(experimentalFeatures).setEnabled(Feature.EXPERIMENTAL_BLOCK_EDITOR, true)
    }

    @Test
    fun `onFeatureToggled removes application password credentials when feature is disabled`() = test {
        if (BuildConfig.DEBUG) {
            createViewModel()

            viewModel.onFeatureToggled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE, false)
            advanceUntilIdle()

            verify(applicationPasswordLoginHelper).removeAllApplicationPasswordCredentials()
        }
    }

    @Test
    fun `onFeatureToggled does not remove credentials when application password is enabled`() = test {
        if (BuildConfig.DEBUG) {
            createViewModel()

            viewModel.onFeatureToggled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE, true)
            advanceUntilIdle()

            verify(applicationPasswordLoginHelper, never()).removeAllApplicationPasswordCredentials()
        }
    }

    @Test
    fun `onFeatureToggled does not remove credentials for other features`() = test {
        createViewModel()

        viewModel.onFeatureToggled(Feature.EXPERIMENTAL_BLOCK_EDITOR, false)
        advanceUntilIdle()

        verify(applicationPasswordLoginHelper, never()).removeAllApplicationPasswordCredentials()
    }

    @Test
    fun `onFeatureToggled logs error when removing credentials fails`() = test {
        if (BuildConfig.DEBUG) {
            createViewModel()
            val exception = RuntimeException("Test exception")
            whenever(applicationPasswordLoginHelper.removeAllApplicationPasswordCredentials())
                .doThrow(exception)

            viewModel.onFeatureToggled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE, false)
            advanceUntilIdle()

            verify(appLogWrapper).e(
                eq(AppLog.T.DB),
                any()
            )
        }
    }

    @Test
    fun `state flow emits correct initial state`() = test {
        whenever(experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_BLOCK_EDITOR)).thenReturn(true)
        whenever(experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_BLOCK_EDITOR_THEME_STYLES)).thenReturn(false)

        createViewModel()

        val states = viewModel.switchStates.value
        assertThat(states).isNotEmpty()
        states.forEach { (feature, enabled) ->
            assertThat(enabled).isEqualTo(experimentalFeatures.isEnabled(feature))
        }
    }

    private fun createViewModel() {
        viewModel = ExperimentalFeaturesViewModel(
            experimentalFeatures = experimentalFeatures,
            gutenbergKitFeature = gutenbergKitFeature,
            applicationPasswordLoginHelper = applicationPasswordLoginHelper,
            appLogWrapper = appLogWrapper
        )
    }
}
