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
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreCanceled
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.ToolbarState
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.ToolbarState.DetailsToolbarState
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

    private val restoreState = RestoreState(
            activityId = "activityId",
            rewindId = "rewindId",
            restoreId = 100L,
            siteId = 200L,
            url = null,
            published = Date(1609690147756)
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
    fun `given in details step, when onBackPressed, then invokes wizard finished with cancel`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver

        viewModel.start(null)
        Mockito.clearInvocations(wizardManager)

        whenever(wizardManager.currentStep).thenReturn(RestoreStep.DETAILS.id)
        viewModel.onBackPressed()

        Assertions.assertThat(wizardFinishedObserver.last()).isInstanceOf(RestoreCanceled::class.java)
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

        return Observers(toolbarStates, wizardFinishedObserver, navigationTargetObserver)
    }

    private data class Observers(
        val toolbarState: List<ToolbarState>,
        val wizardFinishedObserver: List<RestoreWizardState>,
        val navigationTargets: List<NavigationTarget>
    )
}
