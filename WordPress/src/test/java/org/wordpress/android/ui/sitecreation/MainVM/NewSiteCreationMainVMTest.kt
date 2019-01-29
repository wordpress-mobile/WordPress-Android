package org.wordpress.android.ui.sitecreation.MainVM

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import android.os.Bundle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.sitecreation.NavigationTarget
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleEmpty
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleGeneral
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleStepCount
import org.wordpress.android.ui.sitecreation.NewSiteCreationStepsProvider
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.SiteCreationStep.DOMAINS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SEGMENTS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_INFO
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_PREVIEW
import org.wordpress.android.ui.sitecreation.SiteCreationStep.VERTICALS
import org.wordpress.android.ui.sitecreation.misc.NewSiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.CreateSiteState
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.CreateSiteState.SiteCreationCompleted

private const val LOCAL_SITE_ID = 1
private const val SEGMENT_ID = 1L
private const val VERTICAL_ID = "m1v1"
private const val SITE_TITLE = "test title"
private const val SITE_TAGLINE = "test tagline"
private const val DOMAIN = "test.domain.com"

private val SITE_CREATION_STEPS = listOf(SITE_INFO, VERTICALS, SEGMENTS, DOMAINS, SITE_PREVIEW)

@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationMainVMTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var tracker: NewSiteCreationTracker
    @Mock lateinit var stepsProvider: NewSiteCreationStepsProvider
    @Mock lateinit var navigationTargetObserver: Observer<NavigationTarget>
    @Mock lateinit var wizardFinishedObserver: Observer<CreateSiteState>
    @Mock lateinit var savedInstanceState: Bundle

    @Captor
    private val captorNavigationTarget: ArgumentCaptor<NavigationTarget>? = null

    private lateinit var viewModel: NewSiteCreationMainVM

    @Before
    fun setUp() {
        whenever(stepsProvider.getSteps()).thenReturn(SITE_CREATION_STEPS)
        viewModel = NewSiteCreationMainVM(stepsProvider, tracker)
        viewModel.start(null)
        viewModel.navigationTargetObservable.observeForever(navigationTargetObserver)
        viewModel.wizardFinishedObservable.observeForever(wizardFinishedObserver)
    }

    @Test
    fun skipClickedResultsInNextStep() {
        viewModel.onSkipClicked()
        verifyNavigatedToNextStep()
    }

    @Test
    fun segmentSelectedResultsInNextStep() {
        viewModel.onSegmentSelected(SEGMENT_ID)
        verifyNavigatedToNextStep()
    }

    @Test
    fun verticalSelectedResultsInNextStep() {
        viewModel.onVerticalsScreenFinished(VERTICAL_ID)
        verifyNavigatedToNextStep()
    }

    @Test
    fun siteInfoFinishedResultsInNextStep() {
        viewModel.onInfoScreenFinished(SITE_TITLE, null)
        verifyNavigatedToNextStep()
    }

    @Test
    fun domainSelectedResultsInNextStep() {
        viewModel.onDomainsScreenFinished(DOMAIN)
        verifyNavigatedToNextStep()
    }

    @Test
    fun siteCreationStateUpdatedWithSelectedSegment() {
        viewModel.onSegmentSelected(SEGMENT_ID)
        assertThat(viewModel.navigationTargetObservable.lastEvent!!.wizardState.segmentId).isEqualTo(SEGMENT_ID)
    }

    @Test
    fun siteCreationStateUpdatedWithSelectedVertical() {
        viewModel.onVerticalsScreenFinished(VERTICAL_ID)
        assertThat(viewModel.navigationTargetObservable.lastEvent!!.wizardState.verticalId).isEqualTo(VERTICAL_ID)
    }

    @Test
    fun siteCreationStateUpdatedWithSiteInfo() {
        viewModel.onInfoScreenFinished(SITE_TITLE, SITE_TAGLINE)
        assertThat(viewModel.navigationTargetObservable.lastEvent!!.wizardState.siteTitle).isEqualTo(SITE_TITLE)
        assertThat(viewModel.navigationTargetObservable.lastEvent!!.wizardState.siteTagLine).isEqualTo(SITE_TAGLINE)
    }

    @Test
    fun siteCreationStateUpdatedWithSelectedDomain() {
        viewModel.onDomainsScreenFinished(DOMAIN)
        assertThat(viewModel.navigationTargetObservable.lastEvent!!.wizardState.domain).isEqualTo(DOMAIN)
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
    fun nextStepAfterOnBackPressedIsCurrentStep() {
        val startingStep = currentStep()
        viewModel.onBackPressed() // navigate to previous step
        viewModel.onSkipClicked() // navigate to next step
        assertThat(currentStep()).isEqualTo(startingStep)
    }

    @Test
    fun backSupressedOnlyForLastStep() {
        while (!isAtLastStep()) {
            assertThat(viewModel.shouldSuppressBackPress()).isFalse()
            viewModel.onSkipClicked() // navigate to a next step
        }
        assertThat(viewModel.shouldSuppressBackPress()).isTrue()
    }

    @Test
    fun titleForFirstStepIsGeneralSiteCreation() {
        assertThat(viewModel.screenTitleForWizardStep(SITE_CREATION_STEPS.first()))
                .isInstanceOf(ScreenTitleGeneral::class.java)
    }

    @Test
    fun titleForLastStepIsEmptyTitle() {
        assertThat(viewModel.screenTitleForWizardStep(SITE_CREATION_STEPS.last()))
                .isInstanceOf(ScreenTitleEmpty::class.java)
    }

    @Test
    fun titlesForOtherThanFirstAndLastStepIsStepCount() {
        val isFirstStep = { index: Int -> index == 0 }
        val isLastStep = { index: Int -> index == SITE_CREATION_STEPS.size - 1 }

        SITE_CREATION_STEPS.filterIndexed({ index, step ->
            !isFirstStep(index) && !isLastStep(index)
        }).forEach() { step ->
            assertThat(viewModel.screenTitleForWizardStep(step)).isInstanceOf(ScreenTitleStepCount::class.java)
        }
    }

    @Test
    fun siteCreationStateWrittenToBundle() {
        viewModel.writeToBundle(savedInstanceState)
        verify(savedInstanceState).putParcelable(any(), argThat() { this is SiteCreationState })
    }

    @Test
    fun siteCreationStateRestored() {
        val expectedState = SiteCreationState()
        whenever(savedInstanceState.getParcelable<SiteCreationState>(any()))
                .thenReturn(expectedState)

        // we need to create a new instance of the VM as the `viewModel` has already been started in setUp()
        val newViewModel = NewSiteCreationMainVM(stepsProvider, tracker)
        newViewModel.start(savedInstanceState)

        /* we need to navigate to the next step as the value of navigationTargetObservable isn't changed when the VM
        is restored from a savedInstanceState. */
        newViewModel.onSkipClicked()

        newViewModel.navigationTargetObservable.observeForever(navigationTargetObserver)
        assertThat(newViewModel.navigationTargetObservable.lastEvent!!.wizardState).isSameAs(expectedState)
    }

    private fun verifyNavigatedToNextStep() {
        // onChange invoked after viewModel.start() and after viewModel.onSkipClicked()
        verify(navigationTargetObserver, times(2)).onChanged(captorNavigationTarget!!.capture())
    }

    private fun currentStep() = viewModel.navigationTargetObservable.lastEvent!!.wizardStep

    private fun isAtLastStep() = SITE_CREATION_STEPS.last().equals(currentStep())
}
