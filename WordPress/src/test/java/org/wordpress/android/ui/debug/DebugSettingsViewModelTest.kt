package org.wordpress.android.ui.debug

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Button
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.DISABLED
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.ENABLED
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.UNKNOWN
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Header
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Row
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiState
import org.wordpress.android.ui.notifications.NotificationManagerWrapper
import org.wordpress.android.util.DebugUtils
import org.wordpress.android.util.config.ManualFeatureConfig
import org.wordpress.android.util.config.FeatureFlagConfig
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.workers.weeklyroundup.WeeklyRoundupNotifier

@ExperimentalCoroutinesApi
class DebugSettingsViewModelTest : BaseUnitTest() {
    @Mock lateinit var manualFeatureConfig: ManualFeatureConfig
    @Mock lateinit var featureFlagConfig: FeatureFlagConfig
    @Mock lateinit var debugUtils: DebugUtils
    @Mock lateinit var weeklyRoundupNotifier: WeeklyRoundupNotifier
    @Mock lateinit var notificationManager: NotificationManagerWrapper
    @Mock lateinit var contextProvider: ContextProvider
    private lateinit var viewModel: DebugSettingsViewModel
    private val uiStates = mutableListOf<UiState>()

    @Before
    fun setUp() {
        viewModel = DebugSettingsViewModel(
                coroutinesTestRule.testDispatcher,
                coroutinesTestRule.testDispatcher,
                manualFeatureConfig,
                featureFlagConfig,
                debugUtils,
                weeklyRoundupNotifier,
                notificationManager,
                contextProvider
        )
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
        whenever(featureFlagConfig.isEnabled(any())).thenReturn(false)
        setup()

        viewModel.start()

        val toggledItem = findFirstFeatureItem()

        val featureKey = toggledItem.title

        whenever(featureFlagConfig.isEnabled(featureKey)).thenReturn(true)

        toggledItem.toggleAction.toggle()

        verify(manualFeatureConfig).setManuallyEnabled(featureKey, true)
        assertUiState(enabledFeature = featureKey, hasRestartButton = true)
    }

    @Test
    fun `toggle item adds restart button at the end`() {
        whenever(featureFlagConfig.isEnabled(any())).thenReturn(false)
        setup()

        viewModel.start()

        findFirstFeatureItem().toggleAction.toggle()

        assertUiState(hasRestartButton = true)

        val restartButton = findRestartButton()

        restartButton.clickAction()

        verify(debugUtils).restartApp()
    }

    private fun setup() {
        viewModel.uiState.observeForever {
            it?.let { uiStates.add(it) }
        }
    }

    @Suppress("NestedBlockDepth")
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
            val rows = mutableListOf<Row>()
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
                    is Row -> {
                        rows.add(uiItem)
                    }
                }
            }
            assertThat(headers).hasSize(4)
            assertThat(headers[0].header).isEqualTo(R.string.debug_settings_remote_features)
            assertThat(headers[1].header).isEqualTo(R.string.debug_settings_features_in_development)
            assertThat(headers[2].header).isEqualTo(R.string.debug_settings_missing_developed_feature)
            assertThat(headers[3].header).isEqualTo(R.string.debug_settings_tools)
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
