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
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.notifications.NotificationManagerWrapper
import org.wordpress.android.util.DebugUtils
import org.wordpress.android.util.config.FeatureFlagConfig
import org.wordpress.android.util.config.ManualFeatureConfig
import org.wordpress.android.util.config.RemoteFieldConfigRepository
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.workers.weeklyroundup.WeeklyRoundupNotifier
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class DebugSettingsViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var manualFeatureConfig: ManualFeatureConfig

    @Mock
    lateinit var featureFlagConfig: FeatureFlagConfig

    @Mock
    lateinit var debugUtils: DebugUtils

    @Mock
    lateinit var weeklyRoundupNotifier: WeeklyRoundupNotifier

    @Mock
    lateinit var notificationManager: NotificationManagerWrapper

    @Mock
    lateinit var contextProvider: ContextProvider

    @Mock
    lateinit var remoteFieldConfigRepository: RemoteFieldConfigRepository

    @Mock
    lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    private lateinit var viewModel: DebugSettingsViewModel
    private val uiStates = mutableListOf<UiState>()

    @Before
    fun setUp() {
        viewModel = DebugSettingsViewModel(
            testDispatcher(),
            testDispatcher(),
            manualFeatureConfig,
            featureFlagConfig,
            remoteFieldConfigRepository,
            debugUtils,
            weeklyRoundupNotifier,
            notificationManager,
            contextProvider,
            jetpackFeatureRemovalPhaseHelper
        )
        observeUIState()
    }

    private fun observeUIState() {
        viewModel.uiState.observeForever {
            it?.let { uiStates.add(it) }
        }
    }

    @Test
    fun `given type remote features, when viewmodel starts, then only remote features are fetched`() {
        viewModel.start(DebugSettingsType.REMOTE_FEATURES)

        assertThat(uiStates.last().uiItems).allMatch { it is UiItem.FeatureFlag.RemoteFeatureFlag }
    }

    @Test
    fun `given type local features, when viewmodel starts, then only remote features are fetched`() {
        viewModel.start(DebugSettingsType.FEATURES_IN_DEVELOPMENT)

        assertThat(uiStates.last().uiItems)
            .allMatch { it is UiItem.FeatureFlag.LocalFeatureFlag }
            .allMatch {
                (it as UiItem.FeatureFlag.LocalFeatureFlag).enabled == null
            }
    }

    @Test
    fun `given type remote field configs, when viewmodel starts, then only remote configs are fetched`() {
        viewModel.start(DebugSettingsType.REMOTE_FIELD_CONFIGS)

        assertThat(uiStates.last().uiItems).allMatch { it is UiItem.Field }
    }


    @Test
    fun `given manually overidden, when viewmodel starts, then value source is manual`() {
        whenever(manualFeatureConfig.hasManualSetup(any<String>())).thenReturn(true)
        whenever(manualFeatureConfig.isManuallyEnabled(any<String>())).thenReturn(true)

        viewModel.start(DebugSettingsType.REMOTE_FEATURES)

        assertThat(uiStates.last().uiItems)
            .allMatch {
                (it as UiItem.FeatureFlag.RemoteFeatureFlag).enabled == true
            }.allMatch {
                (it as UiItem.FeatureFlag.RemoteFeatureFlag).source == "Manual"
            }
    }

    @Test
    fun `given remote feature values are fetched, when toggle action is invoked, then value is changed`() {
        whenever(featureFlagConfig.isEnabled(any())).thenReturn(false)

        viewModel.start(DebugSettingsType.REMOTE_FEATURES)

        val remoteFeatureFlag = (uiStates.last().uiItems[0] as UiItem.FeatureFlag.RemoteFeatureFlag)

        val featureKey = remoteFeatureFlag.title

        whenever(featureFlagConfig.isEnabled(featureKey)).thenReturn(true)

        remoteFeatureFlag.toggleAction.toggle()

        verify(manualFeatureConfig).setManuallyEnabled(featureKey, true)
        assertTrue((uiStates.last().uiItems[0] as UiItem.FeatureFlag.RemoteFeatureFlag).enabled!!)
    }
}
