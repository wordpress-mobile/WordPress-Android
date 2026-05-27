package org.wordpress.android.ui.prefs.experimentalfeatures

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
class ExperimentalFeaturesViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var experimentalFeatures: ExperimentalFeatures

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var viewModel: ExperimentalFeaturesViewModel

    @Before
    fun setUp() {
        whenever(experimentalFeatures.isEnabled(any())).thenReturn(false)
    }

    @Test
    fun `init shows experimental block editor feature`() = test {
        createViewModel()

        val states = viewModel.switchStates.value

        assertThat(states).containsKey(Feature.EXPERIMENTAL_BLOCK_EDITOR)
    }

    @Test
    fun `init loads enabled state from experimental features`() = test {
        whenever(experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_BLOCK_EDITOR)).thenReturn(true)

        createViewModel()

        val states = viewModel.switchStates.value

        assertThat(states[Feature.EXPERIMENTAL_BLOCK_EDITOR]).isTrue()
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
    fun `state flow emits correct initial state`() = test {
        whenever(experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_BLOCK_EDITOR)).thenReturn(true)

        createViewModel()

        val states = viewModel.switchStates.value
        assertThat(states).isNotEmpty()
        states.forEach { (feature, enabled) ->
            assertThat(enabled).isEqualTo(experimentalFeatures.isEnabled(feature))
        }
    }

    @Test
    fun `toggling feature on tracks event with enabled true`() = test {
        createViewModel()

        viewModel.onFeatureToggled(Feature.EXPERIMENTAL_BLOCK_EDITOR, true)

        verify(analyticsTrackerWrapper).track(
            Stat.EXPERIMENTAL_FEATURE_TOGGLED,
            mapOf(
                "feature" to Feature.EXPERIMENTAL_BLOCK_EDITOR.prefKey,
                "enabled" to true
            )
        )
    }

    @Test
    fun `toggling feature off tracks event with enabled false`() = test {
        createViewModel()

        viewModel.onFeatureToggled(Feature.EXPERIMENTAL_BLOCK_EDITOR, false)

        verify(analyticsTrackerWrapper).track(
            Stat.EXPERIMENTAL_FEATURE_TOGGLED,
            mapOf(
                "feature" to Feature.EXPERIMENTAL_BLOCK_EDITOR.prefKey,
                "enabled" to false
            )
        )
    }

    private fun createViewModel() {
        viewModel = ExperimentalFeaturesViewModel(
            experimentalFeatures = experimentalFeatures,
            appLogWrapper = appLogWrapper,
            appPrefsWrapper = appPrefsWrapper,
            analyticsTrackerWrapper = analyticsTrackerWrapper
        )
    }
}
