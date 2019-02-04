package org.wordpress.android.ui.sitecreation

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import android.os.Bundle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleEmpty
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleGeneral
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleStepCount
import org.wordpress.android.ui.sitecreation.misc.NewSiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.CreateSiteState
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.CreateSiteState.SiteCreationCompleted
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.viewmodel.SingleLiveEvent

private const val LOCAL_SITE_ID = 1
private const val SEGMENT_ID = 1L
private const val VERTICAL_ID = "m1v1"
private const val SITE_TITLE = "test title"
private const val SITE_TAG_LINE = "test tagLine"
private const val DOMAIN = "test.domain.com"
private const val STEP_COUNT = 20
private const val FIRST_STEP_INDEX = 1
private const val LAST_STEP_INDEX = STEP_COUNT

@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationMainVMTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var tracker: NewSiteCreationTracker
    @Mock lateinit var navigationTargetObserver: Observer<NavigationTarget>
    @Mock lateinit var wizardFinishedObserver: Observer<CreateSiteState>
    @Mock lateinit var savedInstanceState: Bundle
    @Mock lateinit var wizardManager: WizardManager<SiteCreationStep>
    @Mock lateinit var siteCreationStep: SiteCreationStep
    private val wizardManagerNavigatorLiveData = SingleLiveEvent<SiteCreationStep>()

    private lateinit var viewModel: NewSiteCreationMainVM

    @Before
    fun setUp() {
        whenever(wizardManager.navigatorLiveData).thenReturn(wizardManagerNavigatorLiveData)
        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = siteCreationStep
            Unit
        }
        viewModel = NewSiteCreationMainVM(tracker, wizardManager)
        viewModel.start(null)
        viewModel.navigationTargetObservable.observeForever(navigationTargetObserver)
        viewModel.wizardFinishedObservable.observeForever(wizardFinishedObserver)
        whenever(wizardManager.stepsCount).thenReturn(STEP_COUNT)
        // clear invocations since viewModel.start() calls wizardManager.showNextStep
        clearInvocations(wizardManager)
    }

    @Test
    fun skipClickedResultsInNextStep() {
        viewModel.onSkipClicked()
        verify(wizardManager).showNextStep()
    }

    @Test
    fun segmentSelectedResultsInNextStep() {
        viewModel.onSegmentSelected(SEGMENT_ID)
        verify(wizardManager).showNextStep()
    }

    @Test
    fun verticalSelectedResultsInNextStep() {
        viewModel.onVerticalsScreenFinished(VERTICAL_ID)
        verify(wizardManager).showNextStep()
    }

    @Test
    fun siteInfoFinishedResultsInNextStep() {
        viewModel.onInfoScreenFinished(SITE_TITLE, null)
        verify(wizardManager).showNextStep()
    }

    @Test
    fun domainSelectedResultsInNextStep() {
        viewModel.onDomainsScreenFinished(DOMAIN)
        verify(wizardManager).showNextStep()
    }

    @Test
    fun siteCreationStateUpdatedWithSelectedSegment() {
        viewModel.onSegmentSelected(SEGMENT_ID)
        assertThat(currentWizardState(viewModel).segmentId).isEqualTo(SEGMENT_ID)
    }

    @Test
    fun siteCreationStateUpdatedWithSelectedVertical() {
        viewModel.onVerticalsScreenFinished(VERTICAL_ID)
        assertThat(currentWizardState(viewModel).verticalId).isEqualTo(VERTICAL_ID)
    }

    @Test
    fun siteCreationStateUpdatedWithSiteInfo() {
        viewModel.onInfoScreenFinished(
                SITE_TITLE,
                SITE_TAG_LINE
        )
        assertThat(currentWizardState(viewModel).siteTitle).isEqualTo(SITE_TITLE)
        assertThat(currentWizardState(viewModel).siteTagLine).isEqualTo(SITE_TAG_LINE)
    }

    @Test
    fun siteCreationStateUpdatedWithSelectedDomain() {
        viewModel.onDomainsScreenFinished(DOMAIN)
        assertThat(currentWizardState(viewModel).domain).isEqualTo(DOMAIN)
    }

    @Test
    fun wizardFinishedInvokedOnSitePreviewCompleted() {
        val state = SiteCreationCompleted(LOCAL_SITE_ID)
        viewModel.onSitePreviewScreenFinished(state)

        val captor = ArgumentCaptor.forClass(CreateSiteState::class.java)
        verify(wizardFinishedObserver).onChanged(captor.capture())

        assertThat(captor.value).isEqualTo(state)
    }

    @Test
    fun onBackPressedPropagatedToWizardManager() {
        viewModel.onBackPressed()
        verify(wizardManager).onBackPressed()
    }

    @Test
    fun backNotSuppressedWhenNotLastStep() {
        whenever(wizardManager.isLastStep()).thenReturn(false)
        assertThat(viewModel.shouldSuppressBackPress()).isFalse()
    }

    @Test
    fun backSuppressedForLastStep() {
        whenever(wizardManager.isLastStep()).thenReturn(true)
        assertThat(viewModel.shouldSuppressBackPress()).isTrue()
    }

    @Test
    fun titleForFirstStepIsGeneralSiteCreation() {
        whenever(wizardManager.stepPosition(siteCreationStep)).thenReturn(FIRST_STEP_INDEX)
        assertThat(viewModel.screenTitleForWizardStep(siteCreationStep))
                .isInstanceOf(ScreenTitleGeneral::class.java)
    }

    @Test
    fun titleForLastStepIsEmptyTitle() {
        whenever(wizardManager.stepPosition(siteCreationStep)).thenReturn(LAST_STEP_INDEX)
        assertThat(viewModel.screenTitleForWizardStep(siteCreationStep))
                .isInstanceOf(ScreenTitleEmpty::class.java)
    }

    @Test
    fun titlesForOtherThanFirstAndLastStepIsStepCount() {
        (FIRST_STEP_INDEX + 1 until STEP_COUNT).forEach { stepIndex ->
            whenever(wizardManager.stepPosition(siteCreationStep)).thenReturn(stepIndex)

            assertThat(viewModel.screenTitleForWizardStep(siteCreationStep))
                    .isInstanceOf(ScreenTitleStepCount::class.java)
        }
    }

    @Test
    fun siteCreationStateWrittenToBundle() {
        viewModel.writeToBundle(savedInstanceState)
        verify(savedInstanceState).putParcelable(any(), argThat { this is SiteCreationState })
    }

    @Test
    fun siteCreationStateRestored() {
        val expectedState = SiteCreationState()
        whenever(savedInstanceState.getParcelable<SiteCreationState>("key_site_creation_state"))
                .thenReturn(expectedState)

        // we need to create a new instance of the VM as the `viewModel` has already been started in setUp()
        val newViewModel = NewSiteCreationMainVM(tracker, wizardManager)
        newViewModel.start(savedInstanceState)

        /* we need simulate navigation to the next step as wizardManager.showNextStep() isn't invoked
        when the VM is restored from a savedInstanceState. */
        wizardManagerNavigatorLiveData.value = siteCreationStep

        newViewModel.navigationTargetObservable.observeForever(navigationTargetObserver)
        assertThat(currentWizardState(newViewModel)).isSameAs(expectedState)
    }

    @Test
    fun siteCreationStepIndexRestored() {
        val index = 17
        whenever(savedInstanceState.getInt("key_current_step")).thenReturn(index)

        // siteCreationState is not nullable - we need to set it
        whenever(savedInstanceState.getParcelable<SiteCreationState>("key_site_creation_state"))
                .thenReturn(SiteCreationState())

        // we need to create a new instance of the VM as the `viewModel` has already been started in setUp()
        val newViewModel = NewSiteCreationMainVM(tracker, wizardManager)
        newViewModel.start(savedInstanceState)

        verify(wizardManager).setCurrentStepIndex(index)
    }

    private fun currentWizardState(vm: NewSiteCreationMainVM) =
            vm.navigationTargetObservable.lastEvent!!.wizardState
}
