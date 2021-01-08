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
    fun `when view model is started, process moves to next step`() {
        viewModel.start(null)

        verify(wizardManager).showNextStep()
    }

    @Test
    fun `when details is finished, process moves to next step`() {
        viewModel.start(null)
        // need to clear invocations because nextStep is called on start
        clearInvocations(wizardManager)

        viewModel.onBackupDownloadDetailsFinished(rewindId, downloadId, published)
        verify(wizardManager).showNextStep()
    }

    @Test
    fun `when progress is finished, process moves to next step`() {
        viewModel.start(null)
        clearInvocations(wizardManager)

        viewModel.onBackupDownloadProgressFinished(url)
        verify(wizardManager).showNextStep()
    }

    @Test
    fun `when details is finished, state is updated properly`() {
        val navigationTargets = initObservers().navigationTargets

        viewModel.start(null)
        clearInvocations(wizardManager)
        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = BackupDownloadStep.PROGRESS
            Unit
        }

        viewModel.onBackupDownloadDetailsFinished(rewindId, downloadId, published)

        assertThat(navigationTargets.last().wizardState.rewindId).isEqualTo(rewindId)
        assertThat(navigationTargets.last().wizardState.downloadId).isEqualTo(downloadId)
        assertThat(navigationTargets.last().wizardState.published).isEqualTo(published)
    }

    @Test
    fun `when progress is finished, state is updated properly`() {
        val navigationTargets = initObservers().navigationTargets

        viewModel.start(null)
        clearInvocations(wizardManager)
        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = BackupDownloadStep.COMPLETE
            Unit
        }

        viewModel.onBackupDownloadProgressFinished(url)

        assertThat(navigationTargets.last().wizardState.url).isEqualTo(url)
    }

    @Test
    fun `when onBackPressed while in details step, invokes wizard finished with cancel`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver

        viewModel.start(null)
        clearInvocations(wizardManager)

        whenever(wizardManager.currentStep).thenReturn(0)
        viewModel.onBackPressed()

        assertThat(wizardFinishedObserver.last()).isInstanceOf(BackupDownloadCanceled::class.java)
    }

    @Test
    fun `when onBackPressed while in progress step, invokes wizard finished with BackupDownloadInProgress`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver
        viewModel.start(null)
        clearInvocations(wizardManager)
        viewModel.onBackupDownloadDetailsFinished(rewindId, downloadId, published)

        whenever(wizardManager.currentStep).thenReturn(1)
        viewModel.onBackPressed()

        assertThat(wizardFinishedObserver.last()).isInstanceOf(BackupDownloadInProgress::class.java)
    }

    @Test
    fun `when onBackPressed while in complete step, invokes wizard finished with BackupDownloadCompleted`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver
        viewModel.start(null)
        clearInvocations(wizardManager)

        whenever(wizardManager.currentStep).thenReturn(2)
        viewModel.onBackPressed()

        assertThat(wizardFinishedObserver.last()).isInstanceOf(BackupDownloadCompleted::class.java)
    }

    @Test
    fun `when viewModel starts, toolbarState contains no entries`() {
        val toolbarStates = initObservers().toolbarState

        viewModel.start(savedInstanceState = null)

        assertThat(toolbarStates.size).isEqualTo(0)
    }

    @Test
    fun `backupDownloadState is writtenToBundle`() {
        viewModel.start(savedInstanceState = null)

        viewModel.writeToBundle(savedInstanceState)
        verify(savedInstanceState)
                .putParcelable(any(), argThat { this is BackupDownloadState })
    }

    @Test
    fun `when setToolbarState is invoked, toolbar state is updated`() {
        val toolbarStates = initObservers().toolbarState

        viewModel.start(null)
        viewModel.setToolbarState(DetailsToolbarState())
        assertThat(toolbarStates.last()).isInstanceOf(DetailsToolbarState::class.java)

        viewModel.setToolbarState(ProgressToolbarState())
        assertThat(toolbarStates.last()).isInstanceOf(ProgressToolbarState::class.java)

        viewModel.setToolbarState(CompleteToolbarState())
        assertThat(toolbarStates.last()).isInstanceOf(CompleteToolbarState::class.java)
    }

    @Test
    fun `step index is restored`() {
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
