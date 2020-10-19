package org.wordpress.android.util.config.manual

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.util.config.RemoteConfig
import org.wordpress.android.util.config.manual.ManualFeatureConfigViewModel.FeatureUiItem.Button
import org.wordpress.android.util.config.manual.ManualFeatureConfigViewModel.FeatureUiItem.Feature
import org.wordpress.android.util.config.manual.ManualFeatureConfigViewModel.FeatureUiItem.Feature.State.DISABLED
import org.wordpress.android.util.config.manual.ManualFeatureConfigViewModel.FeatureUiItem.Feature.State.ENABLED
import org.wordpress.android.util.config.manual.ManualFeatureConfigViewModel.FeatureUiItem.Feature.State.UNKNOWN
import org.wordpress.android.util.config.manual.ManualFeatureConfigViewModel.FeatureUiItem.Header
import org.wordpress.android.util.config.manual.ManualFeatureConfigViewModel.UiState
import org.wordpress.android.viewmodel.Event

class ManualFeatureConfigViewModelTest : BaseUnitTest() {
    @Mock lateinit var manualFeatureConfig: ManualFeatureConfig
    @Mock lateinit var remoteConfig: RemoteConfig
    private lateinit var viewModel: ManualFeatureConfigViewModel
    private val uiStates = mutableListOf<UiState>()
    private val restartActions = mutableListOf<Event<Unit>>()

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = ManualFeatureConfigViewModel(TEST_DISPATCHER, manualFeatureConfig, remoteConfig)
    }

    @Test
    fun `loads flags on start`() {
        setup()

        viewModel.start()

        assertUiState()
    }

    @Test
    fun `loads flags as enabled from manual config`() {
        whenever(manualFeatureConfig.hasManualSetup(any<String>())).thenReturn(true)
        whenever(manualFeatureConfig.isManuallyEnabled(any<String>())).thenReturn(true)
        setup()

        viewModel.start()

        assertUiState(ENABLED)
    }

    @Test
    fun `loads flags as disabled from manual config`() {
        whenever(manualFeatureConfig.hasManualSetup(any<String>())).thenReturn(true)
        whenever(manualFeatureConfig.isManuallyEnabled(any<String>())).thenReturn(false)
        setup()

        viewModel.start()

        assertUiState(DISABLED)
    }

    @Test
    fun `toggle item changes value and reloads data`() {
        whenever(remoteConfig.isEnabled(any())).thenReturn(false)
        setup()

        viewModel.start()

        val toggledItem = findFirstFeatureItem()

        val featureKey = toggledItem.title

        whenever(remoteConfig.isEnabled(featureKey)).thenReturn(true)

        toggledItem.toggleAction.toggle()

        verify(manualFeatureConfig).setManuallyEnabled(featureKey, true)
        assertUiState(enabledFeature = featureKey, hasRestartButton = true)
    }

    @Test
    fun `toggle item adds restart button at the end`() {
        whenever(remoteConfig.isEnabled(any())).thenReturn(false)
        setup()

        viewModel.start()

        findFirstFeatureItem().toggleAction.toggle()

        assertUiState(hasRestartButton = true)

        val restartButton = findRestartButton()

        restartButton.clickAction()

        assertThat(restartActions).hasSize(1)
    }

    private fun setup() {
        viewModel.uiState.observeForever {
            it?.let { uiStates.add(it) }
        }
        viewModel.restartAction.observeForever {
            it?.let { restartActions.add(it) }
        }
    }

    private fun assertUiState(
        expectedState: Feature.State? = null,
        enabledFeature: String? = null,
        hasRestartButton: Boolean = false
    ) {
        uiStates.last().apply {
            val headers = mutableListOf<Header>()
            val remoteItems = mutableListOf<Feature>()
            val developedItems = mutableListOf<Feature>()
            val buttons = mutableListOf<Button>()
            for (uiItem in this.uiItems) {
                when (uiItem) {
                    is Header -> headers.add(uiItem)
                    is Feature -> {
                        if (headers.size < 2) {
                            remoteItems.add(uiItem)
                        } else {
                            developedItems.add(uiItem)
                        }
                    }
                    is Button -> {
                        buttons.add(uiItem)
                    }
                }
            }
            assertThat(headers).hasSize(3)
            assertThat(headers[0].header).isEqualTo(R.string.manual_config_remote_features)
            assertThat(headers[1].header).isEqualTo(R.string.manual_config_features_in_development)
            assertThat(headers[2].header).isEqualTo(R.string.missing_developed_feature)
            remoteItems.filter { it.title != enabledFeature }
                    .forEach { assertThat(it.state).isEqualTo(expectedState ?: DISABLED) }
            developedItems.filter { it.title != enabledFeature }
                    .forEach { assertThat(it.state).isEqualTo(expectedState ?: UNKNOWN) }
            if (enabledFeature != null) {
                assertThat(remoteItems.find { it.title == enabledFeature }!!.state).isEqualTo(ENABLED)
            }
            if (hasRestartButton) {
                assertThat(buttons).hasSize(1)
            } else {
                assertThat(buttons).hasSize(0)
            }
        }
    }

    private fun findFirstFeatureItem(): Feature {
        return uiStates.last().uiItems.find { it is Feature } as Feature
    }

    private fun findRestartButton(): Button {
        return uiStates.last().uiItems.find { it is Button } as Button
    }
}
