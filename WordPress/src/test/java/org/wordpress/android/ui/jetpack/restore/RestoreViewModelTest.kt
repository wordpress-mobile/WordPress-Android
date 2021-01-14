package org.wordpress.android.ui.jetpack.restore

import android.os.Bundle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.CONTENTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.MEDIA_UPLOADS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.PLUGINS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.ROOTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.SQLS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.THEMES
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreCanceled
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.ToolbarState
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.ToolbarState.DetailsToolbarState
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.ToolbarState.WarningToolbarState
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.viewmodel.SingleLiveEvent
import java.util.Date

@InternalCoroutinesApi
class RestoreViewModelTest : BaseUnitTest() {
    @Mock lateinit var wizardManager: WizardManager<RestoreStep>
    @Mock lateinit var restoreStep: RestoreStep
    @Mock lateinit var savedInstanceState: Bundle
    private val wizardManagerNavigatorLiveData = SingleLiveEvent<RestoreStep>()

    private lateinit var viewModel: RestoreViewModel

    private val rewindId = "rewindId"
    private val restoreId = 100L
    private val published = Date(1609690147756)
    private val url = "url"

    private val restoreState = RestoreState(
            activityId = "activityId",
            rewindId = "rewindId",
            restoreId = 100L,
            siteId = 200L,
            url = null,
            published = Date(1609690147756)
    )

    private val optionsSelected = listOf(
            Pair(THEMES.id, true),
            Pair(PLUGINS.id, true),
            Pair(MEDIA_UPLOADS.id, true),
            Pair(SQLS.id, true),
            Pair(ROOTS.id, true),
            Pair(CONTENTS.id, true)
    )

    @Before
    fun setUp() {
        whenever(wizardManager.navigatorLiveData).thenReturn(wizardManagerNavigatorLiveData)
        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = restoreStep
            Unit
        }
        viewModel = RestoreViewModel(wizardManager)
    }

    @Test
    fun `given view model, when started, then process moves to next step`() {
        viewModel.start(null)

        Mockito.verify(wizardManager).showNextStep()
    }

    @Test
    fun `given in details step, when finished, then process moves to next step`() {
        viewModel.start(null)
        // need to clear invocations because nextStep is called on start
        Mockito.clearInvocations(wizardManager)

        viewModel.onRestoreDetailsFinished(rewindId, optionsSelected, published)
        Mockito.verify(wizardManager).showNextStep()
    }

    @Test
    fun `given in details step, when finished, state is updated properly`() {
        val navigationTargets = initObservers().navigationTargets

        viewModel.start(null)
        Mockito.clearInvocations(wizardManager)
        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = RestoreStep.WARNING
            Unit
        }

        viewModel.onRestoreDetailsFinished(rewindId, optionsSelected, published)

        Assertions.assertThat(navigationTargets.last().wizardState)
                .isEqualTo(RestoreState(
                        rewindId = rewindId,
                        optionsSelected = optionsSelected,
                        published = published))
    }

    @Test
    fun `given in details step, when onBackPressed, then invokes wizard finished with cancel`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver

        viewModel.start(null)
        Mockito.clearInvocations(wizardManager)

        whenever(wizardManager.currentStep).thenReturn(RestoreStep.DETAILS.id)
        viewModel.onBackPressed()

        Assertions.assertThat(wizardFinishedObserver.last()).isInstanceOf(RestoreCanceled::class.java)
    }

    @Test
    fun `given in warning step, when onBackPressed, then process moves to previous step `() {
        val onBackPressedObserver = initObservers().onBackPressedObserver
        viewModel.start(null)
        Mockito.clearInvocations(wizardManager)

        whenever(wizardManager.currentStep).thenReturn(RestoreStep.WARNING.id)
        viewModel.onBackPressed()

        Assertions.assertThat(onBackPressedObserver.last()).isInstanceOf(Unit::class.java)
    }

    @Test
    fun `given viewModel, when starts, toolbarState contains no entries`() {
        val toolbarStates = initObservers().toolbarState

        viewModel.start(null)

        Assertions.assertThat(toolbarStates.size).isEqualTo(0)
    }

    @Test
    fun `given restoreState, when writeToBundle is invoked, state is writtenToBundle`() {
        viewModel.start(null)

        viewModel.writeToBundle(savedInstanceState)

        Mockito.verify(savedInstanceState)
                .putParcelable(any(), argThat { this is RestoreState })
    }

    @Test
    fun `given in detail step, when setToolbarState is invoked, then toolbar state is updated`() {
        val toolbarStates = initObservers().toolbarState

        viewModel.start(null)

        viewModel.setToolbarState(DetailsToolbarState())

        Assertions.assertThat(toolbarStates.last()).isInstanceOf(DetailsToolbarState::class.java)
    }

    @Test
    fun `given in warning step, when setToolbarState is invoked, then toolbar state is updated`() {
        val toolbarStates = initObservers().toolbarState

        viewModel.start(null)

        viewModel.setToolbarState(WarningToolbarState())

        Assertions.assertThat(toolbarStates.last()).isInstanceOf(WarningToolbarState::class.java)
    }

    @Test
    fun `given step index, when returned from background, then step index is restored`() {
        val index = 2

        whenever(savedInstanceState.getInt(KEY_RESTORE_CURRENT_STEP)).thenReturn(index)
        whenever(savedInstanceState.getParcelable<RestoreState>(KEY_RESTORE_STATE))
                .thenReturn(restoreState)

        viewModel.start(savedInstanceState = savedInstanceState)

        Mockito.verify(wizardManager).setCurrentStepIndex(index)
    }

    private fun initObservers(): Observers {
        val toolbarStates = mutableListOf<ToolbarState>()
        viewModel.toolbarStateObservable.observeForever { toolbarStates.add(it) }

        val wizardFinishedObserver = mutableListOf<RestoreWizardState>()
        viewModel.wizardFinishedObservable.observeForever { wizardFinishedObserver.add(it.peekContent()) }

        val navigationTargetObserver = mutableListOf<NavigationTarget>()
        viewModel.navigationTargetObservable.observeForever { navigationTargetObserver.add(it) }

        val onBackPressedObserver = mutableListOf<Unit>()
        viewModel.onBackPressedObservable.observeForever { onBackPressedObserver.add(it) }

        return Observers(toolbarStates, wizardFinishedObserver, navigationTargetObserver, onBackPressedObserver)
    }

    private data class Observers(
        val toolbarState: List<ToolbarState>,
        val wizardFinishedObserver: List<RestoreWizardState>,
        val navigationTargets: List<NavigationTarget>,
        val onBackPressedObserver: List<Unit>
    )
}
