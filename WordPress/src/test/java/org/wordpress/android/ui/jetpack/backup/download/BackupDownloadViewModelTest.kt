package org.wordpress.android.ui.jetpack.backup.download

import android.os.Bundle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadCanceled
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadCompleted
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadInProgress
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState.CompleteToolbarState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState.DetailsToolbarState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState.ErrorToolbarState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState.ProgressToolbarState
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.viewmodel.SingleLiveEvent
import java.util.Date

@InternalCoroutinesApi
class BackupDownloadViewModelTest : BaseUnitTest() {
    @Mock lateinit var wizardManager: WizardManager<BackupDownloadStep>
    @Mock lateinit var backupDownloadStep: BackupDownloadStep
    @Mock lateinit var savedInstanceState: Bundle
    private val wizardManagerNavigatorLiveData = SingleLiveEvent<BackupDownloadStep>()

    private lateinit var viewModel: BackupDownloadViewModel

    private val rewindId = "rewindId"
    private val downloadId = 100L
    private val published = Date(1609690147756)
    private val url = "url"

    private val backupDownloadState = BackupDownloadState(
            activityId = "activityId",
            rewindId = "rewindId",
            downloadId = 100L,
            siteId = 200L,
            url = null,
            published = Date(1609690147756)
    )

    @Before
    fun setUp() {
        whenever(wizardManager.navigatorLiveData).thenReturn(wizardManagerNavigatorLiveData)
        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = backupDownloadStep
            Unit
        }
        viewModel = BackupDownloadViewModel(wizardManager)
    }

    @Test
    fun `given view model, when started, then process moves to next step`() {
        viewModel.start(null)

        verify(wizardManager).showNextStep()
    }

    @Test
    fun `given in details step, when finished, then process moves to next step`() {
        viewModel.start(null)
        // need to clear invocations because nextStep is called on start
        clearInvocations(wizardManager)

        viewModel.onBackupDownloadDetailsFinished(rewindId, downloadId, published)
        verify(wizardManager).showNextStep()
    }

    @Test
    fun `given in progress step, when finished, then process moves to next step`() {
        viewModel.start(null)
        clearInvocations(wizardManager)

        viewModel.onBackupDownloadProgressFinished(url)
        verify(wizardManager).showNextStep()
    }

    @Test
    fun `given in details step, when finished, state is updated properly`() {
        val navigationTargets = initObservers().navigationTargets

        viewModel.start(null)
        clearInvocations(wizardManager)
        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = BackupDownloadStep.PROGRESS
            Unit
        }

        viewModel.onBackupDownloadDetailsFinished(rewindId, downloadId, published)

        assertThat(navigationTargets.last().wizardState)
                .isEqualTo(BackupDownloadState(rewindId = rewindId, downloadId = downloadId, published = published))
    }

    @Test
    fun `given in progress step, when finished, state is updated properly`() {
        val navigationTargets = initObservers().navigationTargets

        viewModel.start(null)
        clearInvocations(wizardManager)
        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = BackupDownloadStep.COMPLETE
            Unit
        }

        viewModel.onBackupDownloadProgressFinished(url)

        assertThat(navigationTargets.last().wizardState).isEqualTo(BackupDownloadState(url = url))
    }

    @Test
    fun `given in details step, when onBackPressed, then invokes wizard finished with cancel`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver

        viewModel.start(null)
        clearInvocations(wizardManager)

        whenever(wizardManager.currentStep).thenReturn(BackupDownloadStep.DETAILS.id)
        viewModel.onBackPressed()

        assertThat(wizardFinishedObserver.last()).isInstanceOf(BackupDownloadCanceled::class.java)
    }

    @Test
    fun `given in progress step, when onBackPressed, then invokes wizard finished with BackupDownloadInProgress`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver
        viewModel.start(null)
        clearInvocations(wizardManager)
        viewModel.onBackupDownloadDetailsFinished(rewindId, downloadId, published)

        whenever(wizardManager.currentStep).thenReturn(BackupDownloadStep.PROGRESS.id)
        viewModel.onBackPressed()

        assertThat(wizardFinishedObserver.last()).isInstanceOf(BackupDownloadInProgress::class.java)
    }

    @Test
    fun `given in complete step, when onBackPressed, then invokes wizard finished with BackupDownloadCompleted`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver
        viewModel.start(null)
        clearInvocations(wizardManager)

        whenever(wizardManager.currentStep).thenReturn(BackupDownloadStep.COMPLETE.id)
        viewModel.onBackPressed()

        assertThat(wizardFinishedObserver.last()).isInstanceOf(BackupDownloadCompleted::class.java)
    }

    @Test
    fun `given viewModel, when starts, toolbarState contains no entries`() {
        val toolbarStates = initObservers().toolbarState

        viewModel.start(null)

        assertThat(toolbarStates.size).isEqualTo(0)
    }

    @Test
    fun `given backupDownloadState, when writeToBundle is invoked, state is writtenToBundle`() {
        viewModel.start(null)

        viewModel.writeToBundle(savedInstanceState)

        verify(savedInstanceState)
                .putParcelable(any(), argThat { this is BackupDownloadState })
    }

    @Test
    fun `given in detail step, when setToolbarState is invoked, then toolbar state is updated`() {
        val toolbarStates = initObservers().toolbarState

        viewModel.start(null)

        viewModel.setToolbarState(DetailsToolbarState())

        assertThat(toolbarStates.last()).isInstanceOf(DetailsToolbarState::class.java)
    }

    @Test
    fun `given in progress step, when setToolbarState is invoked, then toolbar state is updated`() {
        val toolbarStates = initObservers().toolbarState

        viewModel.start(null)

        viewModel.setToolbarState(ProgressToolbarState())

        assertThat(toolbarStates.last()).isInstanceOf(ProgressToolbarState::class.java)
    }

    @Test
    fun `given in complete error step, when setToolbarState is invoked, then toolbar state is updated`() {
        val toolbarStates = initObservers().toolbarState
        viewModel.start(null)

        viewModel.setToolbarState(ErrorToolbarState())

        assertThat(toolbarStates.last()).isInstanceOf(ErrorToolbarState::class.java)
    }

    @Test
    fun `given in complete step, when setToolbarState is invoked, then toolbar state is updated`() {
        val toolbarStates = initObservers().toolbarState

        viewModel.start(null)

        viewModel.setToolbarState(CompleteToolbarState())

        assertThat(toolbarStates.last()).isInstanceOf(CompleteToolbarState::class.java)
    }

    @Test
    fun `given step index, when returned from background, then step index is restored`() {
        val index = 2

        whenever(savedInstanceState.getInt(KEY_BACKUP_DOWNLOAD_CURRENT_STEP)).thenReturn(index)
        whenever(savedInstanceState.getParcelable<BackupDownloadState>(KEY_BACKUP_DOWNLOAD_STATE))
                .thenReturn(backupDownloadState)

        viewModel.start(savedInstanceState = savedInstanceState)

        verify(wizardManager).setCurrentStepIndex(index)
    }

    private fun initObservers(): Observers {
        val toolbarStates = mutableListOf<ToolbarState>()
        viewModel.toolbarStateObservable.observeForever { toolbarStates.add(it) }

        val wizardFinishedObserver = mutableListOf<BackupDownloadWizardState>()
        viewModel.wizardFinishedObservable.observeForever { wizardFinishedObserver.add(it.peekContent()) }

        val navigationTargetObserver = mutableListOf<NavigationTarget>()
        viewModel.navigationTargetObservable.observeForever { navigationTargetObserver.add(it) }

        return Observers(toolbarStates, wizardFinishedObserver, navigationTargetObserver)
    }

    private data class Observers(
        val toolbarState: List<ToolbarState>,
        val wizardFinishedObserver: List<BackupDownloadWizardState>,
        val navigationTargets: List<NavigationTarget>
    )
}
