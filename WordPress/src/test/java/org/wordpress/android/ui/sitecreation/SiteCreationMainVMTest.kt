package org.wordpress.android.ui.sitecreation

import android.os.Bundle
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.experiments.Variation.Control
import org.wordpress.android.fluxc.model.experiments.Variation.Treatment
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleEmpty
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleGeneral
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleStepCount
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.CreateSiteState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.CreateSiteState.SiteCreationCompleted
import org.wordpress.android.ui.sitecreation.usecases.FetchHomePageLayoutsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.config.SiteCreationDomainPurchasingFeatureConfig
import org.wordpress.android.util.experiments.SiteCreationDomainPurchasingExperiment
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.DialogHolder

private const val LOCAL_SITE_ID = 1
private const val SEGMENT_ID = 1L
private const val VERTICAL = "Test Vertical"
private const val DOMAIN = "test.domain.com"
private const val STEP_COUNT = 20
private const val FIRST_STEP_INDEX = 1
private const val LAST_STEP_INDEX = STEP_COUNT

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteCreationMainVMTest : BaseUnitTest() {
    @Mock
    lateinit var tracker: SiteCreationTracker

    @Mock
    lateinit var navigationTargetObserver: Observer<NavigationTarget>

    @Mock
    lateinit var wizardFinishedObserver: Observer<CreateSiteState>

    @Mock
    lateinit var wizardExitedObserver: Observer<Unit>

    @Mock
    lateinit var dialogActionsObserver: Observer<DialogHolder>

    @Mock
    lateinit var onBackPressedObserver: Observer<Unit>

    @Mock
    lateinit var savedInstanceState: Bundle

    @Mock
    lateinit var wizardManager: WizardManager<SiteCreationStep>

    @Mock
    lateinit var siteCreationStep: SiteCreationStep

    @Mock
    lateinit var networkUtils: NetworkUtilsWrapper

    @Mock
    lateinit var dispatcher: Dispatcher

    @Mock
    lateinit var fetchHomePageLayoutsUseCase: FetchHomePageLayoutsUseCase

    @Mock
    lateinit var imageManager: ImageManager

    @Mock
    lateinit var jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil

    @Mock
    lateinit var domainPurchasingExperiment: SiteCreationDomainPurchasingExperiment

    @Mock
    lateinit var purchasingFeatureConfig: SiteCreationDomainPurchasingFeatureConfig

    private val wizardManagerNavigatorLiveData = SingleLiveEvent<SiteCreationStep>()

    private lateinit var viewModel: SiteCreationMainVM

    @Before
    fun setUp() {
        whenever(wizardManager.navigatorLiveData).thenReturn(wizardManagerNavigatorLiveData)
        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = siteCreationStep
            Unit
        }
        viewModel = getNewViewModel()
        viewModel.start(null, SiteCreationSource.UNSPECIFIED)
        viewModel.navigationTargetObservable.observeForever(navigationTargetObserver)
        viewModel.wizardFinishedObservable.observeForever(wizardFinishedObserver)
        viewModel.dialogActionObservable.observeForever(dialogActionsObserver)
        viewModel.exitFlowObservable.observeForever(wizardExitedObserver)
        viewModel.onBackPressedObservable.observeForever(onBackPressedObserver)
        whenever(wizardManager.stepsCount).thenReturn(STEP_COUNT)
        // clear invocations since viewModel.start() calls wizardManager.showNextStep
        clearInvocations(wizardManager)
    }

    @Test
    fun domainSelectedResultsInNextStep() {
        viewModel.onDomainsScreenFinished(DOMAIN)
        verify(wizardManager).showNextStep()
    }

    @Test
    fun siteCreationStateUpdatedWithSelectedDomain() {
        viewModel.onDomainsScreenFinished(DOMAIN)
        assertThat(currentWizardState(viewModel).domain).isEqualTo(DOMAIN)
    }

    @Test
    fun wizardFinishedInvokedOnSitePreviewCompleted() {
        val state = SiteCreationCompleted(LOCAL_SITE_ID, false)
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
    fun onSiteIntentSkippedPropagatedToWizardManager() {
        viewModel.onSiteIntentSkipped()
        verify(wizardManager).showNextStep()
    }

    @Test
    fun onSiteIntentSelectedPropagatedToWizardManager() {
        viewModel.onSiteIntentSelected(VERTICAL)
        assertThat(currentWizardState(viewModel).siteIntent).isEqualTo(VERTICAL)
        verify(wizardManager).showNextStep()
    }

    @Test
    fun backNotSuppressedWhenNotLastStep() {
        whenever(wizardManager.isLastStep()).thenReturn(false)
        viewModel.onBackPressed()
        verify(onBackPressedObserver).onChanged(anyOrNull())
    }

    @Test
    fun backSuppressedWhenLastStep() {
        whenever(wizardManager.isLastStep()).thenReturn(true)
        viewModel.onBackPressed()
        verifyNoMoreInteractions(onBackPressedObserver)
    }

    @Test
    fun dialogShownOnBackPressedWhenLastStepAndSiteCreationNotCompleted() {
        whenever(wizardManager.isLastStep()).thenReturn(true)
        viewModel.onBackPressed()
        verify(dialogActionsObserver).onChanged(any())
    }

    @Test
    fun flowExitedOnBackPressedWhenLastStepAndSiteCreationCompleted() {
        whenever(wizardManager.isLastStep()).thenReturn(true)
        viewModel.onSiteCreationCompleted()
        viewModel.onBackPressed()
        verify(wizardExitedObserver).onChanged(anyOrNull())
    }

    @Test
    fun flowExitedOnDialogPositiveButtonClicked() {
        viewModel.onPositiveDialogButtonClicked(TAG_WARNING_DIALOG)
        verify(wizardExitedObserver).onChanged(anyOrNull())
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
    fun titleForDomainStepIsChooseADomain() {
        whenever(siteCreationStep.name).thenReturn(SiteCreationStep.DOMAINS.name)
        assertThat(viewModel.screenTitleForWizardStep(siteCreationStep))
            .isEqualTo(ScreenTitleGeneral(R.string.new_site_creation_domain_header_title))
    }

    @Test
    fun titlesForOtherThanFirstAndLastStepIsStepCount() {
        (FIRST_STEP_INDEX + 1 until LAST_STEP_INDEX).forEach { stepIndex ->
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
        /* we need to model a real use case of data only existing for steps the user has visited (Segment only in
        this case). Otherwise, subsequent steps' state will be cleared and make the test fail. (issue #10189)*/
        val expectedState = SiteCreationState(segmentId = SEGMENT_ID)
        whenever(savedInstanceState.getParcelable<SiteCreationState>(KEY_SITE_CREATION_STATE))
            .thenReturn(expectedState)

        // we need to create a new instance of the VM as the `viewModel` has already been started in setUp()
        val newViewModel = getNewViewModel()
        newViewModel.start(savedInstanceState, SiteCreationSource.UNSPECIFIED)

        /* we need to simulate navigation to the next step (Domain selection, see comment above) as
        wizardManager.showNextStep() isn't invoked when the VM is restored from a savedInstanceState. */
        wizardManagerNavigatorLiveData.value = SiteCreationStep.DOMAINS

        newViewModel.navigationTargetObservable.observeForever(navigationTargetObserver)
        assertThat(currentWizardState(newViewModel)).isSameAs(expectedState)
    }

    @Test
    fun siteCreationStepIndexRestored() {
        val index = 17
        whenever(savedInstanceState.getInt(KEY_CURRENT_STEP)).thenReturn(index)

        // siteCreationState is not nullable - we need to set it
        whenever(savedInstanceState.getParcelable<SiteCreationState>(KEY_SITE_CREATION_STATE))
            .thenReturn(SiteCreationState())

        // we need to create a new instance of the VM as the `viewModel` has already been started in setUp()
        val newViewModel = getNewViewModel()
        newViewModel.start(savedInstanceState, SiteCreationSource.UNSPECIFIED)

        verify(wizardManager).setCurrentStepIndex(index)
    }

    @Test
    fun `given null instance state, when start, then site creation accessed including source is tracked`() {
        val newViewModel = getNewViewModel()
        newViewModel.start(null, SiteCreationSource.UNSPECIFIED)

        // Because setup is run before every test, we expect this to be tracked twice
        // Once on the first instantiation
        // Once on the new start
        verify(tracker, atLeastOnce()).trackSiteCreationAccessed(SiteCreationSource.UNSPECIFIED)
    }

    @Test
    fun `given instance state is not null, when start, then site creation accessed is not tracked`() {
        val expectedState = SiteCreationState(segmentId = SEGMENT_ID)
        whenever(savedInstanceState.getParcelable<SiteCreationState>(KEY_SITE_CREATION_STATE))
            .thenReturn(expectedState)

        val newViewModel = getNewViewModel()
        newViewModel.start(savedInstanceState, SiteCreationSource.UNSPECIFIED)

        // Because setup is run before every test, we expect this to be tracked on that first instance only
        verify(tracker, times(1)).trackSiteCreationAccessed(SiteCreationSource.UNSPECIFIED)
    }

    @Test
    fun `given domain purchasing experiment off, when start, then experiment is not tracked`() {
        whenever(purchasingFeatureConfig.isEnabledState()).thenReturn(false)
        whenever(domainPurchasingExperiment.getVariation()).thenReturn(mock())

        getNewViewModel().start(null, SiteCreationSource.UNSPECIFIED)

        verify(tracker, never()).trackSiteCreationDomainPurchasingExperimentVariation(any())
    }
    @Test
    fun `given domain purchasing experiment on, when start in control variation, then experiment is tracked`() {
        whenever(purchasingFeatureConfig.isEnabledState()).thenReturn(true)
        whenever(domainPurchasingExperiment.getVariation()).thenReturn(Control)

        getNewViewModel().start(null, SiteCreationSource.UNSPECIFIED)

        verify(tracker).trackSiteCreationDomainPurchasingExperimentVariation(Control)
    }

    @Test
    fun `given domain purchasing experiment on, when start in treatment variation, then experiment is tracked`() {
        whenever(purchasingFeatureConfig.isEnabledState()).thenReturn(true)
        whenever(domainPurchasingExperiment.getVariation()).thenReturn(mock<Treatment>())

        getNewViewModel().start(null, SiteCreationSource.UNSPECIFIED)

        verify(tracker).trackSiteCreationDomainPurchasingExperimentVariation(isA<Treatment>())
    }

    private fun currentWizardState(vm: SiteCreationMainVM) =
        vm.navigationTargetObservable.lastEvent!!.wizardState

    private fun getNewViewModel() = SiteCreationMainVM(
        tracker,
        wizardManager,
        networkUtils,
        dispatcher,
        fetchHomePageLayoutsUseCase,
        imageManager,
        jetpackFeatureRemovalOverlayUtil,
        domainPurchasingExperiment,
        purchasingFeatureConfig,
    )
}
